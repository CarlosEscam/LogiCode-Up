import { cloneElement } from 'react'
import { DRAG_TYPE, stepText } from './flow.js'

// Lienzo del DFD del emulador: dibuja el árbol de pasos (flow.js) de arriba abajo. Cada parte calcula cuánto ocupa a la
// izquierda y a la derecha de su eje (left, right) y su alto (h); draw(cx, y) la dibuja con su eje en cx desde y.
// Entre dos pasos (y al inicio y al final de cada rama) hay un conector con un «+» donde se inserta una figura: con clic
// (onInsert) o soltando una figura arrastrada desde la paleta. Clic en una figura la selecciona (onSelect).
// Decisión: Sí a la izquierda, No a la derecha, y se unen debajo. Mientras/Para: el cuerpo baja y regresa por la
// izquierda; la salida va por la derecha. Repetir: el cuerpo va primero y, si la condición no se cumple, regresa arriba.

const NODE = { w: 170, h: 40 }
const DIAMOND = { w: 190, h: 60 }
const GAP = 34
const HGAP = 14
const SIDE = 16
const MAX_TEXT = 24

const short = (text) => (text.length > MAX_TEXT ? `${text.slice(0, MAX_TEXT - 1)}…` : text)
const KIND = { leer: 'io', escribir: 'io', asignar: 'process', si: 'decision', mientras: 'loop', para: 'loop', repetir: 'loop' }

