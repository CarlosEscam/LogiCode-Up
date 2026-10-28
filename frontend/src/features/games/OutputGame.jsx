import { useState } from 'react'
import Solved from './Solved.jsx'

export default function OutputGame({ level, onSolved, onNext }) {
  const [choice, setChoice] = useState(null)
  const [checked, setChecked] = useState(null)
  const correct = checked === level.answer

  return (
    <section className="card card--wide" aria-labelledby="nivel-titulo">
      <h2 id="nivel-titulo">{level.title}</h2>
      <pre className="console">{level.code}</pre>
      <form
        onSubmit={(event) => {
          event.preventDefault()
          setChecked(choice)
          if (choice === level.answer) onSolved()
        }}
      >
        <fieldset className="choices">
          <legend>{level.question ?? '¿Qué muestra en pantalla?'}</legend>
          {level.options.map((option, index) => (
            <label key={option}>
              <input type="radio" name="salida" checked={choice === index} onChange={() => setChoice(index)} /> {level.question ? option : <code>{option}</code>}
            </label>
          ))}
        </fieldset>
        <button type="submit" className="button" disabled={choice == null}>
          Comprobar
        </button>
      </form>
      {checked != null &&
        (correct ? (
          <Solved onNext={onNext}>¡Correcto! {level.explanation}</Solved>
        ) : (
          <p className="notice notice--error" role="status">
            No es esa. Sigue el programa línea por línea y anota cuánto vale cada variable{level.question ? ' y qué pasa en cada pin' : ''}.
          </p>
        ))}
    </section>
  )
}
