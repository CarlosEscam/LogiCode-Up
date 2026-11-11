import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatDateTime } from '../activities/labels.js'

// Control del docente: por actividad, cuántas entregas ya tienen nota y cuántas esperan revisión. Usa las
// cifras que ya calcula la planilla (RF-18), así que cuenta el último intento de cada estudiante.
export default function ControlPanel({ courseId, canGrade, onGraded }) {
  const book = useQuery(`/courses/${courseId}/gradebook`)
  if (book.status === 'loading' && !book.data) return <p role="status">Cargando…</p>
  if (book.status === 'error') return <FormStatus error={book.error} />
  const { columns, rows } = book.data
  const students = rows.length
  const pending = (stats) => Math.max(0, students - stats.submitted - stats.overdue)
  const total = (pick) => columns.reduce((sum, column) => sum + pick(column.stats), 0)

  return (
    <section className="card card--wide" aria-labelledby="control-titulo">
      <h2 id="control-titulo">Control de calificaciones</h2>
      <dl className="overview overview--color">
        <div>
          <dt>Calificadas</dt>
          <dd>{total((s) => s.graded)}</dd>
        </div>
        <div>
          <dt>Por revisar</dt>
          <dd>
            <Link to={`/cursos/${courseId}?seccion=revisar`}>{total((s) => s.inReview)}</Link>
          </dd>
        </div>
        <div>
          <dt>Sin entregar</dt>
          <dd>{total(pending)}</dd>
        </div>
        <div>
          <dt>Vencidas sin entrega (0,0)</dt>
          <dd>{total((s) => s.overdue)}</dd>
        </div>
      </dl>
      {canGrade && (
        <GradeAllWithAi
          courseId={courseId}
          onQueued={() => {
            book.reload()
            onGraded?.()
          }}
        />
      )}
      {columns.length === 0 ? (
        <p>El curso aún no tiene actividades publicadas.</p>
      ) : (
        <div className="table-wrap" tabIndex={0}>
          <table>
            <caption className="visually-hidden">Entregas calificadas y pendientes por actividad</caption>
            <thead>
              <tr>
                <th scope="col">Actividad</th>
                <th scope="col">Cierre</th>
                <th scope="col">Avance</th>
                <th scope="col">Calificadas</th>
                <th scope="col">Por revisar</th>
                <th scope="col">Sin entregar</th>
                <th scope="col">Vencidas</th>
              </tr>
            </thead>
            <tbody>
              {columns.map((column) => (
                <tr key={column.activityId}>
                  <th scope="row">
                    <Link to={`/actividades/${column.activityId}`}>{column.title}</Link>
                  </th>
                  <td>{formatDateTime(column.closesAt)}</td>
                  <td>
                    <ProgressBar stats={column.stats} students={students} />
                  </td>
                  <td>
                    {column.stats.graded} de {students}
                  </td>
                  <td>
                    {column.stats.inReview > 0 ? (
                      <span className="badge badge--warning">{column.stats.inReview}</span>
                    ) : (
                      0
                    )}
                  </td>
                  <td>{pending(column.stats)}</td>
                  <td>{column.stats.overdue}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

// Barra de avance de una actividad: calificadas, por revisar y vencidas sobre el total de estudiantes. Es solo
// visual (las cifras están en las columnas de al lado).
function ProgressBar({ stats, students }) {
  const share = (n) => `${students ? (100 * n) / students : 0}%`
  return (
    <div className="progress-bar" aria-hidden="true">
      <span className="progress-bar__graded" style={{ width: share(stats.graded) }} />
      <span className="progress-bar__review" style={{ width: share(stats.inReview) }} />
      <span className="progress-bar__overdue" style={{ width: share(stats.overdue) }} />
    </div>
  )
}

// Botón "Calificar todo con IA": manda a la IA cada entrega del curso que espera al docente. La IA publica sola
// con 90 % de confianza o más; el resto vuelve a Por revisar.
export function GradeAllWithAi({ courseId, onQueued }) {
  const run = useSubmit(async () => {
    const result = await apiPost(`/courses/${courseId}/ai-grading`)
    onQueued?.()
    return result
  })
  const result = run.result
  return (
    <div className="toolbar">
      <button
        type="button"
        className="button"
        disabled={run.pending}
        onClick={() =>
          window.confirm('¿Mandar a la IA todas las entregas que esperan revisión? Publicará sola las que califique con 90 % de confianza o más.') &&
          run.submit()
        }
      >
        {run.pending ? 'Enviando…' : 'Calificar todo con IA'}
      </button>
      <FormStatus
        error={run.error}
        success={
          result &&
          (result.queued === 0 && result.unreadable === 0
            ? 'No hay entregas esperando revisión.'
            : `La IA está calificando ${result.queued} ${result.queued === 1 ? 'entrega' : 'entregas'}.` +
              (result.unreadable > 0 ? ` ${result.unreadable} no las puede leer (Word, video…) y siguen en Por revisar.` : ''))
        }
      />
    </div>
  )
}
