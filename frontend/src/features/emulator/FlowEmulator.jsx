import { useEffect, useMemo, useState } from 'react'
import { errorLineOf } from '../pseint/errorLine.js'
import PseintRunner from '../pseint/PseintRunner.jsx'
import FileBar from './FileBar.jsx'
import FlowDiagram from './FlowDiagram.jsx'
import StepEditor from './StepEditor.jsx'
import { checkFlow, findStep, newStep, parseProject, projectText, stepText, toPseint } from './flow.js'
import { readDraft, writeDraft } from './storage.js'

const field = (name, fieldKey, placeholder, size) => ({ name, field: fieldKey, placeholder, size })

// Cada modo nombra los pasos a su manera; el programa y la ejecución son los mismos (flow.js).
const MODES = {
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

function initial(mode) {
  try {
    const saved = readDraft(mode.key, null)
    if (saved) return parseProject(saved, mode.key)
  } catch {
    // Borrador dañado: se empieza con el ejemplo.
  }
  return mode.start.map(([type, values]) => newStep(type, values))
}

// Emulador de DFD y de Scratch (bloques): el estudiante arma el programa, LogiCode lo traduce a PSeInt y lo ejecuta en el
// servidor. Sin nota ni entrega; el proyecto se guarda en el navegador y, si quiere, en un archivo .json.
export default function FlowEmulator({ mode: modeKey }) {
  const mode = MODES[modeKey]
  const [steps, setSteps] = useState(() => initial(mode))
  const [errorId, setErrorId] = useState(null)
  const { code, lineSteps } = useMemo(() => toPseint(steps), [steps])
  const problem = checkFlow(steps)
  const describe = (id) => {
    const step = findStep(steps, id)
    return step ? `En «${mode.text(step)}»` : 'En el programa'
  }

  useEffect(() => writeDraft(mode.key, projectText(steps, mode.key)), [steps, mode.key])

  const edit = (next) => {
    setSteps(next)
    setErrorId(null)
  }

  return (
    <>
      <p>
        {mode.key === 'dfd'
          ? 'Arma el diagrama de flujo con los pasos de abajo: se dibuja solo. Al ejecutarlo, LogiCode lo convierte a PSeInt y lo corre.'
          : 'Arma tu programa con bloques, como en Scratch: preguntar, decir, variables, condiciones y ciclos. Al ejecutarlo verás lo que dice el programa.'}{' '}
        Los textos van entre comillas (<code>"Hola"</code>) y se unen con coma (<code>"Hola ", nombre</code>).
      </p>
      <FileBar
        idPrefix={mode.key}
        extension=".json"
        defaultName={mode.defaultName}
        getText={() => projectText(steps, mode.key)}
        onOpen={(text) => {
          try {
            edit(parseProject(text, mode.key))
            return null
          } catch (error) {
            return error.message
          }
        }}
        onNew={() => edit([])}
      />
      <div className={mode.key === 'dfd' ? 'flow-emulator' : undefined}>
        <div>
          <h2>{mode.key === 'dfd' ? 'Pasos del diagrama' : 'Bloques'}</h2>
          <StepEditor mode={mode} steps={steps} onChange={edit} errorId={errorId} />
        </div>
        {mode.key === 'dfd' && (
          <div>
            <h2>Diagrama</h2>
            <FlowDiagram steps={steps} errorId={errorId} />
          </div>
        )}
      </div>
      <PseintRunner
        code={code}
        idPrefix={`${mode.key}-ejecutar`}
        problem={problem ? `${describe(problem.id)}: ${problem.message}` : null}
        onResult={(result) => {
          if (problem) setErrorId(problem.id)
          else setErrorId(lineSteps[errorLineOf(result.error)] ?? null)
        }}
        formatError={(error) => {
          const line = errorLineOf(error)
          return line ? error.replace(/^Línea \d+:/, `${describe(lineSteps[line])}:`) : error
        }}
      />
      <details>
        <summary>Ver el programa en PSeInt</summary>
        <pre className="console">{code}</pre>
      </details>
    </>
  )
}
