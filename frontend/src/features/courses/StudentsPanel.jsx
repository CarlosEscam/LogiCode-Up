import { useState } from 'react'
import { apiDelete, apiPost, apiUpload } from '../../shared/api/httpClient.js'
import { usePagedQuery } from '../../shared/api/usePagedQuery.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { Pager } from '../../shared/ui/Pager.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'

const dateFormatter = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeZone: 'America/Bogota' })

// Inscripción por cédula. Inscribir también habilita la cédula para que el estudiante se registre.
export default function StudentsPanel({ courseId, onChange }) {
  const [page, setPage] = useState(0)
  const [feedback, setFeedback] = useState({ error: null, success: null })
  const students = usePagedQuery(`/courses/${courseId}/enrollments`, { page, size: 50 })

  function changed() {
    students.reload()
    onChange()
  }

  const single = useSubmit(async (event) => {
    const form = event.currentTarget
    const { cedula, fullName } = formValues(event)
    const result = await apiPost(`/courses/${courseId}/enrollments`, { cedula: cedula.trim(), fullName: fullName.trim() || null })
    form.reset()
    changed()
    return result
  })

  const bulk = useSubmit(async (event) => {
    const form = event.currentTarget
    const file = new FormData(form).get('file')
    if (!file || !file.name) throw new Error('Selecciona un archivo .xlsx o .csv.')
    const result = await apiUpload(`/courses/${courseId}/enrollments/import`, file)
    form.reset()
    changed()
    return result
  })

  async function unenroll(student) {
    if (!window.confirm(`¿Retirar a ${student.fullName ?? student.cedula} del curso? Su cuenta no se borra.`)) return
    setFeedback({ error: null, success: null })
    try {
      await apiDelete(`/courses/${courseId}/enrollments/${student.cedula}`)
      setFeedback({ error: null, success: `Cédula ${student.cedula} retirada del curso.` })
      changed()
    } catch (error) {
      setFeedback({ error: error.message, success: null })
    }
  }

  return (
    <>
      <section className="card card--wide" aria-labelledby="inscribir-titulo">
        <h2 id="inscribir-titulo">Inscribir estudiantes</h2>
        <form onSubmit={single.submit} noValidate className="form-row">
          <Field label="Cédula" name="cedula" inputMode="numeric" required error={single.fieldErrors.cedula} />
          <Field label="Nombre (opcional)" name="fullName" />
          <button type="submit" className="button" disabled={single.pending}>
            Inscribir
          </button>
        </form>
        <FormStatus error={single.error} success={single.result && `Cédula ${single.result.cedula} inscrita.`} />

        <form onSubmit={bulk.submit} className="form-row">
          <input type="file" name="file" accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" aria-label="Archivo de estudiantes (Excel o CSV)" />
          <button type="submit" className="button" disabled={bulk.pending}>
            {bulk.pending ? 'Importando…' : 'Importar lista'}
          </button>
        </form>
        <p className="page__note">
          Excel (<code>.xlsx</code>) con la cédula en la columna A y el nombre, opcional, en la B; o CSV con{' '}
          <code>cedula</code> o <code>cedula,nombre</code>. Máximo 2000 filas y 1 MB.
        </p>
        <FormStatus
          error={bulk.error}
          success={bulk.result && `Inscritos: ${bulk.result.enrolled}. Ya estaban inscritos: ${bulk.result.alreadyEnrolled}.`}
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

      <section className="card card--wide" aria-labelledby="inscritos-titulo">
        <h2 id="inscritos-titulo">Inscritos</h2>
        <FormStatus error={feedback.error ?? students.error} success={feedback.success} />
        <div className="table-wrap" tabIndex={0}>
          <table>
            <thead>
              <tr>
                <th scope="col">Cédula</th>
                <th scope="col">Nombre</th>
                <th scope="col">Registro</th>
                <th scope="col">Inscrito</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {students.data?.items.map((student) => (
                <tr key={student.cedula}>
                  <td>{student.cedula}</td>
                  <td>{student.fullName ?? '—'}</td>
                  <td>{student.registered ? 'Registrado' : 'Pendiente'}</td>
                  <td>{dateFormatter.format(new Date(student.enrolledAt))}</td>
                  <td>
                    <button type="button" className="button button--danger button--small" onClick={() => unenroll(student)}>
                      Retirar
                    </button>
                  </td>
                </tr>
              ))}
              {students.data?.items.length === 0 && (
                <tr>
                  <td colSpan={5}>Aún no hay estudiantes inscritos.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pager data={students.data} onPage={setPage} />
      </section>
    </>
  )
}
