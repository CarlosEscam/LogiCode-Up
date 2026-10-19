import { useSyncExternalStore } from 'react'

// Enrutador mínimo basado en la History API. Evita una dependencia nueva mientras la
// aplicación tenga pocas rutas planas; Nginx ya devuelve index.html para cualquier ruta.

const listeners = new Set()

function subscribe(listener) {
  listeners.add(listener)
  window.addEventListener('popstate', listener)
  return () => {
    listeners.delete(listener)
    window.removeEventListener('popstate', listener)
  }
}

function snapshot() {
  return window.location.pathname + window.location.search
}

export function navigate(to, { replace = false } = {}) {
  if (to === snapshot()) return
  window.history[replace ? 'replaceState' : 'pushState'](null, '', to)
  window.scrollTo?.(0, 0)
  listeners.forEach((listener) => listener())
}

export function useLocation() {
  const current = useSyncExternalStore(subscribe, snapshot)
  const url = new URL(current, window.location.origin)
  return { path: url.pathname, query: url.searchParams }
}
