import { useEffect, useState } from 'react'

// Borradores del Emulador en el navegador del estudiante (localStorage); nada se envía al servidor.
export function readDraft(key, fallback) {
  try {
    return localStorage.getItem(`logicode.emulador.${key}`) ?? fallback
  } catch {
    return fallback
  }
}

export function writeDraft(key, text) {
  try {
    localStorage.setItem(`logicode.emulador.${key}`, text)
  } catch {
    // Sin almacenamiento (modo privado): el trabajo sigue en pantalla y se puede descargar.
  }
}

// Guarda el borrador cada vez que cambia el texto.
export function useDraft(key, fallback) {
  const [text, setText] = useState(() => readDraft(key, fallback))
  useEffect(() => writeDraft(key, text), [key, text])
  return [text, setText]
}
