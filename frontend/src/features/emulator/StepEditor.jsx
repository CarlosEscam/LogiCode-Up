import { change, contains, move, remove } from '../games/blocks.js'
import { DRAG_TYPE, STEP_TYPES } from './flow.js'

// Área del programa del emulador de Scratch: los bloques apilados bajo «al presionar ▶». target es la rama donde van los
// bloques nuevos (null = el programa principal); onDropType(target, tipo) recibe un bloque soltado desde la paleta.
export default function StepEditor({ mode, steps, onChange, errorId, target, onTarget, onDropType }) {
  const tools = { mode, errorId, target, onTarget, onChange, steps, onDropType }
  return (
    <div className={`step-editor step-editor--${mode.key}`} {...dropProps(null, onDropType)}>
      <div className="step step--hat">
        <span className="step__word">al presionar ▶</span>
      </div>
      {steps.length > 0 && <StepList list={steps} {...tools} />}
      <button
        type="button"
        className={`step-editor__end${target ? '' : ' step-editor__end--target'}`}
        aria-pressed={!target}
        onClick={() => onTarget(null)}
      >
        {steps.length === 0 ? 'Arrastra aquí un bloque de la izquierda (o haz clic en él)' : 'Los bloques nuevos van al final del programa'}
      </button>
    </div>
  )
}

// Soltar un bloque de la paleta en una rama (o en el programa principal si target es null).
function dropProps(target, onDropType) {
  if (!onDropType) return {}
  return {
    onDragOver: (event) => {
      if (event.dataTransfer.types.includes(DRAG_TYPE)) event.preventDefault()
    },
    onDrop: (event) => {
      const type = event.dataTransfer.getData(DRAG_TYPE)
      if (!type) return
      event.preventDefault()
      event.stopPropagation()
      onDropType(target, type)
    },
  }
}

function StepList({ list, ...tools }) {
  return (
    <ol className="step-list">
      {list.map((step, index) => (
        <StepItem key={step.id} step={step} first={index === 0} last={index === list.length - 1} {...tools} />
      ))}
    </ol>
  )
}

function StepItem({ step, first, last, ...tools }) {
  const { mode, errorId, onChange, steps, target, onTarget, onDropType } = tools
  const label = mode.labels[step.type]
  const update = (values) => onChange(change(steps, step.id, values))
  const name = label.button
  return (
    <li className={`step step--${mode.kinds[step.type]}${step.id === errorId ? ' step--error' : ''}`}>
      <div className="step__row">
        {label.phrase.map((part, index) =>
          typeof part === 'string' ? (
            <span key={index} className="step__word">
              {part}
            </span>
          ) : (
            <input
              key={index}
              className="step__input code"
              aria-label={`${name}: ${part.name}`}
              placeholder={part.placeholder}
              value={step[part.field]}
              size={Math.max(part.size ?? 6, String(step[part.field]).length + 1)}
              maxLength={500}
              spellCheck={false}
              onChange={(event) => update({ [part.field]: event.target.value })}
            />
          ),
        )}
        <span className="step__actions">
          <button type="button" className="step__tool" aria-label={`Subir ${name}`} disabled={first} onClick={() => onChange(move(steps, step.id, -1))}>
            ↑
          </button>
          <button type="button" className="step__tool" aria-label={`Bajar ${name}`} disabled={last} onClick={() => onChange(move(steps, step.id, 1))}>
            ↓
          </button>
          <button
            type="button"
            className="step__tool"
            aria-label={`Quitar ${name}`}
            onClick={() => {
              const next = remove(steps, step.id)
              if (target && (target.id === step.id || !contains(next, target.id))) onTarget(null)
              onChange(next)
            }}
          >
            ✕
          </button>
        </span>
      </div>
      {STEP_TYPES[step.type].branches?.map((branch) => {
        const selected = target?.id === step.id && target.branch === branch
        const where = { id: step.id, branch }
        return (
          <div key={branch} className={`step__branch${selected ? ' step__branch--target' : ''}`} {...dropProps(where, onDropType)}>
            {step[branch].length > 0 && <StepList list={step[branch]} {...tools} />}
            <button type="button" className="step__slot" aria-pressed={selected} onClick={() => onTarget(where)}>
              {selected ? `Los bloques nuevos van aquí (${label.branches[branch]})` : `Poner bloques en «${label.branches[branch]}»`}
            </button>
          </div>
        )
      })}
    </li>
  )
}
