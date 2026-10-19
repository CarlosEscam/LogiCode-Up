import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import SystemStatus from './SystemStatus.jsx'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('SystemStatus', () => {
  it('muestra la versión del esquema cuando la API responde', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          application: 'LogiCode',
          version: '0.1.0',
          environment: 'local',
          serverTime: '2026-10-18T20:00:00-05:00',
          timeZone: 'America/Bogota',
          schemaVersion: '1',
        }),
      }),
    )

    render(<SystemStatus />)

    expect(await screen.findByText('Esquema en versión 1')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith('/api/v1/system/info', expect.anything())
  })

  it('muestra un mensaje comprensible cuando el servidor no responde', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 502, json: async () => { throw new Error() } }))

    render(<SystemStatus />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible conectar con el servidor')
  })
})
