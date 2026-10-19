import { useEffect, useState } from 'react'
import { apiGet } from '../../shared/api/httpClient.js'

export function useSystemInfo() {
  const [state, setState] = useState({ status: 'loading', data: null })

  useEffect(() => {
    const controller = new AbortController()
    apiGet('/system/info', { signal: controller.signal })
      .then((data) => setState({ status: 'ready', data }))
      .catch((error) => {
        if (error.name !== 'AbortError') setState({ status: 'error', data: null })
      })
    return () => controller.abort()
  }, [])

  return state
}
