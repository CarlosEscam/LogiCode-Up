// Intérprete del juego de bloques: un robot en una cuadrícula ('#' muro, '.' libre, 'S' salida, 'M' meta).
// Es puro (sin React) para probarlo y para el modo paso a paso: devuelve cada estado del robot.

export const BLOCK_LABELS = {
  avanzar: 'Avanzar',
  izquierda: 'Girar a la izquierda',
  derecha: 'Girar a la derecha',
  repetir: 'Repetir',
  si: 'Si hay camino adelante',
  mientras: 'Mientras no llegue a la meta',
}

export const DIRECTIONS = ['N', 'E', 'S', 'O']
const DELTA = { N: [-1, 0], E: [0, 1], S: [1, 0], O: [0, -1] }

// Tope de pasos: un "mientras" que nunca llega a la meta no cuelga la página.
export const MAX_STEPS = 400

export function parseLevel(level) {
  const grid = level.grid.map((row) => row.split(''))
  let start = null
  let goal = null
  grid.forEach((row, r) =>
    row.forEach((cell, c) => {
      if (cell === 'S') start = { r, c, dir: level.facing }
      if (cell === 'M') goal = { r, c }
    }),
  )
  return { grid, start, goal }
}

export function countBlocks(program) {
  return program.reduce((total, block) => total + 1 + countBlocks(block.body ?? []) + countBlocks(block.else ?? []), 0)
}

// Ejecuta el programa. trace: un estado por acción ({ r, c, dir, blockId }), empezando por la salida.
// outcome: 'goal' (llegó), 'crash' (chocó con un muro o el borde), 'ended' (terminó sin llegar) o 'limit'.
export function runProgram(program, level) {
  const { grid, start, goal } = parseLevel(level)
  const robot = { ...start }
  const trace = [{ ...robot, blockId: null }]
  let steps = 0

  const free = (r, c) => r >= 0 && r < grid.length && c >= 0 && c < grid[r].length && grid[r][c] !== '#'
  const ahead = () => [robot.r + DELTA[robot.dir][0], robot.c + DELTA[robot.dir][1]]
  const atGoal = () => robot.r === goal.r && robot.c === goal.c

  class Stop extends Error {
    constructor(outcome) {
      super(outcome)
      this.outcome = outcome
    }
  }

  function tick() {
    steps += 1
    if (steps > MAX_STEPS) throw new Stop('limit')
  }

  function run(blocks) {
    for (const block of blocks) {
      tick()
      switch (block.type) {
        case 'avanzar': {
          const [r, c] = ahead()
          if (!free(r, c)) {
            trace.push({ ...robot, blockId: block.id, crashed: true })
            throw new Stop('crash')
          }
          Object.assign(robot, { r, c })
          trace.push({ ...robot, blockId: block.id })
          if (atGoal()) throw new Stop('goal')
          break
        }
        case 'izquierda':
        case 'derecha': {
          const turn = block.type === 'derecha' ? 1 : 3
          robot.dir = DIRECTIONS[(DIRECTIONS.indexOf(robot.dir) + turn) % 4]
          trace.push({ ...robot, blockId: block.id })
          break
        }
        case 'repetir':
          for (let i = 0; i < block.times; i += 1) run(block.body)
          break
        case 'si':
          run(free(...ahead()) ? block.body : (block.else ?? []))
          break
        case 'mientras':
          while (!atGoal()) {
            tick()
            run(block.body)
          }
          break
        default:
          throw new Error(`Bloque desconocido: ${block.type}`)
      }
    }
  }

  try {
    run(program)
    return { trace, outcome: atGoal() ? 'goal' : 'ended' }
  } catch (error) {
    if (error instanceof Stop) return { trace, outcome: error.outcome }
    throw error
  }
}

// Inserta, cambia, mueve o quita bloques en el árbol del programa (también lo usa el Emulador) (target: { id, branch } o null = programa principal).
export function insert(blocks, target, block) {
  if (!target) return [...blocks, block]
  return blocks.map((item) => ({
    ...item,
    ...(item.body && { body: item.id === target.id && target.branch === 'body' ? [...item.body, block] : insert(item.body, target, block) }),
    ...(item.else && { else: item.id === target.id && target.branch === 'else' ? [...item.else, block] : insert(item.else, target, block) }),
  }))
}
export function change(blocks, id, update) {
  return blocks.map((item) => {
    if (item.id === id) return { ...item, ...update }
    return { ...item, ...(item.body && { body: change(item.body, id, update) }), ...(item.else && { else: change(item.else, id, update) }) }
  })
}
export function remove(blocks, id) {
  return blocks
    .filter((item) => item.id !== id)
    .map((item) => ({ ...item, ...(item.body && { body: remove(item.body, id) }), ...(item.else && { else: remove(item.else, id) }) }))
}
export function contains(blocks, id) {
  return blocks.some((item) => item.id === id || contains(item.body ?? [], id) || contains(item.else ?? [], id))
}

// Sube (-1) o baja (+1) un bloque dentro de su misma lista.
export function move(blocks, id, delta) {
  const index = blocks.findIndex((item) => item.id === id)
  if (index >= 0) {
    const to = index + delta
    if (to < 0 || to >= blocks.length) return blocks
    const next = [...blocks]
    ;[next[index], next[to]] = [next[to], next[index]]
    return next
  }
  return blocks.map((item) => ({ ...item, ...(item.body && { body: move(item.body, id, delta) }), ...(item.else && { else: move(item.else, id, delta) }) }))
}

// Reto de bloques del docente (RF-12): mismas reglas del tablero que valida el servidor (BlockChallenge.java).
export function boardProblem(rows) {
  if (rows.length === 0 || rows.length > 12) return 'El tablero debe tener entre 1 y 12 filas.'
  if (rows.some((row) => row.length !== rows[0].length || row.length > 12)) return 'Todas las filas deben tener el mismo largo (hasta 12 casillas).'
  if (rows.some((row) => /[^#.SM]/.test(row))) return 'Usa solo # (muro), . (libre), S (salida) y M (meta).'
  const text = rows.join('')
  if (text.split('S').length !== 2 || text.split('M').length !== 2) return 'El tablero necesita exactamente una S y una M.'
  return null
}

// Programa entregado en un reto (RF-12) como texto con sangría, o null si el texto no es un programa de bloques.
export function programText(text) {
  let program
  try {
    program = JSON.parse(text)
  } catch {
    return null
  }
  if (!Array.isArray(program)) return null
  const lines = []
  const write = (blocks, depth) => {
    for (const block of blocks) {
      if (!BLOCK_LABELS[block?.type]) throw new Error('bloque desconocido')
      const pad = '  '.repeat(depth)
      lines.push(pad + BLOCK_LABELS[block.type] + (block.type === 'repetir' ? ` ${block.times} veces` : ''))
      write(block.body ?? [], depth + 1)
      if (block.type === 'si') {
        lines.push(`${pad}Si no`)
        write(block.else ?? [], depth + 1)
      }
    }
  }
  try {
    write(program, 0)
  } catch {
    return null
  }
  return lines.join('\n')
}
