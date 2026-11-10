import { useEffect, useMemo, useRef, useState } from 'react'
import Solved from './Solved.jsx'
import { BLOCK_LABELS, change, contains, countBlocks, insert, parseLevel, remove, runProgram } from './blocks.js'
import { loadSkin, saveSkin } from './progress.js'
import { RobotSkin, SkinPicker } from './skins.jsx'

const CONTAINERS = new Set(['repetir', 'si', 'mientras'])
const DIRECTION_NAMES = { N: 'arriba', E: 'a la derecha', S: 'abajo', O: 'a la izquierda' }
const OUTCOME_TEXT = {
  crash: 'El robot chocó. Revisa el bloque resaltado.',
  ended: 'El programa terminó, pero el robot no llegó a la meta.',
  limit: 'El robot dio demasiados pasos sin llegar: revisa tus ciclos.',
}
const SPEED_MS = 350

let nextId = 0
const newBlock = (type) => ({
  id: `b${nextId++}`,
  type,
  ...(type === 'repetir' && { times: 2 }),
  ...(CONTAINERS.has(type) && { body: [] }),
  ...(type === 'si' && { else: [] }),
})
// Copia el programa inicial de un nivel con ids nuevos.
const copy = (blocks) =>
  blocks.map((block) => {
    const base = newBlock(block.type)
    return { ...base, ...block, id: base.id, body: block.body ? copy(block.body) : base.body, else: block.else ? copy(block.else) : base.else }
  })

// También se usa en los retos de bloques de una actividad (RF-12): sin título, y avisando cada cambio del programa.
export default function BlocksGame({ level, onSolved, onNext, onProgramChange }) {
  const [program, setProgram] = useState(() => copy(level.start ?? []))
  const [target, setTarget] = useState(null)
  const [run, setRun] = useState(null) // { result, step, playing }
  const [skin, setSkin] = useState(loadSkin)
  const { grid, start } = useMemo(() => parseLevel(level), [level])
  const used = countBlocks(program)
  const full = used >= level.maxBlocks

  const finished = run && run.step >= run.result.trace.length - 1
  const robot = run ? run.result.trace[run.step] : start
  const visited = new Set(run ? run.result.trace.slice(0, run.step + 1).map((state) => `${state.r},${state.c}`) : [])

  useEffect(() => {
    if (!run?.playing || finished) return undefined
    const timer = setTimeout(() => setRun((current) => ({ ...current, step: current.step + 1 })), SPEED_MS)
    return () => clearTimeout(timer)
  }, [run, finished])

  useEffect(() => {
    onProgramChange?.(program)
  }, [program, onProgramChange])

  const won = finished && run.result.outcome === 'goal'
  const reported = useRef(false)
  useEffect(() => {
    if (won && !reported.current) {
      reported.current = true
      onSolved?.()
    }
  }, [won, onSolved])

  function edit(next) {
    setProgram(next)
    setRun(null)
  }

  function begin(playing) {
    const result = runProgram(program, level)
    setRun({ result, step: playing ? 0 : Math.min(1, result.trace.length - 1), playing })
  }

  return (
    <section className={level.title ? 'card card--wide' : undefined} aria-labelledby={level.title ? 'nivel-titulo' : undefined} aria-label={level.title ? undefined : 'Reto de bloques'}>
      {level.title && (
        <h2 id="nivel-titulo">
          {level.topic}: {level.title}
        </h2>
      )}
      {level.goal && <p>{level.goal}</p>}
      <div className="blocks-game">
        <div>
          <div className="robot-grid" style={{ gridTemplateColumns: `repeat(${grid[0].length}, 2.5rem)` }} role="img" aria-label={`Tablero de ${grid.length} por ${grid[0].length}`}>
            {grid.flatMap((row, r) =>
              row.map((cell, c) => {
                const here = robot.r === r && robot.c === c
                return (
                  <div
                    key={`${r},${c}`}
                    className={`robot-cell robot-cell--${cell === '#' ? 'wall' : cell === 'M' ? 'goal' : 'free'}${visited.has(`${r},${c}`) ? ' robot-cell--visited' : ''}${here && robot.crashed ? ' robot-cell--crash' : ''}`}
                  >
                    {here ? <RobotSkin skin={skin} dir={robot.dir} /> : cell === 'M' ? '★' : ''}
                  </div>
                )
              }),
            )}
          </div>
          <p className="page__note" role="status">
            Robot en la fila {robot.r + 1}, columna {robot.c + 1}, mirando {DIRECTION_NAMES[robot.dir]}. La meta es la estrella.
          </p>
          <SkinPicker
            value={skin}
            onChange={(next) => {
              setSkin(next)
              saveSkin(next)
            }}
          />
        </div>
        <div>
          <h3>Bloques ({used} de {level.maxBlocks})</h3>
          <p className="page__note">
            Agregando en: <strong>{target ? describeTarget(program, target) : 'el programa principal'}</strong>
            {target && (
              <button type="button" className="button button--secondary button--small" onClick={() => setTarget(null)}>
                Volver al principal
              </button>
            )}
          </p>
          <div className="toolbar" aria-label="Bloques disponibles">
            {level.allowed.map((type) => (
              <button
                key={type}
                type="button"
                className={`button button--small block-button block-button--${type}`}
                disabled={full}
                onClick={() => edit(insert(program, target, newBlock(type)))}
              >
                + {BLOCK_LABELS[type]}
              </button>
            ))}
          </div>
          {program.length === 0 ? (
            <p>El programa está vacío. Agrega bloques con los botones.</p>
          ) : (
            <BlockList
              blocks={program}
              active={run ? robot.blockId : null}
              target={target}
              onTarget={setTarget}
              onChange={(id, update) => edit(change(program, id, update))}
              onRemove={(id) => {
                if (target && (target.id === id || !contains(remove(program, id), target.id))) setTarget(null)
                edit(remove(program, id))
              }}
            />
          )}
          <div className="toolbar">
            <button type="button" className="button" disabled={program.length === 0} onClick={() => begin(true)}>
              Ejecutar
            </button>
            <button
              type="button"
              className="button button--secondary"
              disabled={program.length === 0 || (run && finished)}
              onClick={() => (run && !run.playing ? setRun({ ...run, step: run.step + 1 }) : begin(false))}
            >
              Paso a paso
            </button>
            <button type="button" className="button button--secondary" disabled={!run} onClick={() => setRun(null)}>
              Reiniciar
            </button>
          </div>
          {finished && won &&
            (level.title ? (
              <Solved onNext={onNext}>¡El robot llegó a la meta con {used} bloques!</Solved>
            ) : (
              <p className="notice notice--ok" role="status">
                ¡El robot llegó a la meta con {used} bloques!
              </p>
            ))}
          {finished && !won && (
            <p className="notice notice--error" role="status">
              {OUTCOME_TEXT[run.result.outcome]}
            </p>
          )}
        </div>
      </div>
    </section>
  )
}

