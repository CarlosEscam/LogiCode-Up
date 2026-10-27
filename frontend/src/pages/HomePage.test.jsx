import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../app/App.jsx'

afterEach(() => vi.unstubAllGlobals())

describe('Página de inicio (RF-23)', () => {
  it('presenta misión, visión y explicación de la plataforma a un visitante', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: false, status: 401, headers: new Headers(), json: async () => ({}) })))

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Entrar a LogiCode' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Misión' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Visión' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Qué puedes hacer' })).toBeInTheDocument()
  })
})
