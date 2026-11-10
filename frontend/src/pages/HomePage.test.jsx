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

describe('Inicio con sesión de estudiante', () => {
  it('muestra sus pendientes y oculta el estado técnico de la plataforma', async () => {
    const student = { id: 's1', fullName: 'Ana Torres', role: 'STUDENT', permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'ANSWER_ACTIVITIES'] }
    const routes = {
      '/api/v1/me': student,
      '/api/v1/courses': [{ id: 'c1', name: 'Pensamiento Computacional' }],
      '/api/v1/courses/c1/activities': [
        { id: 'a1', title: 'Taller 2', progress: { state: 'OPEN', deadline: new Date(Date.now() + 2 * 864e5 + 6e4).toISOString() } },
        { id: 'a2', title: 'Tarea 1', progress: { state: 'SUBMITTED', deadline: '2026-01-01T00:00:00Z' } },
      ],
    }
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url) => {
        const body = routes[url.split('?')[0]]
        return { ok: Boolean(body), status: body ? 200 : 404, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body ?? {} }
      }),
    )

    render(<App />)

    expect(await screen.findByRole('link', { name: 'Taller 2' })).toHaveAttribute('href', '/actividades/a1')
    expect(screen.getByText('· faltan 2 días')).toBeInTheDocument()
    expect(screen.queryByText('Tarea 1')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Estado de la plataforma' })).not.toBeInTheDocument()
  })
})
