// Prueba de contrato del consumidor (guía 7, T-106): cada llamada a la API que hace la interfaz existe, con su método,
// en el contrato que publica el backend (docs/api/openapi.json). El backend verifica su lado en LogiCodeApplicationIT,
// así que un cambio de ruta en cualquiera de los dos rompe una prueba antes de llegar al navegador.
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SRC = join(import.meta.dirname, '..')
const contrato = JSON.parse(readFileSync(join(SRC, '..', '..', 'docs', 'api', 'openapi.json'), 'utf8'))

const METODO = { Get: 'get', Post: 'post', Patch: 'patch', Put: 'put', Delete: 'delete', Upload: 'post', SendForm: 'post' }
const LLAMADAS = [
  [/\bapi(Get|Post|Patch|Put|Delete|Upload|SendForm)\(\s*(['`])(\/[^'`]*)\2/g, (m) => METODO[m[1]], 3],
  [/\buse(?:Paged)?Query\(\s*(['`])(\/[^'`]*)\1/g, () => 'get', 2],
  [/\$\{API_BASE\}(\/[^'`"\s]*)/g, () => 'get', 1],
]

function archivos(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? archivos(join(dir, e.name)) : /\.(js|jsx)$/.test(e.name) && !/\.test\./.test(e.name) ? [join(dir, e.name)] : [])
}

// `${x}` en medio de la ruta puede ser un parámetro ({id}) o un segmento fijo elegido en el código (close/reopen);
// pegado al final sin barra es la consulta (?page=…).
export function aPatron(ruta) {
  const limpia = ruta.split('?')[0].replace(/([^/])\$\{[^}]*\}/g, '$1').replace(/\$\{[^}]*\}/g, '\u0000')
  const escapada = limpia.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replaceAll('\u0000', '[^/]+')
  return new RegExp(`^/api/v1${escapada}$`)
}

const llamadas = archivos(SRC).flatMap((archivo) => {
  const codigo = readFileSync(archivo, 'utf8')
  return LLAMADAS.flatMap(([re, metodo, grupo]) =>
    [...codigo.matchAll(re)].map((m) => ({ archivo: archivo.slice(SRC.length + 1), metodo: metodo(m), ruta: m[grupo] })))
})

describe('contrato de la API', () => {
  it('encuentra las llamadas de la interfaz', () => {
    expect(llamadas.length).toBeGreaterThan(80)
  })

  it('cada llamada existe en docs/api/openapi.json con su método', () => {
    const faltan = llamadas.filter(({ metodo, ruta }) => {
      const patron = aPatron(ruta)
      return !Object.entries(contrato.paths).some(([p, ops]) => patron.test(p) && ops[metodo])
    })
    expect(faltan.map((l) => `${l.metodo.toUpperCase()} ${l.ruta} (${l.archivo})`)).toEqual([])
  })

  it('convierte rutas con variables en patrones', () => {
    expect(aPatron('/courses/${courseId}/ai-calibration/${name}').test('/api/v1/courses/{courseId}/ai-calibration/close')).toBe(true)
    expect(aPatron('/forum/threads${query}').test('/api/v1/forum/threads')).toBe(true)
    expect(aPatron('/courses/${id}').test('/api/v1/courses/{courseId}/topics')).toBe(false)
  })
})
