import { useCallback, useEffect, useState } from 'react'
import { apiGet } from './httpClient.js'

// Consulta paginada (PageResult del backend) con recarga manual tras una acción.
export function usePagedQuery(path, params) {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null))
  const [version, setVersion] = useState(0)
  const key = `${path}?${query}#${version}`
  const [state, setState] = useState({ key: null, data: null, error: null })

  useEffect(() => {
    const controller = new AbortController()
    apiGet(key.slice(0, key.lastIndexOf('#')), { signal: controller.signal })
      .then((data) => setState({ key, data, error: null }))
      .catch((error) => {
        if (error.name !== 'AbortError') setState({ key, data: null, error: error.message })
      })
    return () => controller.abort()
  }, [key])

  const reload = useCallback(() => setVersion((value) => value + 1), [])
  const status = state.key !== key ? 'loading' : state.error ? 'error' : 'ready'
  // Mientras carga la página siguiente se siguen mostrando los datos anteriores.
  return { status, data: state.data, error: state.error, reload }
}
