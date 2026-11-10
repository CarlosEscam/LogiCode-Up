import { useEffect, useMemo, useState } from 'react'
import { errorLineOf } from '../pseint/errorLine.js'
import { change, move, remove } from '../games/blocks.js'
import FileBar from './FileBar.jsx'
import FlowDiagram from './FlowDiagram.jsx'
import PseintHighlight from './highlight.jsx'
import RunConsole from './RunConsole.jsx'
import { DRAG_TYPE, STEP_TYPES, checkFlow, findStep, insertAt, newStep, parseProject, projectText, stepText, toPseint } from './flow.js'
import { MODES, initialSteps } from './modes.js'
import { writeDraft } from './storage.js'
import { statusText, useConsole } from './useConsole.js'

const mode = MODES.dfd
const ZOOMS = [0.5, 0.75, 1, 1.25, 1.5]

// Figura de la paleta: la misma forma y el mismo color que en el lienzo.
function PaletteIcon({ type }) {
  const kind = { leer: 'io', escribir: 'io', asignar: 'process', si: 'decision', mientras: 'loop', para: 'loop', repetir: 'loop' }[type]
  const className = `dfd__shape dfd__shape--${kind}`
  let outline
  if (type === 'leer' || type === 'escribir') outline = <polygon points="8,2 46,2 40,22 2,22" className={className} />
  else if (type === 'para') outline = <polygon points="8,2 40,2 46,12 40,22 8,22 2,12" className={className} />
  else if (type === 'asignar') outline = <rect x="2" y="2" width="44" height="20" rx="3" className={className} />
  else outline = <polygon points="24,1 47,12 24,23 1,12" className={className} />
  return (
    <svg viewBox="0 0 48 24" width="48" height="24" aria-hidden="true" focusable="false">
      {outline}
    </svg>
  )
}

// Emulador de DFD con aspecto de editor de diagramas (como freeDFDWeb), con los colores de LogiCode: paleta de figuras a
// la izquierda, lienzo en el centro, pseudocódigo en vivo a la derecha y panel de ejecución abajo. La figura se arrastra
// a un «+» del diagrama (o se elige y se hace clic en el «+»); clic en una figura para editarla. Al ejecutar, el
// diagrama se traduce a PSeInt y corre en el intérprete del servidor; Leer pide el dato en la consola.
export default function DfdEmulator() {
  const [steps, setSteps] = useState(() => initialSteps(mode))
  const [selectedId, setSelectedId] = useState(null)
  const [armed, setArmed] = useState(null)
  const [zoom, setZoom] = useState(1)
  const [showCode, setShowCode] = useState(true)
  const [errorId, setErrorId] = useState(null)
  const [notice, setNotice] = useState(null)
  const { code, lineSteps } = useMemo(() => toPseint(steps), [steps])
  const selected = selectedId ? findStep(steps, selectedId) : null
  const describe = (id) => {
    const step = id && findStep(steps, id)
    return step ? `En «${stepText(step)}»` : 'En el programa'
  }
  const run = useConsole(code, (result) => {
    const line = errorLineOf(result.error)
    if (line) setErrorId(lineSteps[line] ?? null)
  })

  useEffect(() => writeDraft(mode.key, projectText(steps, mode.key)), [steps])

  const edit = (next) => {
    setSteps(next)
    setErrorId(null)
  }

  function start() {
    const problem = checkFlow(steps)
    setErrorId(problem?.id ?? null)
    if (problem) setSelectedId(problem.id)
    run.start(problem ? `${describe(problem.id)}: ${problem.message}` : undefined)
  }

  function insert(owner, index, droppedType) {
    const type = droppedType && STEP_TYPES[droppedType] ? droppedType : armed
    if (!type) {
      setNotice('Primero elige una figura de la paleta (o arrástrala hasta el +).')
      return
    }
    const step = newStep(type)
    edit(insertAt(steps, owner, index, step))
    setSelectedId(step.id)
    setArmed(null)
    setNotice(null)
  }

  const describeSlot = (owner, index) => {
    if (!owner) return `programa principal, posición ${index + 1}`
    const step = findStep(steps, owner.id)
    return `rama «${mode.labels[step.type].branches[owner.branch]}» de ${stepText(step)}, posición ${index + 1}`
  }

  return (
    <>
      <p>
        Arrastra una figura de la paleta hasta un <strong>+</strong> del diagrama (o elígela y haz clic en el <strong>+</strong>). Haz clic en una
        figura para escribir lo que hace. Pulsa <strong>▶ Ejecutar</strong> (o F5): cuando llegue a un Leer, escribe el dato en la consola.
      </p>
      <div
        className="dfd-ide"
        onKeyDown={(event) => {
          if (event.key === 'F5') {
            event.preventDefault()
            if (!run.busy) start()
          }
        }}
      >
        <div className="dfd-ide__toolbar">
          <FileBar
            idPrefix="dfd"
            extension=".json"
            defaultName={mode.defaultName}
            getText={() => projectText(steps, mode.key)}
            onOpen={(text) => {
              try {
                edit(parseProject(text, mode.key))
                setSelectedId(null)
                return null
              } catch (error) {
                return error.message
              }
            }}
            onNew={() => {
              edit([])
              setSelectedId(null)
            }}
          />
          <div className="toolbar dfd-ide__view" aria-label="Vista">
            <button type="button" className="button button--secondary button--small" aria-label="Alejar" disabled={zoom === ZOOMS[0]} onClick={() => setZoom(ZOOMS[ZOOMS.indexOf(zoom) - 1])}>
              −
            </button>
            <button type="button" className="button button--secondary button--small" aria-label="Zoom al 100 %" onClick={() => setZoom(1)}>
              {Math.round(zoom * 100)} %
            </button>
            <button type="button" className="button button--secondary button--small" aria-label="Acercar" disabled={zoom === ZOOMS.at(-1)} onClick={() => setZoom(ZOOMS[ZOOMS.indexOf(zoom) + 1])}>
              +
            </button>
            <button type="button" className="button button--secondary button--small" aria-pressed={showCode} onClick={() => setShowCode(!showCode)}>
              Pseudocódigo
            </button>
          </div>
          <div className="ide__run">
            {run.busy && (
              <button type="button" className="button button--secondary" onClick={run.stop}>
                ■ Detener
              </button>
            )}
            <button type="button" className="button ide__play" disabled={run.status === 'running'} onClick={start}>
              ▶ Ejecutar
            </button>
          </div>
        </div>
        <div className={`dfd-ide__layout${showCode ? '' : ' dfd-ide__layout--no-code'}`}>
          <section className="dfd-palette" aria-labelledby="dfd-paleta">
            <h2 id="dfd-paleta" className="dfd-ide__title">
              Figuras
            </h2>
            <ul>
              {mode.palette.map((type) => (
                <li key={type}>
                  <button
                    type="button"
                    className="dfd-palette__item"
                    draggable
                    aria-pressed={armed === type}
                    onDragStart={(event) => {
                      event.dataTransfer.setData(DRAG_TYPE, type)
                      event.dataTransfer.effectAllowed = 'copy'
                      setArmed(type)
                    }}
                    onClick={() => {
                      setArmed(armed === type ? null : type)
                      setNotice(null)
                    }}
                  >
                    <PaletteIcon type={type} />
                    <span>{mode.labels[type].button}</span>
                  </button>
                </li>
              ))}
            </ul>
            <p className="page__note">{armed ? `Ahora haz clic en un + del diagrama para poner «${mode.labels[armed].button}».` : 'Arrástrala al + donde va.'}</p>
          </section>
          <div className="dfd-canvas">
            {notice && (
              <p className="notice notice--error" role="status">
                {notice}
              </p>
            )}
            <div className="dfd-canvas__scroll">
              <FlowDiagram
                steps={steps}
                errorId={errorId}
                selectedId={selectedId}
                onSelect={(id) => setSelectedId(id === selectedId ? null : id)}
                onInsert={insert}
                armed={Boolean(armed)}
                zoom={zoom}
                describeSlot={describeSlot}
              />
            </div>
            {selected && (
              <StepProperties
                step={selected}
                onChange={(values) => edit(change(steps, selected.id, values))}
                onMove={(delta) => edit(move(steps, selected.id, delta))}
                onRemove={() => {
                  edit(remove(steps, selected.id))
                  setSelectedId(null)
                }}
                onClose={() => setSelectedId(null)}
              />
            )}
          </div>
          {showCode && (
            <section className="dfd-code" aria-labelledby="dfd-pseudocodigo">
              <h2 id="dfd-pseudocodigo" className="dfd-ide__title">
                Pseudocódigo (PSeInt)
              </h2>
              <pre className="code dfd-code__text">
                <PseintHighlight text={code} />
              </pre>
            </section>
          )}
        </div>
        <div className="dfd-ide__exec">
          <RunConsole
            run={run}
            idPrefix="dfd"
            hint="Pulsa ▶ Ejecutar para correr el diagrama."
            formatError={(error) => {
              const line = errorLineOf(error)
              return line ? error.replace(/^Línea \d+:/, `${describe(lineSteps[line])}:`) : error
            }}
          />
        </div>
        <div className="ide__status">
          <span>{statusText(run)}</span>
          <span>
            {countSteps(steps)} {countSteps(steps) === 1 ? 'figura' : 'figuras'}
          </span>
          <span>DFD · LogiCode</span>
        </div>
      </div>
    </>
  )
}

