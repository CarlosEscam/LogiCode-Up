// Prueba de carga del endpoint crítico (guía 8, T-101): GET /api/v1/courses/{id}/activities, lo que abre cada
// estudiante antes del cierre de una actividad. Volumen: un curso tiene unos 30 estudiantes; 50 usuarios virtuales
// que repiten la consulta cada segundo son más que todo el curso a la vez.
// Nunca contra el piloto ni internet: solo acepta localhost salvo LOGICODE_CARGA_PERMITIDA=si. Uso en
// pruebas-carga/README.md.
import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE = (__ENV.LOGICODE_URL || 'http://localhost:3000').replace(/\/$/, '')
if (!/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(BASE) && __ENV.LOGICODE_CARGA_PERMITIDA !== 'si') {
  throw new Error(`Solo se prueba contra este equipo (localhost); ${BASE} no lo es.`)
}

export const options = {
  stages: [
    { duration: '10s', target: 50 }, // subida
    { duration: '30s', target: 50 }, // sostenimiento
    { duration: '10s', target: 0 }, // bajada
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
}

export function setup() {
  const jar = http.cookieJar()
  http.get(`${BASE}/api/v1/auth/csrf`)
  const xsrf = jar.cookiesForURL(BASE)['XSRF-TOKEN'][0]
  const login = http.post(`${BASE}/api/v1/auth/login`,
    JSON.stringify({ cedula: __ENV.LOGICODE_CEDULA || '1000000205', password: __ENV.LOGICODE_CLAVE || 'Demo#2026' }),
    { headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': xsrf } })
  if (login.status !== 200) throw new Error(`No se pudo iniciar sesión: ${login.status}`)
  const session = jar.cookiesForURL(BASE).LOGICODE_SESSION[0]
  const courses = http.get(`${BASE}/api/v1/courses`, { cookies: { LOGICODE_SESSION: session } }).json()
  const course = (courses.content || courses)[0]
  return { session, courseId: course.id }
}

export default function (data) {
  const r = http.get(`${BASE}/api/v1/courses/${data.courseId}/activities`, {
    cookies: { LOGICODE_SESSION: data.session },
    tags: { name: 'GET /courses/{id}/activities' },
  })
  check(r, { 'responde 200': (x) => x.status === 200, 'trae actividades': (x) => x.json().length > 0 })
  sleep(1)
}
