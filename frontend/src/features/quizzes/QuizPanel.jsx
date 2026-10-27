import { useState } from 'react'
import { apiPost, apiPut } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { navigate } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatDateTime, formatScore, formatSeconds } from '../activities/labels.js'
import { QUESTION_KIND_LABELS } from './labels.js'

// Quiz de una actividad (RF-10, RF-21): el docente elige el tiempo total y las preguntas del banco;
// el estudiante empieza o continúa su intento, ve sus intentos y el ranking (RN-13, RN-14).
export default function QuizPanel({ activity, onChanged }) {
  const quiz = useQuery(`/activities/${activity.id}/quiz`)
  const [editing, setEditing] = useState(false)
  const data = quiz.data

  return (
    <>
      <section className="card card--wide" aria-labelledby="quiz-titulo">
        <h2 id="quiz-titulo">Quiz</h2>
        <FormStatus error={quiz.error} />
        {quiz.status === 'ready' && !data && (
          <p>{activity.canManage ? 'Elige el tiempo y las preguntas del banco para poder publicarlo.' : 'El quiz aún no tiene preguntas.'}</p>
        )}
        {data && (
          <p>
            {data.questionCount} preguntas · {data.maxPoints} puntos · {data.timeLimitMinutes} minutos en total
            {activity.kind === 'PRACTICE_QUIZ' ? ' · de repaso, sin nota' : ' · la nota va de 0 a 5 según los puntos obtenidos'}
          </p>
        )}
        {data?.questions && <QuestionList questions={data.questions} />}
        {activity.canManage && quiz.status !== 'loading' && !data?.hasAttempts && (
          <>
            <button type="button" className="button button--secondary button--small" onClick={() => setEditing(!editing)}>
              {editing ? 'Cancelar' : data ? 'Cambiar preguntas o tiempo' : 'Armar el quiz'}
            </button>
            {editing && (
              <QuizEditor
                activity={activity}
                quiz={data}
                onSaved={() => {
                  setEditing(false)
                  quiz.reload()
                  onChanged()
                }}
              />
            )}
          </>
        )}
        {data?.hasAttempts && activity.canManage && (
          <p className="page__note">Ya hay intentos: las preguntas y el tiempo no cambian para que todos respondan el mismo quiz.</p>
        )}
        {activity.progress && data && <StudentAttempts activity={activity} />}
      </section>
      {data && <Ranking activity={activity} />}
    </>
  )
}

function QuestionList({ questions }) {
  return (
    <ol className="quiz-questions">
      {questions.map((question) => (
        <li key={question.id}>
          <strong>{question.prompt}</strong>{' '}
          <span className="page__note">
            {QUESTION_KIND_LABELS[question.kind]} · {question.points} {question.points === 1 ? 'punto' : 'puntos'}
          </span>
          <AnswerKey question={question} />
        </li>
      ))}
    </ol>
  )
}

export function AnswerKey({ question }) {
  if (question.kind === 'SHORT') {
    return <div className="page__note">Respuestas aceptadas: {question.acceptedAnswers.join(' · ')}</div>
  }
  return (
    <ul className="options">
      {question.options.map((option, index) => (
        <li key={index} className={option.correct ? 'option option--correct' : 'option'}>
          {option.text}
          {option.correct && <span className="visually-hidden"> (correcta)</span>}
        </li>
      ))}
    </ul>
  )
}

function QuizEditor({ activity, quiz, onSaved }) {
  const bank = useQuery(`/courses/${activity.courseId}/questions`)
  // El orden del quiz es el orden en que se marcan las preguntas.
  const [chosen, setChosen] = useState(() => quiz?.questions.map((question) => question.id) ?? [])
  const save = useSubmit(async (event) => {
    const minutes = Number(new FormData(event.currentTarget).get('timeLimitMinutes'))
    const result = await apiPut(`/activities/${activity.id}/quiz`, { timeLimitMinutes: minutes, questionIds: chosen })
    onSaved()
    return result
  })
  const toggle = (id) => setChosen((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]))
  const points = (bank.data ?? []).filter((question) => chosen.includes(question.id)).reduce((sum, question) => sum + question.points, 0)

  return (
    <form onSubmit={save.submit} noValidate className="subform">
      <Field
        id="quiz-minutos"
        label="Tiempo total (minutos)"
        name="timeLimitMinutes"
        type="number"
        min="1"
        max="300"
        required
        defaultValue={quiz?.timeLimitMinutes ?? 20}
        hint="Al acabarse el tiempo, el intento se envía solo con las respuestas guardadas."
      />
      <FormStatus error={bank.error} />
      {bank.data?.length === 0 && (
        <p>
          El banco del curso está vacío. Agrega preguntas en la sección{' '}
          <Link to={`/cursos/${activity.courseId}?seccion=preguntas`}>Banco de preguntas</Link> del curso.
        </p>
      )}
      {bank.data?.length > 0 && (
        <fieldset className="choices">
          <legend>
            Preguntas del banco ({chosen.length} elegidas, {points} puntos)
          </legend>
          {bank.data.map((question) => (
            <label key={question.id}>
              <input type="checkbox" checked={chosen.includes(question.id)} onChange={() => toggle(question.id)} />{' '}
              {chosen.includes(question.id) && <strong>{chosen.indexOf(question.id) + 1}. </strong>}
              {question.prompt} <span className="page__note">({QUESTION_KIND_LABELS[question.kind]}, {question.points} pts)</span>
            </label>
          ))}
        </fieldset>
      )}
      <button type="submit" className="button" disabled={save.pending}>
        Guardar quiz
      </button>
      <FormStatus error={save.error} />
    </form>
  )
}