function countSteps(steps) {
  return steps.reduce((total, step) => total + 1 + countSteps(step.body ?? []) + countSteps(step.else ?? []), 0)
}

// Panel de la figura elegida: lo que hace (sus campos) y moverla o quitarla.
function StepProperties({ step, onChange, onMove, onRemove, onClose }) {
  const label = mode.labels[step.type]
  return (
    <section className="dfd-properties" aria-labelledby="dfd-propiedades">
      <h3 id="dfd-propiedades">{label.button}</h3>
      <div className="dfd-properties__fields">
        {label.phrase.map((part, index) =>
          typeof part === 'string' ? (
            <span key={index} className="step__word">
              {part}
            </span>
          ) : (
            <label key={index} className="dfd-properties__field">
              <span className="visually-hidden">{`${label.button}: ${part.name}`}</span>
              <input
                className="step__input code"
                placeholder={part.placeholder}
                value={step[part.field]}
                size={Math.max(part.size ?? 6, String(step[part.field]).length + 1)}
                maxLength={500}
                spellCheck={false}
                autoFocus={index === label.phrase.findIndex((item) => typeof item !== 'string')}
                onChange={(event) => onChange({ [part.field]: event.target.value })}
              />
            </label>
          ),
        )}
      </div>
      <p className="page__note">Los textos van entre comillas ("Hola") y se unen con coma ("Hola ", nombre).</p>
      <div className="toolbar">
        <button type="button" className="button button--secondary button--small" onClick={() => onMove(-1)}>
          ↑ Subir
        </button>
        <button type="button" className="button button--secondary button--small" onClick={() => onMove(1)}>
          ↓ Bajar
        </button>
        <button type="button" className="button button--danger button--small" onClick={onRemove}>
          Quitar figura
        </button>
        <button type="button" className="button button--secondary button--small" onClick={onClose}>
          Listo
        </button>
      </div>
    </section>
  )
}
