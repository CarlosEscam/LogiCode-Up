import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'
import { MAX_STEPS, boardProblem, countBlocks, programText, runProgram } from './blocks.js'
import { BLOCK_LEVELS, DFD_LEVELS, ORDER_LEVELS, OUTPUT_LEVELS, dfdBlanks, shuffle } from './levels.js'

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

  it('las respuestas de los otros juegos están entre sus opciones', () => {
    OUTPUT_LEVELS.forEach((level) => expect(level.options[level.answer]).toBeDefined())
    DFD_LEVELS.forEach((level) => dfdBlanks(level).forEach((blank) => expect(level.options).toContain(blank.answer)))
    ORDER_LEVELS.forEach((level) => expect(shuffle(level.lines)).not.toEqual(level.lines))
  })
})

describe('Página de juegos', () => {
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
    expect(screen.getByRole('tab', { name: '¿Qué imprime? (1/6)' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Siguiente nivel' }))
    expect(screen.getByRole('heading', { name: 'Si … SiNo' })).toBeInTheDocument()
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
})
