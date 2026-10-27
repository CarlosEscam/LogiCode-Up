// Intérprete del juego de bloques: un robot en una cuadrícula ('#' muro, '.' libre, 'S' salida, 'M' meta).
// Es puro (sin React) para probarlo y para el modo paso a paso: devuelve cada estado del robot.

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
