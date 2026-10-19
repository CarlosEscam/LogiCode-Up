// Cliente HTTP mínimo para la API REST de LogiCode.
// Todas las rutas son relativas al prefijo versionado; la interfaz nunca decide permisos ni notas.
// La sesión viaja en una cookie HttpOnly; las peticiones que cambian datos llevan el token CSRF
// que el backend deja en la cookie XSRF-TOKEN (patrón SPA de Spring Security).

export const API_BASE = '/api/v1'
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || problem?.title || `Error ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  // Errores de validación por campo devueltos en el Problem Details ({ field, message }).
  get fieldErrors() {
    return Object.fromEntries((this.problem?.errors ?? []).map((error) => [error.field, error.message]))
  }
}

export async function apiGet(path, { signal } = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin',
    signal,
  })
  return readResponse(response)
}

export function apiPost(path, body) {
  return send('POST', path, body)
}

export function apiPatch(path, body) {
  return send('PATCH', path, body)
}

export function apiPut(path, body) {
  return send('PUT', path, body)
}

export function apiDelete(path) {
  return send('DELETE', path)
}

// Envía un archivo como multipart/form-data en el campo "file".
export function apiUpload(path, file) {
  const form = new FormData()
  form.append('file', file)
  return send('POST', path, form)
}

// Envía un formulario multipart con avance de carga (archivos grandes: hasta 1 GB en videos).
// fetch no informa el avance de la subida, por eso se usa XMLHttpRequest.
export async function apiSendForm(path, form, { onProgress } = {}) {
  const token = await csrfToken()
  const response = await new Promise((resolve, reject) => {
    const request = new XMLHttpRequest()
    request.open('POST', `${API_BASE}${path}`)
    request.withCredentials = true
    request.setRequestHeader('Accept', 'application/json')
    request.setRequestHeader(CSRF_HEADER, token)
    if (onProgress) {
      request.upload.onprogress = (event) => {
        if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
    request.onload = () =>
      resolve({
        ok: request.status >= 200 && request.status < 300,
        status: request.status,
        json: async () => JSON.parse(request.responseText),
      })
    request.onerror = () => reject(new TypeError('Fallo de red'))
    request.send(form)
  })
  return readResponse(response)
}

async function send(method, path, body, retried = false) {
  const isForm = body instanceof FormData
  const headers = { Accept: 'application/json', [CSRF_HEADER]: await csrfToken() }
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json'

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  })

  // Un 403 sin Problem Details suele indicar un token CSRF vencido (p. ej. tras cerrar sesión):
  // se pide uno nuevo y se reintenta una sola vez.
  if (response.status === 403 && !retried && !isProblem(response)) {
    await fetchCsrfToken()
    return send(method, path, body, true)
  }
  return readResponse(response)
}

async function csrfToken() {
  return readCookie(CSRF_COOKIE) ?? (await fetchCsrfToken())
}

async function fetchCsrfToken() {
  await fetch(`${API_BASE}/auth/csrf`, { credentials: 'same-origin', headers: { Accept: 'application/json' } })
  return readCookie(CSRF_COOKIE) ?? ''
}

function readCookie(name) {
  const entry = document.cookie.split('; ').find((cookie) => cookie.startsWith(`${name}=`))
  return entry ? decodeURIComponent(entry.slice(name.length + 1)) : null
}

function isProblem(response) {
  return (response.headers?.get?.('Content-Type') ?? '').includes('json')
}

async function readResponse(response) {
  if (!response.ok) {
    let problem = null
    try {
      problem = await response.json()
    } catch {
      // La respuesta de error no trae JSON (p. ej. el backend no está disponible).
    }
    throw new ApiError(response.status, problem)
  }
  if (response.status === 204) return null
  return response.json()
}
