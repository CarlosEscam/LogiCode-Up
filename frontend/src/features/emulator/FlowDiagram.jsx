import { cloneElement } from 'react'
import { stepText } from './flow.js'

// Dibuja el DFD del emulador a partir del árbol de pasos (flow.js), de arriba abajo. Cada parte calcula cuánto ocupa a la
// izquierda y a la derecha de su eje (left, right) y su alto (h); draw(cx, y) la dibuja con su eje en cx desde y.
// Decisión: Sí a la izquierda, No a la derecha, y se unen debajo. Mientras/Para: el cuerpo baja por el Sí y regresa por la
// izquierda; la salida va por la derecha. Repetir: el cuerpo va primero y, si la condición no se cumple, regresa arriba.

const NODE = { w: 170, h: 40 }
const DIAMOND = { w: 190, h: 60 }
const GAP = 28
const HGAP = 14
const SIDE = 16
const MAX_TEXT = 24

const short = (text) => (text.length > MAX_TEXT ? `${text.slice(0, MAX_TEXT - 1)}…` : text)

export default function FlowDiagram({ steps, errorId }) {
  const parts = []
  const add = (element) => parts.push(cloneElement(element, { key: String(parts.length) }))
  const line = (points, head = true) =>
    add(<polyline points={points.map((p) => p.join(',')).join(' ')} className="dfd__line" markerEnd={head ? 'url(#emu-punta)' : undefined} />)
  const label = (text, x, y, anchor = 'middle') =>
    add(
      <text x={x} y={y} textAnchor={anchor} className="dfd__branch">
        {text}
      </text>,
    )

  function shape(kind, cx, y, size, text, id) {
    const { w, h } = size
    const left = cx - w / 2
    const className = `dfd__shape${id && id === errorId ? ' dfd__shape--error' : ''}`
    let outline
    if (kind === 'oval') outline = <rect x={left} y={y} width={w} height={h} rx={h / 2} className={className} />
    else if (kind === 'io') {
      const slant = 14
      outline = <polygon points={`${left + slant},${y} ${left + w},${y} ${left + w - slant},${y + h} ${left},${y + h}`} className={className} />
    } else if (kind === 'diamond') {
      outline = <polygon points={`${cx},${y} ${cx + w / 2},${y + h / 2} ${cx},${y + h} ${cx - w / 2},${y + h / 2}`} className={className} />
    } else if (kind === 'hexagon') {
      const cut = 16
      outline = (
        <polygon
          points={`${left + cut},${y} ${left + w - cut},${y} ${left + w},${y + h / 2} ${left + w - cut},${y + h} ${left + cut},${y + h} ${left},${y + h / 2}`}
          className={className}
        />
      )
    } else outline = <rect x={left} y={y} width={w} height={h} className={className} />
    add(
      <g>
        <title>{text}</title>
        {outline}
        <text x={cx} y={y + h / 2} dominantBaseline="middle" textAnchor="middle" className="dfd__text">
          {short(text)}
        </text>
      </g>,
    )
  }

  function layoutList(list) {
    const items = list.map(layoutStep)
    const h = items.reduce((total, item) => total + item.h, 0) + GAP * Math.max(0, items.length - 1)
    return {
      left: Math.max(0, ...items.map((item) => item.left)),
      right: Math.max(0, ...items.map((item) => item.right)),
      h,
      draw(cx, y) {
        let top = y
        items.forEach((item, index) => {
          if (index > 0) {
            line([[cx, top - GAP], [cx, top]])
          }
          item.draw(cx, top)
          top += item.h + GAP
        })
      },
    }
  }

  function layoutStep(step) {
    const text = stepText(step)
    if (step.type === 'si') {
      const yes = layoutList(step.body)
      const no = layoutList(step.else)
      const offsetYes = Math.max(DIAMOND.w / 2 + HGAP, yes.right + HGAP / 2)
      const offsetNo = Math.max(DIAMOND.w / 2 + HGAP, no.left + HGAP / 2)
      const inner = Math.max(yes.h, no.h)
      return {
        left: Math.max(DIAMOND.w / 2, offsetYes + yes.left),
        right: Math.max(DIAMOND.w / 2, offsetNo + no.right),
        h: DIAMOND.h + GAP + inner + GAP / 2,
        draw(cx, y) {
          shape('diamond', cx, y, DIAMOND, text, step.id)
          const middle = y + DIAMOND.h / 2
          const top = y + DIAMOND.h + GAP
          const merge = top + inner + GAP / 2
          ;[
            [yes, cx - offsetYes, cx - DIAMOND.w / 2, 'Sí', 'end'],
            [no, cx + offsetNo, cx + DIAMOND.w / 2, 'No', 'start'],
          ].forEach(([branch, bx, edge, word, anchor]) => {
            label(word, edge + (anchor === 'end' ? -4 : 4), middle - 6, anchor)
            if (branch.h === 0) {
              line([[edge, middle], [bx, middle], [bx, merge], [cx, merge]], false)
              return
            }
            line([[edge, middle], [bx, middle], [bx, top]])
            branch.draw(bx, top)
            line([[bx, top + branch.h], [bx, merge], [cx, merge]], false)
          })
        },
      }
    }
    if (step.type === 'mientras' || step.type === 'para') {
      const body = layoutList(step.body)
      const size = step.type === 'para' ? NODE : DIAMOND
      const left = Math.max(size.w / 2, body.left) + SIDE
      const right = Math.max(size.w / 2, body.right) + SIDE
      return {
        left,
        right,
        h: size.h + GAP + body.h + GAP,
        draw(cx, y) {
          shape(step.type === 'para' ? 'hexagon' : 'diamond', cx, y, size, text, step.id)
          const middle = y + size.h / 2
          const top = y + size.h + GAP
          label(step.type === 'para' ? 'Repite' : 'Sí', cx + 6, top - 8, 'start')
          if (body.h > 0) {
            line([[cx, y + size.h], [cx, top]])
            body.draw(cx, top)
          } else line([[cx, y + size.h], [cx, top]], false)
          const back = top + body.h + GAP / 2
          line([[cx, top + body.h], [cx, back], [cx - left, back], [cx - left, middle], [cx - size.w / 2, middle]])
          const exit = back + GAP / 2
          label(step.type === 'para' ? 'Termina' : 'No', cx + size.w / 2 + 4, middle - 6, 'start')
          line([[cx + size.w / 2, middle], [cx + right, middle], [cx + right, exit], [cx, exit]], false)
        },
      }
    }
    if (step.type === 'repetir') {
      const body = layoutList(step.body)
      const left = Math.max(DIAMOND.w / 2, body.left) + SIDE
      return {
        left,
        right: Math.max(DIAMOND.w / 2, body.right),
        h: GAP / 2 + body.h + GAP + DIAMOND.h,
        draw(cx, y) {
          const join = y + GAP / 4
          const top = y + GAP / 2
          line([[cx, y], [cx, top]], false)
          body.draw(cx, top)
          const diamondTop = top + body.h + GAP
          line([[cx, top + body.h], [cx, diamondTop]])
          shape('diamond', cx, diamondTop, DIAMOND, text, step.id)
          const middle = diamondTop + DIAMOND.h / 2
          label('No', cx - DIAMOND.w / 2 - 4, middle - 6, 'end')
          line([[cx - DIAMOND.w / 2, middle], [cx - left, middle], [cx - left, join], [cx, join]])
          label('Sí', cx + 6, diamondTop + DIAMOND.h + 12, 'start')
        },
      }
    }
    const kind = step.type === 'leer' || step.type === 'escribir' ? 'io' : 'process'
    return { left: NODE.w / 2, right: NODE.w / 2, h: NODE.h, draw: (cx, y) => shape(kind, cx, y, NODE, text, step.id) }
  }

  const program = layoutList(steps)
  const left = Math.max(NODE.w / 2, program.left) + 8
  const right = Math.max(NODE.w / 2, program.right) + 8
  shape('oval', 0, 8, NODE, 'Inicio')
  let y = 8 + NODE.h + GAP
  line([[0, 8 + NODE.h], [0, y]])
  if (program.h > 0) {
    program.draw(0, y)
    y += program.h + GAP
    line([[0, y - GAP], [0, y]])
  }
  shape('oval', 0, y, NODE, 'Fin')
  y += NODE.h + 8

  const description = ['Inicio', ...flatten(steps), 'Fin'].join('; ')
  return (
    <svg className="dfd dfd--emulator" viewBox={`${-left} 0 ${left + right} ${y}`} style={{ maxWidth: `${left + right}px` }} role="img" aria-label={`Diagrama de flujo: ${description}.`}>
      <defs>
        <marker id="emu-punta" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
          <path d="M0,0 L10,5 L0,10 z" className="dfd__head" />
        </marker>
      </defs>
      {parts}
    </svg>
  )
}

function flatten(steps) {
  return steps.flatMap((step) => {
    const text = stepText(step)
    if (step.type === 'si') return [`decisión ${text}: si sí, ${flatten(step.body).join(', ') || 'nada'}; si no, ${flatten(step.else).join(', ') || 'nada'}`]
    if (step.type === 'repetir') return [`repetir ${flatten(step.body).join(', ') || 'nada'} hasta que ${step.cond}`]
    if (step.body) return [`${text}: ${flatten(step.body).join(', ') || 'nada'}`]
    return [text]
  })
}
