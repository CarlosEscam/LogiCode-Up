// Escenario del emulador de Scratch. El programa corre en el intérprete de PSeInt; los bloques de movimiento y esperar
// escriben una línea con marca («§mover 10», «§girar -15», «§ir 0 50», «§esperar 1») y el resto de líneas es lo que el
// personaje dice. Aquí se convierte esa salida en eventos y se calcula dónde está el personaje tras los primeros n.
// Coordenadas como en Scratch: el centro es (0, 0), x de −240 a 240, y de −180 a 180, dirección 90 = hacia la derecha.

export const STAGE = { width: 480, height: 360 }
const MARK = /^§(mover|girar|ir|esperar) (.*)$/

export function stageEvents(output) {
  return output
    .split('\n')
    .slice(0, -1) // la última pieza está vacía o es una línea a medio escribir (Sin Saltar)
    .map((line) => {
      const match = MARK.exec(line)
      if (!match) return { type: 'decir', text: line }
      const numbers = match[2].trim().split(/\s+/).map(Number)
      if (numbers.some((value) => !Number.isFinite(value))) return { type: 'decir', text: line }
      return { type: match[1], values: numbers }
    })
}

export const isMark = (line) => MARK.test(line)

const clamp = (value, limit) => Math.max(-limit, Math.min(limit, value))
const round = (value) => Math.round(value * 100) / 100

export function stageAt(events, count) {
  const sprite = { x: 0, y: 0, dir: 90, bubble: '' }
  for (const event of events.slice(0, count)) {
    const [a, b] = event.values ?? []
    if (event.type === 'decir') sprite.bubble = event.text
    else if (event.type === 'mover') {
      const radians = (sprite.dir * Math.PI) / 180
      sprite.x = clamp(round(sprite.x + a * Math.sin(radians)), STAGE.width / 2)
      sprite.y = clamp(round(sprite.y + a * Math.cos(radians)), STAGE.height / 2)
    } else if (event.type === 'girar') sprite.dir = (((sprite.dir + a) % 360) + 360) % 360
    else if (event.type === 'ir') {
      sprite.x = clamp(a, STAGE.width / 2)
      sprite.y = clamp(b, STAGE.height / 2)
    }
  }
  return sprite
}

// Pausa antes de mostrar el evento: movimiento visible paso a paso y «esperar» real (hasta 5 s).
export function eventDelay(event) {
  if (event.type === 'esperar') return Math.min(5000, Math.max(0, event.values[0] * 1000))
  return event.type === 'decir' ? 120 : 60
}
