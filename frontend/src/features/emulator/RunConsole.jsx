import { useEffect, useRef, useState } from 'react'
import { Verdict } from '../pseint/PseintRunner.jsx'
import { ERROR_KIND_LABELS } from '../pseint/errorLine.js'

// Consola de salida del Emulador (PSeInt y DFD): muestra lo que escribe el programa con cada dato que dio la persona en
// su lugar, pide el dato de cada Leer y al final dice si compila y si corrió sin errores. run viene de useConsole.
// clean cambia el texto antes de mostrarlo (Scratch quita las líneas de movimiento, que se ven en el escenario).
export default function RunConsole({ run, idPrefix, formatError = (error) => error, hint, clean = (text) => text }) {
  const [answer, setAnswer] = useState('')
  const input = useRef(null)

  useEffect(() => {
    if (run.status === 'waiting') input.current?.focus()
  }, [run.status])

  const pieces = []
  if (run.result || run.reads.length > 0) {
    let shown = 0
    const output = run.result?.output ?? ''
    run.reads.forEach((read, index) => {
      pieces.push(<span key={`o${index}`}>{clean(output.slice(shown, read.at))}</span>)
      pieces.push(
        <span key={`i${index}`} className="ide-console__typed">
          {read.value}
          {'\n'}
        </span>,
      )
      shown = read.at
    })
    if (run.status !== 'running') pieces.push(<span key="fin">{clean(output.slice(shown))}</span>)
  }
  const result = run.status === 'done' ? run.result : null

  return (
    <section className="ide-console" aria-labelledby={`${idPrefix}-consola`}>
      <h2 id={`${idPrefix}-consola`} className="ide-console__title">
        Consola de salida
      </h2>
      <div className="ide-console__screen" role="log" aria-live="polite" aria-label="Consola de salida">
        {run.status === 'idle' && !run.error && <span className="ide-console__hint">{hint}</span>}
        {run.status !== 'idle' && <span className="ide-console__hint">▶ Ejecutando…{'\n'}</span>}
        {pieces}
        {run.status === 'waiting' && (
          <form
            className="ide-console__ask"
            onSubmit={(event) => {
              event.preventDefault()
              run.answer(answer)
              setAnswer('')
            }}
          >
            <label htmlFor={`${idPrefix}-dato`} className="ide-console__prompt">
              &gt;<span className="visually-hidden"> Dato para Leer</span>
            </label>
            <input
              ref={input}
              id={`${idPrefix}-dato`}
              className="ide-console__input"
              value={answer}
              maxLength={500}
              autoComplete="off"
              spellCheck={false}
              onChange={(event) => setAnswer(event.target.value)}
            />
          </form>
        )}
      </div>
      <div role="status" aria-label="Resultado de la ejecución">
        {result && <Verdict errorKind={result.errorKind} />}
        {result?.error && (
          <p className="notice notice--error">
            <strong>{ERROR_KIND_LABELS[result.errorKind] ?? 'Error'}:</strong> {formatError(result.error)}
          </p>
        )}
        {run.error && <p className="notice notice--error">{run.error}</p>}
      </div>
    </section>
  )
}
