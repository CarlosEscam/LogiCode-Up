import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatSize } from '../content/labels.js'
import { RubricTable } from './RubricPanel.jsx'
import {
  AI_OUTCOME_LABELS,
  AI_OUTCOME_TONES,
  GRADE_SOURCE_LABELS,
  GRADE_STATUS_LABELS,
  GRADE_STATUS_TONES,
  REVIEW_REASON_HELP,
  REVIEW_REASON_LABELS,
  formatDateTime,
  formatDuration,
  formatPoints,
  formatScore,
  submissionFileUrl,
} from './labels.js'
import { useRefreshWhile } from './useRefreshWhile.js'

// Una entrega con sus archivos, la rúbrica con que se evalúa, la nota, el resultado de la IA y el
// historial (RF-17). El docente del curso califica o confirma aquí; el estudiante ve la nota cuando está publicada.
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
  const processing = grade.data?.grade.status === 'PROCESSING'
  useRefreshWhile(processing, grade.reload)
  if (grade.status === 'loading' && !grade.data) return null
  if (grade.status === 'error') {
    return (
      <section className="card card--wide" aria-label="Nota">
        <FormStatus error={grade.error} />
      </section>
    )
  }
  const { grade: current, rubric, history, aiEvaluations, canGrade, canRetryWithAi } = grade.data
  const latest = aiEvaluations[0]
  const proposal = current.status === 'PENDING_REVIEW' && latest?.proposedScore != null ? latest : null
  return (
    <>
      <section className="card card--wide" aria-labelledby="nota-titulo">
        <div className="topic__header">
          <h2 id="nota-titulo">Nota</h2>
          <span className={`badge badge--${GRADE_STATUS_TONES[current.status]}`}>{GRADE_STATUS_LABELS[current.status]}</span>
        </div>
        <GradeSummary current={current} canGrade={canGrade} />
        {canRetryWithAi && <RetryWithAi submissionId={submissionId} onDone={grade.reload} />}
        {canGrade && (
          <GradeForm
            key={latest?.id ?? 'sin-ia'}
            submissionId={submissionId}
            current={current}
            proposal={proposal}
            onSaved={grade.reload}
          />
        )}
      </section>

      {aiEvaluations.length > 0 && <AiEvaluations evaluations={aiEvaluations} />}

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

function GradeSummary({ current, canGrade }) {
  if (current.status === 'PUBLISHED') {
    return (
      <>
        <p className="score">{formatScore(current.score)}</p>
        <p className="page__note">
          {current.source === 'AI' ? 'Calificada automáticamente por la IA' : `Calificada por ${current.gradedByName ?? 'el docente'}`} el{' '}
          {formatDateTime(current.gradedAt)}
        </p>
        {current.feedback && <p className="statement">{current.feedback}</p>}
      </>
    )
  }
  if (current.status === 'PROCESSING') {
    return (
      <p role="status">
        {canGrade
          ? 'La IA está calificando esta entrega. En el equipo local puede tardar unos minutos; esta página se actualiza sola. Si prefieres, puedes calificarla tú.'
          : 'Tu entrega se está calificando. La nota aparecerá aquí cuando esté lista.'}
      </p>
    )
  }
  if (!canGrade) return <p>La entrega está en revisión. La nota aparecerá aquí cuando se publique.</p>
  return (
    <p>
      {current.reviewReason && <strong>{REVIEW_REASON_LABELS[current.reviewReason]}. </strong>}
      {REVIEW_REASON_HELP[current.reviewReason] ?? 'Esta entrega espera tu calificación.'}
    </p>
  )
}

function RetryWithAi({ submissionId, onDone }) {
  const retry = useSubmit(async () => {
    const result = await apiPost(`/submissions/${submissionId}/grade/ai-retry`)
    onDone()
    return result
  })
  return (
    <form onSubmit={retry.submit} className="subform">
      <button type="submit" className="button button--secondary" disabled={retry.pending}>
        Reintentar con IA
      </button>
      <FormStatus error={retry.error} />
    </form>
  )
}

// Resultado de la IA para el docente (RF-17): lectura, legibilidad, confianza, puntos por criterio y propuesta.
function AiEvaluations({ evaluations }) {
  const [latest, ...older] = evaluations
  return (
    <section className="card card--wide" aria-labelledby="ia-titulo">
      <div className="topic__header">
        <h2 id="ia-titulo">Resultado de la IA</h2>
        <span className={`badge badge--${AI_OUTCOME_TONES[latest.outcome]}`}>{AI_OUTCOME_LABELS[latest.outcome]}</span>
      </div>
      <AiEvaluation evaluation={latest} />
      {older.length > 0 && (
        <details className="ai-older">
          <summary>Evaluaciones anteriores ({older.length})</summary>
          {older.map((evaluation) => (
            <div key={evaluation.id} className="ai-older__item">
              <p>
                <span className={`badge badge--${AI_OUTCOME_TONES[evaluation.outcome]}`}>{AI_OUTCOME_LABELS[evaluation.outcome]}</span>{' '}
                {formatDateTime(evaluation.createdAt)}
              </p>
              <AiEvaluation evaluation={evaluation} />
            </div>
          ))}
        </details>
      )}
    </section>
  )
}

function AiEvaluation({ evaluation }) {
  if (evaluation.outcome === 'FAILED') {
    return (
      <>
        <p className="notice notice--error">{evaluation.error}</p>
        <p className="page__note">
          {formatDateTime(evaluation.createdAt)} · modelo {evaluation.model} · {formatDuration(evaluation.durationMs)}
        </p>
      </>
    )
  }
  return (
    <>
      <dl className="facts">
        <div>
          <dt>Legible</dt>
          <dd>{evaluation.legible ? 'Sí' : 'No'}</dd>
        </div>
        <div>
          <dt>Confianza reportada</dt>
          <dd>{evaluation.confidence} %</dd>
        </div>
        <div>
          <dt>Nota propuesta</dt>
          <dd>{formatScore(evaluation.proposedScore)}</dd>
        </div>
        <div>
          <dt>Tiempo</dt>
          <dd>{formatDuration(evaluation.durationMs)}</dd>
        </div>
      </dl>
      {evaluation.criteria.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Criterio</th>
                <th scope="col">Puntos</th>
                <th scope="col">Comentario</th>
              </tr>
            </thead>
            <tbody>
              {evaluation.criteria.map((criterion) => (
                <tr key={criterion.name}>
                  <td>{criterion.name}</td>
                  <td>
                    {formatPoints(criterion.points)} de {formatPoints(criterion.maxPoints)}
                  </td>
                  <td>{criterion.comment ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {evaluation.feedback && (
        <>
          <h3>Retroalimentación propuesta</h3>
          <p className="statement">{evaluation.feedback}</p>
        </>
      )}
      {evaluation.transcription && (
        <>
          <h3>Lo que leyó la IA</h3>
          <pre className="transcription">{evaluation.transcription}</pre>
        </>
      )}
      <p className="page__note">
        {formatDateTime(evaluation.createdAt)} · modelo {evaluation.model}
      </p>
    </>
  )
}

// Calificación del docente: nota de 0,0 a 5,0 con un decimal, guiada por la rúbrica. Si la IA dejó una
// propuesta (confianza baja), el formulario la trae para confirmarla o ajustarla.
function GradeForm({ submissionId, current, proposal, onSaved }) {
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
  const initialScore = published ? formatScore(current.score) : proposal ? formatScore(proposal.proposedScore) : ''
  const initialFeedback = published ? current.feedback : proposal?.feedback
  return (
    <form onSubmit={save.submit} noValidate className="subform">
      {proposal && <p className="page__note">La nota y la retroalimentación vienen de la propuesta de la IA. Puedes ajustarlas antes de confirmar.</p>}
      <Field
        label={published ? 'Corregir nota (0,0 a 5,0)' : 'Nota (0,0 a 5,0)'}
        name="score"
        inputMode="decimal"
        required
        defaultValue={initialScore}
        error={save.fieldErrors.score}
        className="input--short"
      />
      <div className="field">
        <label htmlFor="nota-retroalimentacion">Retroalimentación para el estudiante (opcional)</label>
        <textarea id="nota-retroalimentacion" name="feedback" rows={3} maxLength={5000} defaultValue={initialFeedback ?? ''} />
      </div>
      <button type="submit" className="button" disabled={save.pending}>
        {published ? 'Guardar corrección' : proposal ? 'Confirmar nota' : 'Publicar nota'}
      </button>
      <FormStatus error={save.error} success={save.result && 'Nota publicada.'} />
    </form>
  )
}
