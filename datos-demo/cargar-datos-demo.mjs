// Carga los datos de demostración y prueba de LogiCode por la API, como lo haría una persona:
// pasa por las mismas reglas de negocio y permisos que la aplicación. Ver datos-demo/README.md.
//
// Uso (con la aplicación corriendo):
//   node datos-demo/cargar-datos-demo.mjs
// Variables opcionales: LOGICODE_URL (por defecto http://localhost:3000),
// LOGICODE_ADMIN_CEDULA y LOGICODE_ADMIN_PASSWORD (si no están, se piden al iniciar),
// LOGICODE_DEMO_EN_PILOTO=si para cargar en una instalación en modo piloto (ver la Etapa 10 del README).
//
// Cada etapa agrega su sección al final de main(). Requiere Node 20 o superior.

import { readFileSync } from 'node:fs'
import { stdin, stdout } from 'node:process'
import readline from 'node:readline'
import zlib from 'node:zlib'

const BASE = (process.env.LOGICODE_URL ?? 'http://localhost:3000').replace(/\/$/, '')
const API = `${BASE}/api/v1`

// Solo para cuentas de demostración. Nunca la uses en una cuenta real.
export const DEMO_PASSWORD = 'Demo#2026'

const HOUR = 3600e3
const DAY = 24 * HOUR
const at = (offsetMs) => new Date(Date.now() + offsetMs).toISOString()

// ---------- Personas de demostración (cédulas y correos ficticios) ----------

const TEACHER = { cedula: '1000000101', fullName: 'Laura Méndez Ríos', email: 'laura.mendez@demo.logicode.test' }
const PENDING_TEACHER = { cedula: '1000000102', fullName: 'Jorge Pabón Silva', email: 'jorge.pabon@demo.logicode.test' }
const ORDINARY_ADMIN = { cedula: '1000000103', fullName: 'Diana Rojas Castro', email: 'diana.rojas@demo.logicode.test' }
const STUDENTS = {
  ana: { cedula: '1000000201', fullName: 'Ana Sofía Torres', email: 'ana.torres@demo.logicode.test' },
  bruno: { cedula: '1000000202', fullName: 'Bruno Díaz León', email: 'bruno.diaz@demo.logicode.test' },
  camila: { cedula: '1000000203', fullName: 'Camila Peña Gómez', email: 'camila.pena@demo.logicode.test' },
  david: { cedula: '1000000204', fullName: 'David Suárez Mora', email: 'david.suarez@demo.logicode.test' },
  elena: { cedula: '1000000205', fullName: 'Elena Vargas Ruiz', email: 'elena.vargas@demo.logicode.test' },
  felipe: { cedula: '1000000206', fullName: 'Felipe Ortiz Cano', email: 'felipe.ortiz@demo.logicode.test' },
}
// Inscrita en el curso pero aún sin registrarse.
const UNREGISTERED = { cedula: '1000000207', fullName: 'Gabriela Ruiz Peña' }
// Habilitado y registrado, pero no inscrito en el curso: solo ve lo público.
const OUTSIDER = { cedula: '1000000208', fullName: 'Hugo Lara Medina', email: 'hugo.lara@demo.logicode.test' }

// ---------- Cliente HTTP con cookies y CSRF ----------

class Session {
  cookies = new Map()

  async request(method, path, { json, form, expect } = {}) {
    const headers = { Accept: 'application/json' }
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = await this.csrf()
    headers.Cookie = this.cookieHeader()
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
    if (expect ? !expect.includes(response.status) : response.status >= 400) {
      const error = new Error(`${method} ${path} → ${response.status}: ${data?.detail ?? data?.title ?? text}`)
      error.status = response.status
      throw error
    }
    return data
  }

  get(path, options) {
    return this.request('GET', path, options)
  }
  post(path, json, options) {
    return this.request('POST', path, { json, ...options })
  }
  put(path, json) {
    return this.request('PUT', path, { json })
  }
  patch(path, json) {
    return this.request('PATCH', path, { json })
  }
  postForm(path, form) {
    return this.request('POST', path, { form })
  }

  async csrf() {
    if (!this.cookies.has('XSRF-TOKEN')) {
      const response = await fetch(`${API}/auth/csrf`, { headers: { Cookie: this.cookieHeader() } })
      this.store(response)
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

  cookieHeader() {
    return [...this.cookies].map(([name, value]) => `${name}=${value}`).join('; ')
  }

  static async login(cedula, password) {
    const session = new Session()
    await session.post('/auth/login', { cedula, password })
    // Spring Security cambia el token CSRF al iniciar sesión.
    session.cookies.delete('XSRF-TOKEN')
    return session
  }
}

// ---------- Archivos de ejemplo generados aquí (sin depender de archivos externos) ----------

function pdf(title, lines) {
  const latin1 = (text) => text.replace(/[\\()]/g, (c) => `\\${c}`)
  const content = [
    'BT /F1 20 Tf 60 760 Td',
    `(${latin1(title)}) Tj`,
    '/F1 12 Tf 0 -34 Td 16 TL',
    ...lines.map((line) => `(${latin1(line)}) '`),
    'ET',
  ].join('\n')
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>',
    `<< /Length ${Buffer.byteLength(content, 'latin1')} >>\nstream\n${content}\nendstream`,
  ]
  let out = '%PDF-1.4\n'
  const offsets = []
  objects.forEach((object, index) => {
    offsets.push(Buffer.byteLength(out, 'latin1'))
    out += `${index + 1} 0 obj\n${object}\nendobj\n`
  })
  const xref = Buffer.byteLength(out, 'latin1')
  out += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  out += offsets.map((offset) => `${String(offset).padStart(10, '0')} 00000 n \n`).join('')
  out += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  return Buffer.from(out, 'latin1')
}

const CRC_TABLE = Array.from({ length: 256 }, (_, n) => {
  let c = n
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
  return c >>> 0
})
function crc32(buffer) {
  let c = 0xffffffff
  for (const byte of buffer) c = CRC_TABLE[(c ^ byte) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}
function chunk(type, data) {
  const length = Buffer.alloc(4)
  length.writeUInt32BE(data.length)
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data])
  const crc = Buffer.alloc(4)
  crc.writeUInt32BE(crc32(body))
  return Buffer.concat([length, body, crc])
}
// Imagen PNG simple: bloques de colores que simulan un diagrama (o la foto de una hoja).
function png(width, height, paint) {
  const rows = []
  for (let y = 0; y < height; y++) {
    const row = Buffer.alloc(1 + width * 3)
    for (let x = 0; x < width; x++) row.set(paint(x, y), 1 + x * 3)
    rows.push(row)
  }
  const header = Buffer.alloc(13)
  header.writeUInt32BE(width, 0)
  header.writeUInt32BE(height, 4)
  header.set([8, 2, 0, 0, 0], 8)
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header),
    chunk('IDAT', zlib.deflateSync(Buffer.concat(rows))),
    chunk('IEND', Buffer.alloc(0)),
  ])
}
const diagramPng = () =>
  png(360, 240, (x, y) => {
    const inBox = (bx, by) => x >= bx && x < bx + 90 && y >= by && y < by + 40
    if (inBox(135, 20) || inBox(135, 100) || inBox(135, 180)) return [37, 99, 235]
    if (x >= 178 && x < 182 && ((y >= 60 && y < 100) || (y >= 140 && y < 180))) return [55, 65, 81]
    return [248, 250, 252]
  })
