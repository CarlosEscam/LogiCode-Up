import { useState } from 'react'
import { apiDelete, apiPost, apiPut, apiSendForm } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { navigate } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { ActivityFields } from './ActivityForm.jsx'
import { activityBody } from './activityBody.js'
import PseintRunner from '../pseint/PseintRunner.jsx'
import QuizPanel from '../quizzes/QuizPanel.jsx'
import RubricPanel from './RubricPanel.jsx'
import {
  ACTIVITY_KIND_LABELS,
  FILE_GROUP_LABELS,
  STATUS_LABELS,
  acceptFor,
  formatDateTime,
  formatPercent,
  formatScore,
  fromBogotaInput,
  isQuizKind,
  studentSituation,
  toBogotaInput,
} from './labels.js'
import { useRefreshWhile } from './useRefreshWhile.js'

const MAX_FILES = 5

// Detalle de una actividad: enunciado, rúbrica y, según quién la mira, el formulario de entrega
// del estudiante o la lista de inscritos con sus entregas, notas y excepciones de plazo.
export default function ActivityPage({ id }) {
  const activity = useQuery(`/activities/${id}`)
  const [notice, setNotice] = useState(null)

  if (activity.status === 'loading' && !activity.data) return <p role="status">Cargando…</p>
  if (activity.status === 'error') {
    return (
      <section className="card">
        <h1>Actividad no disponible</h1>
        <FormStatus error={activity.error} />
        <p>
          <Link to="/cursos">Volver a cursos</Link>
        </p>
      </section>
    )
  }

  const data = activity.data
  const reload = () => activity.reload()
  const quiz = isQuizKind(data.kind)
  return (
    <>
      <section className="card card--wide" aria-labelledby="actividad-titulo">
        <p className="page__note">
          <Link to={`/cursos/${data.courseId}?seccion=actividades`}>Volver a las actividades del curso</Link>
        </p>
        <div className="topic__header">
          <h1 id="actividad-titulo">{data.title}</h1>
          <span className="badge">{ACTIVITY_KIND_LABELS[data.kind]}</span>
          {data.status === 'DRAFT' && <span className="badge badge--private">{STATUS_LABELS.DRAFT}</span>}
        </div>
        <dl className="facts">
          <div>
            <dt>Apertura</dt>
            <dd>{formatDateTime(data.opensAt)}</dd>
          </div>
          <div>
            <dt>Cierre</dt>
            <dd>{formatDateTime(data.closesAt)}</dd>
          </div>
          <div>
            <dt>Porcentaje</dt>
            <dd>{formatPercent(data.weightPercent)}</dd>
          </div>
          <div>
            <dt>Intentos</dt>
            <dd>{data.kind === 'PRACTICE_QUIZ' ? 'Libres' : data.maxAttempts}</dd>
          </div>
          {!quiz && (
            <div>
              <dt>Se entrega como</dt>
              <dd>{deliveryModes(data)}</dd>
            </div>
          )}
        </dl>
        <p className="statement">{data.statement}</p>
        {data.progress && <StudentStatus activity={data} />}
        {data.canManage && <ManageActions activity={data} onChanged={reload} setNotice={setNotice} />}
        <FormStatus error={notice?.error} success={notice?.success} />
      </section>

      {quiz ? <QuizPanel activity={data} onChanged={reload} /> : <RubricPanel activity={data} />}

      {data.progress && !quiz && <StudentWork activity={data} onSubmitted={reload} />}
      {data.canViewSubmissions && data.kind !== 'PRACTICE_QUIZ' && <Roster activity={data} />}
    </>
  )
}

function deliveryModes(activity) {
  const modes = []
  if (activity.acceptsText) modes.push('texto')
  if (activity.acceptsFiles) modes.push(`archivos: ${activity.fileGroups.map((group) => FILE_GROUP_LABELS[group]).join('; ')}`)
  if (activity.acceptsPhotos) modes.push('fotos')
  return modes.join(', ')
}

