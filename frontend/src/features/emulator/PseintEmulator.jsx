import { useEffect, useRef, useState } from 'react'
import { apiPost } from '../../shared/api/httpClient.js'
import { Verdict } from '../pseint/PseintRunner.jsx'
import { ERROR_KIND_LABELS, errorLineOf } from '../pseint/errorLine.js'
import CodeEditor from './CodeEditor.jsx'
import FileBar from './FileBar.jsx'
import PseintHighlight from './highlight.jsx'
import { useDraft } from './storage.js'

const EXAMPLES = {
  saludo: `Algoritmo Saludo
    Definir nombre Como Caracter
    Escribir "¿Cómo te llamas?"
    Leer nombre
    Escribir "Hola, ", nombre
FinAlgoritmo`,
  promedio: `Algoritmo Promedio
    Definir n, i Como Entero
    Definir nota, suma Como Real
    Leer n
    suma <- 0
    Para i <- 1 Hasta n Hacer
        Leer nota
        suma <- suma + nota
    FinPara
    Escribir "Promedio: ", suma / n
FinAlgoritmo`,
  mayor: `Algoritmo Mayor
    Definir a, b Como Entero
    Leer a, b
    Si a > b Entonces
        Escribir a
    SiNo
        Escribir b
    FinSi
FinAlgoritmo`,
  paraCada: `Algoritmo Notas
    Dimension notas[4]
    Para i <- 1 Hasta 4 Hacer
        Leer notas[i]
    FinPara
    suma <- 0
    Para Cada nota De notas Hacer
        suma <- suma + nota
    FinPara
    Escribir "Promedio: ", suma / 4
FinAlgoritmo`,
}

const EXAMPLE_LABELS = {
  saludo: 'Saludo (Leer y Escribir)',
  promedio: 'Promedio de notas (Para)',
  mayor: 'El mayor de dos números (Si)',
  paraCada: 'Promedio de un arreglo (Para Cada)',
}

const NEEDS_INPUT = /faltan datos de entrada/
const MAX_READS = 200

// Corre el programa en el servidor como si la consola fuera interactiva. El servidor necesita todas las entradas de una
// vez, así que cuando el programa llega a un Leer sin dato se muestra lo que alcanzó a escribir, la consola pide el dato
// y se vuelve a ejecutar con todos los datos dados. El intérprete es determinista (AZAR tiene semilla fija), así que cada
// ejecución repite la salida anterior y sigue desde ahí.
function useConsole(code, onDone) {
  const [state, setState] = useState({ status: 'idle', reads: [], result: null, error: null })
  const turn = useRef(0) // descarta la respuesta de una ejecución que se detuvo o se reemplazó

  async function execute(reads, fresh) {
    const mine = ++turn.current
    setState((current) => ({ ...current, ...(fresh && { result: null }), status: 'running', reads, error: null }))
    try {
      const result = await apiPost('/pseint/runs', { code, input: reads.map((read) => read.value).join('\n') })
      if (mine !== turn.current) return
      const waiting = result.errorKind === 'RUNTIME' && NEEDS_INPUT.test(result.error ?? '') && reads.length < MAX_READS
      setState({ status: waiting ? 'waiting' : 'done', reads, result, error: null })
      if (!waiting) onDone(result)
    } catch (error) {
      if (mine !== turn.current) return
      setState({ status: 'idle', reads, result: null, error: error instanceof TypeError ? 'No fue posible conectar con el servidor. Intenta de nuevo.' : error.message })
    }
  }

  return {
    ...state,
    start: () => (code.trim() ? execute([], true) : setState({ status: 'idle', reads: [], result: null, error: 'Escribe el programa antes de ejecutarlo.' })),
    answer: (value) => execute([...state.reads, { value, at: state.result.output.length }]),
    stop: () => {
      turn.current += 1
      setState((current) => ({ ...current, status: current.result ? 'stopped' : 'idle' }))
    },
  }
}

