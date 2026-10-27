import { useState } from 'react'
import Solved from './Solved.jsx'

// Caza el error: primero se marca la línea que falla y, cuando se encuentra, se elige cómo corregirla.
export default function ErrorGame({ level, onSolved, onNext }) {
  const [line, setLine] = useState(null)
  const [found, setFound] = useState(false)
  const [lineChecked, setLineChecked] = useState(false)
  const [fix, setFix] = useState(null)
  const [fixResult, setFixResult] = useState(null) // 'ok' o 'wrong'

  function checkLine(event) {
    event.preventDefault()
    setLineChecked(true)
    setFound(line === level.bug)
  }

  function checkFix(event) {
    event.preventDefault()
    const ok = fix === level.fix
    setFixResult(ok ? 'ok' : 'wrong')
    if (ok) onSolved()
  }

  return (
    <section className="card card--wide" aria-labelledby="nivel-titulo">
      <h2 id="nivel-titulo">{level.title}</h2>
      <p>{level.goal}</p>
      <dl className="error-outputs">
        <dt>Debería mostrar</dt>
        <dd>
          <code className="error-outputs__text">{level.expected}</code>
        </dd>
        <dt>Muestra</dt>
        <dd>{level.output == null ? 'No termina nunca: el ciclo se repite sin fin.' : <code className="error-outputs__text">{level.output}</code>}</dd>
      </dl>
      <form onSubmit={checkLine}>
        <fieldset className="choices error-lines" disabled={found}>
          <legend>1. ¿Qué línea tiene el error?</legend>
          {level.lines.map((text, index) =>
            text.trim() ? (
              <label key={index} className={found && index === level.bug ? 'error-lines__line error-lines__line--bug' : 'error-lines__line'}>
                <input type="radio" name="linea" aria-label={`Línea ${index + 1}: ${text.trim()}`} checked={line === index} onChange={() => { setLine(index); setLineChecked(false) }} />
                <span className="error-lines__number">{index + 1}</span>
                <code className="error-lines__code" style={{ paddingLeft: `${(text.length - text.trimStart().length) / 2}ch` }}>
                  {text.trim()}
                </code>
              </label>
            ) : null,
          )}
        </fieldset>
        {!found && (
          <button type="submit" className="button" disabled={line == null}>
            Comprobar línea
          </button>
        )}
      </form>
      {lineChecked && !found && (
        <p className="notice notice--error" role="status">
          La línea {line + 1} está bien. Sigue el programa con los valores y busca dónde deja de hacer lo que pide.
        </p>
      )}
      {found && (
        <form onSubmit={checkFix} className="subform">
          <p className="notice notice--ok">¡Esa es! El error está en la línea {level.bug + 1}.</p>
          <fieldset className="choices">
            <legend>2. ¿Cómo se corrige la línea {level.bug + 1}?</legend>
            {level.fixes.map((option, index) => (
              <label key={option}>
                <input type="radio" name="correccion" checked={fix === index} onChange={() => { setFix(index); setFixResult(null) }} />{' '}
                <code className="error-lines__code">{option.trim()}</code>
              </label>
            ))}
          </fieldset>
          <button type="submit" className="button" disabled={fix == null}>
            Comprobar corrección
          </button>
        </form>
      )}
      {fixResult === 'ok' && <Solved onNext={onNext}>¡Corregido! {level.explanation}</Solved>}
      {fixResult === 'wrong' && (
        <p className="notice notice--error" role="status">
          Con ese cambio el programa todavía no muestra lo que debería. Síguelo línea por línea con la corrección y elige otra.
        </p>
      )}
    </section>
  )
}
