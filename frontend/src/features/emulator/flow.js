// Programa de los emuladores de DFD y Scratch: un árbol de pasos que se traduce a PSeInt y se ejecuta con el mismo
// intérprete del servidor. Así los dos dicen si compila y dan los mismos errores, sin un intérprete más.
// Es puro (sin React) para probarlo.

// fields: campos de texto de cada paso. branches: listas de pasos que lleva adentro.
export const STEP_TYPES = {
  leer: { fields: ['vars'] },
  preguntar: { fields: ['prompt', 'var'] },
  escribir: { fields: ['expr'] },
  asignar: { fields: ['var', 'expr'] },
  cambiar: { fields: ['var', 'expr'] },
  si: { fields: ['cond'], branches: ['body', 'else'] },
  mientras: { fields: ['cond'], branches: ['body'] },
  para: { fields: ['var', 'from', 'to', 'step'], branches: ['body'] },
  repetir: { fields: ['cond'], branches: ['body'] },
  veces: { fields: ['times'], branches: ['body'] },
}

const DEFAULTS = { step: '1', times: '10' }
const FIELD_NAMES = {
  vars: 'las variables',
  prompt: 'la pregunta',
  var: 'la variable',
  expr: 'el valor',
  cond: 'la condición',
  from: 'el valor inicial',
  to: 'el valor final',
  step: 'el paso',
  times: 'el número de veces',
}
const OPTIONAL = new Set(['prompt'])
const IDENTIFIER = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ][A-Za-z0-9_ÁÉÍÓÚÜÑáéíóúüñ]*$/

let nextId = 0
export function newStep(type, values = {}) {
  const spec = STEP_TYPES[type]
  const step = { id: `p${Date.now().toString(36)}${nextId++}`, type }
  spec.fields.forEach((field) => {
    step[field] = values[field] ?? DEFAULTS[field] ?? ''
  })
  spec.branches?.forEach((branch) => {
    step[branch] = (values[branch] ?? []).map((child) => newStep(child.type, child))
  })
  return step
}

// Revisa lo que el intérprete no puede revisar por sí solo: campos vacíos y nombres de variable. Devuelve el primer
// problema ({ id, message }) o null.
export function checkFlow(steps) {
  for (const step of steps) {
    const spec = STEP_TYPES[step.type]
    if (!spec) return { id: step.id, message: `paso desconocido «${step.type}».` }
    for (const field of spec.fields) {
      const value = String(step[field] ?? '').trim()
      if (!value && !OPTIONAL.has(field)) return { id: step.id, message: `falta ${FIELD_NAMES[field]}.` }
      if (/[\n\r]/.test(value)) return { id: step.id, message: `${FIELD_NAMES[field]} debe ir en una sola línea.` }
    }
    const names = step.type === 'leer' ? step.vars.split(',').map((name) => name.trim()) : 'var' in step ? [step.var.trim()] : []
    const wrong = names.find((name) => !IDENTIFIER.test(name))
    if (wrong !== undefined) {
      return { id: step.id, message: wrong ? `«${wrong}» no sirve como nombre de variable: empieza con una letra y usa letras, números o _.` : 'falta un nombre de variable.' }
    }
    for (const branch of spec.branches ?? []) {
      const problem = checkFlow(step[branch])
      if (problem) return problem
    }
  }
  return null
}

// En DFD se escribe ← para asignar; el intérprete usa <-.
const expr = (text) => String(text).trim().replaceAll('←', '<-')

