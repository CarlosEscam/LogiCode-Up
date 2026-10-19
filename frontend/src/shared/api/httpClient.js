// Cliente HTTP mínimo para la API REST de LogiCode.
// Todas las rutas son relativas al prefijo versionado; la interfaz nunca decide permisos ni notas.

export const API_BASE = '/api/v1'

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || problem?.title || `Error ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export async function apiGet(path, { signal } = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { Accept: 'application/json' },
    signal,
  })
  if (!response.ok) {
    let problem = null
    try {
      problem = await response.json()
    } catch {
      // La respuesta de error no trae JSON (p. ej. el backend no está disponible).
    }
    throw new ApiError(response.status, problem)
  }
  return response.json()
}
