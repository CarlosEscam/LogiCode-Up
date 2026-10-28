import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'
import { DIRECTIONS, MAX_STEPS, boardProblem, countBlocks, parseLevel, programText, runProgram } from './blocks.js'
import { ARDUINO_LEVELS, BLOCK_LEVELS, DFD_LEVELS, ERROR_LEVELS, ORDER_LEVELS, OUTPUT_LEVELS, dfdBlanks, shuffle } from './levels.js'

// Menor número de bloques Avanzar/Girar (sin ciclos ni condiciones) que lleva el robot a la meta.
function shortestSequence(level) {
  const { grid, start, goal } = parseLevel(level)
  const delta = { N: [-1, 0], E: [0, 1], S: [1, 0], O: [0, -1] }
  const seen = new Set()
  let frontier = [start]
  for (let blocks = 1; frontier.length > 0; blocks += 1) {
    const next = []
    for (const { r, c, dir } of frontier) {
      const turn = (step) => ({ r, c, dir: DIRECTIONS[(DIRECTIONS.indexOf(dir) + step) % 4] })
      const ahead = { r: r + delta[dir][0], c: c + delta[dir][1], dir }
      if (ahead.r === goal.r && ahead.c === goal.c) return blocks
      const moves = [turn(1), turn(3), ...(['.', 'S'].includes(grid[ahead.r]?.[ahead.c]) ? [ahead] : [])]
      moves.filter((state) => !seen.has(`${state.r},${state.c},${state.dir}`)).forEach((state) => {
        seen.add(`${state.r},${state.c},${state.dir}`)
        next.push(state)
      })
    }
    frontier = next
  }
  return Infinity
}

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
  window.localStorage.clear()
  window.history.replaceState(null, '', '/')
})

describe('Niveles de los juegos', () => {
  it.each(BLOCK_LEVELS.map((level) => [level.id, level]))('el nivel de bloques %s se resuelve dentro del límite', (_, level) => {
    expect(countBlocks(level.solution)).toBeLessThanOrEqual(level.maxBlocks)
    expect(runProgram(level.solution, level).outcome).toBe('goal')
    if (level.start) expect(runProgram(level.start, level).outcome).not.toBe('goal')
  })

  it('los niveles de bloques de ciclos o condiciones no se resuelven con una secuencia simple', () => {
    expect(BLOCK_LEVELS).toHaveLength(24)
    BLOCK_LEVELS.filter((level) => level.allowed.some((type) => ['repetir', 'mientras'].includes(type))).forEach((level) =>
      expect(shortestSequence(level), level.id).toBeGreaterThan(level.maxBlocks),
    )
  })

  it('el robot choca con los muros y no se cuelga en un ciclo sin salida', () => {
    const level = BLOCK_LEVELS[1]
    expect(runProgram([{ id: 'x', type: 'avanzar' }, { id: 'y', type: 'avanzar' }, { id: 'z', type: 'avanzar' }], level).outcome).toBe('crash')
    const spin = runProgram([{ id: 'm', type: 'mientras', body: [{ id: 'g', type: 'derecha' }] }], level)
    expect(spin.outcome).toBe('limit')
    expect(spin.trace.length).toBeLessThan(MAX_STEPS)
    expect(runProgram([{ id: 'v', type: 'mientras', body: [] }], level).outcome).toBe('limit')
  })

  it('muestra un programa entregado como texto y revisa el tablero de un reto (RF-12)', () => {
    const program = [
      { type: 'repetir', times: 2, body: [{ type: 'avanzar' }] },
      { type: 'si', body: [{ type: 'avanzar' }], else: [{ type: 'derecha' }] },
    ]
    expect(programText(JSON.stringify(program))).toBe(
      'Repetir 2 veces\n  Avanzar\nSi hay camino adelante\n  Avanzar\nSi no\n  Girar a la derecha',
    )
    expect(programText('prom <- (a+b)/2')).toBeNull()
    expect(programText('[{"type":"saltar"}]')).toBeNull()
    expect(boardProblem(['S.M'])).toBeNull()
    expect(boardProblem(['S..', 'M'])).not.toBeNull()
    expect(boardProblem(['S..'])).not.toBeNull()
  })

  it('los ids de cada juego no se repiten y las opciones de cada nivel tampoco', () => {
    ;[BLOCK_LEVELS, OUTPUT_LEVELS, ORDER_LEVELS, DFD_LEVELS, ARDUINO_LEVELS].forEach((levels) => expect(new Set(levels.map((level) => level.id)).size).toBe(levels.length))
    ;[...OUTPUT_LEVELS, ...DFD_LEVELS, ...ARDUINO_LEVELS].forEach((level) => expect(new Set(level.options).size, level.id).toBe(level.options.length))
    expect([OUTPUT_LEVELS.length, ORDER_LEVELS.length, DFD_LEVELS.length, ERROR_LEVELS.length, ARDUINO_LEVELS.length]).toEqual([21, 14, 14, 14, 12])
  })

  it('cada nivel de Caza el error marca una línea real y su corrección cambia esa línea', () => {
    expect(new Set(ERROR_LEVELS.map((level) => level.id)).size).toBe(ERROR_LEVELS.length)
    ERROR_LEVELS.forEach((level) => {
      expect(level.lines[level.bug]?.trim(), level.id).toBeTruthy()
      expect(level.fixes[level.fix], level.id).toBeDefined()
      expect(new Set([...level.fixes, level.lines[level.bug]]).size, level.id).toBe(level.fixes.length + 1)
    })
  })

  it('las respuestas de los otros juegos están entre sus opciones', () => {
    ;[...OUTPUT_LEVELS, ...ARDUINO_LEVELS].forEach((level) => expect(level.options[level.answer], level.id).toBeDefined())
    ARDUINO_LEVELS.forEach((level) => expect(level.question, level.id).toBeTruthy())
    DFD_LEVELS.forEach((level) => dfdBlanks(level).forEach((blank) => expect(level.options).toContain(blank.answer)))
    ORDER_LEVELS.forEach((level) => expect(shuffle(level.lines)).not.toEqual(level.lines))
  })

  it('las casillas de un DFD con ciclo incluyen la condición, el cuerpo y las ramas de adentro, en orden', () => {
    const loops = DFD_LEVELS.filter((level) => level.nodes.some((node) => node.shape === 'while'))
    expect(loops.map((level) => level.id)).toEqual(['dfd-11', 'dfd-12', 'dfd-13', 'dfd-14'])
    loops.forEach((level) => expect(dfdBlanks(level).map((node) => node.blank).join(''), level.id).toBe('ABCD'.slice(0, dfdBlanks(level).length)))
    expect(dfdBlanks(DFD_LEVELS.find((level) => level.id === 'dfd-14')).map((node) => node.answer)).toEqual(['¿k <= 10?', '¿nota >= 3?', 'c ← c + 1', 'k ← k + 1'])
  })
})

