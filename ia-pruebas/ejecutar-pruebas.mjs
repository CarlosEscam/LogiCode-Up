// Pruebas de aceptación de la calificación con IA (Etapa 6). Ejecuta cada muestra por el flujo real de LogiCode
// (actividad, rúbrica, entrega de un estudiante, cola, Ollama + Qwen2.5-VL) y registra lo que pide la especificación:
// lectura de símbolos, coincidencia con la nota docente, confianza, legibilidad, latencia y RAM máxima de Ollama.
// No inventa nada: si una muestra no termina, queda registrada como tal. Ver ia-pruebas/README.md.
//
// Uso (desde la raíz del repositorio, con `docker compose --profile ia up` y los datos de demostración cargados):
//   node ia-pruebas/ejecutar-pruebas.mjs ia-pruebas/muestras/muestras.json
// Variables opcionales: LOGICODE_URL (http://localhost:3000), IA_DOCENTE / IA_ESTUDIANTE (cédulas de demostración),
// IA_CLAVE (contraseña de esas cuentas), IA_TOLERANCIA (0.5), IA_ESPERA_MAX_MIN (30).

import { execFileSync } from 'node:child_process'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { basename, dirname, extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const BASE = (process.env.LOGICODE_URL ?? 'http://localhost:3000').replace(/\/$/, '')
const API = `${BASE}/api/v1`
const TEACHER = process.env.IA_DOCENTE ?? '1000000101'
const STUDENT = process.env.IA_ESTUDIANTE ?? '1000000206'
const PASSWORD = process.env.IA_CLAVE ?? 'Demo#2026'
const TOLERANCE = Number(process.env.IA_TOLERANCIA ?? '0.5')
const MAX_WAIT_MS = Number(process.env.IA_ESPERA_MAX_MIN ?? '30') * 60e3
const COURSE_NAME = 'Pruebas de aceptación IA'
const AMBIGUOUS = new Set('0O1lI=<>-+*/()"\',;:'.split(''))

const sleep = (ms) => new Promise((done) => setTimeout(done, ms))

// ---------- Cliente HTTP con cookies y CSRF (igual al de datos-demo) ----------

class Session {
  cookies = new Map()

  async request(method, path, { json, form } = {}) {
    const headers = { Accept: 'application/json' }
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = await this.csrf()
    headers.Cookie = [...this.cookies].map(([name, value]) => `${name}=${value}`).join('; ')
    let body
    if (json !== undefined) {
      headers['Content-Type'] = 'application/json'
      body = JSON.stringify(json)
    } else if (form) {
      body = form
    }
    const response = await fetch(`${API}${path}`, { method, headers, body, redirect: 'manual' })
    this.store(response)
    const text = await response.text()
    const data = text ? JSON.parse(text) : null
    if (response.status >= 400) throw new Error(`${method} ${path} → ${response.status}: ${data?.detail ?? data?.title ?? text}`)
    return data
  }

  async csrf() {
    if (!this.cookies.has('XSRF-TOKEN')) {
      const cookie = [...this.cookies].map(([name, value]) => `${name}=${value}`).join('; ')
      this.store(await fetch(`${API}/auth/csrf`, { headers: { Cookie: cookie } }))
    }
    return this.cookies.get('XSRF-TOKEN') ?? ''
  }

  store(response) {
    for (const cookie of response.headers.getSetCookie()) {
      const [pair] = cookie.split(';')
      const index = pair.indexOf('=')
      const name = pair.slice(0, index)
      const value = pair.slice(index + 1)
      if (value === '' || /max-age=0/i.test(cookie)) this.cookies.delete(name)
      else this.cookies.set(name, value)
    }
  }

  static async login(cedula) {
    const session = new Session()
    await session.request('POST', '/auth/login', { json: { cedula, password: PASSWORD } })
    session.cookies.delete('XSRF-TOKEN')
    return session
  }
}

// ---------- Entorno: versión de Ollama, modelo y memoria del contenedor ----------

function compose(...args) {
  try {
    return execFileSync('docker', ['compose', ...args], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
  } catch {
    return null
  }
}

function environment(model) {
  let host = null
  try {
    host = JSON.parse(execFileSync('docker', ['info', '--format', '{{json .}}'], { encoding: 'utf8' }))
  } catch {
    // Sin acceso a Docker solo se registran los datos de la API.
  }
  return {
    ollama: compose('exec', '-T', 'ollama', 'ollama', '--version'),
    model: compose('exec', '-T', 'ollama', 'ollama', 'show', model),
    cpus: host?.NCPU ?? null,
    memoryGb: host?.MemTotal ? (host.MemTotal / 1024 ** 3).toFixed(1) : null,
  }
}

function toMiB(text) {
  const match = /^([\d.]+)\s*([KMG]i?B|B)/i.exec(text.trim())
  if (!match) return null
  const unit = match[2].toUpperCase().replace('I', 'i')
  const factor = { B: 1 / 1024 ** 2, KB: 1 / 1024, KiB: 1 / 1024, MB: 1, MiB: 1, GB: 1024, GiB: 1024 }[unit] ?? 1
  return Number(match[1]) * factor
}

function memoryNow(container) {
  if (!container) return null
  try {
    const out = execFileSync('docker', ['stats', '--no-stream', '--format', '{{.MemUsage}}', container], { encoding: 'utf8' })
    return toMiB(out.split('/')[0])
  } catch {
    return null
  }
}

// ---------- Métricas ----------

const normalize = (text) => (text ?? '').replace(/\s+/g, ' ').trim()

// Distancia de edición con alineación, para la exactitud general y la de símbolos ambiguos (0/O, 1/l, =, <...).
function readingAccuracy(expected, actual) {
  const a = normalize(expected)
  const b = normalize(actual)
  if (!a) return { characters: null, symbols: null }
  const rows = a.length + 1
  const cols = b.length + 1
  const d = new Uint32Array(rows * cols)
  for (let i = 0; i < rows; i++) d[i * cols] = i
  for (let j = 0; j < cols; j++) d[j] = j
  for (let i = 1; i < rows; i++) {
    for (let j = 1; j < cols; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      d[i * cols + j] = Math.min(d[(i - 1) * cols + j] + 1, d[i * cols + j - 1] + 1, d[(i - 1) * cols + j - 1] + cost)
    }
  }
  let i = a.length
  let j = b.length
  let symbols = 0
  let symbolsOk = 0
  while (i > 0) {
    const here = d[i * cols + j]
    if (j > 0 && here === d[(i - 1) * cols + j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1)) {
      if (AMBIGUOUS.has(a[i - 1])) {
        symbols++
        if (a[i - 1] === b[j - 1]) symbolsOk++
      }
      i--
      j--
    } else if (here === d[(i - 1) * cols + j] + 1) {
      if (AMBIGUOUS.has(a[i - 1])) symbols++
      i--
    } else {
      j--
    }
  }
  return {
    characters: Math.max(0, 1 - d[a.length * cols + b.length] / a.length),
    symbols: symbols ? symbolsOk / symbols : null,
  }
}

function teacherScore(sample, activity) {
  if (!sample.puntosDocente) return null
  const earned = sample.puntosDocente.reduce((sum, value) => sum + value, 0)
  const total = activity.criterios.reduce((sum, criterion) => sum + criterion.puntos, 0)
  return Math.round((earned * 5 * 10) / total) / 10
}

// ---------- Ejecución ----------

async function ensureCourse(teacher) {
  const page = await teacher.request('GET', '/courses?size=100')
  const existing = (page.items ?? page).find((course) => course.name === COURSE_NAME)
  if (existing) return existing
  const course = await teacher.request('POST', '/courses', { json: { name: COURSE_NAME, period: new Date().getFullYear() + '-pruebas' } })
  await teacher.request('POST', `/courses/${course.id}/enrollments`, { json: { cedula: STUDENT } }).catch(() => {})
  return course
}

async function runSample(teacher, student, course, sample, activity, folder, container) {
  const now = Date.now()
  const created = await teacher.request('POST', `/courses/${course.id}/activities`, {
    json: {
      title: `[${sample.id}] ${activity.titulo}`.slice(0, 150),
      kind: 'TASK',
      statement: activity.enunciado,
      acceptsText: false,
      acceptsFiles: false,
      acceptsPhotos: true,
      fileGroups: [],
      maxAttempts: 1,
      opensAt: new Date(now - 60e3).toISOString(),
      closesAt: new Date(now + 7 * 24 * 3600e3).toISOString(),
    },
  })
  await teacher.request('PUT', `/activities/${created.id}/rubric`, {
    json: { expectedAnswer: activity.respuestaEsperada, criteria: activity.criterios.map((c) => ({ name: c.nombre, maxPoints: c.puntos, description: c.descripcion ?? null })) },
  })
  await teacher.request('POST', `/activities/${created.id}/publish`)

  const path = resolve(folder, sample.archivo)
  const type = extname(path).toLowerCase() === '.png' ? 'image/png' : 'image/jpeg'
  const form = new FormData()
  form.append('files', new Blob([readFileSync(path)], { type }), `muestra${extname(path).toLowerCase()}`)
  const started = Date.now()
  const submission = await student.request('POST', `/activities/${created.id}/submissions`, { form })

  let maxMemory = memoryNow(container)
  let detail
  while (Date.now() - started < MAX_WAIT_MS) {
    await sleep(5000)
    const memory = memoryNow(container)
    if (memory != null) maxMemory = Math.max(maxMemory ?? 0, memory)
    detail = await teacher.request('GET', `/submissions/${submission.id}/grade`)
    if (detail.grade.status !== 'PROCESSING') break
  }
  return { detail, wallMs: Date.now() - started, maxMemory }
}

function row(sample, activity, run) {
  const evaluation = run.detail?.aiEvaluations?.[0]
  const expected = teacherScore(sample, activity)
  const proposed = evaluation?.proposedScore ?? null
  const difference = expected != null && proposed != null ? Math.round(Math.abs(proposed - expected) * 10) / 10 : null
  const automatic = run.detail?.grade.status === 'PUBLISHED' && run.detail.grade.source === 'AI'
  const reading = sample.transcripcion && evaluation?.transcription != null ? readingAccuracy(sample.transcripcion, evaluation?.transcription) : { characters: null, symbols: null }
  return {
    id: sample.id,
    condicion: sample.condicion ?? '',
    estado: run.detail?.grade.status === 'PROCESSING' ? 'SIN_TERMINAR' : (evaluation?.outcome ?? run.detail?.grade.reviewReason ?? ''),
    legibleEsperado: sample.legibleEsperado,
    legibleReportado: evaluation?.legible ?? null,
    confianza: evaluation?.confidence ?? null,
    notaDocente: expected,
    notaIA: proposed,
    diferencia: difference,
    publicadaAutomaticamente: automatic,
    falsoAutomatico: automatic && difference != null && difference > TOLERANCE,
    lecturaCaracteres: reading.characters,
    lecturaSimbolos: reading.symbols,
    duracionModeloS: evaluation ? Math.round(evaluation.durationMs / 100) / 10 : null,
    duracionTotalS: Math.round(run.wallMs / 100) / 10,
    ramMaxMiB: run.maxMemory != null ? Math.round(run.maxMemory) : null,
    error: evaluation?.error ?? '',
    transcripcionIA: evaluation?.transcription ?? '',
  }
}

const percent = (value) => (value == null ? '—' : `${(value * 100).toFixed(1)} %`)
const average = (values) => (values.length ? values.reduce((a, b) => a + b, 0) / values.length : null)

function summary(rows, env, model, file) {
  const done = rows.filter((r) => r.estado !== 'SIN_TERMINAR' && r.estado !== 'FAILED')
  const legibility = done.filter((r) => r.legibleReportado != null)
  const graded = done.filter((r) => r.diferencia != null)
  const auto = rows.filter((r) => r.publicadaAutomaticamente)
  const durations = rows.map((r) => r.duracionModeloS).filter((v) => v != null).sort((a, b) => a - b)
  const bucket = (filter) => {
    const items = graded.filter(filter)
    return items.length ? `${items.filter((r) => r.diferencia <= TOLERANCE).length} de ${items.length}` : '—'
  }
  return `# Resultados de las pruebas de aceptación de IA

- Fecha: ${new Date().toLocaleString('es-CO', { timeZone: 'America/Bogota' })} (hora de Colombia)
- Muestras: \`${file}\` (${rows.length})
- Modelo configurado: \`${model}\`
- Ollama: ${env.ollama ?? 'no disponible'}
- Equipo (según Docker): ${env.cpus ?? '?'} CPU, ${env.memoryGb ?? '?'} GB de RAM
- Tolerancia para considerar que la nota coincide: ±${String(TOLERANCE).replace('.', ',')}

## Resumen

| Medida | Resultado |
| --- | --- |
| Muestras evaluadas sin fallo técnico | ${done.length} de ${rows.length} |
| Fallos técnicos o sin terminar | ${rows.length - done.length} |
| Legibilidad detectada correctamente | ${legibility.length ? `${legibility.filter((r) => r.legibleReportado === r.legibleEsperado).length} de ${legibility.length}` : '—'} |
| Exactitud de lectura (caracteres, promedio) | ${percent(average(done.map((r) => r.lecturaCaracteres).filter((v) => v != null)))} |
| Exactitud de símbolos ambiguos (promedio) | ${percent(average(done.map((r) => r.lecturaSimbolos).filter((v) => v != null)))} |
| Nota dentro de la tolerancia | ${graded.length ? `${graded.filter((r) => r.diferencia <= TOLERANCE).length} de ${graded.length}` : '—'} |
| Diferencia media con la nota docente | ${graded.length ? average(graded.map((r) => r.diferencia)).toFixed(2).replace('.', ',') : '—'} |
| Publicadas automáticamente (confianza ≥ 80 %) | ${auto.length} |
| Falsos automáticos (publicadas fuera de la tolerancia) | ${auto.filter((r) => r.falsoAutomatico).length} |
| Coincidencia con confianza ≥ 80 % | ${bucket((r) => r.confianza >= 80)} |
| Coincidencia con confianza < 80 % | ${bucket((r) => r.confianza < 80)} |
| Latencia del modelo (mediana / máxima) | ${durations.length ? `${durations[Math.floor(durations.length / 2)]} s / ${durations.at(-1)} s` : '—'} |
| RAM máxima del contenedor de Ollama | ${Math.max(0, ...rows.map((r) => r.ramMaxMiB ?? 0)) || '—'} MiB |

## Por muestra

| Muestra | Condición | Resultado | Legible (esperado/IA) | Confianza | Nota docente | Nota IA | Dif. | Lectura | Símbolos | Tiempo | RAM máx. |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
${rows
  .map((r) =>
    [r.id, r.condicion, r.estado, `${r.legibleEsperado ? 'sí' : 'no'} / ${r.legibleReportado == null ? '—' : r.legibleReportado ? 'sí' : 'no'}`, r.confianza == null ? '—' : `${r.confianza} %`, r.notaDocente ?? '—', r.notaIA ?? '—', r.diferencia ?? '—', percent(r.lecturaCaracteres), percent(r.lecturaSimbolos), r.duracionModeloS == null ? '—' : `${r.duracionModeloS} s`, r.ramMaxMiB == null ? '—' : `${r.ramMaxMiB} MiB`]
      .join(' | ')
      .replace(/^/, '| ')
      .concat(' |'),
  )
  .join('\n')}

## Detalle del modelo

\`\`\`
${env.model ?? 'No se pudo consultar `ollama show` (¿el servicio ollama está corriendo?).'}
\`\`\`
`
}

function csv(rows) {
  const columns = Object.keys(rows[0])
  const cell = (value) => {
    const text = value == null ? '' : typeof value === 'number' ? String(value) : String(value)
    return /[",\n;]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }
  return [columns.join(','), ...rows.map((r) => columns.map((c) => cell(r[c])).join(','))].join('\n') + '\n'
}

async function main() {
  const file = process.argv[2]
  if (!file) throw new Error('Indica el archivo de muestras, por ejemplo ia-pruebas/muestras/muestras.json')
  const spec = JSON.parse(readFileSync(file, 'utf8'))
  const folder = dirname(resolve(file))
  const teacher = await Session.login(TEACHER)
  const student = await Session.login(STUDENT)
  const course = await ensureCourse(teacher)

  const container = compose('ps', '-q', 'ollama')
  console.log(`LogiCode · pruebas de aceptación de IA → ${BASE} (${spec.muestras.length} muestras)`)
  if (!container) console.log('No se encontró el contenedor ollama de este proyecto: no se medirá la RAM.')
  // Una entrega de calentamiento no se mide: la primera carga del modelo en memoria tarda más.
  let model = process.env.IA_MODELO
  if (!process.env.IA_SIN_CALENTAR) {
    console.log('Calentando el modelo con la primera muestra (no se mide)…')
    const sample0 = spec.muestras[0]
    const warm = await runSample(teacher, student, course, { ...sample0, id: `${sample0.id}-calentamiento` }, spec.actividades[sample0.actividad], folder, container)
    model ??= warm.detail?.aiEvaluations?.[0]?.model
  }

  const rows = []
  for (const sample of spec.muestras) {
    const activity = spec.actividades[sample.actividad]
    if (!activity) throw new Error(`La muestra ${sample.id} usa la actividad "${sample.actividad}", que no está definida`)
    process.stdout.write(`  ${sample.id} (${basename(sample.archivo)})… `)
    const run = await runSample(teacher, student, course, sample, activity, folder, container)
    model ??= run.detail?.aiEvaluations?.[0]?.model
    const result = row(sample, activity, run)
    rows.push(result)
    console.log(`${result.estado} · confianza ${result.confianza ?? '—'} · nota ${result.notaIA ?? '—'} (docente ${result.notaDocente ?? '—'}) · ${result.duracionModeloS ?? '—'} s`)
  }

  model ??= 'qwen2.5vl:7b'
  const env = environment(model)
  const stamp = new Date().toISOString().slice(0, 16).replace(/[-:T]/g, '')
  const out = join(dirname(fileURLToPath(import.meta.url)), 'resultados', stamp)
  mkdirSync(out, { recursive: true })
  writeFileSync(join(out, 'resultados.csv'), csv(rows))
  writeFileSync(join(out, 'resumen.md'), summary(rows, env, model, file))
  console.log(`\nResultados en ${out}`)
}

main().catch((error) => {
  console.error(`\n✗ ${error.message}`)
  process.exit(1)
})
