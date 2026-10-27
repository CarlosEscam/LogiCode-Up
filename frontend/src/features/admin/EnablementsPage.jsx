import { useState } from 'react'
import { apiPost, apiUpload } from '../../shared/api/httpClient.js'
import { Pager } from '../../shared/ui/Pager.jsx'
import { usePagedQuery } from '../../shared/api/usePagedQuery.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'

const dateFormatter = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeZone: 'America/Bogota' })

// Habilitación de cédulas de estudiantes: solo una cédula habilitada puede registrarse.
export default function EnablementsPage() {
  const [page, setPage] = useState(0)
  const enablements = usePagedQuery('/student-enablements', { page, size: 50 })

  const single = useSubmit(async (event) => {
    const form = event.currentTarget
    const { cedula, fullName } = formValues(event)
    const result = await apiPost('/student-enablements', { cedula: cedula.trim(), fullName: fullName.trim() || null })
    form.reset()
    enablements.reload()
    return result
  })

  const bulk = useSubmit(async (event) => {
    const form = event.currentTarget
    const file = new FormData(form).get('file')
    if (!file || !file.name) throw new Error('Selecciona un archivo .csv.')
    const result = await apiUpload('/student-enablements/import', file)
    form.reset()
    enablements.reload()
    return result
  })

  return (
    <>
      <section className="card" aria-labelledby="habilitar-titulo">
        <h1 id="habilitar-titulo">Habilitar estudiantes</h1>
        <form onSubmit={single.submit} noValidate className="form-row">
          <Field label="Cédula" name="cedula" inputMode="numeric" required error={single.fieldErrors.cedula} />
          <Field label="Nombre (opcional)" name="fullName" />
          <button type="submit" className="button" disabled={single.pending}>
            Habilitar
          </button>
        </form>
        <FormStatus error={single.error} success={single.result && `Cédula ${single.result.cedula} habilitada.`} />
      </section>

      <section className="card" aria-labelledby="importar-titulo">
        <h2 id="importar-titulo">Importar desde CSV</h2>
        <p className="page__note">
          Una fila por estudiante: <code>cedula</code> o <code>cedula,nombre</code> (también con punto y coma). Puede
          tener encabezado. Máximo 2000 filas y 1 MB. Para Excel, guarda la hoja como CSV.
        </p>
        <form onSubmit={bulk.submit} className="form-row">
          <input type="file" name="file" accept=".csv,text/csv" aria-label="Archivo CSV" />
          <button type="submit" className="button" disabled={bulk.pending}>
            {bulk.pending ? 'Importando…' : 'Importar'}
          </button>
        </form>
        <FormStatus
          error={bulk.error}
          success={bulk.result && `Habilitadas: ${bulk.result.enabled}. Ya estaban habilitadas: ${bulk.result.alreadyEnabled}.`}
        />
        {bulk.result?.errors.length > 0 && (
          <details open>
            <summary>Filas con problemas ({bulk.result.errors.length})</summary>
            <ul>
              {bulk.result.errors.map((error) => (
                <li key={`${error.line}-${error.value}`}>
                  Fila {error.line} ({error.value || 'vacía'}): {error.reason}
                </li>
              ))}
            </ul>
          </details>
        )}
      </section>

      <section className="card" aria-labelledby="habilitadas-titulo">
        <h2 id="habilitadas-titulo">Cédulas habilitadas</h2>
        <FormStatus error={enablements.error} />
        <div className="table-wrap" tabIndex={0}>
          <table>
            <thead>
              <tr>
                <th scope="col">Cédula</th>
                <th scope="col">Nombre</th>
                <th scope="col">Registro</th>
                <th scope="col">Habilitada</th>
              </tr>
            </thead>
            <tbody>
              {enablements.data?.items.map((item) => (
                <tr key={item.id}>
                  <td>{item.cedula}</td>
                  <td>{item.fullName ?? '—'}</td>
                  <td>{item.registered ? 'Registrado' : 'Pendiente'}</td>
                  <td>{dateFormatter.format(new Date(item.enabledAt))}</td>
                </tr>
              ))}
              {enablements.data?.items.length === 0 && (
                <tr>
                  <td colSpan={4}>Aún no hay cédulas habilitadas.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager data={enablements.data} onPage={setPage} />
      </section>
    </>
  )
}
