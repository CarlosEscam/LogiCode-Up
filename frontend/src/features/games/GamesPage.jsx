import { useState } from 'react'
import { navigate, useLocation } from '../../shared/router/router.js'
import BlocksGame from './BlocksGame.jsx'
import DfdGame from './DfdGame.jsx'
import OrderGame from './OrderGame.jsx'
import OutputGame from './OutputGame.jsx'
import { BLOCK_LEVELS, DFD_LEVELS, ORDER_LEVELS, OUTPUT_LEVELS } from './levels.js'
import { loadProgress, markSolved } from './progress.js'

const GAMES = [
  { key: 'bloques', label: 'Bloques', levels: BLOCK_LEVELS, Game: BlocksGame, about: 'Programa un robot con bloques: secuencias, ciclos, condiciones, rutas y depuración.' },
  { key: 'salida', label: '¿Qué imprime?', levels: OUTPUT_LEVELS, Game: OutputGame, about: 'Lee el programa de PSeInt y elige lo que muestra en pantalla.' },
  { key: 'orden', label: 'Ordena el algoritmo', levels: ORDER_LEVELS, Game: OrderGame, about: 'Pon las instrucciones en el orden correcto.' },
  { key: 'dfd', label: 'Completa el DFD', levels: DFD_LEVELS, Game: DfdGame, about: 'Completa las casillas del diagrama de flujo.' },
]

// Juegos de práctica (RF-22): abiertos a cualquiera, sin nota. El avance se guarda solo en este navegador.
export default function GamesPage() {
  const { query } = useLocation()
  const game = GAMES.find((item) => item.key === query.get('juego')) ?? GAMES[0]
  const level = game.levels.find((item) => item.id === query.get('nivel')) ?? game.levels[0]
  const [progress, setProgress] = useState(loadProgress)
  const solved = new Set(progress[game.key] ?? [])
  const go = (gameKey, levelId) => navigate(`/juegos?juego=${gameKey}${levelId ? `&nivel=${levelId}` : ''}`, { replace: true })
  const index = game.levels.indexOf(level)
  const next = game.levels[index + 1]

  return (
    <>
      <section className="card card--wide" aria-labelledby="juegos-titulo">
        <h1 id="juegos-titulo">Juegos de lógica</h1>
        <p>Practica sin presión: no dan nota. Tu avance queda guardado en este navegador.</p>
        <div className="tabs" role="tablist" aria-label="Juegos">
          {GAMES.map((item) => (
            <button key={item.key} type="button" role="tab" aria-selected={item.key === game.key} onClick={() => go(item.key)}>
              {item.label} ({(progress[item.key] ?? []).length}/{item.levels.length})
            </button>
          ))}
        </div>
        <p className="page__note">{game.about}</p>
        <nav aria-label="Niveles" className="levels">
          {game.levels.map((item, position) => (
            <button
              key={item.id}
              type="button"
              className={`level${item.id === level.id ? ' level--current' : ''}`}
              aria-current={item.id === level.id ? 'step' : undefined}
              onClick={() => go(game.key, item.id)}
            >
              {position + 1}. {item.topic ? `${item.topic}: ` : ''}
              {item.title}
              {solved.has(item.id) && (
                <>
                  <span aria-hidden="true"> ✓</span>
                  <span className="visually-hidden"> (resuelto)</span>
                </>
              )}
            </button>
          ))}
        </nav>
      </section>
      <game.Game
        key={`${game.key}-${level.id}`}
        level={level}
        solved={solved.has(level.id)}
        onSolved={() => setProgress(markSolved(game.key, level.id))}
        onNext={next ? () => go(game.key, next.id) : null}
      />
    </>
  )
}