function StudentStatus({ activity }) {
  const practice = activity.kind === 'PRACTICE_QUIZ'
  const grades = useQuery(practice ? null : `/courses/${activity.courseId}/grades/mine`)
  const grade = grades.data?.find((item) => item.activityId === activity.id)
  useRefreshWhile(grade?.status === 'PROCESSING', grades.reload)
  const situation = studentSituation(activity.progress, grade)
  const { progress } = activity
  return (
    <div className="student-status">
      <p>
        <span className={`badge badge--${situation.tone}`}>{situation.label}</span>
      </p>
      {practice ? (
        <p className="page__note">Quiz de repaso: no da nota y puedes repetirlo hasta {formatDateTime(activity.closesAt)}.</p>
      ) : (
        <p className="page__note">
          Intentos usados: {progress.attemptsUsed} de {progress.attemptsAllowed} · Plazo para entregar:{' '}
          {formatDateTime(progress.deadline)}
          {progress.hasException && ' · Ajustado por tu docente'}
        </p>
      )}
      {grade?.status === 'PUBLISHED' && grade.feedback && (
        <p>
          <strong>Retroalimentación:</strong> {grade.feedback}
        </p>
      )}
    </div>
  )
}

function ManageActions({ activity, onChanged, setNotice }) {
  const [editing, setEditing] = useState(false)

  async function run(action, success) {
    setNotice(null)
    try {
      await action()
      setNotice({ success })
      onChanged()
    } catch (error) {
      setNotice({ error: error.message })
    }
  }

  const save = useSubmit(async (event) => {
    const result = await apiPut(`/activities/${activity.id}`, activityBody(event.currentTarget))
    setEditing(false)
    setNotice({ success: 'Actividad actualizada.' })
    onChanged()
    return result
  })

  return (
    <>
      <div className="toolbar" aria-label="Acciones de la actividad">
        {activity.status === 'DRAFT' && (
          <button
            type="button"
            className="button button--small"
            onClick={() => run(() => apiPost(`/activities/${activity.id}/publish`), 'Actividad publicada: los estudiantes ya la ven.')}
          >
            Publicar
          </button>
        )}
        <button type="button" className="button button--secondary button--small" onClick={() => setEditing(!editing)}>
          {editing ? 'Cancelar edición' : 'Editar actividad'}
        </button>
        <button
          type="button"
          className="button button--danger button--small"
          onClick={async () => {
            if (!window.confirm(`¿Archivar "${activity.title}"? Dejará de mostrarse; las entregas se conservan.`)) return
            try {
              await apiDelete(`/activities/${activity.id}`)
              navigate(`/cursos/${activity.courseId}?seccion=actividades`)
            } catch (error) {
              setNotice({ error: error.message })
            }
          }}
        >
          Archivar
        </button>
      </div>
      {editing && (
        <form onSubmit={save.submit} noValidate className="subform">
          <p className="page__note">Si cambias el enunciado se guarda una versión nueva de la rúbrica para las próximas entregas.</p>
          <ActivityFields prefix="editar-actividad" activity={activity} />
          <button type="submit" className="button" disabled={save.pending}>
            Guardar cambios
          </button>
          <FormStatus error={save.error} />
        </form>
      )}
    </>
  )
}

