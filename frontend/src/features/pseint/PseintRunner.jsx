import { useState } from 'react'
import { apiPost } from '../../shared/api/httpClient.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'

// Ejecuta un programa de PSeInt en el servidor (aislado y con límites, RNF-05) con las entradas
// escritas, una por línea, y muestra lo que el programa escribió.
export default function PseintRunner({ code, idPrefix }) {
  const [input, setInput] = useState('')
  const run = useSubmit(async () => {
    if (!code.trim()) throw new Error('Escribe el programa antes de ejecutarlo.')
    return apiPost('/pseint/runs', { code, input })
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
          <p className="page__note">
            {result.completed ? 'El programa terminó' : 'El programa se detuvo'} · {result.steps} pasos · {result.durationMs} ms
          </p>
          <pre className="console">{result.output || '(sin salida)'}</pre>
          {result.error && (
            <p className="notice notice--error">
              <strong>{ERROR_KIND_LABELS[result.errorKind] ?? 'Error'}:</strong> {result.error}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

const ERROR_KIND_LABELS = {
  SYNTAX: 'Error de sintaxis',
  UNSUPPORTED: 'Aún no disponible en LogiCode',
  RUNTIME: 'Error al ejecutar',
  LIMIT: 'Límite superado',
}
