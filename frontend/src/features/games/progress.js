// Avance de los juegos en este navegador. Es solo una comodidad: si el navegador no deja guardar
// (modo privado, almacenamiento bloqueado), los juegos funcionan igual sin recordar el avance.
const KEY = 'logicode.juegos.v1'

export function loadProgress() {
  try {
    return JSON.parse(window.localStorage.getItem(KEY)) ?? {}
  } catch {
    return {}
  }
}

export function markSolved(game, levelId) {
  const progress = loadProgress()
  const solved = new Set(progress[game] ?? [])
  solved.add(levelId)
  const next = { ...progress, [game]: [...solved] }
  try {
    window.localStorage.setItem(KEY, JSON.stringify(next))
  } catch {
    // Sin almacenamiento: el avance dura solo mientras la página siga abierta.
  }
  return next
}
