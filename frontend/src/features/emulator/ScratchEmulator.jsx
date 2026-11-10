import { useEffect, useMemo, useRef, useState } from 'react'
import { insert } from '../games/blocks.js'
import { errorLineOf } from '../pseint/errorLine.js'
import FileBar from './FileBar.jsx'
import RunConsole from './RunConsole.jsx'
import StepEditor from './StepEditor.jsx'
import { DRAG_TYPE, checkFlow, findStep, newStep, parseProject, projectText, toPseint } from './flow.js'
import { MODES, initialSteps } from './modes.js'
import { STAGE, eventDelay, isMark, stageAt, stageEvents } from './scratchStage.js'
import { writeDraft } from './storage.js'
import { useConsole } from './useConsole.js'

const mode = MODES.scratch
const cleanOutput = (text) =>
  text
    .split('\n')
    .filter((line) => !isMark(line))
    .join('\n')

// Emulador de Scratch organizado como el editor de Scratch, con los colores y un personaje propios de LogiCode:
// categorías, paleta de bloques, área del programa y escenario. Los bloques se arrastran al programa (o se hace clic en
// ellos). Al pulsar ▶ el programa se traduce a PSeInt y corre en el intérprete del servidor; el escenario muestra al
// personaje moviéndose y diciendo lo que el programa escribe, y «preguntar» pide la respuesta en la consola.
export default function ScratchEmulator() {
  const [steps, setSteps] = useState(() => initialSteps(mode))
  const [target, setTarget] = useState(null)
  const [errorId, setErrorId] = useState(null)
  const [shown, setShown] = useState(0)
  const palette = useRef(null)
  const { code, lineSteps } = useMemo(() => toPseint(steps), [steps])
  const run = useConsole(code, (result) => {
    const line = errorLineOf(result.error)
    if (line) setErrorId(lineSteps[line] ?? null)
  })
  const events = useMemo(() => stageEvents(run.result?.output ?? ''), [run.result])
  const sprite = stageAt(events, shown)
  const describe = (id) => {
    const step = id && findStep(steps, id)
    return step ? `En «${mode.text(step)}»` : 'En el programa'
  }

  useEffect(() => writeDraft(mode.key, projectText(steps, mode.key)), [steps])

  // Muestra los eventos uno a uno, con la pausa de cada uno (movimiento visible, «esperar» real).
  useEffect(() => {
    if (run.status === 'stopped' || shown >= events.length) return undefined
    const timer = setTimeout(() => setShown((count) => count + 1), eventDelay(events[shown]))
    return () => clearTimeout(timer)
  }, [shown, events, run.status])

  const edit = (next) => {
    setSteps(next)
    setErrorId(null)
  }
  const add = (where, type) => edit(insert(steps, where, newStep(type)))

  function start() {
    const problem = checkFlow(steps)
    setErrorId(problem?.id ?? null)
    setShown(0)
    run.start(problem ? `${describe(problem.id)}: ${problem.message}` : undefined)
  }

  return (
    <>
      <p>
        Arrastra bloques de la izquierda al programa (o haz clic en ellos) y pulsa la bandera <strong>▶</strong>. El personaje se mueve y dice
        lo que escribe tu programa; si pregunta algo, responde en la consola debajo del escenario.
      </p>
      <div className="scratch-ide">
        <div className="scratch-ide__bar">
          <FileBar
            idPrefix="scratch"
            extension=".json"
            defaultName={mode.defaultName}
            getText={() => projectText(steps, mode.key)}
            onOpen={(text) => {
              try {
                edit(parseProject(text, mode.key))
                setTarget(null)
                return null
              } catch (error) {
                return error.message
              }
            }}
            onNew={() => {
              edit([])
              setTarget(null)
            }}
          />
        </div>
        <div className="scratch-ide__layout">
          <nav className="scratch-categories" aria-label="Categorías de bloques">
            {mode.categories.map((category) => (
              <button
                key={category.key}
                type="button"
                className="scratch-categories__item"
                onClick={() => palette.current?.querySelector(`#scratch-cat-${category.key}`)?.scrollIntoView?.({ block: 'start', behavior: 'smooth' })}
              >
                <span className={`scratch-categories__dot step--${category.key}`} aria-hidden="true" />
                {category.label}
              </button>
            ))}
          </nav>
          <section ref={palette} className="scratch-palette" aria-label="Bloques">
            {mode.categories.map((category) => (
              <div key={category.key}>
                <h2 id={`scratch-cat-${category.key}`} className="scratch-palette__title">
                  {category.label}
                </h2>
                <ul>
                  {category.types.map((type) => (
                    <li key={type}>
                      <button
                        type="button"
                        className={`scratch-block step--${category.key}`}
                        draggable
                        aria-label={`Agregar bloque: ${mode.labels[type].button}`}
                        onDragStart={(event) => {
                          event.dataTransfer.setData(DRAG_TYPE, type)
                          event.dataTransfer.effectAllowed = 'copy'
                        }}
                        onClick={() => add(target, type)}
                      >
                        {mode.labels[type].phrase.map((part, index) =>
                          typeof part === 'string' ? (
                            <span key={index}>{part}</span>
                          ) : (
                            <span key={index} className="scratch-block__pill">
                              {part.placeholder}
                            </span>
                          ),
                        )}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </section>
          <section className="scratch-script" aria-labelledby="scratch-programa">
            <h2 id="scratch-programa" className="visually-hidden">
              Programa
            </h2>
            <StepEditor mode={mode} steps={steps} onChange={edit} errorId={errorId} target={target} onTarget={setTarget} onDropType={add} />
          </section>
          <section className="scratch-stage-column" aria-labelledby="scratch-escenario">
            <div className="scratch-stage__bar">
              <h2 id="scratch-escenario" className="visually-hidden">
                Escenario
              </h2>
              <button type="button" className="scratch-flag" aria-label="▶ Ejecutar" title="Ejecutar" disabled={run.status === 'running'} onClick={start}>
                ▶
              </button>
              <button
                type="button"
                className="scratch-stop"
                aria-label="■ Detener"
                title="Detener"
                disabled={run.status === 'stopped' || (!run.busy && shown >= events.length)}
                onClick={run.stop}
              >
                ■
              </button>
            </div>
            <Stage sprite={sprite} />
            <p className="scratch-stage__info" aria-live="off">
              x: {Math.round(sprite.x)} · y: {Math.round(sprite.y)} · dirección: {Math.round(sprite.dir)}
            </p>
            <RunConsole
              run={run}
              idPrefix="scratch"
              hint="Haz clic en ▶ para empezar."
              clean={cleanOutput}
              formatError={(error) => {
                const line = errorLineOf(error)
                return line ? error.replace(/^Línea \d+:/, `${describe(lineSteps[line])}:`) : error
              }}
            />
          </section>
        </div>
      </div>
    </>
  )
}

// Escenario de 480 × 360 con el personaje de LogiCode (un robot con pantalla) y su globo de diálogo.
function Stage({ sprite }) {
  const x = sprite.x + STAGE.width / 2
  const y = STAGE.height / 2 - sprite.y
  const lines = wrap(sprite.bubble, 26).slice(0, 4)
  const bubbleWidth = Math.max(60, Math.max(...lines.map((line) => line.length), 0) * 7.2 + 20)
  const bubbleHeight = lines.length * 16 + 14
  const bubbleX = Math.min(Math.max(x + 18, 4), STAGE.width - bubbleWidth - 4)
  const bubbleY = Math.max(y - 46 - bubbleHeight, 4)
  return (
    <svg
      className="scratch-stage"
      viewBox={`0 0 ${STAGE.width} ${STAGE.height}`}
      role="img"
      aria-label={`Escenario: el personaje está en x ${Math.round(sprite.x)}, y ${Math.round(sprite.y)}, mirando a ${Math.round(sprite.dir)} grados${sprite.bubble ? ` y dice «${sprite.bubble}»` : ''}.`}
    >
      <rect width={STAGE.width} height={STAGE.height} className="scratch-stage__bg" />
      <path d={`M0 ${STAGE.height - 40} H${STAGE.width}`} className="scratch-stage__floor" />
      <g transform={`translate(${x} ${y}) rotate(${sprite.dir - 90})`}>
        <line x1="0" y1="-30" x2="0" y2="-40" className="logi__antenna" />
        <circle cx="0" cy="-42" r="4" className="logi__light" />
        <rect x="-24" y="-30" width="48" height="44" rx="12" className="logi__body" />
        <rect x="-17" y="-23" width="34" height="22" rx="6" className="logi__screen" />
        <circle cx="-7" cy="-12" r="3.5" className="logi__eye" />
        <circle cx="7" cy="-12" r="3.5" className="logi__eye" />
        <path d="M-6 -5 Q0 -1 6 -5" className="logi__smile" />
        <rect x="-18" y="14" width="10" height="12" rx="3" className="logi__body" />
        <rect x="8" y="14" width="10" height="12" rx="3" className="logi__body" />
        {/* Flecha hacia donde mira (dirección 90 = derecha). */}
        <path d="M24 -8 L32 -8 L28 -14 Z" className="logi__light" />
      </g>
      {lines.length > 0 && (
        <g>
          <rect x={bubbleX} y={bubbleY} width={bubbleWidth} height={bubbleHeight} rx="10" className="scratch-stage__bubble" />
          <path d={`M${Math.min(Math.max(x + 6, bubbleX + 8), bubbleX + bubbleWidth - 8)} ${bubbleY + bubbleHeight} l6 10 l6 -10`} className="scratch-stage__bubble" />
          {lines.map((line, index) => (
            <text key={index} x={bubbleX + 10} y={bubbleY + 19 + index * 16} className="scratch-stage__text">
              {line}
            </text>
          ))}
        </g>
      )}
    </svg>
  )
}

function wrap(text, width) {
  const lines = []
  let current = ''
  for (const word of text.split(/\s+/).filter(Boolean)) {
    if (current && (current + word).length + 1 > width) {
      lines.push(current)
      current = word
    } else current = current ? `${current} ${word}` : word
  }
  if (current) lines.push(current)
  return lines.flatMap((line) => line.match(new RegExp(`.{1,${width}}`, 'g')))
}
