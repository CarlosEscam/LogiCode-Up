import { useState } from 'react'
import { Link } from '../../shared/router/Link.jsx'
import { navigate, useLocation } from '../../shared/router/router.js'
import { TOOL_LABELS } from '../content/labels.js'
import BlocksGame from './BlocksGame.jsx'
import DeskGame from './DeskGame.jsx'
import DfdGame from './DfdGame.jsx'
import ErrorGame from './ErrorGame.jsx'
import OrderGame from './OrderGame.jsx'
import OutputGame from './OutputGame.jsx'
import { ARDUINO_LEVELS, BLOCK_LEVELS, DESK_LEVELS, DFD_LEVELS, ERROR_LEVELS, ORDER_LEVELS, OUTPUT_LEVELS } from './levels.js'
import { loadProgress, markSolved } from './progress.js'

// Para agregar un juego basta otra entrada aquí: herramienta y temas alimentan los filtros del catálogo.
const GAMES = [
  {
    key: 'bloques',
    label: 'Bloques',
    tool: 'SCRATCH',
    topics: ['Secuencias', 'Rutas', 'Ciclos', 'Condiciones', 'Depuración'],
    levels: BLOCK_LEVELS,
    Game: BlocksGame,
    about: 'Programa un robot con bloques: secuencias, ciclos, condiciones, rutas y depuración.',
  },
  {
    key: 'salida',
    label: '¿Qué imprime?',
    tool: 'PSEINT',
    topics: ['Variables', 'Condiciones', 'Ciclos', 'Arreglos', 'Matrices', 'Textos', 'Funciones'],
    levels: OUTPUT_LEVELS,
    Game: OutputGame,
    about: 'Lee el programa de PSeInt y elige lo que muestra en pantalla.',
  },
  {
    key: 'error',
    label: 'Caza el error',
    tool: 'PSEINT',
    topics: ['Depuración', 'Variables', 'Condiciones', 'Ciclos', 'Arreglos', 'Matrices', 'Textos', 'Funciones'],
    levels: ERROR_LEVELS,
    Game: ErrorGame,
    about: 'Cada programa tiene un error: encuentra la línea que falla y elige cómo corregirla.',
  },
  {
    key: 'escritorio',
    label: 'Prueba de escritorio',
    tool: 'PSEINT',
    topics: ['Variables', 'Condiciones', 'Ciclos', 'Arreglos', 'Textos', 'Funciones'],
    levels: DESK_LEVELS,
    Game: DeskGame,
    about: 'Sigue el programa paso a paso y llena la tabla con el valor de cada variable y lo que sale en pantalla.',
  },
  {
    key: 'orden',
    label: 'Ordena el algoritmo',
    tool: 'PSEINT',
    topics: ['Secuencias', 'Condiciones', 'Ciclos', 'Arreglos', 'Matrices', 'Textos'],
    levels: ORDER_LEVELS,
    Game: OrderGame,
    about: 'Pon las instrucciones en el orden correcto.',
  },
  {
    key: 'dfd',
    label: 'Completa el DFD',
    tool: 'DFD',
    topics: ['Secuencias', 'Condiciones', 'Ciclos'],
    levels: DFD_LEVELS,
    Game: DfdGame,
    about: 'Completa las casillas del diagrama de flujo.',
  },
  {
    key: 'arduino',
    label: '¿Qué hace este código?',
    tool: 'ARDUINO',
    topics: ['Sensores y actuadores', 'Variables', 'Condiciones', 'Ciclos'],
    levels: ARDUINO_LEVELS,
    Game: OutputGame,
    about: 'Lee el programa de Arduino y elige lo que pasa en la placa: LED, botón, sensor o Monitor Serie.',
  },
]

const TOPICS = ['Secuencias', 'Variables', 'Condiciones', 'Ciclos', 'Arreglos', 'Matrices', 'Textos', 'Funciones', 'Rutas', 'Depuración', 'Sensores y actuadores']
const TOOLS = [...new Set(GAMES.map((game) => game.tool))]

// Juegos de práctica (RF-22): abiertos a cualquiera, sin nota. El avance se guarda solo en este navegador.
// Sin ?juego= se muestra el catálogo; con él, el juego elegido.
export default function GamesPage() {
  const { query } = useLocation()
  const [progress, setProgress] = useState(loadProgress)
  const game = GAMES.find((item) => item.key === query.get('juego'))
  return game ? (
    <GameView game={game} levelId={query.get('nivel')} progress={progress} setProgress={setProgress} />
  ) : (
    <Catalog progress={progress} />
  )
}

