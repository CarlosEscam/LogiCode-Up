import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError, apiGet, apiPost } from '../../shared/api/httpClient.js'
import { SessionContext } from './sessionContext.js'

// Mantiene la cuenta en sesión (GET /me). Los permisos que trae solo sirven para mostrar u
// ocultar opciones; el backend vuelve a verificarlos en cada petición.
export function SessionProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', account: null })

  const refresh = useCallback(
    () =>
      loadAccount().then((account) => {
        setState({ status: 'ready', account })
        return account
      }),
    [],
  )

  useEffect(() => {
    let active = true
    loadAccount().then((account) => active && setState({ status: 'ready', account }))
    return () => {
      active = false
    }
  }, [])

  const login = useCallback(
    async (cedula, password) => {
      await apiPost('/auth/login', { cedula, password })
      return refresh()
    },
    [refresh],
  )

  const logout = useCallback(async () => {
    try {
      await apiPost('/auth/logout')
    } finally {
      setState({ status: 'ready', account: null })
    }
  }, [])

  // Para cuando el backend cerró la sesión (p. ej. tras cambiar la contraseña).
  const clear = useCallback(() => setState({ status: 'ready', account: null }), [])

  const value = useMemo(() => {
    const permissions = new Set(state.account?.permissions ?? [])
    return { ...state, can: (permission) => permissions.has(permission), refresh, login, logout, clear }
  }, [state, refresh, login, logout, clear])

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

async function loadAccount() {
  try {
    return await apiGet('/me')
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 401) console.warn('No se pudo consultar la sesión', error)
    return null
  }
}
