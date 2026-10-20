import { useEffect } from 'react'

// Vuelve a consultar cada cierto tiempo mientras algo sigue en proceso (p. ej. una entrega que la IA califica).
export function useRefreshWhile(active, reload, intervalMs = 10000) {
  useEffect(() => {
    if (!active) return undefined
    const timer = setInterval(reload, intervalMs)
    return () => clearInterval(timer)
  }, [active, reload, intervalMs])
}