function StudentWork({ activity, onSubmitted }) {
  const mine = useQuery(`/activities/${activity.id}/submissions/mine`)
  // La confirmación queda aquí porque el formulario se oculta si ya no quedan intentos.
  const [receipt, setReceipt] = useState(null)
  return (
    <section className="card card--wide" aria-labelledby="mis-entregas-titulo">
      <h2 id="mis-entregas-titulo">Mi entrega</h2>
      {activity.progress.canSubmit ? (
        <SubmitForm
          activity={activity}
          onSubmitted={(result) => {
            setReceipt(result)
            mine.reload()
            onSubmitted()
          }}
        />
      ) : (
        <p className="page__note">{closedReason(activity.progress)}</p>
      )}
      <FormStatus
        success={receipt && `Entrega recibida (intento ${receipt.attempt}${receipt.late ? ', fuera del plazo original' : ''}).`}
      />
      {mine.data?.length > 0 && (
        <>
          <h3>Intentos enviados</h3>
          <ul className="links">
            {mine.data.map((submission) => (
              <li key={submission.id}>
                <Link to={`/entregas/${submission.id}`}>
                  Intento {submission.attempt} · {formatDateTime(submission.submittedAt)}
                </Link>
                {submission.late && <span className="badge badge--warning">Tarde</span>}
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}

function closedReason(progress) {
  if (progress.state === 'NOT_OPEN') return 'La actividad aún no está abierta.'
  if (progress.attemptsUsed >= progress.attemptsAllowed) return 'Ya usaste los intentos permitidos.'
  return 'La actividad cerró. Si necesitas más plazo, habla con tu docente.'
}

function SubmitForm({ activity, onSubmitted }) {
  const [progress, setProgress] = useState(null)
  const [code, setCode] = useState('')
  const rubric = useQuery(`/activities/${activity.id}/rubric`)
  const graded = rubric.data?.testCaseCount > 0
  const acceptsUploads = activity.acceptsFiles || activity.acceptsPhotos
  const send = useSubmit(async (event) => {
    const form = event.currentTarget
    const data = new FormData(form)
    const files = data.getAll('files').filter((file) => file && file.name)
    data.delete('files')
    files.forEach((file) => data.append('files', file))
    if (files.length > MAX_FILES) throw new Error(`Puedes adjuntar hasta ${MAX_FILES} archivos.`)
    if (!(data.get('text') ?? '').trim() && files.length === 0) {
      throw new Error('La entrega está vacía: escribe tu respuesta o adjunta un archivo.')
    }
    setProgress(files.length > 0 ? 0 : null)
    try {
      const result = await apiSendForm(`/activities/${activity.id}/submissions`, data, { onProgress: setProgress })
      form.reset()
      setCode('')
      onSubmitted(result)
      return result
    } finally {
      setProgress(null)
    }
  })

  return (
    <form onSubmit={send.submit} noValidate>
      {activity.acceptsText && (
        <div className="field">
          <label htmlFor="entrega-texto">{graded ? 'Respuesta (tu programa en PSeInt)' : 'Respuesta'}</label>
          <textarea
            id="entrega-texto"
            name="text"
            rows={graded ? 14 : 6}
            maxLength={50000}
            spellCheck={graded ? false : undefined}
            className={graded ? 'code' : undefined}
            value={code}
            onChange={(event) => setCode(event.target.value)}
            aria-describedby={graded ? 'entrega-texto-ayuda' : undefined}
          />
          {graded && (
            <small id="entrega-texto-ayuda" className="field__hint">
              Se califica sola con {rubric.data.testCaseCount} casos de prueba: cada caso da unas entradas y revisa que tu programa
              muestre lo esperado. También puedes adjuntar tu archivo .psc.
            </small>
          )}
        </div>
      )}
      {activity.acceptsText && graded && <PseintRunner code={code} idPrefix="entrega" />}
      {acceptsUploads && (
        <div className="field">
          <label htmlFor="entrega-archivos">{activity.acceptsPhotos && !activity.acceptsFiles ? 'Fotos' : 'Archivos'}</label>
          <input id="entrega-archivos" type="file" name="files" multiple accept={acceptFor(activity)} aria-describedby="entrega-archivos-ayuda" />
          <small id="entrega-archivos-ayuda" className="field__hint">
            Hasta {MAX_FILES} archivos ({acceptFor(activity).replaceAll(',', ', ')}). Máximo 500 MB cada uno; videos hasta 1 GB.
          </small>
        </div>
      )}
      {progress != null && (
        <p role="status">
          <progress max="100" value={progress} aria-label="Avance de la carga" /> {progress} %
        </p>
      )}
      <button type="submit" className="button" disabled={send.pending}>
        {send.pending ? 'Enviando…' : 'Enviar entrega'}
      </button>
      <FormStatus error={send.error} />
    </form>
  )
}

function Roster({ activity }) {
  const roster = useQuery(`/activities/${activity.id}/submissions`)
  const grades = useQuery(`/activities/${activity.id}/grades`)
  const gradeBySubmission = Object.fromEntries((grades.data ?? []).map((row) => [row.submissionId, row.grade]))

  return (
    <section className="card card--wide" aria-labelledby="inscritos-titulo">
      <h2 id="inscritos-titulo">Entregas de los estudiantes</h2>
      <FormStatus error={roster.error} />
      {roster.status === 'loading' && !roster.data && <p role="status">Cargando…</p>}
      {roster.data?.length === 0 && <p>El curso no tiene estudiantes inscritos.</p>}
      {roster.data?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Estudiante</th>
                <th scope="col">Situación</th>
                <th scope="col">Última entrega</th>
                <th scope="col">Nota</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {roster.data.map((work) => (
                <RosterRow
                  key={work.cedula}
                  work={work}
                  grade={gradeBySubmission[work.lastSubmissionId]}
                  activity={activity}
                  onChanged={roster.reload}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function RosterRow({ work, grade, activity, onChanged }) {
  const [open, setOpen] = useState(false)
  const name = work.fullName ?? `Cédula ${work.cedula}`
  const situation = work.studentId
    ? studentSituation(work.progress, grade)
    : { label: 'Aún no se registra', tone: 'muted' }
  return (
    <>
      <tr>
        <td>
          {name}
          <br />
          <small className="page__note">{work.cedula}</small>
        </td>
        <td>
          <span className={`badge badge--${situation.tone}`}>{situation.label}</span>
          {work.exception && <span className="badge">Excepción</span>}
        </td>
        <td>
          {work.lastSubmittedAt ? (
            <>
              {formatDateTime(work.lastSubmittedAt)} · intento {work.attempts}
              {work.lastLate && <span className="badge badge--warning">Tarde</span>}
            </>
          ) : (
            '—'
          )}
        </td>
        <td>{grade?.status === 'PUBLISHED' ? formatScore(grade.score) : work.progress.state === 'OVERDUE' ? '0,0' : '—'}</td>
        <td>
          <div className="actions">
            {work.lastSubmissionId && (
              <Link className="button button--secondary button--small" to={`/entregas/${work.lastSubmissionId}`} aria-label={`Ver entrega de ${name}`}>
                Ver entrega
              </Link>
            )}
            {activity.canManage && work.studentId && (
              <button
                type="button"
                className="button button--secondary button--small"
                aria-expanded={open}
                aria-label={`Plazo e intentos de ${name}`}
                onClick={() => setOpen(!open)}
              >
                Plazo e intentos
              </button>
            )}
          </div>
        </td>
      </tr>
      {open && (
        <tr>
          <td colSpan={5}>
            <ExceptionForm activity={activity} work={work} name={name} onChanged={onChanged} />
          </td>
        </tr>
      )}
    </>
  )
}

// RN-05: ampliar el plazo (también para reabrir después del cierre) o dar intentos adicionales.
function ExceptionForm({ activity, work, name, onChanged }) {
  const prefix = `excepcion-${work.studentId}`
  const url = `/activities/${activity.id}/exceptions/${work.studentId}`
  const save = useSubmit(async (event) => {
    const data = new FormData(event.currentTarget)
    await apiPut(url, {
      extendedUntil: fromBogotaInput(data.get('extendedUntil')),
      extraAttempts: Number(data.get('extraAttempts') || 0),
      reason: data.get('reason') || null,
    })
    onChanged()
    return true
  })
  const remove = useSubmit(async () => {
    await apiDelete(url)
    onChanged()
    return true
  })
  return (
    <form onSubmit={save.submit} noValidate className="exception-form" aria-label={`Excepción de plazo para ${name}`}>
      <div className="form-row">
        <Field
          id={`${prefix}-hasta`}
          label="Nuevo plazo (hora de Colombia)"
          name="extendedUntil"
          type="datetime-local"
          defaultValue={toBogotaInput(work.exception?.extendedUntil)}
        />
        <Field
          id={`${prefix}-intentos`}
          label="Intentos adicionales"
          name="extraAttempts"
          type="number"
          min="0"
          max="10"
          defaultValue={work.exception?.extraAttempts ?? 0}
        />
      </div>
      <Field id={`${prefix}-motivo`} label="Motivo (opcional)" name="reason" maxLength={500} defaultValue={work.exception?.reason ?? ''} />
      <p className="page__note">Para un segundo intento después del cierre, da un intento adicional y un plazo nuevo.</p>
      <div className="toolbar">
        <button type="submit" className="button button--small" disabled={save.pending}>
          Guardar excepción
        </button>
        {work.exception && (
          <button type="button" className="button button--danger button--small" onClick={() => remove.submit()}>
            Quitar excepción
          </button>
        )}
      </div>
      <FormStatus error={save.error ?? remove.error} success={(save.result || remove.result) && 'Excepción actualizada.'} />
    </form>
  )
}
