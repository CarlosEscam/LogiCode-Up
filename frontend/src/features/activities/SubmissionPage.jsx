import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatSize } from '../content/labels.js'
import { RubricTable } from './RubricPanel.jsx'
import { GRADE_SOURCE_LABELS, GRADE_STATUS_LABELS, formatDateTime, formatScore, submissionFileUrl } from './labels.js'

// Una entrega con sus archivos, la rúbrica con que se evalúa, la nota y su historial (RF-17).
// El docente del curso califica aquí; el estudiante ve la nota cuando está publicada.
export default function SubmissionPage({ id }) {
  const submission = useQuery(`/submissions/${id}`)
  const grade = useQuery(`/submissions/${id}/grade`)

  if (submission.status === 'loading' && !submission.data) return <p role="status">Cargando…</p>
  if (submission.status === 'error') {
    return (
      <section className="card">
        <h1>Entrega no disponible</h1>
        <FormStatus error={submission.error} />
        <p>
          <Link to="/cursos">Volver a cursos</Link>
        </p>
      </section>
    )
  }

  const data = submission.data
  return (
    <>
      <section className="card card--wide" aria-labelledby="entrega-titulo">
        <p className="page__note">
          <Link to={`/actividades/${data.activityId}`}>{data.activityTitle}</Link>
        </p>
        <h1 id="entrega-titulo">
          Entrega de {data.studentName ?? 'estudiante'} · intento {data.attempt}
        </h1>
        <p className="material__meta">
          Enviada {formatDateTime(data.submittedAt)}
          {data.late && <span className="badge badge--warning">Fuera del plazo original</span>}
        </p>
        {data.textAnswer && (
          <>
            <h2>Respuesta</h2>
            <p className="statement">{data.textAnswer}</p>
          </>
        )}
        {data.files.length > 0 && (
          <>
            <h2>Archivos</h2>
            <ul className="materials">
              {data.files.map((file) => (
                <SubmissionFile key={file.id} submissionId={data.id} file={file} />
              ))}
            </ul>
          </>
        )}
      </section>
      <GradeSection grade={grade} submissionId={id} />
    </>
  )
}

function SubmissionFile({ submissionId, file }) {
  const isImage = file.contentType.startsWith('image/')
  return (
    <li className="material material--stacked">
      <div className="material__body">
        <h3 className="material__title">{file.originalName}</h3>
        <p className="material__meta">
          {file.format} · {formatSize(file.sizeBytes)}
        </p>
      </div>
      {isImage && <img className="preview" src={submissionFileUrl(submissionId, file, 'preview')} alt={`Imagen entregada: ${file.originalName}`} />}
      <div className="material__actions">
        {file.previewable && !isImage && (
          <a
            className="button button--secondary button--small"
            href={submissionFileUrl(submissionId, file, 'preview')}
            target="_blank"
            rel="noopener"
            aria-label={`Ver ${file.originalName} (se abre en otra pestaña)`}
          >
            Ver
          </a>
        )}
        <a className="button button--secondary button--small" href={submissionFileUrl(submissionId, file, 'download')} download aria-label={`Descargar ${file.originalName}`}>
          Descargar
        </a>
      </div>
    </li>
  )
}

function GradeSection({ grade, submissionId }) {
  if (grade.status === 'loading' && !grade.data) return null
  if (grade.status === 'error') {
    return (
      <section className="card card--wide" aria-label="Nota">
        <FormStatus error={grade.error} />
      </section>
    )
  }
  const { grade: current, rubric, history, canGrade } = grade.data
  return (
    <>
      <section className="card card--wide" aria-labelledby="nota-titulo">
        <div className="topic__header">
          <h2 id="nota-titulo">Nota</h2>
          <span className={`badge badge--${current.status === 'PUBLISHED' ? 'ok' : 'info'}`}>{GRADE_STATUS_LABELS[current.status]}</span>
        </div>
        {current.status === 'PUBLISHED' ? (
          <>
            <p className="score">{formatScore(current.score)}</p>
            <p className="page__note">
              Calificada por {current.gradedByName ?? GRADE_SOURCE_LABELS[current.source]} el {formatDateTime(current.gradedAt)}
            </p>
            {current.feedback && <p className="statement">{current.feedback}</p>}
          </>
        ) : (
          <p>{canGrade ? 'Esta entrega espera tu calificación.' : 'La entrega está en revisión. La nota aparecerá aquí cuando se publique.'}</p>
        )}
        {canGrade && <GradeForm submissionId={submissionId} current={current} onSaved={grade.reload} />}
      </section>

      <section className="card card--wide" aria-labelledby="rubrica-entrega-titulo">
        <div className="topic__header">
          <h2 id="rubrica-entrega-titulo">Rúbrica de esta entrega</h2>
          <span className="badge">Versión {rubric.number}</span>
        </div>
        <RubricTable rubric={rubric} />
      </section>

      {history.length > 0 && (
        <section className="card card--wide" aria-labelledby="historial-titulo">
          <h2 id="historial-titulo">Historial de la nota</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Fecha</th>
                  <th scope="col">Antes</th>
                  <th scope="col">Después</th>
                  <th scope="col">Por</th>
                  <th scope="col">Retroalimentación</th>
                </tr>
              </thead>
              <tbody>
                {history.map((change, index) => (
                  <tr key={`${change.changedAt}-${index}`}>
                    <td>{formatDateTime(change.changedAt)}</td>
                    <td>{formatScore(change.previousScore)}</td>
                    <td>{formatScore(change.newScore)}</td>
                    <td>{change.changedByName ?? GRADE_SOURCE_LABELS[change.source]}</td>
                    <td>{change.feedback ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </>
  )
}

// Calificación manual (Etapa 5): nota de 0,0 a 5,0 con un decimal, guiada por la rúbrica.
function GradeForm({ submissionId, current, onSaved }) {
  const save = useSubmit(async (event) => {
    const data = new FormData(event.currentTarget)
    const score = String(data.get('score') ?? '').replace(',', '.').trim()
    if (!/^\d(\.\d)?$/.test(score) || Number(score) > 5) {
      throw new Error('Escribe una nota entre 0,0 y 5,0 con un decimal.')
    }
    const result = await apiPost(`/submissions/${submissionId}/grade`, { score: Number(score), feedback: data.get('feedback') || null })
    onSaved()
    return result
  })
  const published = current.status === 'PUBLISHED'
  return (
    <form onSubmit={save.submit} noValidate className="subform">
      <Field
        label={published ? 'Corregir nota (0,0 a 5,0)' : 'Nota (0,0 a 5,0)'}
        name="score"
        inputMode="decimal"
        required
        defaultValue={published ? formatScore(current.score) : ''}
        error={save.fieldErrors.score}
        className="input--short"
      />
      <div className="field">
        <label htmlFor="nota-retroalimentacion">Retroalimentación para el estudiante (opcional)</label>
        <textarea id="nota-retroalimentacion" name="feedback" rows={3} maxLength={5000} defaultValue={current.feedback ?? ''} />
      </div>
      <button type="submit" className="button" disabled={save.pending}>
        {published ? 'Guardar corrección' : 'Publicar nota'}
      </button>
      <FormStatus error={save.error} success={save.result && 'Nota publicada.'} />
    </form>
  )
}
