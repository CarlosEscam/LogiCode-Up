import { API_BASE } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { formatDateTime, formatScore } from '../activities/labels.js'

const CELL_LABELS = { IN_REVIEW: 'En revisión', PENDING: 'Pendiente' }

// Planilla del curso (RF-18, RF-20, RN-07). El backend calcula pesos, avance parcial y nota definitiva;
// aquí solo se muestran. El estudiante recibe solo su fila.
export default function GradebookPanel({ courseId }) {
  const book = useQuery(`/courses/${courseId}/gradebook`)
  if (book.status === 'loading' && !book.data) return <p role="status">Cargando…</p>
  if (book.status === 'error') return <FormStatus error={book.error} />
  const data = book.data
  return data.fullView ? <FullGradebook data={data} courseId={courseId} /> : <MyGrades data={data} />
}

function weightLabel(column) {
  const effective = `${String(Number(column.effectiveWeight)).replace('.', ',')} %`
  return column.weightPercent == null ? `${effective} (reparto)` : effective
}

function CellValue({ cell }) {
  const text = cell.status === 'GRADED' || cell.status === 'OVERDUE' ? formatScore(cell.score) : CELL_LABELS[cell.status]
  const content =
    cell.status === 'OVERDUE' ? (
      <span title="Vencida sin entrega">{text}*</span>
    ) : cell.status === 'GRADED' ? (
      text
    ) : (
      <span className="muted">{text}</span>
    )
  return cell.submissionId ? <Link to={`/entregas/${cell.submissionId}`}>{content}</Link> : content
}

function FullGradebook({ data, courseId }) {
  const stats = data.stats
  return (
    <section className="card card--wide" aria-labelledby="planilla-titulo">
      <h2 id="planilla-titulo">Planilla</h2>
      <p className="page__note">
        Escala 0,0 a 5,0; aprueba con {formatScore(data.passingScore)}. Las actividades sin porcentaje se reparten por igual
        lo que falta para 100 %. Una actividad vencida sin entrega vale 0,0 (marcada con *). El avance parcial promedia
        solo lo que ya tiene nota; la definitiva aparece cuando todas las actividades tienen resultado.
      </p>
      {data.weightsWarning && (
        <p className="notice notice--error" role="alert">
          {data.weightsWarning}
        </p>
      )}
      {data.columns.length > 0 && (
        <p>
          <a className="button button--secondary" href={`${API_BASE}/courses/${courseId}/gradebook/export`} download>
            Exportar a Excel (CSV)
          </a>
        </p>
      )}
      <div className="table-wrap" tabIndex={0}>
        <table className="gradebook">
          <caption className="visually-hidden">Notas por estudiante y actividad</caption>
          <thead>
            <tr>
              <th scope="col">Estudiante</th>
              {data.columns.map((column) => (
                <th key={column.activityId} scope="col">
                  <Link to={`/actividades/${column.activityId}`}>{column.title}</Link>
                  <small className="gradebook__weight">{weightLabel(column)}</small>
                </th>
              ))}
              <th scope="col">Avance parcial</th>
              <th scope="col">Definitiva</th>
            </tr>
          </thead>
          <tbody>
            {data.rows.map((row) => (
              <tr key={row.cedula}>
                <th scope="row">
                  {row.fullName ?? row.cedula}
                  {!row.registered && <small className="gradebook__weight">Sin registrarse</small>}
                </th>
                {row.cells.map((cell) => (
                  <td key={cell.activityId}>
                    <CellValue cell={cell} />
                  </td>
                ))}
                <td>{formatScore(row.partial)}</td>
                <td>
                  <strong>{formatScore(row.definitive)}</strong>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data.rows.length === 0 && <p>El curso aún no tiene estudiantes inscritos.</p>}

      <h3>Estadísticas</h3>
      <p>
        Avance parcial: {stats.withPartial} de {stats.students} estudiantes con nota, promedio {formatScore(stats.averagePartial)},
        aprueban {stats.passingPartial}. Nota definitiva: {stats.withDefinitive} con nota, promedio{' '}
        {formatScore(stats.averageDefinitive)}, aprueban {stats.passingDefinitive}.
      </p>
      {data.columns.length > 0 && (
        <div className="table-wrap" tabIndex={0}>
          <table>
            <thead>
              <tr>
                <th scope="col">Actividad</th>
                <th scope="col">Cierre</th>
                <th scope="col">Entregaron</th>
                <th scope="col">Calificadas</th>
                <th scope="col">En revisión</th>
                <th scope="col">Vencidas</th>
                <th scope="col">Promedio</th>
                <th scope="col">Mín.</th>
                <th scope="col">Máx.</th>
                <th scope="col">Aprueban</th>
              </tr>
            </thead>
            <tbody>
              {data.columns.map((column) => (
                <tr key={column.activityId}>
                  <th scope="row">{column.title}</th>
                  <td>{formatDateTime(column.closesAt)}</td>
                  <td>{column.stats.submitted}</td>
                  <td>{column.stats.graded}</td>
                  <td>{column.stats.inReview}</td>
                  <td>{column.stats.overdue}</td>
                  <td>{formatScore(column.stats.average)}</td>
                  <td>{formatScore(column.stats.min)}</td>
                  <td>{formatScore(column.stats.max)}</td>
                  <td>{column.stats.passing}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function MyGrades({ data }) {
  const row = data.rows[0]
  if (!row) return <p className="card card--wide">No apareces inscrito en este curso.</p>
  return (
    <section className="card card--wide" aria-labelledby="mis-notas-titulo">
      <h2 id="mis-notas-titulo">Mis notas</h2>
      <div className="grade-summary">
        <p>
          Avance parcial <strong>{formatScore(row.partial)}</strong>
          <small>Promedio con pesos de lo que ya tiene nota.</small>
        </p>
        <p>
          Nota definitiva <strong>{formatScore(row.definitive)}</strong>
          <small>
            {row.definitive == null ? 'Aparece cuando todas las actividades tengan resultado.' : 'Todas las actividades tienen resultado.'}
          </small>
        </p>
      </div>
      {data.weightsWarning && <p className="page__note">{data.weightsWarning}</p>}
      <div className="table-wrap" tabIndex={0}>
        <table>
          <thead>
            <tr>
              <th scope="col">Actividad</th>
              <th scope="col">Porcentaje</th>
              <th scope="col">Cierre</th>
              <th scope="col">Resultado</th>
            </tr>
          </thead>
          <tbody>
            {data.columns.map((column, index) => (
              <tr key={column.activityId}>
                <th scope="row">
                  <Link to={`/actividades/${column.activityId}`}>{column.title}</Link>
                </th>
                <td>{weightLabel(column)}</td>
                <td>{formatDateTime(column.closesAt)}</td>
                <td>
                  <CellValue cell={row.cells[index]} />
                  {row.cells[index].status === 'OVERDUE' && ' (vencida sin entrega)'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
