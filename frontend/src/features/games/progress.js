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

// Personaje del robot del juego de bloques (los dibujos están en skins.jsx).
const SKIN_KEY = 'logicode.juegos.personaje'

export const SKINS = [
  { id: 'flecha', label: 'Flecha' },
  { id: 'lapiz', label: 'Lápiz' },
  { id: 'cerebro', label: 'Cerebro' },
  { id: 'fantasma', label: 'Fantasma' },
]

export function loadSkin() {
  try {
    const saved = window.localStorage.getItem(SKIN_KEY)
    return SKINS.some((skin) => skin.id === saved) ? saved : 'flecha'
  } catch {
    return 'flecha'
  }
}

export function saveSkin(skin) {
  try {
    window.localStorage.setItem(SKIN_KEY, skin)
  } catch {
    // Sin almacenamiento: el personaje dura mientras la página siga abierta.
  }
}
