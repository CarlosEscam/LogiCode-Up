import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiPost } from './httpClient.js'

function jsonResponse(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

afterEach(() => {
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

describe('httpClient', () => {
  it('envía el token CSRF de la cookie en las peticiones que cambian datos', async () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(200, { message: 'ok' })))

    await apiPost('/auth/password-recovery', { email: 'a@example.com' })

    const [url, options] = fetch.mock.calls[0]
    expect(url).toBe('/api/v1/auth/password-recovery')
    expect(options.headers['X-XSRF-TOKEN']).toBe('abc123')
    expect(JSON.parse(options.body)).toEqual({ email: 'a@example.com' })
  })

  it('pide el token CSRF cuando aún no existe la cookie', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation(async (url) => {
        if (url === '/api/v1/auth/csrf') {
          document.cookie = 'XSRF-TOKEN=nuevo; path=/'
          return jsonResponse(200, {})
        }
        return { ok: true, status: 204, headers: new Headers() }
      }),
    )

    await apiPost('/auth/logout')

    expect(fetch.mock.calls[0][0]).toBe('/api/v1/auth/csrf')
    expect(fetch.mock.calls[1][1].headers['X-XSRF-TOKEN']).toBe('nuevo')
  })

  it('expone el detalle y los errores por campo del Problem Details', async () => {
    document.cookie = 'XSRF-TOKEN=abc; path=/'
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(400, {
          title: 'Datos inválidos',
          detail: 'Revisa los campos marcados.',
          errors: [{ field: 'email', message: 'Es obligatorio.' }],
        }),
      ),
    )

    const error = await apiPost('/auth/teacher-requests', {}).catch((caught) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.message).toBe('Revisa los campos marcados.')
    expect(error.fieldErrors).toEqual({ email: 'Es obligatorio.' })
  })
})