describe('Página de juegos', () => {
  it('muestra el catálogo y filtra los juegos por herramienta y tema', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.localStorage.setItem('logicode.juegos.v1', JSON.stringify({ salida: ['salida-1'] }))
    window.history.replaceState(null, '', '/juegos')
    render(<App />)

    const list = await screen.findByRole('region', { name: 'Juegos' })
    expect(within(list).getAllByRole('heading', { level: 2 })).toHaveLength(6)
    expect(screen.getByRole('status')).toHaveTextContent('6 juegos · 99 niveles')
    // Con un nivel resuelto, el botón lleva al primero que falta.
    expect(screen.getByRole('link', { name: 'Seguir jugando: ¿Qué imprime?' })).toHaveAttribute('href', `/juegos?juego=salida&nivel=${OUTPUT_LEVELS[1].id}`)

    fireEvent.change(screen.getByLabelText('Tema'), { target: { value: 'Funciones' } })
    expect(within(list).getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)).toEqual(['¿Qué imprime?', 'Caza el error'])
    fireEvent.change(screen.getByLabelText('Herramienta'), { target: { value: 'DFD' } })
    expect(screen.getByText('No hay juegos con estos filtros.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Tema'), { target: { value: '' } })
    expect(within(list).getByRole('heading', { level: 2 })).toHaveTextContent('Completa el DFD')
    window.localStorage.clear()
  })

  it('completa un DFD con un ciclo que tiene una decisión adentro', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.history.replaceState(null, '', '/juegos?juego=dfd&nivel=dfd-14')
    render(<App />)

    const answers = { 'Casilla A (ciclo mientras)': '¿k <= 10?', 'Casilla B (decisión)': '¿nota >= 3?', 'Casilla C (proceso)': 'c ← c + 1', 'Casilla D (proceso)': 'c ← c + 1' }
    for (const [name, value] of Object.entries(answers)) fireEvent.change(await screen.findByLabelText(name), { target: { value } })
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('Revisa la casilla D')
    fireEvent.change(screen.getByLabelText('Casilla D (proceso)'), { target: { value: 'k ← k + 1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('¡Diagrama completo!')
    expect(screen.getByRole('img', { name: /ciclo: mientras ¿k <= 10\?, repite;.*si no, nada;.*fin del ciclo/ })).toBeInTheDocument()
  })

  it('en el juego de Arduino se elige qué pasa en la placa', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.history.replaceState(null, '', '/juegos?juego=arduino&nivel=arduino-6')
    render(<App />)

    expect(await screen.findByRole('heading', { name: '¿Qué hace este código?' })).toBeInTheDocument()
    expect(screen.getByRole('group', { name: '¿Qué aparece en el Monitor Serie?' })).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('1, 2, 3, 4… un número nuevo cada segundo'))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('qué pasa en cada pin')
    fireEvent.click(screen.getByLabelText('1 cada segundo, siempre el mismo'))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('¡Correcto!')
    expect(JSON.parse(window.localStorage.getItem('logicode.juegos.v1'))).toEqual({ arduino: ['arduino-6'] })
  })

  it('se juega sin sesión y recuerda los niveles resueltos', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.history.replaceState(null, '', '/juegos?juego=salida')
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Variables' })).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('6'))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('No es esa')
    fireEvent.click(screen.getByLabelText('9'))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('¡Correcto!')
    expect(JSON.parse(window.localStorage.getItem('logicode.juegos.v1'))).toEqual({ salida: ['salida-1'] })
    expect(screen.getByText('1 de 21 niveles resueltos')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Siguiente nivel' }))
    expect(screen.getByRole('heading', { name: 'Intercambio' })).toBeInTheDocument()
  })

  it('funciona aunque el navegador no deje guardar el avance', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado')
    })
    window.history.replaceState(null, '', '/juegos?juego=dfd')
    render(<App />)

    fireEvent.change(await screen.findByLabelText('Casilla A (proceso)'), { target: { value: 'área ← base * altura' } })
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar' }))
    expect(screen.getByRole('status')).toHaveTextContent('¡Diagrama completo!')
    expect(screen.getByRole('img', { name: /área ← base \* altura/ })).toBeInTheDocument()
    vi.restoreAllMocks()
  })

  it('arma un programa de bloques, lo ejecuta y lo sigue paso a paso', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.history.replaceState(null, '', '/juegos?juego=bloques&nivel=ciclo-1')
    render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: '+ Repetir' }))
    fireEvent.click(screen.getByRole('button', { name: 'Hacer: agregar aquí' }))
    fireEvent.click(screen.getByRole('button', { name: '+ Avanzar' }))
    expect(screen.getByRole('heading', { name: 'Bloques (2 de 2)' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '+ Avanzar' })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Paso a paso' }))
    expect(screen.getByText(/Robot en la fila 1, columna 2/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Paso a paso' }))
    expect(screen.getByText(/Robot en la fila 1, columna 3/)).toBeInTheDocument()
    expect(screen.getByText('El programa terminó, pero el robot no llegó a la meta.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Paso a paso' })).toBeDisabled()

    vi.useFakeTimers()
    fireEvent.change(screen.getByLabelText('Veces'), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ejecutar' }))
    for (let i = 0; i < 12; i += 1) {
      await act(async () => vi.advanceTimersByTime(400))
    }
    expect(screen.getByText(/¡El robot llegó a la meta con 2 bloques!/)).toBeInTheDocument()
    const levels = screen.getByRole('navigation', { name: 'Niveles' })
    expect(within(levels).getByRole('button', { name: /Pasillo largo.*resuelto/ })).toBeInTheDocument()
  })

  it('en Caza el error se encuentra la línea y luego se elige la corrección', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.history.replaceState(null, '', '/juegos?juego=error')
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Área del rectángulo' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('radio', { name: 'Línea 2: base <- 4' }))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar línea' }))
    expect(screen.getByRole('status')).toHaveTextContent('La línea 2 está bien')
    fireEvent.click(screen.getByRole('radio', { name: 'Línea 4: area <- base + altura' }))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar línea' }))
    expect(screen.getByText('¡Esa es! El error está en la línea 4.')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('radio', { name: 'area <- (base + altura) * 2' }))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar corrección' }))
    expect(screen.getByRole('status')).toHaveTextContent('todavía no muestra lo que debería')
    fireEvent.click(screen.getByRole('radio', { name: 'area <- base * altura' }))
    fireEvent.click(screen.getByRole('button', { name: 'Comprobar corrección' }))
    expect(screen.getByRole('status')).toHaveTextContent('¡Corregido!')
    expect(JSON.parse(window.localStorage.getItem('logicode.juegos.v1'))).toEqual({ error: ['error-1'] })
  })

  it('cambia el personaje del robot, lo recuerda y sigue diciendo hacia dónde mira', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(401, { title: 'Sin sesión' })))
    window.history.replaceState(null, '', '/juegos?juego=bloques&nivel=secuencia-3')
    const { container, unmount } = render(<App />)

    expect(await screen.findByRole('radio', { name: 'Flecha' })).toBeChecked()
    expect(container.querySelector('.robot-grid').textContent).toContain('▼')
    fireEvent.click(screen.getByRole('radio', { name: 'Fantasma' }))
    expect(container.querySelector('.robot-grid .robot-skin')).not.toBeNull()
    expect(container.querySelector('.robot-grid').textContent).not.toContain('▼')
    expect(screen.getByText(/Robot en la fila 1, columna 1, mirando abajo/)).toBeInTheDocument()
    expect(window.localStorage.getItem('logicode.juegos.personaje')).toBe('fantasma')
    unmount()

    render(<App />)
    expect(await screen.findByRole('radio', { name: 'Fantasma' })).toBeChecked()
  })
})
