import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { GRADE_STATUS_LABELS, REVIEW_REASON_LABELS, formatDateTime } from './labels.js'
import { useRefreshWhile } from './useRefreshWhile.js'

// Bandeja Por revisar: el último intento de cada estudiante sin nota publicada, del más antiguo al más
// reciente. Incluye las que la IA está calificando y las que esperan al docente, con el motivo (RF-15).
export default function ReviewPanel({ courseId, canGrade }) {
  const queue = useQuery(`/courses/${courseId}/review-queue`)
  useRefreshWhile(queue.data?.some((item) => item.status === 'PROCESSING'), queue.reload)
  return (
    <section className="card card--wide" aria-labelledby="revisar-titulo">
      <h2 id="revisar-titulo">Por revisar</h2>
      <FormStatus error={queue.error} />
      {queue.status === 'loading' && !queue.data && <p role="status">Cargando…</p>}
      {queue.data?.length === 0 && <p>No hay entregas pendientes de calificar.</p>}
      {queue.data?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Estudiante</th>
                <th scope="col">Actividad</th>
                <th scope="col">Entregada</th>
                <th scope="col">Intento</th>
                <th scope="col">Estado</th>
                <th scope="col">Acción</th>
              </tr>
            </thead>
            <tbody>
              {queue.data.map((item) => (
                <tr key={item.submissionId}>
                  <td>{item.studentName ?? '—'}</td>
                  <td>{item.activityTitle}</td>
                  <td>
                    {formatDateTime(item.submittedAt)}
                    {item.late && <span className="badge badge--warning">Tarde</span>}
                  </td>
                  <td>{item.attempt}</td>
                  <td>
                    {item.status === 'PROCESSING' ? (
                      <span className="badge badge--info">{GRADE_STATUS_LABELS.PROCESSING}</span>
                    ) : (
                      <span className="badge badge--warning">{REVIEW_REASON_LABELS[item.reviewReason] ?? GRADE_STATUS_LABELS.PENDING_REVIEW}</span>
                    )}
                  </td>
                  <td>
                    <Link
                      className="button button--small"
                      to={`/entregas/${item.submissionId}`}
                      aria-label={`${canGrade ? 'Calificar' : 'Ver'} entrega de ${item.studentName ?? 'estudiante'} en ${item.activityTitle}`}
                    >
                      {canGrade ? 'Calificar' : 'Ver'}
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
