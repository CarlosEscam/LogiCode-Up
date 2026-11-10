import { newStep, parseProject, stepText } from './flow.js'
import { readDraft } from './storage.js'

const field = (name, fieldKey, placeholder, size) => ({ name, field: fieldKey, placeholder, size })

// Cada modo nombra los pasos a su manera; el programa y la ejecución son los mismos (flow.js).
export const MODES = {
  dfd: {
    key: 'dfd',
    title: 'diagrama',
    defaultName: 'mi-diagrama',
    palette: ['leer', 'escribir', 'asignar', 'si', 'mientras', 'para', 'repetir'],
    kinds: { leer: 'io', escribir: 'io', asignar: 'process', si: 'control', mientras: 'loop', para: 'loop', repetir: 'loop' },
    labels: {
      leer: { button: 'Leer (entrada)', phrase: ['Leer', field('variables', 'vars', 'a, b', 10)] },
      escribir: { button: 'Escribir (salida)', phrase: ['Escribir', field('valor', 'expr', '"Hola ", nombre', 14)] },
      asignar: { button: 'Asignación', phrase: [field('variable', 'var', 'x'), '←', field('valor', 'expr', 'a + b', 12)] },
      si: { button: 'Decisión', phrase: ['Si', field('condición', 'cond', 'a > b', 12)], branches: { body: 'Sí', else: 'No' } },
      mientras: { button: 'Mientras', phrase: ['Mientras', field('condición', 'cond', 'i <= 10', 12)], branches: { body: 'Hacer' } },
      para: {
        button: 'Para',
        phrase: ['Para', field('variable', 'var', 'i', 3), '←', field('desde', 'from', '1', 3), 'hasta', field('hasta', 'to', '10', 3), 'con paso', field('paso', 'step', '1', 3)],
        branches: { body: 'Hacer' },
      },
      repetir: { button: 'Repetir … hasta que', phrase: ['Repetir hasta que', field('condición', 'cond', 'n > 0', 12)], branches: { body: 'Hacer' } },
    },
    text: stepText,
    start: [
      ['leer', { vars: 'a, b' }],
      ['si', { cond: 'a > b', body: [{ type: 'escribir', expr: '"El mayor es ", a' }], else: [{ type: 'escribir', expr: '"El mayor es ", b' }] }],
    ],
  },
  scratch: {
    key: 'scratch',
    title: 'proyecto',
    defaultName: 'mi-proyecto',
    palette: ['preguntar', 'escribir', 'asignar', 'cambiar', 'si', 'veces', 'repetir'],
    kinds: { preguntar: 'sensing', escribir: 'looks', asignar: 'variables', cambiar: 'variables', si: 'control', veces: 'control', repetir: 'control' },
    labels: {
      preguntar: { button: 'preguntar y esperar', phrase: ['preguntar', field('pregunta', 'prompt', '"¿Cómo te llamas?"', 14), 'y guardar en', field('variable', 'var', 'respuesta', 8)] },
      escribir: { button: 'decir', phrase: ['decir', field('mensaje', 'expr', '"¡Hola!"', 14)] },
      asignar: { button: 'fijar variable', phrase: ['fijar', field('variable', 'var', 'puntos', 6), 'a', field('valor', 'expr', '0', 6)] },
      cambiar: { button: 'cambiar variable', phrase: ['cambiar', field('variable', 'var', 'puntos', 6), 'por', field('valor', 'expr', '1', 4)] },
      si: { button: 'si … entonces', phrase: ['si', field('condición', 'cond', 'puntos > 5', 12), 'entonces'], branches: { body: 'entonces', else: 'si no' } },
      veces: { button: 'repetir veces', phrase: ['repetir', field('veces', 'times', '10', 3), 'veces'], branches: { body: 'repetir' } },
      repetir: { button: 'repetir hasta que', phrase: ['repetir hasta que', field('condición', 'cond', 'puntos = 10', 12)], branches: { body: 'repetir' } },
    },
    text: (step) =>
      ({
        preguntar: `preguntar ${step.prompt} y guardar en ${step.var}`,
        escribir: `decir ${step.expr}`,
        asignar: `fijar ${step.var} a ${step.expr}`,
        cambiar: `cambiar ${step.var} por ${step.expr}`,
        si: `si ${step.cond} entonces`,
        veces: `repetir ${step.times} veces`,
        repetir: `repetir hasta que ${step.cond}`,
      })[step.type],
    start: [
      ['preguntar', { prompt: '"¿Cómo te llamas?"', var: 'nombre' }],
      ['escribir', { expr: '"¡Hola, ", nombre, "!"' }],
      ['asignar', { var: 'puntos', expr: '0' }],
      ['veces', { times: '3', body: [{ type: 'cambiar', var: 'puntos', expr: '5' }] }],
      ['escribir', { expr: '"Tienes ", puntos, " puntos"' }],
    ],
  },
}

// Pasos con que abre el modo: el borrador guardado o el ejemplo.
export function initialSteps(mode) {
  try {
    const saved = readDraft(mode.key, null)
    if (saved) return parseProject(saved, mode.key)
  } catch {
    // Borrador dañado: se empieza con el ejemplo.
  }
  return mode.start.map(([type, values]) => newStep(type, values))
}
