// Despliegue de LogiCode en el equipo servidor con verificación y vuelta atrás automática (guía 11, T-104).
// Es entrega continua, no despliegue continuo: una persona decide cuándo correrlo (después de `git pull` del bundle).
//
// Uso, en la carpeta del proyecto en el equipo servidor:
//   node ci/desplegar.mjs                    construye y despliega lo que hay en la carpeta
//   node ci/desplegar.mjs --sin-construir    usa las imágenes ya construidas (logicode-backend, logicode-frontend)
//
// Pasos: 1) anota versión, esquema e imágenes actuales; 2) respaldo inmediato; 3) construye y levanta backend e
// interfaz esperando su verificación de salud; 4) comprueba /api/v1/system/info por la interfaz; 5) si algo falla,
// vuelve a las imágenes anteriores y, si la base ya se migró, restaura el respaldo del paso 2. Detalle en ci/README.md.
import { spawnSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const URL_BASE = (process.env.LOGICODE_URL || 'http://localhost:3000').replace(/\/$/, '')
const ESPERA = process.env.LOGICODE_ESPERA_SALUD || '240' // segundos para que backend e interfaz queden sanos
const construir = !process.argv.includes('--sin-construir')
const t0 = Date.now()
const hora = () => `${((Date.now() - t0) / 1000).toFixed(1).padStart(6)} s`
const log = (m) => console.log(`[${hora()}] ${m}`)

function run(args, { quiet = false } = {}) {
  if (!quiet) log(`$ docker ${args.join(' ')}`)
  const r = spawnSync('docker', args, { cwd: ROOT, encoding: 'utf8', stdio: quiet ? 'pipe' : ['inherit', 'pipe', 'inherit'] })
  if (!quiet && r.stdout) process.stdout.write(r.stdout)
  return { ok: r.status === 0, out: (r.stdout || '').trim() }
}
const compose = (...a) => run(['compose', ...a])

async function info() {
  try {
    const r = await fetch(`${URL_BASE}/api/v1/system/info`, { signal: AbortSignal.timeout(10_000) })
    return r.ok ? await r.json() : null
  } catch {
    return null
  }
}

function imagenActual(servicio) {
  const id = run(['compose', 'ps', '-q', servicio], { quiet: true }).out
  if (!id) return null
  const [nombre, imagen] = run(['inspect', '-f', '{{.Config.Image}} {{.Image}}', id], { quiet: true }).out.split(' ')
  return { nombre, imagen }
}

const esperada = JSON.parse(readFileSync(resolve(ROOT, 'frontend/package.json'), 'utf8')).version
const antes = await info()
const imagenes = { backend: imagenActual('backend'), frontend: imagenActual('frontend') }
if (!antes || !imagenes.backend || !imagenes.frontend) {
  log(`LogiCode no está corriendo bien en ${URL_BASE} (o falta el backend o la interfaz). Este script actualiza una`)
  log('instalación que ya funciona; para la primera vez usa `docker compose up -d --build --wait` (docs/instalacion.md).')
  process.exit(2)
}
log(`Antes: versión ${antes.version}, esquema ${antes.schemaVersion}. Se va a desplegar la ${esperada}.`)

// 2. Respaldo inmediato: es lo único que permite volver atrás si la versión nueva migra la base (T-96).
const respaldo = compose('run', '--rm', '--no-deps', '-T', 'backup', 'crear')
const copia = /Listo: (logicode-[0-9-]+)/.exec(respaldo.out)?.[1]
if (!respaldo.ok || !copia) {
  log('No se pudo hacer el respaldo: no se despliega nada.')
  process.exit(1)
}
log(`Respaldo listo: ${copia}`)

// 3 y 4. Versión nueva y verificación.
async function verificar(version) {
  const ahora = await info()
  if (!ahora) return 'no responde /api/v1/system/info'
  if (version && ahora.version !== version) return `responde la versión ${ahora.version}, no la ${version}`
  return null
}
let falla = null
// --progress plain: en Windows, sin consola propia, el progreso interactivo falla con "failed to get console".
if (construir && !compose('--progress', 'plain', 'build', 'backend', 'frontend').ok) falla = 'no se pudieron construir las imágenes'
if (!falla && !compose('up', '-d', '--no-build', '--wait', '--wait-timeout', ESPERA, 'backend', 'frontend').ok) {
  falla = 'el backend o la interfaz no quedaron sanos a tiempo'
}
if (!falla) falla = await verificar(esperada)
const despues = await info()
if (!falla) {
  log(`Desplegado: versión ${despues.version}, esquema ${despues.schemaVersion}. Copia previa: ${copia}.`)
  process.exit(0)
}

// 5. Vuelta atrás.
log(`FALLÓ: ${falla}. Se vuelve a la versión ${antes.version}.`)
compose('logs', '--tail', '40', 'backend', 'frontend')
const esquemaNuevo = await esquemaEnBase()
const restaurar = esquemaNuevo !== null && String(esquemaNuevo) !== String(antes.schemaVersion)
for (const [servicio, { nombre, imagen }] of Object.entries(imagenes)) run(['image', 'tag', imagen, nombre])
if (restaurar) {
  log(`La base quedó en el esquema ${esquemaNuevo} y la versión anterior usa el ${antes.schemaVersion}: se restaura ${copia}.`)
  compose('stop', 'backend', 'minio')
  if (!compose('run', '--rm', '--no-deps', '-T', 'backup', 'restaurar', copia).ok) {
    log('La restauración falló. Seguir docs/runbooks/Runbook_backend.md, sección 1, paso 6.')
    process.exit(3)
  }
  compose('start', 'minio')
}
compose('up', '-d', '--no-build', '--wait', '--wait-timeout', ESPERA, 'backend', 'frontend')
const error = await verificar(antes.version)
if (error) {
  log(`La vuelta atrás tampoco quedó bien (${error}). Seguir docs/runbooks/Runbook_backend.md.`)
  process.exit(3)
}
const final = await info()
log(`Vuelta atrás lista: versión ${final.version}, esquema ${final.schemaVersion}${restaurar ? `, base restaurada de ${copia}` : ''}.`)
process.exit(1)

// Versión del esquema leída directo de la base: con la versión nueva caída no se le puede preguntar al backend.
async function esquemaEnBase() {
  const r = run(['compose', 'exec', '-T', 'db', 'sh', '-c',
    'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "select max(version::int) from flyway_schema_history where success"'], { quiet: true })
  return r.ok && r.out ? Number(r.out) : null
}
