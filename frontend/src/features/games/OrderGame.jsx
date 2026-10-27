import { useState } from 'react'
import Solved from './Solved.jsx'
import { shuffle } from './levels.js'

export default function OrderGame({ level, onSolved, onNext }) {
  const [order, setOrder] = useState(() => shuffle(level.lines))
  const [result, setResult] = useState(null)

  function move(index, delta) {
    setOrder((current) => {
      const next = [...current]
      ;[next[index], next[index + delta]] = [next[index + delta], next[index]]
      return next
    })
    setResult(null)
  }

  function check() {
    // Se comparan textos: dos líneas iguales pueden ir en cualquiera de sus lugares.
    const ok = order.every((line, index) => line === level.lines[index])
    setResult(ok ? 'ok' : order.findIndex((line, index) => line !== level.lines[index]))
    if (ok) onSolved()
  }

  return (
    <section className="card card--wide" aria-labelledby="nivel-titulo">
      <h2 id="nivel-titulo">{level.title}</h2>
      <p className="page__note">Usa Subir y Bajar para ordenar las instrucciones. La primera y la última ya están en su lugar.</p>
      <ol className="order-lines">
        <li className="order-line order-line--fixed">
          <code>{level.first}</code>
        </li>
        {order.map((line, index) => (
          <li key={`${line}-${index}`} className="order-line">
            <code className="order-line__code">{line}</code>
            <span className="actions">
              <button
                type="button"
                className="button button--secondary button--small"
                disabled={index === 0}
                aria-label={`Subir: ${line.trim()}`}
                onClick={() => move(index, -1)}
              >
                Subir
              </button>
              <button
                type="button"
                className="button button--secondary button--small"
                disabled={index === order.length - 1}
                aria-label={`Bajar: ${line.trim()}`}
                onClick={() => move(index, 1)}
              >
                Bajar
              </button>
            </span>
          </li>
        ))}
        <li className="order-line order-line--fixed">
          <code>{level.last}</code>
        </li>
      </ol>
      <button type="button" className="button" onClick={check}>
        Comprobar
      </button>
      {result === 'ok' && <Solved onNext={onNext}>¡Bien! El algoritmo quedó en orden.</Solved>}
      {typeof result === 'number' && (
        <p className="notice notice--error" role="status">
          Todavía no. La primera línea fuera de lugar es la número {result + 2}: piensa qué debe pasar antes.
        </p>
      )}
    </section>
  )
}