const notebookPhotoPng = () =>
  png(300, 400, (x, y) => {
    if (y % 24 === 0) return [147, 197, 253]
    if (x > 30 && x < 34) return [252, 165, 165]
    if (y > 40 && y < 330 && y % 24 > 8 && y % 24 < 16 && x > 45 && x < 60 + ((y * 7) % 190)) return [30, 41, 59]
    return [255, 253, 245]
  })

function fileForm(fields, files = []) {
  const form = new FormData()
  for (const [name, value] of Object.entries(fields)) if (value != null) form.append(name, value)
  for (const { field, name, type, content } of files) form.append(field, new Blob([content], { type }), name)
  return form
}

// ---------- Utilidades ----------

function log(message) {
  console.log(`  ✓ ${message}`)
}

async function ask(question, { hidden = false } = {}) {
  const rl = readline.createInterface({ input: stdin, output: stdout, terminal: true })
  if (hidden) {
    rl._writeToOutput = (text) => {
      if (text.includes(question)) stdout.write(text)
      else stdout.write('*'.repeat(text.length > 2 ? 0 : text.length))
    }
  }
  const answer = await new Promise((resolve) => rl.question(question, resolve))
  rl.close()
  if (hidden) stdout.write('\n')
  return answer.trim()
}

async function register(path, person) {
  const anonymous = new Session()
  await anonymous.post(`/auth/${path}`, { ...person, password: DEMO_PASSWORD })
}

async function userId(admin, cedula) {
  const page = await admin.get(`/admin/users?q=${cedula}&size=5`)
  const user = page.items.find((item) => item.cedula === cedula)
  if (!user) throw new Error(`No se encontró la cuenta ${cedula}`)
  return user.id
}

// ---------- Etapa 3 · Acceso y permisos ----------

async function stage3(admin) {
  console.log('Etapa 3 · cuentas y permisos')
  await register('teacher-requests', TEACHER)
  await admin.post(`/admin/teachers/${await userId(admin, TEACHER.cedula)}/approve`)
  log(`Docente aprobada: ${TEACHER.fullName} (${TEACHER.cedula})`)
  await register('teacher-requests', PENDING_TEACHER)
  log(`Solicitud de docente pendiente: ${PENDING_TEACHER.fullName}`)
  return Session.login(TEACHER.cedula, DEMO_PASSWORD)
}

// Cuentas que necesitan el curso creado antes (la inscripción también habilita la cédula).
async function stage3Students(admin, teacher) {
  for (const cedula of [ORDINARY_ADMIN.cedula, OUTSIDER.cedula]) {
    await teacher.post('/student-enablements', { cedula })
  }
  for (const student of [...Object.values(STUDENTS), OUTSIDER, ORDINARY_ADMIN]) {
    await register('student-registrations', student)
  }
  log(`${Object.keys(STUDENTS).length} estudiantes inscritos y registrados; ${UNREGISTERED.fullName} inscrita sin registrarse`)
  log(`${OUTSIDER.fullName}: habilitado y registrado, fuera del curso`)
  await admin.patch(`/admin/users/${await userId(admin, ORDINARY_ADMIN.cedula)}/role`, { role: 'ADMIN' })
  log(`Administradora ordinaria: ${ORDINARY_ADMIN.fullName}`)
  const students = {}
  for (const [key, student] of Object.entries(STUDENTS)) students[key] = await Session.login(student.cedula, DEMO_PASSWORD)
  return students
}

// ---------- Etapa 4 · Curso, temas y biblioteca ----------

async function stage4(teacher) {
  console.log('Etapa 4 · curso, temas y biblioteca')
  const course = await teacher.post('/courses', {
    name: 'Pensamiento Computacional · Grupo A',
    period: '2026-2',
    description: 'Lógica de programación con PSeInt, DFD, Scratch y Arduino. Curso de demostración.',
  })
  for (const person of [...Object.values(STUDENTS), UNREGISTERED]) {
    await teacher.post(`/courses/${course.id}/enrollments`, { cedula: person.cedula, fullName: person.fullName })
  }
  log(`Curso "${course.name}" con ${Object.keys(STUDENTS).length + 1} inscritos`)

  const topics = {}
  for (const [key, title, tool, description] of [
    ['pseint', 'Algoritmos y variables en PSeInt', 'PSEINT', 'Entrada, proceso y salida; tipos de datos y asignación.'],
    ['dfd', 'Diagramas de flujo con DFD', 'DFD', 'Símbolos, decisiones y ciclos en diagramas.'],
    ['scratch', 'Primeros bloques en Scratch', 'SCRATCH', 'Secuencias, eventos y ciclos con bloques.'],
    ['arduino', 'Sensores y actuadores con Arduino', 'ARDUINO', 'Lectura de sensores y control de un LED.'],
  ]) {
    topics[key] = await teacher.post(`/courses/${course.id}/topics`, { title, tool, description })
  }
  await teacher.put(`/courses/${course.id}/next-class`, { topicId: topics.pseint.id })
  log('4 temas; próxima clase: Algoritmos y variables en PSeInt')

  const material = (topic, fields, file) =>
    teacher.postForm(`/topics/${topic.id}/materials`, fileForm(fields, file ? [{ field: 'file', ...file }] : []))
  await material(
    topics.pseint,
    { title: 'Guía 1: variables y asignación', kind: 'GUIDE', visibility: 'PUBLIC', description: 'Guía pública para la biblioteca.' },
    {
      name: 'guia-variables.pdf',
      type: 'application/pdf',
      content: pdf('Guía 1: variables y asignación', [
        'Una variable guarda un dato que puede cambiar durante el algoritmo.',
        'En PSeInt se asigna con <- : edad <- 18',
        'Tipos: entero, real, caracter y lógico.',
        'Ejercicio: lea dos números y muestre su suma.',
      ]),
    },
  )
  await material(
    topics.pseint,
    { title: 'Ejemplo: promedio de tres notas', kind: 'TOOL_FILE', visibility: 'PRIVATE' },
    {
      name: 'promedio.psc',
      type: 'text/plain',
      content: 'Algoritmo Promedio\n\tLeer n1, n2, n3\n\tprom <- (n1 + n2 + n3) / 3\n\tEscribir "Promedio: ", prom\nFinAlgoritmo\n',
    },
  )
  await material(topics.pseint, { title: 'Descargar PSeInt', kind: 'LINK', visibility: 'PUBLIC', url: 'https://pseint.sourceforge.net/' })
  await material(
    topics.dfd,
    { title: 'Diagrama de la clase: decisión simple', kind: 'IMAGE', visibility: 'PRIVATE' },
    { name: 'diagrama-decision.png', type: 'image/png', content: diagramPng() },
  )
  await material(topics.scratch, { title: 'Editor de Scratch en línea', kind: 'LINK', visibility: 'PUBLIC', url: 'https://scratch.mit.edu/projects/editor/' })
  await material(
    topics.arduino,
    { title: 'Código: encender un LED', kind: 'TOOL_FILE', visibility: 'PRIVATE' },
    {
      name: 'led.ino',
      type: 'text/plain',
      content: 'void setup() {\n  pinMode(13, OUTPUT);\n}\n\nvoid loop() {\n  digitalWrite(13, HIGH);\n  delay(500);\n  digitalWrite(13, LOW);\n  delay(500);\n}\n',
    },
  )
  log('6 materiales: 3 públicos en la biblioteca (guía PDF y 2 enlaces) y 3 privados del curso')
  return course
}