// Emulador de PSeInt (RF-12) con aspecto de editor: código con colores a la izquierda y consola a la derecha, donde
// Leer pide el dato. Código libre, sin nota ni entrega. El servidor ejecuta con límites de tiempo, pasos, memoria y
// salida; el intérprete no tiene acceso a archivos ni a la red.
export default function PseintEmulator() {
  const [code, setCode] = useDraft('pseint', EXAMPLES.saludo)
  const [errorLine, setErrorLine] = useState(null)
  const [position, setPosition] = useState([1, 1])
  const [answer, setAnswer] = useState('')
  const answerInput = useRef(null)
  const run = useConsole(code, (result) => setErrorLine(errorLineOf(result.error)))
  const busy = run.status === 'running' || run.status === 'waiting'

  useEffect(() => {
    if (run.status === 'waiting') answerInput.current?.focus()
  }, [run.status])

  const edit = (next) => {
    setCode(next)
    setErrorLine(null)
  }
  const start = () => {
    setErrorLine(null)
    setAnswer('')
    run.start()
  }

  // El texto de la consola: lo que escribió el programa, con cada dato que dio la persona en su lugar.
  const pieces = []
  if (run.result || run.reads.length > 0) {
    let shown = 0
    const output = run.result?.output ?? ''
    run.reads.forEach((read, index) => {
      pieces.push(<span key={`o${index}`}>{output.slice(shown, read.at)}</span>)
      pieces.push(
        <span key={`i${index}`} className="ide-console__typed">
          {read.value}
          {'\n'}
        </span>,
      )
      shown = read.at
    })
    pieces.push(<span key="fin">{run.status === 'running' ? output.slice(shown, shown) : output.slice(shown)}</span>)
  }
  const result = run.status === 'done' ? run.result : null
  const statusText = {
    idle: 'Intérprete listo',
    running: 'Ejecutando…',
    waiting: 'Esperando un dato en la consola',
    done: result && !result.errorKind ? `Ejecución exitosa · ${result.steps} pasos · ${result.durationMs} ms` : 'El programa se detuvo por un error',
    stopped: 'Ejecución detenida',
  }[run.status]

  return (
    <>
      <p>
        Escribe tu algoritmo y pulsa <strong>▶ Ejecutar</strong> (o F9). Cuando el programa llegue a un <code>Leer</code>, escribe el dato en
        la consola y pulsa Enter. Es el mismo intérprete que califica las actividades con casos de prueba.
      </p>
      <div
        className="ide"
        onKeyDown={(event) => {
          if (event.key === 'F9' || (event.key === 'Enter' && (event.ctrlKey || event.metaKey))) {
            event.preventDefault()
            if (!busy) start()
          }
        }}
      >
        <div className="ide__toolbar">
          <FileBar
            idPrefix="pseint"
            extension=".psc"
            defaultName="mi-algoritmo"
            getText={() => code}
            onOpen={(text) => edit(text)}
            onNew={() => edit('Algoritmo SinTitulo\n    \nFinAlgoritmo\n')}
          />
          <div className="field ide__examples">
            <label htmlFor="pseint-ejemplo">Ejemplos</label>
            <select
              id="pseint-ejemplo"
              value=""
              onChange={(event) => {
                const key = event.target.value
                if (key && (Object.values(EXAMPLES).includes(code) || window.confirm('El ejemplo reemplaza lo que hay en el editor. ¿Continuar?'))) {
                  edit(EXAMPLES[key])
                }
              }}
            >
              <option value="">Cargar…</option>
              {Object.entries(EXAMPLE_LABELS).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="ide__run">
            {busy && (
              <button type="button" className="button button--secondary" onClick={run.stop}>
                ■ Detener
              </button>
            )}
            <button type="button" className="button ide__play" disabled={run.status === 'running'} onClick={start}>
              ▶ Ejecutar
            </button>
          </div>
        </div>
        <div className="ide__panes">
          <CodeEditor
            id="pseint-codigo"
            label="Programa (.psc)"
            hideLabel
            value={code}
            onChange={edit}
            errorLine={errorLine}
            rows={20}
            Highlight={PseintHighlight}
            onCursor={(line, column) => setPosition([line, column])}
          />
          <section className="ide-console" aria-labelledby="pseint-consola">
            <h2 id="pseint-consola" className="ide-console__title">
              Consola de salida
            </h2>
            <div className="ide-console__screen" role="log" aria-live="polite" aria-label="Consola de salida">
              {run.status === 'idle' && !run.error && <span className="ide-console__hint">Pulsa ▶ Ejecutar para correr tu algoritmo.</span>}
              {run.status !== 'idle' && <span className="ide-console__hint">▶ Ejecutando…{'\n'}</span>}
              {pieces}
              {run.status === 'waiting' && (
                <form
                  className="ide-console__ask"
                  onSubmit={(event) => {
                    event.preventDefault()
                    run.answer(answer)
                    setAnswer('')
                  }}
                >
                  <label htmlFor="pseint-dato" className="ide-console__prompt">
                    &gt;<span className="visually-hidden"> Dato para Leer</span>
                  </label>
                  <input
                    ref={answerInput}
                    id="pseint-dato"
                    className="ide-console__input"
                    value={answer}
                    maxLength={500}
                    autoComplete="off"
                    spellCheck={false}
                    onChange={(event) => setAnswer(event.target.value)}
                  />
                </form>
              )}
            </div>
            <div role="status" aria-label="Resultado de la ejecución">
              {result && <Verdict errorKind={result.errorKind} />}
              {result?.error && (
                <p className="notice notice--error">
                  <strong>{ERROR_KIND_LABELS[result.errorKind] ?? 'Error'}:</strong> {result.error}
                </p>
              )}
              {run.error && <p className="notice notice--error">{run.error}</p>}
            </div>
          </section>
        </div>
        <div className="ide__status">
          <span>{statusText}</span>
          <span>
            Línea {position[0]}, Col {position[1]}
          </span>
          <span>PSeInt · LogiCode</span>
        </div>
      </div>
      <details>
        <summary>Qué entiende el intérprete</summary>
        <p>
          Algoritmo/Proceso, Definir, Dimension (posiciones 0 a n), Leer, Escribir (con Sin Saltar), asignación con &lt;- o =, Si,
          Segun, Mientras, Repetir, Para con Paso, Para Cada (recorre un arreglo), SubProceso/Funcion con parámetros por valor o por referencia, operadores Y, O, NO,
          MOD y ^, y funciones como RC, ABS, TRUNC, REDON, AZAR, LONGITUD, SUBCADENA, MAYUSCULAS y CONVERTIRANUMERO.
        </p>
        <p>Límites por ejecución: 2 segundos, un millón de pasos, 20 000 caracteres de salida y 10 000 posiciones en arreglos.</p>
      </details>
    </>
  )
}
