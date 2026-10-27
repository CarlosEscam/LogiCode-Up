import { useState } from 'react'
import Solved from './Solved.jsx'
import { dfdBlanks } from './levels.js'

const WIDTH = 440
const CENTER = WIDTH / 2
const NODE = { w: 210, h: 44 }
const BRANCH = { w: 170, h: 44, left: 105, right: 335 }
const DIAMOND = { w: 220, h: 72 }
const GAP = 34

export default function DfdGame({ level, onSolved, onNext }) {
  const blanks = dfdBlanks(level)
  const [chosen, setChosen] = useState({})
  const [result, setResult] = useState(null)

  function check(event) {
    event.preventDefault()
    const wrong = blanks.filter((node) => chosen[node.blank] !== node.answer).map((node) => node.blank)
    setResult(wrong)
    if (wrong.length === 0) onSolved()
  }

  return (
    <section className="card card--wide" aria-labelledby="nivel-titulo">
      <h2 id="nivel-titulo">{level.title}</h2>
      <Diagram level={level} chosen={chosen} />
      <form onSubmit={check} className="subform">
        {blanks.map((node) => (
          <div key={node.blank} className="field">
            <label htmlFor={`casilla-${node.blank}`}>
              Casilla {node.blank} ({SHAPE_LABELS[node.shape]})
            </label>
            <select
              id={`casilla-${node.blank}`}
              value={chosen[node.blank] ?? ''}
              onChange={(event) => {
                setChosen((current) => ({ ...current, [node.blank]: event.target.value }))
                setResult(null)
              }}
            >
              <option value="">Elige…</option>
              {level.options.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>
        ))}
        <button type="submit" className="button" disabled={blanks.some((node) => !chosen[node.blank])}>
          Comprobar
        </button>
      </form>
      {result?.length === 0 && <Solved onNext={onNext}>¡Diagrama completo!</Solved>}
      {result?.length > 0 && (
        <p className="notice notice--error" role="status">
          Revisa {result.length === 1 ? 'la casilla' : 'las casillas'} {result.join(' y ')}. Fíjate también en la forma: cada una indica un tipo de paso.
        </p>
      )}
      <p className="page__note">
        Formas: óvalo = inicio o fin; paralelogramo = leer o escribir; rectángulo = proceso (cálculo); rombo = decisión.
      </p>
    </section>
  )
}

const SHAPE_LABELS = { start: 'inicio', end: 'fin', io: 'entrada o salida', process: 'proceso', decision: 'decisión' }

// Dibuja el DFD de arriba abajo. Las ramas de una decisión van a los lados y se unen antes del siguiente nodo.
function Diagram({ level, chosen }) {
  const parts = []
  const steps = []
  let y = 12
  let from = null // punto desde donde sale la flecha al siguiente nodo

  const label = (node) => (node.blank ? (chosen[node.blank] ?? `Casilla ${node.blank}`) : node.text)

  level.nodes.forEach((node, index) => {
    if (from) parts.push(<Arrow key={`a${index}`} points={[from, [CENTER, y]]} />)
    if (node.shape !== 'decision') {
      parts.push(<Shape key={`n${index}`} node={node} x={CENTER} y={y} size={NODE} text={label(node)} />)
      steps.push(`${SHAPE_LABELS[node.shape]}: ${label(node)}`)
      y += NODE.h
      from = [CENTER, y]
      y += GAP
      return
    }
    parts.push(<Shape key={`n${index}`} node={node} x={CENTER} y={y} size={DIAMOND} text={label(node)} />)
    const middle = y + DIAMOND.h / 2
    const branchY = y + DIAMOND.h + GAP
    const sides = [
      [node.yes, BRANCH.left, CENTER - DIAMOND.w / 2, 'Sí'],
      [node.no, BRANCH.right, CENTER + DIAMOND.w / 2, 'No'],
    ]
    const mergeY = branchY + BRANCH.h + GAP / 2
    steps.push(`decisión: ${label(node)}; si sí, ${label(node.yes)}; si no, ${label(node.no)}`)
    sides.forEach(([branch, bx, edge, text]) => {
      parts.push(<Arrow key={`b${index}${text}`} points={[[edge, middle], [bx, middle], [bx, branchY]]} />)
      parts.push(
        <text key={`t${index}${text}`} x={(edge + bx) / 2} y={middle - 6} textAnchor="middle" className="dfd__branch">
          {text}
        </text>,
      )
      parts.push(<Shape key={`n${index}${text}`} node={branch} x={bx} y={branchY} size={BRANCH} text={label(branch)} />)
      parts.push(<Arrow key={`m${index}${text}`} points={[[bx, branchY + BRANCH.h], [bx, mergeY], [CENTER, mergeY]]} head={false} />)
    })
    from = [CENTER, mergeY]
    y = mergeY + GAP
  })

  return (
    <svg className="dfd" viewBox={`0 0 ${WIDTH} ${y}`} role="img" aria-labelledby="dfd-desc">
      <title id="dfd-desc">{`Diagrama de flujo: ${steps.join('; ')}.`}</title>
      <defs>
        <marker id="dfd-punta" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
          <path d="M0,0 L10,5 L0,10 z" className="dfd__head" />
        </marker>
      </defs>
      {parts}
    </svg>
  )
}

function Arrow({ points, head = true }) {
  return <polyline points={points.map((p) => p.join(',')).join(' ')} className="dfd__line" markerEnd={head ? 'url(#dfd-punta)' : undefined} />
}

function Shape({ node, x, y, size, text }) {
  const { w, h } = size
  const left = x - w / 2
  const className = `dfd__shape${node.blank ? ' dfd__shape--blank' : ''}`
  let outline
  if (node.shape === 'start' || node.shape === 'end') {
    outline = <rect x={left} y={y} width={w} height={h} rx={h / 2} className={className} />
  } else if (node.shape === 'io') {
    const slant = 14
    outline = <polygon points={`${left + slant},${y} ${left + w},${y} ${left + w - slant},${y + h} ${left},${y + h}`} className={className} />
  } else if (node.shape === 'decision') {
    outline = <polygon points={`${x},${y} ${x + w / 2},${y + h / 2} ${x},${y + h} ${x - w / 2},${y + h / 2}`} className={className} />
  } else {
    outline = <rect x={left} y={y} width={w} height={h} className={className} />
  }
  return (
    <g>
      {outline}
      <text x={x} y={y + h / 2} dominantBaseline="middle" textAnchor="middle" className="dfd__text">
        {text}
      </text>
    </g>
  )
}