export default function FlowDiagram({ steps, errorId, selectedId, onSelect, onInsert, armed, zoom = 1, describeSlot }) {
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

  function slot(cx, cy, owner, index) {
    if (!onInsert) return
    const where = describeSlot ? describeSlot(owner, index) : `posición ${index + 1}`
    const insert = (type) => onInsert(owner, index, type)
    add(
      <g
        className={`dfd__slot${armed ? ' dfd__slot--armed' : ''}`}
        role="button"
        tabIndex={0}
        aria-label={`Insertar aquí: ${where}`}
        onClick={() => insert()}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            insert()
          }
        }}
        onDragOver={(event) => {
          event.preventDefault()
          event.currentTarget.classList.add('dfd__slot--over')
        }}
        onDragLeave={(event) => event.currentTarget.classList.remove('dfd__slot--over')}
        onDrop={(event) => {
          event.preventDefault()
          event.currentTarget.classList.remove('dfd__slot--over')
          insert(event.dataTransfer.getData(DRAG_TYPE) || undefined)
        }}
      >
        <title>{`Insertar aquí: ${where}`}</title>
        <circle cx={cx} cy={cy} r={14} className="dfd__slot-area" />
        <circle cx={cx} cy={cy} r={8} className="dfd__slot-dot" />
        <path d={`M${cx - 4},${cy} H${cx + 4} M${cx},${cy - 4} V${cy + 4}`} className="dfd__slot-plus" />
      </g>,
    )
  }

  function shape(kind, cx, y, size, text, step) {
    const { w, h } = size
    const left = cx - w / 2
    const id = step?.id
    const color = kind === 'oval' ? 'terminal' : (KIND[step?.type] ?? 'process')
    const className = `dfd__shape dfd__shape--${color}${id && id === selectedId ? ' dfd__shape--selected' : ''}${id && id === errorId ? ' dfd__shape--error' : ''}`
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
    } else outline = <rect x={left} y={y} width={w} height={h} rx={4} className={className} />
    const body = (
      <>
        <title>{text}</title>
        {outline}
        <text x={cx} y={y + h / 2} dominantBaseline="middle" textAnchor="middle" className="dfd__text">
          {short(text)}
        </text>
      </>
    )
    if (!step || !onSelect) return add(<g>{body}</g>)
    add(
      <g
        className="dfd__node"
        role="button"
        tabIndex={0}
        aria-pressed={step.id === selectedId}
        aria-label={`Figura: ${text}`}
        onClick={() => onSelect(step.id)}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            onSelect(step.id)
          }
        }}
      >
        {body}
      </g>,
    )
  }

  // Lista de pasos: un conector antes de cada paso y otro al final, cada uno con su «+».
  function layoutList(list, owner) {
    const items = list.map(layoutStep)
    return {
      left: Math.max(16, ...items.map((item) => item.left)),
      right: Math.max(16, ...items.map((item) => item.right)),
      h: items.reduce((total, item) => total + item.h, 0) + GAP * (items.length + 1),
      draw(cx, y) {
        let top = y
        for (let index = 0; index <= items.length; index += 1) {
          line([[cx, top], [cx, top + GAP]], index < items.length)
          slot(cx, top + GAP / 2 - 2, owner, index)
          top += GAP
          if (index < items.length) {
            items[index].draw(cx, top)
            top += items[index].h
          }
        }
      },
    }
  }

  function layoutStep(step) {
    const text = stepText(step)
    if (step.type === 'si') {
      const yes = layoutList(step.body, { id: step.id, branch: 'body' })
      const no = layoutList(step.else, { id: step.id, branch: 'else' })
      const offsetYes = Math.max(DIAMOND.w / 2 + HGAP, yes.right + HGAP / 2)
      const offsetNo = Math.max(DIAMOND.w / 2 + HGAP, no.left + HGAP / 2)
      const inner = Math.max(yes.h, no.h)
      return {
        left: Math.max(DIAMOND.w / 2, offsetYes + yes.left),
        right: Math.max(DIAMOND.w / 2, offsetNo + no.right),
        h: DIAMOND.h + GAP / 2 + inner + GAP / 2,
        draw(cx, y) {
          shape('diamond', cx, y, DIAMOND, text, step)
          const middle = y + DIAMOND.h / 2
          const top = y + DIAMOND.h + GAP / 2
          const merge = top + inner + GAP / 2
          ;[
            [yes, cx - offsetYes, cx - DIAMOND.w / 2, 'Sí', 'end'],
            [no, cx + offsetNo, cx + DIAMOND.w / 2, 'No', 'start'],
          ].forEach(([branch, bx, edge, word, anchor]) => {
            label(word, edge + (anchor === 'end' ? -4 : 4), middle - 6, anchor)
            line([[edge, middle], [bx, middle], [bx, top]], false)
            branch.draw(bx, top)
            line([[bx, top + branch.h], [bx, merge], [cx, merge]], false)
          })
        },
      }
    }
    if (step.type === 'mientras' || step.type === 'para') {
      const body = layoutList(step.body, { id: step.id, branch: 'body' })
      const size = step.type === 'para' ? NODE : DIAMOND
      const left = Math.max(size.w / 2, body.left) + SIDE
      const right = Math.max(size.w / 2, body.right) + SIDE
      return {
        left,
        right,
        h: size.h + body.h + GAP / 2,
        draw(cx, y) {
          shape(step.type === 'para' ? 'hexagon' : 'diamond', cx, y, size, text, step)
          const middle = y + size.h / 2
          const top = y + size.h
          label(step.type === 'para' ? 'Repite' : 'Sí', cx + 16, top + 12, 'start')
          body.draw(cx, top)
          const back = top + body.h
          line([[cx, back], [cx - left, back], [cx - left, middle], [cx - size.w / 2, middle]])
          const exit = back + GAP / 2
          label(step.type === 'para' ? 'Termina' : 'No', cx + size.w / 2 + 4, middle - 6, 'start')
          line([[cx + size.w / 2, middle], [cx + right, middle], [cx + right, exit], [cx, exit]], false)
        },
      }
    }
    if (step.type === 'repetir') {
      const body = layoutList(step.body, { id: step.id, branch: 'body' })
      const left = Math.max(DIAMOND.w / 2, body.left) + SIDE
      return {
        left,
        right: Math.max(DIAMOND.w / 2, body.right),
        h: 6 + body.h + DIAMOND.h,
        draw(cx, y) {
          const join = y + 3
          line([[cx, y], [cx, y + 6]], false)
          body.draw(cx, y + 6)
          const diamondTop = y + 6 + body.h
          shape('diamond', cx, diamondTop, DIAMOND, text, step)
          const middle = diamondTop + DIAMOND.h / 2
          label('No', cx - DIAMOND.w / 2 - 4, middle - 6, 'end')
          line([[cx - DIAMOND.w / 2, middle], [cx - left, middle], [cx - left, join], [cx, join]])
          label('Sí', cx + 6, diamondTop + DIAMOND.h + 12, 'start')
        },
      }
    }
    const kind = step.type === 'leer' || step.type === 'escribir' ? 'io' : 'process'
    return { left: NODE.w / 2, right: NODE.w / 2, h: NODE.h, draw: (cx, y) => shape(kind, cx, y, NODE, text, step) }
  }

  const program = layoutList(steps, null)
  const left = Math.max(NODE.w / 2, program.left) + 8
  const right = Math.max(NODE.w / 2, program.right) + 8
  shape('oval', 0, 8, NODE, 'Inicio')
  program.draw(0, 8 + NODE.h)
  const end = 8 + NODE.h + program.h
  shape('oval', 0, end, NODE, 'Fin')
  const height = end + NODE.h + 8
  const width = left + right

  const description = ['Inicio', ...flatten(steps), 'Fin'].join('; ')
  return (
    <svg
      className="dfd dfd--emulator"
      viewBox={`${-left} 0 ${width} ${height}`}
      width={width * zoom}
      height={height * zoom}
      role="group"
      aria-label={`Diagrama de flujo: ${description}.`}
    >
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
