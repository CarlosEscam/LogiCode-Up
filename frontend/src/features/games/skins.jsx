// Personajes del robot del juego de bloques. Cada uno muestra hacia dónde mira: la flecha y el lápiz giran;
// el cerebro y el fantasma siguen de pie, miran con los ojos hacia ese lado y llevan un triángulo en el borde.
// Es solo apariencia: el texto que describe la posición y la dirección no cambia.
import { SKINS } from './progress.js'

const ARROWS = { N: '▲', E: '▶', S: '▼', O: '◀' }
const ROTATE = { N: -90, E: 0, S: 90, O: 180 }
const LOOK = { N: [0, -1.4], E: [1.4, 0], S: [0, 1.4], O: [1.4, 0] }
const INK = '#1f2328'

export function RobotSkin({ skin, dir }) {
  if (skin === 'lapiz') {
    // Dibujado mirando a la derecha; hacia la izquierda se refleja para que la cara no quede de cabeza.
    const turn = dir === 'O' ? 'translate(40 0) scale(-1 1)' : `rotate(${ROTATE[dir]} 20 20)`
    return (
      <svg className="robot-skin" viewBox="0 0 40 40" aria-hidden="true" focusable="false">
        <g transform={turn}>
          <Pencil />
        </g>
      </svg>
    )
  }
  if (skin === 'cerebro' || skin === 'fantasma') {
    const Character = skin === 'cerebro' ? Brain : Ghost
    return (
      <svg className="robot-skin" viewBox="0 0 40 40" aria-hidden="true" focusable="false">
        <g transform={`${dir === 'O' ? 'translate(40 0) scale(-1 1) ' : ''}translate(20 20) scale(0.8) translate(-20 -20)`}>
          <Character look={LOOK[dir]} />
        </g>
        <polygon points="39.5,20 34.5,15.5 34.5,24.5" fill="currentColor" stroke="#fff" strokeWidth="1" transform={`rotate(${ROTATE[dir]} 20 20)`} />
      </svg>
    )
  }
  return ARROWS[dir]
}

// El cerebro de pie, sin flecha de dirección: es la mascota de la página de inicio y de Iniciar sesión.
export function BrainMascot() {
  return (
    <svg className="mascot__brain" viewBox="6 2 28 36" aria-hidden="true" focusable="false">
      <Brain look={[0.6, 0]} />
    </svg>
  )
}

export function SkinPicker({ value, onChange }) {
  return (
    <fieldset className="skin-picker">
      <legend>Personaje del robot</legend>
      {SKINS.map((skin) => (
        <label key={skin.id} className={`skin-picker__option${skin.id === value ? ' skin-picker__option--current' : ''}`}>
          <input type="radio" name="personaje" value={skin.id} checked={skin.id === value} onChange={() => onChange(skin.id)} />
          <span className="skin-picker__preview" aria-hidden="true">
            <RobotSkin skin={skin.id} dir="E" />
          </span>
          {skin.label}
        </label>
      ))}
    </fieldset>
  )
}

function Eyes({ look, children }) {
  return <g transform={`translate(${look[0]} ${look[1]})`}>{children}</g>
}

function Pencil() {
  return (
    <g stroke={INK} strokeWidth="1.4" strokeLinejoin="round">
      <rect x="2.5" y="13" width="7" height="14" rx="3" fill="#e9c3b8" />
      <rect x="8" y="13" width="4" height="14" fill="#3d3d3d" />
      <rect x="11.5" y="13" width="16.5" height="14" fill="#eadcc0" />
      <path d="M28 13 L37 20 L28 27 Q30 23.5 28 20 Q30 16.5 28 13 Z" fill="#f7f0e1" />
      <path d="M34.2 17.7 L38.5 20 L34.2 22.3 Z" fill={INK} />
      <path d="M13.5 23.5 H26 M13.5 25.6 H26" stroke="#b6a17c" strokeWidth="0.9" />
      <circle cx="16.5" cy="15.5" r="4" fill="#fff" />
      <circle cx="23.5" cy="15.5" r="4" fill="#fff" />
      <circle cx="17.8" cy="16" r="2.1" fill={INK} stroke="none" />
      <circle cx="24.8" cy="16" r="2.1" fill={INK} stroke="none" />
      <circle cx="18.4" cy="15.2" r="0.7" fill="#fff" stroke="none" />
      <circle cx="25.4" cy="15.2" r="0.7" fill="#fff" stroke="none" />
      <path d="M17 21.2 Q20 23.6 23 21.2" fill="none" strokeLinecap="round" />
    </g>
  )
}