// ---------- Etapa 5 · Actividades y entregas ----------

async function stage5(teacher, students, course) {
  console.log('Etapa 5 · actividades, entregas y notas')
  const defaults = { acceptsText: true, acceptsFiles: false, acceptsPhotos: false, fileGroups: [], maxAttempts: 1 }
  const activity = (fields) => teacher.post(`/courses/${course.id}/activities`, { ...defaults, ...fields })
  const rubric = (activityId, expectedAnswer, criteria) =>
    teacher.put(`/activities/${activityId}/rubric`, {
      expectedAnswer,
      criteria: criteria.map(([name, maxPoints, description]) => ({ name, maxPoints, description })),
    })
  const submit = (student, activityId, text, files = []) =>
    student.postForm(`/activities/${activityId}/submissions`, fileForm({ text }, files.map((file) => ({ field: 'files', ...file }))))
  const grade = (submission, score, feedback) => teacher.post(`/submissions/${submission.id}/grade`, { score, feedback })

  // 1. Tarea cerrada: calificada, una corrección de nota, una pendiente, dos vencidas y una reapertura.
  const promedio = {
    ...defaults,
    title: 'Tarea 1: algoritmo del promedio',
    kind: 'TASK',
    statement:
      'Escribe en PSeInt un algoritmo que lea tres notas de 0 a 5 y muestre su promedio.\nPuedes escribir el pseudocódigo o adjuntar el PDF exportado.',
    acceptsFiles: true,
    fileGroups: ['DOCUMENTS'],
    weightPercent: 30,
    opensAt: at(-HOUR),
    closesAt: at(DAY),
  }
  const a1 = await activity(promedio)
  await rubric(a1.id, 'Algoritmo Promedio: Leer n1, n2, n3; prom <- (n1 + n2 + n3) / 3; Escribir prom; FinAlgoritmo', [
    ['Lee las tres notas', 1.5, 'Usa Leer con tres variables'],
    ['Calcula el promedio correctamente', 2, 'Suma y divide entre 3'],
    ['Muestra el resultado', 1.5, null],
  ])
  await teacher.post(`/activities/${a1.id}/publish`)
  const anaA1 = await submit(students.ana, a1.id, 'Adjunto mi algoritmo exportado.', [
    {
      name: 'promedio-ana.pdf',
      type: 'application/pdf',
      content: pdf('Algoritmo Promedio · Ana Sofía Torres', ['Leer n1, n2, n3', 'prom <- (n1 + n2 + n3) / 3', 'Escribir "Promedio: ", prom']),
    },
  ])
  const brunoA1 = await submit(students.bruno, a1.id, 'Leer n1, n2, n3\nprom <- n1 + n2 + n3 / 3\nEscribir prom')
  const camilaA1 = await submit(students.camila, a1.id, 'Algoritmo Promedio\n  Leer n1, n2, n3\n  prom <- (n1 + n2 + n3) / 3\n  Escribir "El promedio es ", prom\nFinAlgoritmo')
  await submit(students.david, a1.id, 'Leer a, b, c\nEscribir (a+b+c)/3')
  await grade(anaA1, 4.5, 'Muy bien planteado.')
  await grade(anaA1, 4.2, 'Corrección: falta validar que las notas estén entre 0 y 5.')
  await grade(brunoA1, 3.0, 'Revisa la precedencia: la división se hace antes que la suma; usa paréntesis.')
  await grade(camilaA1, 5.0, 'Excelente, completo y claro.')
  // Se cierra: Elena y Felipe no entregaron (0,0); David queda en Por revisar.
  await teacher.put(`/activities/${a1.id}`, { ...promedio, opensAt: at(-10 * DAY), closesAt: at(-3 * DAY) })
  await teacher.put(`/activities/${a1.id}/exceptions/${await idOf(students.felipe)}`, {
    extendedUntil: at(2 * DAY),
    extraAttempts: 0,
    reason: 'Incapacidad médica presentada a tiempo.',
  })
  log('Tarea 1 (cerrada): 3 calificadas (una corregida), 1 por revisar, 2 vencidas con 0,0; plazo ampliado a Felipe')

  // 2. Taller abierto con fotos y dos intentos.
  const a2 = await activity({
    title: 'Taller 2: tabla de multiplicar con Mientras',
    kind: 'WORKSHOP',
    statement: 'Resuelve a mano en tu cuaderno el algoritmo que muestra la tabla de multiplicar de un número usando Mientras.\nToma una foto clara de la hoja y súbela.',
    acceptsPhotos: true,
    weightPercent: 30,
    maxAttempts: 2,
    opensAt: at(-2 * DAY),
    closesAt: at(5 * DAY),
  })
  await rubric(a2.id, 'i <- 1; Mientras i <= 10 Hacer Escribir n, " x ", i, " = ", n * i; i <- i + 1; FinMientras', [
    ['Inicializa y actualiza el contador', 2],
    ['Condición del ciclo correcta', 1.5],
    ['Muestra cada línea de la tabla', 1.5],
  ])
  await teacher.post(`/activities/${a2.id}/publish`)
  const photo = { name: 'foto-cuaderno.png', type: 'image/png', content: notebookPhotoPng() }
  await submit(students.ana, a2.id, null, [photo])
  await submit(students.bruno, a2.id, 'No tengo cámara, lo escribo:\ni <- 1\nMientras i <= 10 Hacer\n  Escribir n * i\n  i <- i + 1\nFinMientras')
  const elenaA2 = await submit(students.elena, a2.id, null, [photo])
  await grade(elenaA2, 3.5, 'La foto se ve bien; olvidaste incrementar el contador. Tienes un segundo intento.')
  await submit(students.elena, a2.id, 'Segundo intento: agregué i <- i + 1 dentro del ciclo.', [photo])
  log('Taller 2 (abierto): 3 entregas por revisar, una de ellas segundo intento')

  // 3. Evaluación publicada que aún no abre, y 4. borrador sin rúbrica.
  const a3 = await activity({
    title: 'Evaluación 1: condicionales',
    kind: 'EXAM',
    statement: 'Escribe un algoritmo que lea la nota final y diga si el estudiante aprueba (nota >= 3,0) o reprueba.',
    weightPercent: 40,
    opensAt: at(3 * DAY),
    closesAt: at(3 * DAY + 2 * HOUR),
  })
  await rubric(a3.id, 'Si nota >= 3 Entonces Escribir "Aprueba" SiNo Escribir "Reprueba" FinSi', [
    ['Lee la nota', 1],
    ['Condición correcta', 2.5],
    ['Mensajes para ambos casos', 1.5],
  ])
  await teacher.post(`/activities/${a3.id}/publish`)
  await activity({
    title: 'Proyecto: animación en Scratch',
    kind: 'TASK',
    statement: 'Crea una animación con al menos un ciclo y un evento. Entrega el archivo .sb3.',
    acceptsText: false,
    acceptsFiles: true,
    fileGroups: ['TOOL_FILES'],
    maxAttempts: 2,
    opensAt: at(7 * DAY),
    closesAt: at(14 * DAY),
  })
  log('Evaluación 1 publicada (abre en 3 días) y Proyecto Scratch en borrador sin rúbrica')
}

// ---------- Etapa 6 · Calificación con IA ----------

