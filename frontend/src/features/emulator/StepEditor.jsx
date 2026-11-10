import { useState } from 'react'
import { change, contains, insert, move, remove } from '../games/blocks.js'
import { STEP_TYPES, newStep } from './flow.js'

// Editor del árbol de pasos de DFD y Scratch. mode trae, por tipo de paso, el nombre del botón, la frase con sus
// campos y los nombres de las ramas. Los pasos nuevos van al final de la rama elegida («agregar aquí»).
export default function StepEditor({ mode, steps, onChange, errorId }) {
  const [target, setTarget] = useState(null)
  const tools = { mode, errorId, target, setTarget, onChange, steps }
  return (
    <div className={`step-editor step-editor--${mode.key}`}>
      <p className="page__note">
        Agregando en: <strong>{target ? branchName(mode, steps, target) : 'el programa principal'}</strong>{' '}
        {target && (
          <button type="button" className="button button--secondary button--small" onClick={() => setTarget(null)}>
            Volver al principal
          </button>
        )}
      </p>
      <div className="toolbar" aria-label="Pasos disponibles">
        {mode.palette.map((type) => (
          <button
            key={type}
            type="button"
            className={`button button--small step-button step--${mode.kinds[type]}`}
            onClick={() => onChange(insert(steps, target, newStep(type)))}
          >
            + {mode.labels[type].button}
          </button>
        ))}
      </div>
      {steps.length === 0 ? <p>El programa está vacío. Agrega pasos con los botones.</p> : <StepList list={steps} {...tools} />}
    </div>
  )
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
  const { mode, errorId, onChange, steps, target, setTarget } = tools
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
          <button type="button" className="button button--secondary button--small" aria-label={`Subir ${name}`} disabled={first} onClick={() => onChange(move(steps, step.id, -1))}>
            ↑
          </button>
          <button type="button" className="button button--secondary button--small" aria-label={`Bajar ${name}`} disabled={last} onClick={() => onChange(move(steps, step.id, 1))}>
            ↓
          </button>
          <button
            type="button"
            className="button button--danger button--small"
            aria-label={`Quitar ${name}`}
            onClick={() => {
              const next = remove(steps, step.id)
              if (target && (target.id === step.id || !contains(next, target.id))) setTarget(null)
              onChange(next)
            }}
          >
            Quitar
          </button>
        </span>
      </div>
      {STEP_TYPES[step.type].branches?.map((branch) => {
        const selected = target?.id === step.id && target.branch === branch
        return (
          <div key={branch} className={`step__branch${selected ? ' step__branch--target' : ''}`}>
            <button type="button" className="button button--secondary button--small" aria-pressed={selected} onClick={() => setTarget({ id: step.id, branch })}>
              {label.branches[branch]}: agregar aquí
            </button>
            {step[branch].length > 0 && <StepList list={step[branch]} {...tools} />}
          </div>
        )
      })}
    </li>
  )
}

function branchName(mode, steps, target) {
  const find = (list) => {
    for (const step of list) {
      if (step.id === target.id) return step
      for (const branch of STEP_TYPES[step.type].branches ?? []) {
        const inner = find(step[branch])
        if (inner) return inner
      }
    }
    return null
  }
  const step = find(steps)
  return step ? `«${mode.labels[step.type].branches[target.branch]}» de ${mode.labels[step.type].button}` : 'el programa principal'
}