function describeTarget(program, target) {
  const find = (blocks) => {
    for (const block of blocks) {
      if (block.id === target.id) return block
      const inner = find(block.body ?? []) ?? find(block.else ?? [])
      if (inner) return inner
    }
    return null
  }
  const block = find(program)
  if (!block) return 'el programa principal'
  if (block.type === 'si') return target.branch === 'else' ? '"si no" del Si' : '"entonces" del Si'
  return `dentro de ${BLOCK_LABELS[block.type]}`
}

function BlockList({ blocks, active, target, onTarget, onChange, onRemove }) {
  return (
    <ol className="block-list">
      {blocks.map((block) => (
        <li key={block.id} className={`block block--${block.type}${block.id === active ? ' block--active' : ''}`}>
          <div className="block__row">
            <span>{BLOCK_LABELS[block.type]}</span>
            {block.type === 'repetir' && (
              <select aria-label="Veces" value={block.times} onChange={(event) => onChange(block.id, { times: Number(event.target.value) })}>
                {Array.from({ length: 11 }, (_, i) => i + 2).map((n) => (
                  <option key={n} value={n}>
                    {n} veces
                  </option>
                ))}
              </select>
            )}
            <button type="button" className="button button--danger button--small" aria-label={`Quitar ${BLOCK_LABELS[block.type]}`} onClick={() => onRemove(block.id)}>
              Quitar
            </button>
          </div>
          {CONTAINERS.has(block.type) && (
            <Branch block={block} branch="body" label={block.type === 'si' ? 'Entonces' : 'Hacer'} {...{ active, target, onTarget, onChange, onRemove }} />
          )}
          {block.type === 'si' && <Branch block={block} branch="else" label="Si no" {...{ active, target, onTarget, onChange, onRemove }} />}
        </li>
      ))}
    </ol>
  )
}

function Branch({ block, branch, label, active, target, onTarget, onChange, onRemove }) {
  const selected = target?.id === block.id && target.branch === branch
  return (
    <div className={`block__branch${selected ? ' block__branch--target' : ''}`}>
      <button type="button" className="button button--secondary button--small" aria-pressed={selected} onClick={() => onTarget({ id: block.id, branch })}>
        {label}: agregar aquí
      </button>
      {block[branch].length > 0 && <BlockList blocks={block[branch]} {...{ active, target, onTarget, onChange, onRemove }} />}
    </div>
  )
}
