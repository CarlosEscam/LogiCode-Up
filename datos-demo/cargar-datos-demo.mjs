// Carga los datos de demostración y prueba de LogiCode por la API, como lo haría una persona:
// pasa por las mismas reglas de negocio y permisos que la aplicación. Ver datos-demo/README.md.
//
// Uso (con la aplicación corriendo):
//   node datos-demo/cargar-datos-demo.mjs
// Variables opcionales: LOGICODE_URL (por defecto http://localhost:3000),
// LOGICODE_ADMIN_CEDULA y LOGICODE_ADMIN_PASSWORD (si no están, se piden al iniciar).
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
// archivo de PSeInt y un PDF que la IA no lee. Las evalúa el modelo real si la IA está activada; si no,
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
    log(`${processing} entregas en la cola de la IA; el PDF pasó directo a Por revisar. Mira el avance en la bandeja Por revisar.`)
  } else {
    log('La IA está desactivada: las entregas quedaron en Por revisar. Al activarla, usa "Reintentar con IA" en cada una.')
  }
}

async function idOf(session) {
  return (await session.get('/me')).id
}

// ---------- Programa ----------

async function main() {
  console.log(`LogiCode · datos de demostración → ${BASE}`)
  await fetch(`${API}/system/info`).catch(() => {
    throw new Error(`No hay respuesta en ${BASE}. ¿Está corriendo "docker compose up"?`)
  })

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

  console.log(`\nListo. Todas las cuentas de demostración usan la contraseña ${DEMO_PASSWORD}. Detalle en datos-demo/README.md`)
}

// Con datos de etapas anteriores ya cargados, agrega solo lo que falta (no necesita la cuenta inicial).
async function addMissingStages(teacher) {
  const courses = await teacher.get('/courses?size=50')
  const course = (courses.items ?? courses).find((c) => c.name === 'Pensamiento Computacional · Grupo A')
  if (!course) throw new Error('La docente de demostración existe, pero no se encontró su curso. Empieza de cero (ver README).')
  const activities = await teacher.get(`/courses/${course.id}/activities`)
  if (activities.some((a) => a.title === STAGE6_TITLE)) {
    console.log('Los datos de demostración ya están cargados hasta la etapa actual. No se cambió nada.')
    return
  }
  const students = {}
  for (const [key, student] of Object.entries(STUDENTS)) students[key] = await Session.login(student.cedula, DEMO_PASSWORD)
  await stage6(teacher, students, course)
  console.log('\nListo: se agregaron los datos de la Etapa 6.')
}

main().catch((error) => {
  console.error(`\n✗ ${error.message}`)
  process.exit(1)
})
