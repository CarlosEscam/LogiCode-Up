import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { formatDateTime } from './labels.js'

// Bandeja Por revisar: el último intento de cada estudiante que aún no tiene nota, del más antiguo
// al más reciente. En la Etapa 6 llegarán aquí también las que la IA no pueda calificar con confianza.
export default function ReviewPanel({ courseId, canGrade }) {
  const queue = useQuery(`/courses/${courseId}/review-queue`)
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
