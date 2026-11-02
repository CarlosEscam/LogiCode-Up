import { useState } from 'react'
import { sameDeskValue } from './levels.js'
import Solved from './Solved.jsx'

// Prueba de escritorio: se sigue el programa a mano y se llena la tabla de valores. La primera fila es el ejemplo.
export default function DeskGame({ level, onSolved, onNext }) {
  const [typed, setTyped] = useState({})
  const [wrong, setWrong] = useState(null) // casillas por revisar desde la última comprobación
  const cells = level.rows.flatMap((row, r) => (r === 0 ? [] : row.slice(1).flatMap((value, c) => (value == null ? [] : [{ key: `${r}-${c}`, r, value }]))))
  const [solved, setSolved] = useState(false)

  function check(event) {
    event.preventDefault()
    const bad = cells.filter((cell) => !sameDeskValue(typed[cell.key] ?? '', cell.value)).map((cell) => cell.key)
    setWrong(bad)
    if (bad.length === 0) {
      setSolved(true)
      onSolved()
    }
  }

  const firstWrong = wrong?.length ? cells.find((cell) => cell.key === wrong[0]) : null

  return (
    <section className="card card--wide" aria-labelledby="nivel-titulo">
      <h2 id="nivel-titulo">{level.title}</h2>
      <p>{level.goal}</p>
      <pre className="console">{level.code}</pre>
      <form onSubmit={check}>
        <p className="page__note">
          Anota el valor de cada variable después de la línea o de la vuelta. En una condición escribe Verdadero o Falso, y en Pantalla lo que se muestra. Las casillas con — no se llenan.
        </p>
        <div className="table-wrap">
          <table className="desk-table">
            <caption className="visually-hidden">Tabla de la prueba de escritorio</caption>
            <thead>
              <tr>
                <th scope="col">Paso</th>
                {level.columns.map((column) => (
                  <th key={column} scope="col">
                    <code>{column}</code>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {level.rows.map(([step, ...values], r) => (
                <tr key={step + r}>
                  <th scope="row">
                    <code>{step}</code>
                  </th>
                  {values.map((value, c) => {
                    const key = `${r}-${c}`
                    if (value == null) return <td key={key} className="desk-table__empty">—</td>
                    if (r === 0) return <td key={key}><code>{value}</code></td>
                    const bad = wrong?.includes(key)
                    return (
                      <td key={key}>
                        <input
                          className={`desk-table__input${bad ? ' desk-table__input--wrong' : ''}`}
                          aria-label={`${step}, ${level.columns[c]}`}
                          aria-invalid={bad || undefined}
                          autoComplete="off"
                          spellCheck={false}
                          value={typed[key] ?? ''}
                          readOnly={solved}
                          onChange={(event) => {
                            setTyped((previous) => ({ ...previous, [key]: event.target.value }))
                            if (bad) setWrong((previous) => previous.filter((item) => item !== key))
                          }}
                        />
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!solved && (
          <button type="submit" className="button">
            Comprobar
          </button>
        )}
      </form>
      {solved && <Solved onNext={onNext}>¡Tabla completa! {level.explanation}</Solved>}
      {firstWrong && (
        <p className="notice notice--error" role="status">
          {wrong.length === 1 ? 'Hay 1 casilla por revisar' : `Hay ${wrong.length} casillas por revisar`} (en rojo). La primera está en la fila «{level.rows[firstWrong.r][0]}»: vuelve a esa línea y
          sigue el programa con los valores de la fila anterior.
        </p>
      )}
    </section>
  )
}