const STAGE6_TITLE = 'Taller 3: mayor de dos números (foto o archivo)'
const sampleFile = (name) => readFileSync(new URL(`./muestras/${name}`, import.meta.url))

// Entregas variadas para ver cada camino de la IA: foto legible, foto con un error, foto ilegible, texto,
// archivo de PSeInt y un PDF (desde P-08 la IA lee su texto y su página). Las evalúa el modelo real si la IA está activada; si no,
// quedan en Por revisar como calificación manual y la docente puede usar "Reintentar con IA" después.
async function stage6(teacher, students, course) {
  console.log('Etapa 6 · calificación con IA')
  const activity = await teacher.post(`/courses/${course.id}/activities`, {
    title: STAGE6_TITLE,
    kind: 'WORKSHOP',
    statement:
      'Escribe un algoritmo en PSeInt que lea dos números enteros y muestre cuál es el mayor.\nPuedes subir una foto clara de tu cuaderno, escribir la respuesta o adjuntar el archivo .psc.',
    acceptsText: true,
    acceptsFiles: true,
    acceptsPhotos: true,
    fileGroups: ['DOCUMENTS', 'TOOL_FILES'],
    weightPercent: null,
    maxAttempts: 2,
    opensAt: at(-DAY),
    closesAt: at(6 * DAY),
  })
  await teacher.put(`/activities/${activity.id}/rubric`, {
    expectedAnswer:
      'Algoritmo Mayor\n  Leer a, b\n  Si a > b Entonces\n    Escribir "El mayor es ", a\n  SiNo\n    Escribir "El mayor es ", b\n  FinSi\nFinAlgoritmo',
    criteria: [
      { name: 'Lee los dos números', maxPoints: 1, description: 'Usa Leer con dos variables' },
      { name: 'Condición correcta', maxPoints: 2.5, description: 'Compara a > b (o equivalente) y elige bien la rama' },
      { name: 'Muestra el mayor en ambos casos', maxPoints: 1.5, description: null },
    ],
  })
  await teacher.post(`/activities/${activity.id}/publish`)
  const submit = (student, text, files = []) =>
    student.postForm(`/activities/${activity.id}/submissions`, fileForm({ text }, files.map((file) => ({ field: 'files', ...file }))))
  const jpg = (name) => ({ name, type: 'image/jpeg', content: sampleFile(name) })

  const submissions = [
    await submit(students.ana, null, [jpg('cuaderno-legible.jpg')]),
    await submit(students.bruno, null, [jpg('cuaderno-con-error.jpg')]),
    await submit(students.david, null, [jpg('cuaderno-borroso.jpg')]),
    await submit(students.camila, 'Algoritmo Mayor\n  Leer a, b\n  Si a >= b Entonces\n    Escribir "Mayor: ", a\n  SiNo\n    Escribir "Mayor: ", b\n  FinSi\nFinAlgoritmo'),
    await submit(students.elena, null, [
      {
        name: 'mayor.psc',
        type: 'text/plain',
        content: Buffer.from('Algoritmo Mayor\r\n\tLeer a, b\r\n\tSi a > b Entonces\r\n\t\tEscribir a\r\n\tFinSi\r\nFinAlgoritmo\r\n', 'latin1'),
      },
    ]),
    await submit(students.felipe, 'Adjunto el PDF exportado.', [
      { name: 'mayor-felipe.pdf', type: 'application/pdf', content: pdf('Algoritmo Mayor · Felipe', ['Leer a, b', 'Si a > b Entonces', '  Escribir a']) },
    ]),
  ]
  const states = await Promise.all(submissions.map((s) => teacher.get(`/submissions/${s.id}/grade`)))
  const processing = states.filter((detail) => detail.grade.status === 'PROCESSING').length
  log(`${STAGE6_TITLE}: 6 entregas (3 fotos, texto, archivo .psc y un PDF)`)
  if (processing > 0) {
    log(`${processing} entregas en la cola de la IA. Mira el avance en la bandeja Por revisar.`)
  } else {
    log('La IA está desactivada: las entregas quedaron en Por revisar. Al activarla, usa "Reintentar con IA" en cada una.')
  }
}

// ---------- Etapa 7 · Quizzes, PSeInt y juegos ----------

const STAGE7_TITLE = 'Taller 4: suma de 1 a n (casos de prueba)'

