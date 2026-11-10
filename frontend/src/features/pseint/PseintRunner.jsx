import { useState } from 'react'
import { apiPost } from '../../shared/api/httpClient.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'

// Ejecuta un programa de PSeInt en el servidor (aislado y con límites, RNF-05) con las entradas
// escritas, una por línea, y muestra si compila, lo que el programa escribió y el error, si lo hubo.
// onResult recibe cada resultado; formatError cambia el texto del error (el Emulador de DFD y bloques lo usa para
// nombrar el bloque en vez de la línea del código que generó). problem: error de compilación ya conocido; se muestra
// al ejecutar sin llamar al servidor.
export default function PseintRunner({ code, idPrefix, onResult, formatError = (error) => error, problem }) {
  const [input, setInput] = useState('')
  const run = useSubmit(async () => {
    if (!code.trim()) throw new Error('Escribe el programa antes de ejecutarlo.')
    const outcome = problem
      ? { output: '', completed: false, errorKind: 'SYNTAX', error: problem, steps: 0, durationMs: 0 }
      : await apiPost('/pseint/runs', { code, input })
    onResult?.(outcome)
    return outcome
  })
  const result = run.result
  return (
    <div className="pseint-runner">
      <div className="field">
        <label htmlFor={`${idPrefix}-entradas`}>Entradas para probar (una por línea)</label>
        <textarea
          id={`${idPrefix}-entradas`}
          rows={3}
          maxLength={10000}
          spellCheck={false}
          className="code"
          value={input}
          onChange={(event) => setInput(event.target.value)}
        />
      </div>
      <button type="button" className="button button--secondary" disabled={run.pending} onClick={() => run.submit()}>
        {run.pending ? 'Ejecutando…' : 'Ejecutar'}
      </button>
      <FormStatus error={run.error} />
      {result && (
        <div role="status" aria-label="Resultado de la ejecución">
          <Verdict errorKind={result.errorKind} />
          <p className="page__note">
            {result.completed ? 'El programa terminó' : 'El programa se detuvo'} · {result.steps} pasos · {result.durationMs} ms
          </p>
          <pre className="console">{result.output || '(sin salida)'}</pre>
          {result.error && (
            <p className="notice notice--error">
              <strong>{ERROR_KIND_LABELS[result.errorKind] ?? 'Error'}:</strong> {formatError(result.error)}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

// Primero dice si el programa compila (se pudo leer) y luego si se ejecutó sin errores.
export function Verdict({ errorKind }) {
  if (errorKind === 'SYNTAX' || errorKind === 'UNSUPPORTED') {
    return <p className="verdict verdict--error">✗ No compila: corrige el error de abajo y vuelve a ejecutar.</p>
  }
  if (errorKind) return <p className="verdict verdict--warn">✓ Compila, pero ✗ falló al ejecutarse.</p>
  return <p className="verdict verdict--ok">✓ Compila y se ejecutó sin errores.</p>
}

const ERROR_KIND_LABELS = {
  SYNTAX: 'Error de sintaxis',
  UNSUPPORTED: 'Aún no disponible en LogiCode',
  RUNTIME: 'Error al ejecutar',
  LIMIT: 'Límite superado',
}
