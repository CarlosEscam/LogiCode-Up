import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import CalibrationPanel from './CalibrationPanel.jsx'

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

const OPEN = { calibrating: true, minimum: 10, compared: 3, agreeing: 2, wouldHaveFailed: 1, concordancePercent: 66, closedAt: null, canChange: true }

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Calibración de la IA', () => {
  it('muestra la concordancia y no deja cerrar sin las comparaciones necesarias', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(200, OPEN)))
    render(<CalibrationPanel courseId="c1" />)
    expect(await screen.findByText('En calibración')).toBeInTheDocument()
    expect(screen.getByText('3 de 10 necesarias')).toBeInTheDocument()
    expect(screen.getByText('66 % (2)')).toBeInTheDocument()
    const close = screen.getByRole('button', { name: 'Dejar que la IA publique sola' })
    expect(close).toBeDisabled()
    expect(close).toHaveAccessibleDescription('Faltan 7 notas comparadas para poder cerrar la calibración.')
    expect(screen.getByText('Faltan 7 notas comparadas para poder cerrar la calibración.')).toBeInTheDocument()
  })

  it('cierra la calibración cuando hay suficientes comparaciones', async () => {
    let state = { ...OPEN, compared: 10, agreeing: 9, wouldHaveFailed: 1, concordancePercent: 90 }
    const fetchMock = vi.fn(async (url, options = {}) => {
      if (options.method === 'POST' && url.endsWith('/ai-calibration/close')) {
        state = { ...state, calibrating: false, closedAt: '2026-11-04T15:00:00Z' }
      }
      return respond(200, state)
    })
    vi.stubGlobal('fetch', fetchMock)
    render(<CalibrationPanel courseId="c1" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Dejar que la IA publique sola' }))
    expect(await screen.findByText('Calibración cerrada')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Volver a calibrar' })).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url, o]) => url.endsWith('/ai-calibration/close') && o?.method === 'POST')).toBe(true)
  })

  it('no muestra botones a quien solo consulta', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => respond(200, { ...OPEN, canChange: false })))
    render(<CalibrationPanel courseId="c1" />)
    expect(await screen.findByText('En calibración')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