// Traduce el árbol a PSeInt. lineSteps[n] es el id del paso que generó la línea n (desde 1).
export function toPseint(steps) {
  const lines = ['Algoritmo Programa']
  const lineSteps = [null, null]
  let loops = 0
  const emit = (depth, text, id) => {
    lines.push(`${'    '.repeat(depth)}${text}`)
    lineSteps.push(id)
  }
  const write = (list, depth) =>
    list.forEach((step) => {
      const { id } = step
      switch (step.type) {
        case 'leer':
          emit(depth, `Leer ${step.vars.split(',').map((name) => name.trim()).join(', ')}`, id)
          break
        case 'preguntar':
          if (step.prompt.trim()) emit(depth, `Escribir ${expr(step.prompt)}`, id)
          emit(depth, `Leer ${step.var.trim()}`, id)
          break
        case 'escribir':
          emit(depth, `Escribir ${expr(step.expr)}`, id)
          break
        case 'asignar':
          emit(depth, `${step.var.trim()} <- ${expr(step.expr)}`, id)
          break
        case 'cambiar':
          emit(depth, `${step.var.trim()} <- ${step.var.trim()} + (${expr(step.expr)})`, id)
          break
        case 'si':
          emit(depth, `Si ${expr(step.cond)} Entonces`, id)
          write(step.body, depth + 1)
          if (step.else.length > 0) {
            emit(depth, 'SiNo', id)
            write(step.else, depth + 1)
          }
          emit(depth, 'FinSi', id)
          break
        case 'mientras':
          emit(depth, `Mientras ${expr(step.cond)} Hacer`, id)
          write(step.body, depth + 1)
          emit(depth, 'FinMientras', id)
          break
        case 'para':
          emit(depth, `Para ${step.var.trim()} <- ${expr(step.from)} Hasta ${expr(step.to)} Con Paso ${expr(step.step)} Hacer`, id)
          write(step.body, depth + 1)
          emit(depth, 'FinPara', id)
          break
        case 'repetir':
          emit(depth, 'Repetir', id)
          write(step.body, depth + 1)
          emit(depth, `Hasta Que ${expr(step.cond)}`, id)
          break
        case 'veces': {
          // Contador propio por ciclo, para que dos «repetir» anidados no se pisen.
          loops += 1
          const counter = `vez_${loops}`
          emit(depth, `Para ${counter} <- 1 Hasta ${expr(step.times)} Hacer`, id)
          write(step.body, depth + 1)
          emit(depth, 'FinPara', id)
          break
        }
        default:
          throw new Error(`Paso desconocido: ${step.type}`)
      }
    })
  write(steps, 1)
  lines.push('FinAlgoritmo')
  lineSteps.push(null)
  return { code: `${lines.join('\n')}\n`, lineSteps }
}

export function findStep(steps, id) {
  for (const step of steps) {
    if (step.id === id) return step
    for (const branch of STEP_TYPES[step.type]?.branches ?? []) {
      const inner = findStep(step[branch], id)
      if (inner) return inner
    }
  }
  return null
}

// Proyecto guardado en el equipo: { herramienta, version, pasos }. Devuelve los pasos o lanza un error con el motivo.
export function parseProject(text, mode) {
  let data
  try {
    data = JSON.parse(text)
  } catch {
    throw new Error('El archivo no es un proyecto del Emulador (no es JSON válido).')
  }
  if (data?.herramienta !== mode || !Array.isArray(data.pasos)) {
    throw new Error(`El archivo no es un proyecto de ${mode === 'dfd' ? 'DFD' : 'Scratch'} del Emulador.`)
  }
  const build = (list, depth) => {
    if (depth > 30) throw new Error('El proyecto tiene demasiados niveles anidados.')
    return list.map((item) => {
      if (!STEP_TYPES[item?.type]) throw new Error('El proyecto tiene un paso desconocido.')
      const values = {}
      STEP_TYPES[item.type].fields.forEach((field) => {
        values[field] = String(item[field] ?? '').slice(0, 500)
      })
      const step = newStep(item.type, values)
      STEP_TYPES[item.type].branches?.forEach((branch) => {
        step[branch] = build(Array.isArray(item[branch]) ? item[branch] : [], depth + 1)
      })
      return step
    })
  }
  return build(data.pasos, 0)
}

export function projectText(steps, mode) {
  const strip = (list) =>
    list.map((step) => {
      const copy = { ...step }
      delete copy.id
      STEP_TYPES[step.type].branches?.forEach((branch) => {
        copy[branch] = strip(step[branch])
      })
      return copy
    })
  return `${JSON.stringify({ herramienta: mode, version: 1, pasos: strip(steps) }, null, 2)}\n`
}

// Texto de un paso en el DFD (también nombra el paso en los errores).
export function stepText(step) {
  switch (step.type) {
    case 'leer':
      return `Leer ${step.vars}`
    case 'escribir':
      return `Escribir ${step.expr}`
    case 'asignar':
      return `${step.var} ← ${step.expr}`
    case 'si':
      return `¿${step.cond}?`
    case 'mientras':
      return `Mientras ${step.cond}`
    case 'para':
      return `Para ${step.var} ← ${step.from} hasta ${step.to}${step.step.trim() === '1' ? '' : ` paso ${step.step}`}`
    case 'repetir':
      return `¿${step.cond}?`
    default:
      return step.type
  }
}