// Banco de preguntas, un quiz calificable ya cerrado (con revisión y ranking), uno de repaso abierto
// y una actividad de PSeInt que se califica sola con casos de prueba. Los juegos no guardan datos en el servidor.
async function stage7(teacher, students, course) {
  console.log('Etapa 7 · quizzes, PSeInt y juegos')
  const question = (kind, prompt, { points = 1, options = [], accepted = [], explanation = null }) =>
    teacher.post(`/courses/${course.id}/questions`, {
      kind,
      prompt,
      points,
      explanation,
      options: options.map(([text, correct]) => ({ text, correct })),
      acceptedAnswers: accepted,
    })
  const q = {
    ciclo: await question('SINGLE', '¿Qué ciclo revisa su condición al final, de modo que se ejecuta al menos una vez?', {
      options: [['Mientras', false], ['Repetir … Hasta Que', true], ['Para', false], ['Según', false]],
      explanation: 'Repetir ejecuta el cuerpo y luego revisa la condición.',
    }),
    mod: await question('SINGLE', 'Si x <- 7, ¿qué muestra Escribir x MOD 2?', {
      options: [['3', false], ['1', true], ['0', false], ['3.5', false]],
      explanation: 'MOD da el residuo de la división: 7 = 2 × 3 + 1.',
    }),
    repetitivas: await question('MULTIPLE', '¿Cuáles son estructuras repetitivas?', {
      points: 2,
      options: [['Para', true], ['Mientras', true], ['Si … Entonces', false], ['Repetir … Hasta Que', true]],
      explanation: 'Si es una estructura de decisión, no repite nada.',
    }),
    finito: await question('TRUE_FALSE', 'Un algoritmo debe terminar después de un número finito de pasos.', {
      options: [['Verdadero', true], ['Falso', false]],
    }),
    asignar: await question('TRUE_FALSE', 'En PSeInt, ni = ni <- sirven para asignar un valor a una variable.', {
      options: [['Verdadero', false], ['Falso', true]],
      explanation: 'Las dos formas asignan en el perfil flexible; <- es la más clara.',
    }),
    mostrar: await question('SHORT', '¿Qué instrucción de PSeInt muestra datos en pantalla?', { accepted: ['Escribir', 'Imprimir', 'Mostrar'] }),
    logico: await question('SINGLE', '¿Qué tipo de dato guarda Verdadero o Falso?', {
      options: [['Entero', false], ['Real', false], ['Lógico', true], ['Caracter', false]],
    }),
    nombre: await question('SHORT', '¿Qué tipo de dato usarías para guardar un nombre?', { accepted: ['Caracter', 'Texto', 'Cadena'] }),
    real: await question('SINGLE', '¿De qué tipo es el valor 3.5?', {
      options: [['Entero', false], ['Real', true], ['Lógico', false], ['Caracter', false]],
    }),
    // El repaso muestra las respuestas al terminar: no comparte preguntas con el Quiz 1 calificable (P-04).
    entero: await question('TRUE_FALSE', 'El valor 12 es de tipo Entero.', {
      options: [['Verdadero', true], ['Falso', false]],
    }),
  }
  log('Banco de preguntas: 10 preguntas de los cuatro tipos')

  // Respuestas por texto de opción: { clave: ['Para', 'Mientras'] } o { clave: 'Escribir' } en las cortas.
  const answersFor = (attempt, picks) => ({
    answers: Object.entries(picks).map(([key, pick]) => {
      const item = attempt.questions.find((x) => x.id === q[key].id)
      if (item.kind === 'SHORT') return { questionId: item.id, choices: [], text: pick }
      const texts = [pick].flat()
      return { questionId: item.id, choices: texts.map((text) => item.options.indexOf(text)), text: null }
    }),
  })
  const take = async (student, activityId, picks, { finish = true } = {}) => {
    const attempt = await student.post(`/activities/${activityId}/quiz/attempts`)
    if (!finish) return student.put(`/quiz-attempts/${attempt.id}/answers`, answersFor(attempt, picks))
    return student.post(`/quiz-attempts/${attempt.id}/finish`, answersFor(attempt, picks))
  }
  const allRight = {
    ciclo: 'Repetir … Hasta Que',
    mod: '1',
    repetitivas: ['Para', 'Mientras', 'Repetir … Hasta Que'],
    finito: 'Verdadero',
    asignar: 'Falso',
    mostrar: 'Escribir',
  }

  // 1. Quiz calificable: se responde abierto y luego se cierra para ver la revisión y el ranking.
  const quizFields = {
    title: 'Quiz 1: ciclos y condiciones',
    kind: 'QUIZ',
    statement: 'Seis preguntas sobre ciclos, condiciones y PSeInt. Tienes 15 minutos y un intento.',
    acceptsText: false,
    acceptsFiles: false,
    acceptsPhotos: false,
    fileGroups: [],
    weightPercent: null,
    maxAttempts: 1,
    opensAt: at(-2 * DAY),
    closesAt: at(2 * DAY),
  }
  const quiz = await teacher.post(`/courses/${course.id}/activities`, quizFields)
  await teacher.put(`/activities/${quiz.id}/quiz`, {
    timeLimitMinutes: 15,
    questionIds: ['ciclo', 'mod', 'repetitivas', 'finito', 'asignar', 'mostrar'].map((key) => q[key].id),
  })
  await teacher.post(`/activities/${quiz.id}/publish`)
  await take(students.ana, quiz.id, allRight)
  await take(students.bruno, quiz.id, { ...allRight, repetitivas: ['Para', 'Mientras'], mostrar: 'escribir' })
  await take(students.camila, quiz.id, { ...allRight, mod: '0', asignar: 'Verdadero' })
  // David deja el intento abierto con dos respuestas: el servidor lo envía solo al acabarse sus 15 minutos.
  await take(students.david, quiz.id, { ciclo: 'Repetir … Hasta Que', finito: 'Verdadero' }, { finish: false })
  await teacher.put(`/activities/${quiz.id}`, { ...quizFields, closesAt: at(-HOUR) })
  await teacher.put(`/activities/${quiz.id}/exceptions/${await idOf(students.felipe)}`, {
    extendedUntil: at(2 * DAY),
    extraAttempts: 0,
    reason: 'Estuvo en una salida académica el día del quiz.',
  })
  log('Quiz 1 (cerrado, con revisión): Ana 5,0; Bruno y Camila 3,6 (en el ranking desempata el tiempo); David se envía solo; Elena 0,0; Felipe con plazo ampliado')

  // 2. Quiz de repaso abierto: sin nota, se repite; el ranking cuenta el mejor intento.
  const practice = await teacher.post(`/courses/${course.id}/activities`, {
    ...quizFields,
    title: 'Repaso: tipos de datos',
    kind: 'PRACTICE_QUIZ',
    statement: 'Practica cuantas veces quieras. No da nota y al terminar ves las respuestas correctas.',
    opensAt: at(-DAY),
    closesAt: at(7 * DAY),
  })
  await teacher.put(`/activities/${practice.id}/quiz`, {
    timeLimitMinutes: 5,
    questionIds: ['logico', 'nombre', 'real', 'entero'].map((key) => q[key].id),
  })
  await teacher.post(`/activities/${practice.id}/publish`)
  const practiceRight = { logico: 'Lógico', nombre: 'Caracter', real: 'Real', entero: 'Verdadero' }
  await take(students.ana, practice.id, { ...practiceRight, logico: 'Entero', real: 'Entero' })
  await take(students.ana, practice.id, practiceRight)
  await take(students.bruno, practice.id, { ...practiceRight, nombre: 'Número' })
  log('Repaso: tipos de datos (abierto): Ana con dos intentos, Bruno con uno')

  // 3. PSeInt con casos de prueba: se califica sola al recibir la entrega (RF-12, RF-16).
  const sum = await teacher.post(`/courses/${course.id}/activities`, {
    title: STAGE7_TITLE,
    kind: 'WORKSHOP',
    statement:
      'Escribe un algoritmo en PSeInt que lea un número entero n (0 o más) y muestre la suma 1 + 2 + … + n.\nEscribe el programa en la respuesta o adjunta tu archivo .psc. Se califica solo con casos de prueba.',
    acceptsText: true,
    acceptsFiles: true,
    acceptsPhotos: false,
    fileGroups: ['TOOL_FILES'],
    weightPercent: null,
    maxAttempts: 2,
    opensAt: at(-DAY),
    closesAt: at(5 * DAY),
  })
  await teacher.put(`/activities/${sum.id}/rubric`, {
    expectedAnswer: 'Algoritmo Suma\n  Leer n\n  s <- 0\n  Para i <- 1 Hasta n Hacer\n    s <- s + i\n  FinPara\n  Escribir s\nFinAlgoritmo',
    criteria: [{ name: 'Casos de prueba aprobados', maxPoints: 5, description: 'Cada caso da un n y revisa la suma que se muestra' }],
    testCases: [
      { input: '5', expectedOutput: '15' },
      { input: '10', expectedOutput: '55' },
      { input: '0', expectedOutput: '0' },
    ],
  })
  await teacher.post(`/activities/${sum.id}/publish`)
  const submitCode = (student, text, files = []) =>
    student.postForm(`/activities/${sum.id}/submissions`, fileForm({ text }, files.map((file) => ({ field: 'files', ...file }))))
  await submitCode(
    students.ana,
    'Algoritmo Suma\n  Definir n, i, s Como Entero\n  Leer n\n  s <- 0\n  Para i <- 1 Hasta n Hacer\n    s <- s + i\n  FinPara\n  Escribir "La suma es ", s\nFinAlgoritmo',
  )
  // Repetir corre al menos una vez: con n = 0 muestra 1 y falla ese caso.
  await submitCode(students.bruno, 'Algoritmo Suma\n  Leer n\n  s <- 0\n  i <- 1\n  Repetir\n    s <- s + i\n    i <- i + 1\n  Hasta Que i > n\n  Escribir s\nFinAlgoritmo')
  await submitCode(students.camila, 'Algoritmo Suma\n  Leer n\n  s <- 0\n  Para i <- 1 Hasta n Hacer\n    s <- s + i\n  Escribir s\nFinAlgoritmo')
  await submitCode(students.david, null, [
    { name: 'suma.psc', type: 'text/plain', content: Buffer.from('Proceso Suma\r\n\tLeer n\r\n\tEscribir n * (n + 1) / 2\r\nFinProceso\r\n', 'latin1') },
  ])
  log(`${STAGE7_TITLE}: Ana 5,0 y David 5,0 (archivo .psc), Bruno 3,3 (falla n = 0); Camila con error de sintaxis, en Por revisar`)
  log('Juegos: abre /juegos (sin sesión). El avance se guarda en el navegador; no hay datos que cargar.')
}