function StudentAttempts({ activity }) {
  const mine = useQuery(`/activities/${activity.id}/quiz/attempts/mine`)
  const open = mine.data?.find((attempt) => attempt.status === 'IN_PROGRESS')
  const start = useSubmit(async () => {
    const attempt = await apiPost(`/activities/${activity.id}/quiz/attempts`)
    navigate(`/quiz-intentos/${attempt.id}`)
    return attempt
  })
  const { progress } = activity
  const practice = activity.kind === 'PRACTICE_QUIZ'
  const canStart = open || (practice ? progress.state === 'OPEN' : progress.canSubmit)

  return (
    <div className="subform">
      {canStart ? (
        <>
          <button type="button" className="button" disabled={start.pending} onClick={() => start.submit()}>
            {open ? 'Continuar mi intento' : practice && mine.data?.length > 0 ? 'Practicar otra vez' : 'Empezar el quiz'}
          </button>
          {!open && <p className="page__note">El tiempo corre desde que empiezas y no se detiene si cierras la página.</p>}
        </>
      ) : (
        <p className="page__note">{closedText(progress)}</p>
      )}
      <FormStatus error={start.error} />
      {mine.data?.length > 0 && (
        <>
          <h3>Mis intentos</h3>
          <ul className="links">
            {mine.data.map((attempt) => (
              <li key={attempt.id}>
                <Link to={`/quiz-intentos/${attempt.id}`}>
                  Intento {attempt.number} · {formatDateTime(attempt.startedAt)}
                </Link>{' '}
                {attempt.status === 'IN_PROGRESS' ? (
                  <span className="badge badge--info">En curso</span>
                ) : (
                  <span className="badge badge--ok">
                    {formatScore(attempt.score)} · {attempt.correctCount} de {attempt.questionCount} correctas
                  </span>
                )}
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
}

function closedText(progress) {
  if (progress.state === 'NOT_OPEN') return 'El quiz aún no está abierto.'
  if (progress.attemptsUsed >= progress.attemptsAllowed) return 'Ya usaste los intentos permitidos.'
  return 'El quiz cerró. Si necesitas otro intento, habla con tu docente.'
}

function Ranking({ activity }) {
  const ranking = useQuery(`/activities/${activity.id}/quiz/ranking`)
  if (ranking.status === 'error') return null
  return (
    <section className="card card--wide" aria-labelledby="ranking-titulo">
      <h2 id="ranking-titulo">Ranking</h2>
      <p className="page__note">Por nota y, si empatan, por menor tiempo.{activity.kind === 'PRACTICE_QUIZ' && ' Cuenta el mejor intento.'}</p>
      {ranking.data?.length === 0 && <p>Aún nadie ha terminado el quiz.</p>}
      {ranking.data?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Puesto</th>
                <th scope="col">Estudiante</th>
                <th scope="col">Nota</th>
                <th scope="col">Correctas</th>
                <th scope="col">Tiempo</th>
              </tr>
            </thead>
            <tbody>
              {ranking.data.map((row) => (
                <tr key={row.position} className={row.mine ? 'row--mine' : undefined}>
                  <td>{row.position}</td>
                  <td>
                    {row.attemptId ? <Link to={`/quiz-intentos/${row.attemptId}`}>{row.studentName}</Link> : row.studentName}
                    {row.mine && ' (tú)'}
                  </td>
                  <td>{formatScore(row.score)}</td>
                  <td>
                    {row.correctCount} de {row.questionCount}
                  </td>
                  <td>{formatSeconds(row.durationSeconds)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