// Las nubes del cerebro: primero con borde grueso y encima sin borde, para que solo quede el contorno exterior.
const LOBES = [
  [12.5, 15, 6.5],
  [20, 10.5, 7],
  [27.5, 15, 6.5],
  [14.5, 22, 6.5],
  [25.5, 22, 6.5],
  [20, 18.5, 7.5],
]

function Brain({ look }) {
  return (
    <g strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 28 L14.5 34 M23 28 L25.5 34" stroke={INK} strokeWidth="2" />
      <ellipse cx="13.5" cy="35.2" rx="4.6" ry="2.6" fill="#2f8a57" stroke={INK} strokeWidth="1.3" />
      <ellipse cx="26.5" cy="35.2" rx="4.6" ry="2.6" fill="#2f8a57" stroke={INK} strokeWidth="1.3" />
      <circle cx="11.2" cy="35.4" r="1" fill="#fff" />
      <circle cx="28.8" cy="35.4" r="1" fill="#fff" />
      {LOBES.map(([cx, cy, r]) => (
        <circle key={`borde-${cx}-${cy}`} cx={cx} cy={cy} r={r} fill={INK} stroke={INK} strokeWidth="3" />
      ))}
      {LOBES.map(([cx, cy, r]) => (
        <circle key={`relleno-${cx}-${cy}`} cx={cx} cy={cy} r={r} fill="#f49ac1" />
      ))}
      <path d="M20 4 Q18 6.5 20.5 9 M9.5 13 Q12 10.5 14.5 12.5 M23.5 7 Q26 9.5 28.5 8 M30.5 12 Q32.5 14.5 31 17 M9 19 Q10.5 21.5 9.5 24 M29.5 21 Q27.5 24 24.5 24.5 M15 26.5 Q17.5 25.5 19.5 27" fill="none" stroke="#c2185b" strokeWidth="1.3" strokeLinecap="round" />
      <ellipse cx="16.8" cy="16.5" rx="3.2" ry="3.4" fill="#fff" stroke={INK} strokeWidth="1.2" />
      <ellipse cx="23.6" cy="16.5" rx="3.2" ry="3.4" fill="#fff" stroke={INK} strokeWidth="1.2" />
      <Eyes look={look}>
        <circle cx="16.8" cy="17" r="1.7" fill={INK} />
        <circle cx="23.6" cy="17" r="1.7" fill={INK} />
      </Eyes>
      <path d="M13.6 15 Q16.8 12.4 20 15 M20.4 15 Q23.6 12.4 26.8 15" fill="#f49ac1" stroke={INK} strokeWidth="1.2" />
      <path d="M15.5 21.5 Q20 27 24.5 21.5 Z" fill="#5c1030" stroke={INK} strokeWidth="1.2" />
      <path d="M17 22.2 H23" stroke="#fff" strokeWidth="1.2" />
    </g>
  )
}

function Ghost({ look }) {
  return (
    <g strokeLinejoin="round">
      <ellipse cx="13.5" cy="35.4" rx="4.8" ry="2.5" fill="#5b8fd9" stroke={INK} strokeWidth="1.3" />
      <ellipse cx="26.5" cy="35.4" rx="4.8" ry="2.5" fill="#5b8fd9" stroke={INK} strokeWidth="1.3" />
      <path
        d="M8 31 V18 A12 12 0 0 1 32 18 V31 L28 28.5 L24 31 L20 28.5 L16 31 L12 28.5 Z"
        fill="#fbf5e1"
        stroke={INK}
        strokeWidth="1.6"
      />
      <path d="M28 12 Q30.5 15 30.5 20" fill="none" stroke="#e3d6ad" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="33.5" cy="21.5" r="2.4" fill="#fff" stroke={INK} strokeWidth="1.2" />
      <Eyes look={look}>
        <ellipse cx="16.5" cy="17" rx="2.4" ry="3.8" fill={INK} />
        <ellipse cx="23" cy="17" rx="2.4" ry="3.8" fill={INK} />
        <ellipse cx="17.1" cy="15.4" rx="0.8" ry="1.2" fill="#fff" />
        <ellipse cx="23.6" cy="15.4" rx="0.8" ry="1.2" fill="#fff" />
      </Eyes>
    </g>
  )
}