// ---------- Etapa 8 · Foro y operación docente ----------

const STAGE8_TITLE = 'Bienvenidos al foro de Pensamiento Computacional'

async function stage8(teacher, students) {
  console.log('Etapa 8 · foro, planilla y panel de administración')
  const thread = (author, category, title, body) => author.post('/forum/threads', { category, title, body })
  const reply = (author, id, body) => author.post(`/forum/threads/${id}/posts`, { body })

  const welcome = await thread(
    teacher,
    'GENERAL',
    STAGE8_TITLE,
    'Aquí resolvemos dudas entre todos.\n\n1. Elige la herramienta correcta al abrir un hilo.\n2. Copia el código o la parte del diagrama que no funciona.\n3. No publiques respuestas de quizzes ni talleres abiertos.',
  )
  await teacher.patch(`/forum/threads/${welcome.id}`, { pinned: true })

  const loop = await thread(
    students.ana,
    'PSEINT',
    '¿Por qué mi ciclo Mientras no termina?',
    'Mi programa se queda pegado:\n\nMientras i <= 10 Hacer\n    Escribir i\nFinMientras\n\n¿Qué me falta?',
  )
  await reply(students.bruno, loop.id, 'Te falta aumentar i dentro del ciclo: i <- i + 1. Si no, la condición nunca cambia.')
  await reply(teacher, loop.id, 'Exacto. En el editor de PSeInt de LogiCode verás que el programa se corta por el límite de pasos; agrega el aumento y prueba de nuevo.')

  const dfd = await thread(students.camila, 'DFD', '¿Qué figura uso para leer datos?', 'En el diagrama no sé si leer va en rectángulo o en otra figura.')
  await reply(teacher, dfd.id, 'Leer y escribir van en el paralelogramo; el rectángulo es para asignaciones como suma <- a + b.')

  const scratch = await thread(students.david, 'SCRATCH', 'Mi gato no se mueve con las flechas', 'Puse "al presionar tecla flecha derecha" pero no pasa nada.')
  await reply(students.elena, scratch.id, 'Revisa que el bloque "mover 10 pasos" esté pegado debajo del evento y que hayas hecho clic en el escenario antes de presionar.')

  const arduino = await thread(students.felipe, 'ARDUINO', '¿El LED necesita resistencia?', 'Voy a conectar un LED al pin 13. ¿Le pongo resistencia?')
  await reply(teacher, arduino.id, 'Sí: una de 220 Ω en serie con el LED. Cierro el hilo porque quedó resuelto.')
  await teacher.patch(`/forum/threads/${arduino.id}`, { closed: true })

  const spam = await thread(students.bruno, 'GENERAL', 'Cambio respuestas del Quiz 1', 'Escríbanme y les paso las respuestas.')
  const diana = await Session.login(ORDINARY_ADMIN.cedula, DEMO_PASSWORD)
  await diana.request('DELETE', `/forum/threads/${spam.id}`, { expect: [204] })

  log(`Foro: 5 hilos (bienvenida fijada, PSeInt, DFD, Scratch y Arduino cerrado) con respuestas; uno eliminado por ${ORDINARY_ADMIN.fullName}`)
  log('Planilla: pestaña "Planilla" del curso (docente y Administradores) y "Mis notas" (estudiantes); exportación a Excel (.xlsx) o CSV desde ahí')
  log('Panel de administración: /admin con la cuenta de Diana o una cuenta inicial; los respaldos los hace el servicio "backup"')
}

// ---------- RF-12 · retos de bloques que configura la docente ----------

const BLOCKS_TITLE = 'Reto 1: la escalera del robot (bloques)'

// Un reto de bloques calificable: el servidor vuelve a ejecutar el programa de cada entrega (5,0 si llega a la meta).
async function stageBlocks(teacher, students, course) {
  console.log('RF-12 · retos de bloques')
  const challenge = await teacher.post(`/courses/${course.id}/activities`, {
    title: BLOCKS_TITLE,
    kind: 'WORKSHOP',
    statement:
      'Lleva al robot por la escalera hasta la meta usando como máximo 6 bloques. Pista: el mismo par de pasos se repite.',
    acceptsText: true,
    acceptsFiles: false,
    acceptsPhotos: false,
    fileGroups: [],
    weightPercent: null,
    maxAttempts: 3,
    opensAt: at(-DAY),
    closesAt: at(7 * DAY),
  })
  await teacher.put(`/activities/${challenge.id}/rubric`, {
    expectedAnswer: 'Repetir 3 veces: Avanzar, Girar a la derecha, Avanzar, Girar a la izquierda. Al final, Avanzar.',
    criteria: [{ name: 'El robot llega a la meta', maxPoints: 5, description: 'Se califica solo al ejecutar el programa' }],
    blockChallenge: {
      grid: ['S.###', '#..##', '##..#', '###.M'],
      facing: 'E',
      maxBlocks: 6,
      allowed: ['avanzar', 'izquierda', 'derecha', 'repetir'],
    },
  })
  await teacher.post(`/activities/${challenge.id}/publish`)
  const b = (type, body) => (body ? { type, times: 3, body } : { type })
  const step = [b('avanzar'), b('derecha'), b('avanzar'), b('izquierda')]
  const submit = (student, program) =>
    student.postForm(`/activities/${challenge.id}/submissions`, fileForm({ text: JSON.stringify(program) }))
  await submit(students.ana, [b('repetir', step), b('avanzar')])
  await submit(students.bruno, [b('repetir', [b('avanzar'), b('derecha'), b('avanzar')])])
  await submit(students.camila, [{ ...b('repetir', step), times: 2 }, b('avanzar')])
  log(`${BLOCKS_TITLE}: Ana 5,0; Bruno 0,0 (choca); Camila 0,0 (termina antes de la meta); los demás sin entregar`)
}

// ---------- P-07 · lista de estudiantes en Excel ----------

const EXCEL_LIST = 'lista-estudiantes.xlsx'
const EXCEL_STUDENTS = ['1000000209', '1000000210']