function Catalog({ progress }) {
  const [filters, setFilters] = useState({ q: '', tool: '', topic: '' })
  const [search, setSearch] = useState('')
  const q = filters.q.toLowerCase()
  const shown = GAMES.filter(
    (game) =>
      (!filters.tool || game.tool === filters.tool) &&
      (!filters.topic || game.topics.includes(filters.topic)) &&
      (!q || [game.label, game.about, ...game.topics].some((text) => text.toLowerCase().includes(q))),
  )
  const levelCount = shown.reduce((total, game) => total + game.levels.length, 0)

  return (
    <>
      <section className="card card--wide page-head" aria-labelledby="juegos-titulo">
        <p className="eyebrow">Practica sin presión: no dan nota</p>
        <h1 id="juegos-titulo">Juegos de lógica</h1>
        <p className="page__note">Tu avance queda guardado en este navegador.</p>
        <form
          className="filters"
          role="search"
          onSubmit={(event) => {
            event.preventDefault()
            setFilters((previous) => ({ ...previous, q: search.trim() }))
          }}
        >
          <label>
            Buscar
            <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Nombre del juego o tema" />
          </label>
          <label>
            Herramienta
            <select value={filters.tool} onChange={(event) => setFilters((previous) => ({ ...previous, tool: event.target.value }))}>
              <option value="">Todas</option>
              {TOOLS.map((tool) => (
                <option key={tool} value={tool}>
                  {TOOL_LABELS[tool]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Tema
            <select value={filters.topic} onChange={(event) => setFilters((previous) => ({ ...previous, topic: event.target.value }))}>
              <option value="">Todos</option>
              {TOPICS.map((topic) => (
                <option key={topic}>{topic}</option>
              ))}
            </select>
          </label>
          <button type="submit" className="button">
            Buscar
          </button>
        </form>
      </section>

      <section className="page-body" aria-label="Juegos">
        <p className="page__note" role="status">
          {shown.length === 1 ? '1 juego' : `${shown.length} juegos`} · {levelCount} niveles
        </p>
        <ul className="materials materials--grid">
          {shown.map((game) => {
            const solved = progress[game.key] ?? []
            const done = solved.length
            const action = done ? 'Seguir jugando' : 'Jugar'
            // Seguir jugando abre el primer nivel sin resolver.
            const nextLevel = game.levels.find((item) => !solved.includes(item.id)) ?? game.levels[0]
            return (
              <li key={game.key} className="material game-card">
                <GameArt gameKey={game.key} />
                <div className="material__body">
                  <h2 className="material__title">{game.label}</h2>
                  <p className="material__meta">
                    <span className="badge">{TOOL_LABELS[game.tool]}</span>
                    {game.topics.map((topic) => (
                      <span key={topic} className="badge badge--muted">
                        {topic}
                      </span>
                    ))}
                  </p>
                  <p className="material__description">{game.about}</p>
                  <Progress done={done} total={game.levels.length} />
                </div>
                <div className="material__actions">
                  <Link to={`/juegos?juego=${game.key}&nivel=${nextLevel.id}`} className="button button--small" aria-label={`${action}: ${game.label}`}>
                    {action}
                  </Link>
                </div>
              </li>
            )
          })}
        </ul>
        {shown.length === 0 && <p>No hay juegos con estos filtros.</p>}
      </section>
    </>
  )
}

function Progress({ done, total }) {
  return (
    <div className="game-progress">
      <span>
        {done} de {total} niveles resueltos
      </span>
      <span className="game-progress__bar" aria-hidden="true">
        <span style={{ width: `${(100 * done) / total}%` }} />
      </span>
    </div>
  )
}

// Dibujo de cada juego en su tarjeta; es decorativo.
function GameArt({ gameKey }) {
  const art = {
    bloques: (
      <span className="game-art__grid">
        {'pxwwwwxxwwwwxxg'.split('').map((cell, index) => (
          <span key={index} className={`game-art__cell game-art__cell--${cell}`} />
        ))}
      </span>
    ),
    salida: <code>{'Para i <- 1 Hasta 3\n  Escribir i * 2\nFinPara\n// ¿Qué imprime?'}</code>,
    error: (
      <code>
        {'base <- 4\n'}
        <mark>{'area <- base + altura'}</mark>
        {'\nEscribir area'}
      </code>
    ),
    escritorio: <code>{'Paso     i  suma\nVuelta 1 1  1\nVuelta 2 2  3\nVuelta 3 3  ?'}</code>,
    orden: <code>{'1. Leer a, b\n2. suma <- a + b\n3. Escribir suma'}</code>,
    dfd: (
      <svg viewBox="0 0 150 96" width="150" height="96" focusable="false">
        <rect x="45" y="4" width="60" height="18" rx="9" fill="#fff" stroke="#2b2540" />
        <path d="M75 22v10M75 54v10" stroke="#2b2540" />
        <path d="M75 32l28 11-28 11-28-11z" fill="#fff" stroke="#2b2540" />
        <rect x="45" y="64" width="60" height="20" fill="#eee6f5" stroke="#56378a" strokeDasharray="4 3" />
        <text x="75" y="78" fontSize="10" textAnchor="middle" fill="#56378a">
          ?
        </text>
      </svg>
    ),
    arduino: <code>{'digitalWrite(13, HIGH);\ndelay(1000);\n// ¿Qué hace?'}</code>,
  }
  return (
    <div className={`game-art game-art--${gameKey}`} aria-hidden="true">
      {art[gameKey]}
    </div>
  )
}

function GameView({ game, levelId, progress, setProgress }) {
  const level = game.levels.find((item) => item.id === levelId) ?? game.levels[0]
  const solved = new Set(progress[game.key] ?? [])
  const go = (levelKey) => navigate(`/juegos?juego=${game.key}&nivel=${levelKey}`, { replace: true })
  const next = game.levels[game.levels.indexOf(level) + 1]

  return (
    <>
      <section className="card card--wide page-head" aria-labelledby="juegos-titulo">
        <p className="page__note">
          <Link to="/juegos">← Juegos</Link> · {TOOL_LABELS[game.tool]} · {game.topics.join(', ')}
        </p>
        <h1 id="juegos-titulo">{game.label}</h1>
        <p className="page__note">{game.about}</p>
        <Progress done={solved.size} total={game.levels.length} />
        <nav aria-label="Niveles" className="levels">
          {game.levels.map((item, position) => (
            <button
              key={item.id}
              type="button"
              className={`level${item.id === level.id ? ' level--current' : ''}${solved.has(item.id) ? ' level--done' : ''}`}
              aria-current={item.id === level.id ? 'step' : undefined}
              onClick={() => go(item.id)}
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
        onNext={next ? () => go(next.id) : null}
      />
    </>
  )
}
