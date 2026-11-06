// CI de LogiCode en este equipo (guías 9 y 12, T-102). Corre las mismas etapas y puertas que
// .github/workflows/ci.yml y ci-security.yml, que solo llaman a este archivo: lo que pasa aquí es lo que pasaría en
// GitHub. Cada etapa corre en una imagen de Docker fija, así no hace falta instalar Java, Maven, Semgrep ni Trivy.
//
// Uso (en la carpeta del proyecto, con Docker encendido):
//   node ci/ci-local.mjs            todas las etapas
//   node ci/ci-local.mjs lint       solo una: lint | backend | frontend | seguridad
// Termina con código 1 si alguna puerta falla. Detalle en ci/README.md.
import { spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const RESULTADOS = join(ROOT, 'ci', 'resultados')
const IMG = {
  node: 'node:22-alpine',
  maven: 'maven:3.9-eclipse-temurin-21',
  semgrep: 'semgrep/semgrep:1.184.0',
  trivy: 'aquasec/trivy:0.69.3',
}
// Argumentos extra para cada `docker run` (por ejemplo un proxy de la red). Vacío en un PC normal.
const EXTRA = (process.env.LOGICODE_CI_DOCKER_ARGS || '').split(/\s+/).filter(Boolean)
const IMAGENES = (process.env.LOGICODE_CI_IMAGENES || 'logicode-backend logicode-frontend').split(/\s+/).filter(Boolean)
const SOCKET = '/var/run/docker.sock:/var/run/docker.sock'

function sh(cmd, args, opts = {}) {
  console.log(`\n$ ${cmd} ${args.join(' ')}`)
  return spawnSync(cmd, args, { stdio: 'inherit', cwd: ROOT, ...opts }).status === 0
}
const docker = (image, mounts, workdir, command, more = []) =>
  sh('docker', ['run', '--rm', ...EXTRA, ...more, ...mounts.flatMap((m) => ['-v', m]), '-w', workdir, image, ...command])

const nodeEnFrontend = (script) =>
  docker(IMG.node, [`${ROOT}:/w`, 'logicode-ci-node:/w/frontend/node_modules'], '/w/frontend',
    ['sh', '-c', `npm ci --no-audit --no-fund && ${script}`])

const etapas = {
  lint() {
    const compose = (archivos, env = {}) => sh('docker', ['compose', '--env-file', '.env.example',
      ...archivos.flatMap((f) => ['-f', f]), 'config', '--quiet'], { env: { ...process.env, ...env } })
    const piloto = { LOGICODE_PUBLIC_URL: 'https://ejemplo.invalid' }
    return nodeEnFrontend('npm run lint -- --deny-warnings')
      && compose(['docker-compose.yml'])
      && compose(['docker-compose.yml', 'docker-compose.piloto.yml'], piloto)
      && compose(['docker-compose.yml', 'docker-compose.brevo.yml'])
  },

  // Pruebas unitarias, de arquitectura (ArchUnit) y de integración con PostgreSQL real (Testcontainers), y la puerta
  // de cobertura de JaCoCo (backend/pom.xml, logicode.cobertura.minima).
  backend() {
    // Se monta todo el proyecto: la prueba de contrato compara con docs/api/openapi.json.
    return docker(IMG.maven, [`${ROOT}:/w`, 'logicode-ci-m2:/root/.m2', SOCKET], '/w/backend',
      ['mvn', '-B', '-ntp', 'verify'],
      ['-e', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal', '--add-host', 'host.docker.internal:host-gateway'])
  },

  frontend() {
    return nodeEnFrontend('npm test && npm run build')
  },

  // SAST con Semgrep (falla con hallazgos de severidad ERROR) y SCA, secretos y configuración con Trivy (falla con
  // HIGH o CRITICAL que no estén justificados en .trivyignore.yaml), sobre el código y sobre las dos imágenes.
  seguridad() {
    mkdirSync(RESULTADOS, { recursive: true })
    const reglas = (process.env.LOGICODE_SEMGREP_REGLAS || 'p/java p/javascript p/secrets p/dockerfile p/docker-compose')
      .split(/\s+/).flatMap((r) => ['--config', r])
    const okSemgrep = docker(IMG.semgrep, [`${ROOT}:/src`, ...(process.env.LOGICODE_SEMGREP_MONTAR ? [process.env.LOGICODE_SEMGREP_MONTAR] : [])], '/src',
      ['semgrep', 'scan', '--metrics=off', '--disable-version-check', ...reglas,
        '--exclude=*.test.*', '--exclude=**/test/**', '--json-output=ci/resultados/semgrep.json',
        'backend/src/main', 'frontend/src', 'backend/Dockerfile', 'frontend/Dockerfile', 'frontend/nginx',
        'docker-compose.yml', 'docker-compose.piloto.yml', 'respaldos'])
    const hallazgos = okSemgrep ? JSON.parse(readFileSync(join(RESULTADOS, 'semgrep.json'), 'utf8')).results : []
    const porSeveridad = Object.groupBy(hallazgos, (h) => h.extra.severity)
    for (const h of hallazgos) console.log(`  ${h.extra.severity.padEnd(7)} ${h.path}:${h.start.line}  ${h.check_id.split('.').pop()}`)
    const errores = porSeveridad.ERROR?.length ?? 0
    console.log(`Semgrep: ${hallazgos.length} hallazgos (${Object.entries(porSeveridad).map(([s, l]) => `${s} ${l.length}`).join(', ') || 'ninguno'}); la puerta pide 0 ERROR.`)

    const trivy = (args, more = []) => docker(IMG.trivy, [`${ROOT}:/src`, 'logicode-ci-trivy:/root/.cache/trivy', ...more], '/src',
      [...args, '--severity', 'HIGH,CRITICAL', '--exit-code', '1', '--ignorefile', '/src/.trivyignore.yaml'])
    const okFs = trivy(['fs', '--scanners', 'vuln,secret,misconfig', '--skip-dirs', 'frontend/node_modules',
      '--skip-dirs', 'backend/target', '--skip-dirs', 'ci/resultados', '.'])
    const faltan = IMAGENES.filter((i) => !sh('docker', ['image', 'inspect', '--format', '{{.Id}}', i]))
    if (faltan.length && !sh('docker', ['compose', '--env-file', '.env.example', 'build', 'backend', 'frontend'])) return false
    const okImagenes = IMAGENES.map((i) => trivy(['image', '--scanners', 'vuln,secret', i], [SOCKET])).every(Boolean)
    return okSemgrep && errores === 0 && okFs && okImagenes
  },
}

const pedidas = process.argv.slice(2).length ? process.argv.slice(2) : Object.keys(etapas)
const desconocidas = pedidas.filter((e) => !etapas[e])
if (desconocidas.length) {
  console.error(`Etapa desconocida: ${desconocidas.join(', ')}. Usa: ${Object.keys(etapas).join(' | ')}`)
  process.exit(2)
}
if (!existsSync(join(ROOT, '.env.example'))) throw new Error('Corre esto desde una copia completa del proyecto.')

const resumen = []
for (const e of pedidas) {
  const inicio = Date.now()
  console.log(`\n=== Etapa ${e} ===`)
  const ok = etapas[e]()
  resumen.push({ e, ok, s: Math.round((Date.now() - inicio) / 1000) })
  if (!ok) break // como en GitHub: si una etapa falla, las siguientes no corren
}
console.log('\n=== Resumen ===')
for (const r of resumen) console.log(`${r.ok ? 'OK   ' : 'FALLA'} ${r.e} (${r.s} s)`)
for (const e of pedidas.slice(resumen.length)) console.log(`---   ${e} (no corrió)`)
process.exit(resumen.every((r) => r.ok) && resumen.length === pedidas.length ? 0 : 1)