// Inscribe al curso desde muestras/lista-estudiantes.xlsx: dos estudiantes nuevos (sin registrarse), una cédula que ya
// estaba y una fila con error. La planilla se exporta a .xlsx desde la pestaña "Planilla"; no hay datos que cargar.
async function stageExcel(teacher, students, course) {
  console.log('P-07 y P-08 · Excel y PDF')
  const xlsx = { field: 'file', name: EXCEL_LIST, type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', content: sampleFile(EXCEL_LIST) }
  const result = await teacher.postForm(`/courses/${course.id}/enrollments/import`, fileForm({}, [xlsx]))
  log(`${EXCEL_LIST}: ${result.enrolled} inscritos (Iván y Juliana, sin registrarse), ${result.alreadyEnrolled} ya estaba y ${result.errors.length} fila con error`)
  log('Planilla: "Exportar a Excel (.xlsx)" en la pestaña Planilla. PDF con IA: la entrega de Felipe en el Taller 3')
}

// ---------- Curso completo · 20 estudiantes, 15 actividades y entregas para la IA ----------

const FULL_COURSE = JSON.parse(readFileSync(new URL('./curso-completo.json', import.meta.url), 'utf8')).actividades
const FULL_COURSE_TITLE = FULL_COURSE[0].titulo
// 14 estudiantes más (ficticios) para llegar a 20 activos en el curso.
const MORE_STUDENTS = [
  'Karen Rincón Pardo', 'Luis Fernando Gil Ortega', 'Mariana Quintero Sanz', 'Nicolás Becerra Leal',
  'Olga Patricia Niño Vera', 'Pablo Andrés Rey Duarte', 'Quintín Salas Mejía', 'Rosa Elena Cárdenas Ríos',
  'Santiago Ibáñez Polo', 'Tatiana Lozano Prada', 'Úrsula Meneses Pinto', 'Valentina Acosta Lara',
  'Wilmer Jaimes Contreras', 'Ximena Delgado Ortiz',
].map((fullName, i) => {
  const [first, , last] = fullName.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().split(' ')
  return { cedula: String(1000000211 + i), fullName, email: `${first}.${last ?? 'demo'}@demo.logicode.test` }
})

// Qué entrega cada estudiante en cada actividad, fijo para que la carga se repita igual:
// la mitad correctas, un cuarto con error y un cuarto incompletas; 40 % foto, 30 % PDF, 20 % texto y 10 % archivo.
function plan(i, k) {
  const r = (i * 3 + k * 5) % 8
  const variant = r < 4 ? 'correcta' : r < 6 ? 'error' : 'incompleta'
  const f = (i * 7 + k * 3) % 10
  const format = f < 4 ? 'foto' : f < 7 ? 'pdf' : f < 9 ? 'texto' : 'archivo'
  return { variant, format, blurry: format === 'foto' && (i + k) % 6 === 0 }
}

async function stageFullCourse(teacher, students, course) {
  console.log('Curso completo · 20 estudiantes, 15 actividades y entregas para la IA')
  const all = Object.entries(STUDENTS).map(([key, person]) => ({ person, session: students[key] }))
  all.push(...(await enrollNew(teacher, course, MORE_STUDENTS)))
  log(`${MORE_STUDENTS.length} estudiantes más inscritos y registrados: ${all.length} activos en el curso`)
  await aiWorkshops(teacher, course, all, { label: '6 talleres nuevos (Taller 5 a 10), 15 actividades en el curso' })
}

// Inscribe y registra estudiantes nuevos; devuelve sus sesiones.
async function enrollNew(teacher, course, people) {
  const sessions = []
  for (const person of people) {
    await teacher.post(`/courses/${course.id}/enrollments`, { cedula: person.cedula, fullName: person.fullName })
    await register('student-registrations', person)
    sessions.push({ person, session: await Session.login(person.cedula, DEMO_PASSWORD) })
  }
  return sessions
}

// Los 6 talleres de curso-completo.json con una entrega de cada estudiante. offset cambia quién entrega qué,
// para que dos cursos no queden iguales; renumber los numera desde 1 (en un curso nuevo no hay Taller 1 a 4).
async function aiWorkshops(teacher, course, all, { label, offset = 0, renumber = false }) {
  const counts = { foto: 0, pdf: 0, texto: 0, archivo: 0, borrosa: 0 }
  const submissions = []
  for (const [k, a] of FULL_COURSE.entries()) {
    const title = renumber ? a.titulo.replace(/^Taller \d+/, `Taller ${k + 1}`) : a.titulo
    const activity = await teacher.post(`/courses/${course.id}/activities`, {
      title,
      kind: 'WORKSHOP',
      statement: a.enunciado,
      acceptsText: true,
      acceptsFiles: true,
      acceptsPhotos: true,
      fileGroups: ['DOCUMENTS', 'TOOL_FILES'],
      weightPercent: null,
      maxAttempts: 2,
      opensAt: at(-2 * DAY),
      closesAt: at(7 * DAY),
    })
    await teacher.put(`/activities/${activity.id}/rubric`, {
      expectedAnswer: a.esperada,
      criteria: a.criterios.map(([name, maxPoints, description]) => ({ name, maxPoints, description })),
    })
    await teacher.post(`/activities/${activity.id}/publish`)
    for (const [i, { person, session }] of all.entries()) {
      const { variant, format, blurry } = plan(i + offset, k)
      const lines = a.variantes[variant]
      let text = null
      let file = null
      if (format === 'texto') text = lines.join('\n')
      if (format === 'pdf') file = { name: `${a.clave}.pdf`, type: 'application/pdf', content: pdf(`${title} · ${person.fullName}`, lines) }
      if (format === 'archivo') file = { name: a.archivo, type: 'text/plain', content: Buffer.from(lines.join('\r\n'), 'latin1') }
      if (format === 'foto') {
        const name = `${a.clave}-${variant}-${blurry ? 'borrosa' : 'legible'}.jpg`
        file = { name, type: 'image/jpeg', content: sampleFile(`curso/${name}`) }
      }
      counts[format]++
      if (blurry) counts.borrosa++
      const form = fileForm({ text }, file ? [{ field: 'files', ...file }] : [])
      submissions.push(await session.postForm(`/activities/${activity.id}/submissions`, form))
    }
  }
  log(`${label}. ${submissions.length} entregas: ${counts.foto} fotos ` +
    `(${counts.borrosa} borrosas), ${counts.pdf} PDF, ${counts.texto} en texto y ${counts.archivo} archivos .psc o .ino`)
  const states = await Promise.all(submissions.map((s) => teacher.get(`/submissions/${s.id}/grade`)))
  const processing = states.filter((detail) => detail.grade.status === 'PROCESSING').length
  if (processing > 0) {
    log(`${processing} entregas en la cola de la IA; a cerca de 1 minuto cada una, unas ${Math.ceil(processing / 60)} horas. ` +
      'Mira el avance en Control o Por revisar.')
  } else {
    log('La IA está desactivada: las entregas quedaron en Por revisar. Al activarla, usa "Calificar todo con IA" en Control.')
  }
}

// ---------- Más cursos · un segundo docente con los Grupos B y C ----------

const SECOND_TEACHER = { cedula: '1000000104', fullName: 'Andrés Felipe Molina Arias', email: 'andres.molina@demo.logicode.test' }
const FIRST_NAMES = ['Alejandro', 'Beatriz', 'Carlos', 'Daniela', 'Esteban', 'Fernanda', 'Gustavo', 'Helena', 'Ignacio', 'Juana',
  'Kevin', 'Lorena', 'Manuel', 'Natalia', 'Óscar', 'Paola', 'Ricardo', 'Sara', 'Tomás', 'Viviana']
const SURNAMES = ['Arango', 'Bautista', 'Contreras', 'Durán', 'Escobar', 'Flórez', 'Galvis', 'Hernández', 'Jaramillo', 'Leal',
  'Medina', 'Navarro', 'Ospina', 'Parra', 'Quiroga', 'Rangel', 'Sandoval', 'Toro', 'Uribe', 'Villamizar']
const ascii = (text) => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
// 20 estudiantes ficticios por curso: cédulas 1000000301 a 320 (Grupo B) y 1000000401 a 420 (Grupo C).
const groupStudents = (base, shift, step) =>
  Array.from({ length: 20 }, (_, i) => {
    const first = FIRST_NAMES[(i + shift) % 20]
    const last = SURNAMES[(i * step + shift) % 20]
    const cedula = String(base + i + 1)
    return {
      cedula,
      fullName: `${first} ${last} ${SURNAMES[(i * 7 + shift + 5) % 20]}`,
      email: `${ascii(first)}.${ascii(last)}${cedula.slice(-3)}@demo.logicode.test`,
    }
  })
const MORE_COURSES = [
  { name: 'Pensamiento Computacional · Grupo B', people: groupStudents(1000000300, 0, 3), offset: 3 },
  { name: 'Pensamiento Computacional · Grupo C', people: groupStudents(1000000400, 10, 7), offset: 5 },
]

// Diana (administradora ordinaria de la demostración) aprueba al docente: tiene ese permiso, así que esta parte
// no necesita la cuenta inicial.
async function stageMoreCourses() {
  console.log('Más cursos · Grupos B y C con un segundo docente')
  await register('teacher-requests', SECOND_TEACHER)
  const diana = await Session.login(ORDINARY_ADMIN.cedula, DEMO_PASSWORD)
  await diana.post(`/admin/teachers/${await userId(diana, SECOND_TEACHER.cedula)}/approve`)
  log(`Docente aprobado por ${ORDINARY_ADMIN.fullName}: ${SECOND_TEACHER.fullName} (${SECOND_TEACHER.cedula})`)
  const teacher = await Session.login(SECOND_TEACHER.cedula, DEMO_PASSWORD)
  for (const { name, people, offset } of MORE_COURSES) {
    const course = await teacher.post('/courses', {
      name,
      period: '2026-2',
      description: 'Lógica de programación con PSeInt y Arduino. Curso de demostración.',
    })
    for (const [title, tool, description] of [
      ['Algoritmos y condicionales en PSeInt', 'PSEINT', 'Entrada, proceso y salida; decisiones con Si.'],
      ['Ciclos en PSeInt', 'PSEINT', 'Para y Mientras.'],
      ['Primeros pasos con Arduino', 'ARDUINO', 'Salidas digitales y tiempos.'],
    ]) {
      await teacher.post(`/courses/${course.id}/topics`, { title, tool, description })
    }
    const all = await enrollNew(teacher, course, people)
    log(`"${name}": 3 temas y ${all.length} estudiantes inscritos y registrados`)
    await aiWorkshops(teacher, course, all, { label: `"${name}": 6 talleres`, offset, renumber: true })
  }
}

async function idOf(session) {
  return (await session.get('/me')).id
}

// ---------- Etapa 10 · piloto ----------

// Las cuentas demo tienen contraseña pública: en una instalación del piloto (con estudiantes reales) no se cargan,
// salvo que se pida a propósito para presentar con datos ficticios por HTTPS.
function stage10(info) {
  if (info.environment === 'piloto' && process.env.LOGICODE_DEMO_EN_PILOTO !== 'si') {
    throw new Error('Esta instalación está en modo piloto. Los datos de demostración tienen una contraseña pública y no se ' +
      'cargan ahí. Si es una instalación solo para presentar, ejecuta con LOGICODE_DEMO_EN_PILOTO=si.')
  }
}

// ---------- Programa ----------

async function main() {
  console.log(`LogiCode · datos de demostración → ${BASE}`)
  const info = await fetch(`${API}/system/info`)
    .then((response) => response.json())
    .catch((error) => {
      const certificate = /certificate|self.signed|UNABLE_TO_VERIFY/i.test(String(error.cause?.code ?? error.cause))
      throw new Error(certificate
        ? `${BASE} usa el certificado local del piloto: ejecuta con NODE_EXTRA_CA_CERTS=certificados/logicode-ca.crt.`
        : `No hay respuesta en ${BASE}. ¿Está corriendo "docker compose up"?`)
    })
  stage10(info)

  let existing = null
  try {
    existing = await Session.login(TEACHER.cedula, DEMO_PASSWORD)
  } catch (error) {
    if (error.status !== 401 && error.status !== 400) throw error
  }
  if (existing) {
    await addMissingStages(existing)
    return
  }

  const cedula = process.env.LOGICODE_ADMIN_CEDULA || (await ask('Cédula de una cuenta inicial (privilegio completo): '))
  const password = process.env.LOGICODE_ADMIN_PASSWORD || (await ask('Contraseña: ', { hidden: true }))
  const admin = await Session.login(cedula, password)
  const me = await admin.get('/me')
  if (!me.fullPrivileges) throw new Error('Esa cuenta no tiene privilegio completo; usa una de las cuentas iniciales.')

  const teacher = await stage3(admin)
  const course = await stage4(teacher)
  const students = await stage3Students(admin, teacher)
  await stage5(teacher, students, course)
  await stage6(teacher, students, course)
  await stage7(teacher, students, course)
  await stage8(teacher, students)
  await stageBlocks(teacher, students, course)
  await stageExcel(teacher, students, course)
  await stageFullCourse(teacher, students, course)
  await stageMoreCourses()

  console.log(`\nListo. Todas las cuentas de demostración usan la contraseña ${DEMO_PASSWORD}. Detalle en datos-demo/README.md`)
}

// Con datos de etapas anteriores ya cargados, agrega solo lo que falta (no necesita la cuenta inicial).
async function addMissingStages(teacher) {
  const courses = await teacher.get('/courses?size=50')
  const course = (courses.items ?? courses).find((c) => c.name === 'Pensamiento Computacional · Grupo A')
  if (!course) throw new Error('La docente de demostración existe, pero no se encontró su curso. Empieza de cero (ver README).')
  const titles = new Set((await teacher.get(`/courses/${course.id}/activities`)).map((a) => a.title))
  const threads = await teacher.get(`/forum/threads?q=${encodeURIComponent(STAGE8_TITLE)}`)
  threads.items.forEach((thread) => titles.add(thread.title))
  const enrolled = await teacher.get(`/courses/${course.id}/enrollments?size=100`)
  if (enrolled.items.some((student) => student.cedula === EXCEL_STUDENTS[0])) titles.add(EXCEL_LIST)
  const secondTeacher = await Session.login(SECOND_TEACHER.cedula, DEMO_PASSWORD).catch(() => null)
  if (secondTeacher) titles.add(SECOND_TEACHER.cedula)
  const missing = [
    ['la Etapa 6', STAGE6_TITLE, stage6],
    ['la Etapa 7', STAGE7_TITLE, stage7],
    ['la Etapa 8', STAGE8_TITLE, stage8],
    ['los retos de bloques', BLOCKS_TITLE, stageBlocks],
    ['la lista en Excel', EXCEL_LIST, stageExcel],
    ['los 20 estudiantes y las 15 actividades', FULL_COURSE_TITLE, stageFullCourse],
    ['los Grupos B y C', SECOND_TEACHER.cedula, stageMoreCourses],
  ].filter(([, title]) => !titles.has(title))
  if (missing.length === 0) {
    console.log('Los datos de demostración ya están cargados hasta la etapa actual. No se cambió nada.')
    return
  }
  const students = {}
  for (const [key, student] of Object.entries(STUDENTS)) students[key] = await Session.login(student.cedula, DEMO_PASSWORD)
  for (const [, , stage] of missing) await stage(teacher, students, course)
  console.log(`\nListo: se agregaron los datos de ${missing.map(([label]) => label).join(' y ')}.`)
}

main().catch((error) => {
  console.error(`\n✗ ${error.message}`)
  process.exit(1)
})
