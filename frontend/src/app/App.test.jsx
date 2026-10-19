import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'

const ADMIN = {
  id: 'a1',
  cedula: '900001',
  fullName: 'Administradora Uno',
  role: 'ADMIN',
  fullPrivileges: false,
  email: 'admin@example.com',
  emailVerified: true,
  permissions: ['MANAGE_USERS', 'VIEW_AUDIT', 'APPROVE_TEACHERS'],
}

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

function stubApi(routes) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url, options = {}) => {
      const key = `${options.method ?? 'GET'} ${url.split('?')[0]}`
      const handler = routes[key]
      if (!handler) return respond(404, { title: 'No encontrado' })
      return typeof handler === 'function' ? handler(url, options) : handler
    }),
  )
}

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

describe('App', () => {
  it('envía a iniciar sesión cuando se abre una página privada sin sesión', async () => {
    window.history.replaceState(null, '', '/mi-cuenta')
    stubApi({ 'GET /api/v1/me': respond(401, { title: 'No autenticado' }), 'GET /api/v1/system/info': respond(502) })

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/ingresar')
  })

  it('inicia sesión y muestra solo las opciones que permite la cuenta', async () => {
    window.history.replaceState(null, '', '/ingresar')
    let loggedIn = false
    stubApi({
      'GET /api/v1/me': () => (loggedIn ? respond(200, ADMIN) : respond(401, {})),
      'POST /api/v1/auth/login': (url, options) => {
        loggedIn = JSON.parse(options.body).password === 'Clave#2026'
        return loggedIn ? respond(200, {}) : respond(401, { detail: 'Cédula o contraseña incorrectas.' })
      },
    })

    render(<App />)
    fireEvent.change(await screen.findByLabelText('Cédula'), { target: { value: '900001' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'Clave#2026' } })
    await act(async () => fireEvent.click(screen.getByRole('button', { name: 'Ingresar' })))

    expect(await screen.findByRole('heading', { name: 'Mi cuenta' })).toBeInTheDocument()
    const nav = screen.getByRole('navigation', { name: 'Principal' })
    expect(nav).toHaveTextContent('Usuarios')
    expect(nav).toHaveTextContent('Auditoría')
    expect(nav).not.toHaveTextContent('Habilitar estudiantes')
  })

  it('muestra el error de la API cuando la contraseña es incorrecta', async () => {
    window.history.replaceState(null, '', '/ingresar')
    stubApi({
      'GET /api/v1/me': respond(401, {}),
      'POST /api/v1/auth/login': respond(401, { detail: 'Cédula o contraseña incorrectas.' }),
    })

    render(<App />)
    fireEvent.change(await screen.findByLabelText('Cédula'), { target: { value: '900001' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'mala' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Cédula o contraseña incorrectas.')
  })

  it('confirma el correo una sola vez con el token del enlace', async () => {
    window.history.replaceState(null, '', '/verificar-correo?token=abc')
    stubApi({
      'GET /api/v1/me': respond(401, {}),
      'POST /api/v1/auth/email-verifications/confirm': respond(200, {
        email: 'admin@example.com',
        passwordSetupRequired: true,
        message: 'Correo verificado.',
      }),
    })

    render(<App />)

    expect(await screen.findByText('Correo verificado.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Crea una aquí' })).toHaveAttribute('href', '/recuperar-contrasena')
    const confirmCalls = fetch.mock.calls.filter(([url]) => url.endsWith('/confirm'))
    expect(confirmCalls).toHaveLength(1)
  })
})
