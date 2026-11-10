import { useEffect, useMemo, useState } from 'react'
import { errorLineOf } from '../pseint/errorLine.js'
import PseintRunner from '../pseint/PseintRunner.jsx'
import FileBar from './FileBar.jsx'
import FlowDiagram from './FlowDiagram.jsx'
import StepEditor from './StepEditor.jsx'
import { checkFlow, findStep, parseProject, projectText, toPseint } from './flow.js'
import { MODES, initialSteps } from './modes.js'
import { writeDraft } from './storage.js'


// Emulador de DFD y de Scratch (bloques): el estudiante arma el programa, LogiCode lo traduce a PSeInt y lo ejecuta en el
// servidor. Sin nota ni entrega; el proyecto se guarda en el navegador y, si quiere, en un archivo .json.
export default function FlowEmulator({ mode: modeKey }) {
  const mode = MODES[modeKey]
  const [steps, setSteps] = useState(() => initialSteps(mode))
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
