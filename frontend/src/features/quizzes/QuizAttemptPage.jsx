import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, apiPost, apiPut } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { formatDateTime, formatScore, formatSeconds } from '../activities/labels.js'
import { QUESTION_KIND_LABELS } from './labels.js'

const AUTOSAVE_MS = 1500

// Un intento de quiz: mientras está en curso, las respuestas se guardan solas y al acabarse el tiempo
// se envía (RN-13); si el navegador se cierra, el servidor lo envía igual con lo guardado.
// Terminado, muestra el resultado y, cuando está permitida, la revisión con la clave.
export default function QuizAttemptPage({ id }) {
  const attempt = useQuery(`/quiz-attempts/${id}`)
  if (attempt.status === 'loading' && !attempt.data) return <p role="status">Cargando…</p>
  if (attempt.status === 'error') {
    return (
      <section className="card">
        <h1>Intento no disponible</h1>
        <FormStatus error={attempt.error} />
        <p>
          <Link to="/cursos">Volver a cursos</Link>
        </p>
      </section>
    )
  }
  const data = attempt.data
  return data.status === 'IN_PROGRESS' ? (
    <AnswerForm key={data.id} attempt={data} onFinished={attempt.reload} />
  ) : (
    <Result attempt={data} />
  )
}

function initialAnswers(attempt) {
  return Object.fromEntries(attempt.questions.map((q) => [q.id, { choices: q.chosen ?? [], text: q.text ?? '' }]))
}

function toRequest(answers) {
  return {
    answers: Object.entries(answers).map(([questionId, answer]) => ({ questionId, choices: answer.choices, text: answer.text || null })),
  }
}

function AnswerForm({ attempt, onFinished }) {
  const [answers, setAnswers] = useState(() => initialAnswers(attempt))
  const [saveState, setSaveState] = useState('saved')
  const [error, setError] = useState(null)
  const [sending, setSending] = useState(false)
  const remaining = useRemaining(attempt.deadline)
  const latest = useRef(answers)
  const autoSent = useRef(false)
  useEffect(() => {
    latest.current = answers
  }, [answers])

  const finish = useCallback(async () => {
    setSending(true)
    setError(null)
    try {
      await apiPost(`/quiz-attempts/${attempt.id}/finish`, toRequest(latest.current))
    } catch (failure) {
      // 409: el servidor ya lo envió (se acabó el tiempo); igual se muestra el resultado.
      if (!(failure instanceof ApiError && failure.status === 409)) {
        setError(failure.message)
        setSending(false)
        return
      }
    }
    onFinished()
  }, [attempt.id, onFinished])

  // Guardado automático un momento después del último cambio.
  useEffect(() => {
    if (saveState !== 'dirty') return undefined
    const timer = setTimeout(async () => {
      setSaveState('saving')
      try {
        await apiPut(`/quiz-attempts/${attempt.id}/answers`, toRequest(latest.current))
        setSaveState((state) => (state === 'saving' ? 'saved' : state))
      } catch (failure) {
        if (failure instanceof ApiError && failure.status === 409) onFinished()
        else {
          setSaveState('dirty')
          setError('No se pudieron guardar las respuestas. Revisa tu conexión; se reintentará.')
        }
      }
    }, AUTOSAVE_MS)
    return () => clearTimeout(timer)
  }, [answers, saveState, attempt.id, onFinished])

  // Se envía solo una vez al acabarse el tiempo; si falla, queda el botón y el servidor lo envía igual.
  useEffect(() => {
    if (remaining > 0 || autoSent.current) return
    autoSent.current = true
    finish()
  }, [remaining, finish])

  function update(questionId, change) {
    setAnswers((current) => ({ ...current, [questionId]: { ...current[questionId], ...change } }))
    setSaveState('dirty')
    setError(null)
  }

  const answered = attempt.questions.filter((q) => answers[q.id].choices.length > 0 || answers[q.id].text.trim()).length
  return (
    <form
      className="card card--wide"
      aria-labelledby="intento-titulo"
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        finish()
      }}
    >
      <h1 id="intento-titulo">{attempt.practice ? 'Quiz de repaso' : 'Quiz'} · intento {attempt.number}</h1>
      <div className="quiz-timer" role="timer" aria-live="off">
        <strong>Tiempo restante: {clock(remaining)}</strong> · {answered} de {attempt.questionCount} respondidas ·{' '}
        <span role="status">{SAVE_LABELS[saveState]}</span>
      </div>
      {remaining <= 60 && remaining > 0 && (
        <p className="notice notice--error" role="alert">
          Queda menos de un minuto: el intento se enviará solo.
        </p>
      )}
      <ol className="quiz-questions quiz-questions--answer">
        {attempt.questions.map((question, index) => (
          <li key={question.id}>
            <Question question={question} number={index + 1} answer={answers[question.id]} onChange={(change) => update(question.id, change)} />
          </li>
        ))}
      </ol>
      <FormStatus error={error} />
      <button type="submit" className="button" disabled={sending}>
        {sending ? 'Enviando…' : 'Enviar intento'}
      </button>
    </form>
  )
}

const SAVE_LABELS = { saved: 'Respuestas guardadas', dirty: 'Cambios sin guardar', saving: 'Guardando…' }

function Question({ question, number, answer, onChange }) {
  const name = `pregunta-${question.id}`
  return (
    <fieldset className="choices">
      <legend>
        <strong>{number}.</strong> {question.prompt}{' '}
        <span className="page__note">
          ({QUESTION_KIND_LABELS[question.kind]}, {question.points} {question.points === 1 ? 'punto' : 'puntos'})
        </span>
      </legend>
      {question.kind === 'SHORT' ? (
        <input
          aria-label={`Respuesta a la pregunta ${number}`}
          maxLength={500}
          value={answer.text}
          onChange={(event) => onChange({ text: event.target.value })}
        />
      ) : (
        question.options.map((option, index) => {
          const multiple = question.kind === 'MULTIPLE'
          const checked = answer.choices.includes(index)
          return (
            <label key={index}>
              <input
                type={multiple ? 'checkbox' : 'radio'}
                name={name}
                checked={checked}
                onChange={() =>
                  onChange({
                    choices: multiple ? (checked ? answer.choices.filter((c) => c !== index) : [...answer.choices, index].sort()) : [index],
                  })
                }
              />{' '}
              {option}
            </label>
          )
        })
      )}
      {question.kind === 'MULTIPLE' && <small className="field__hint">Marca todas las correctas.</small>}
    </fieldset>
  )
}

// Segundos que faltan para la hora límite, actualizados cada segundo.
// shortcut: usa el reloj del equipo; si está muy desfasado, el servidor (que manda) envía el intento a su hora.
function useRemaining(deadline) {
  const compute = useCallback(() => Math.max(0, Math.ceil((new Date(deadline).getTime() - Date.now()) / 1000)), [deadline])
  const [remaining, setRemaining] = useState(compute)
  useEffect(() => {
    const timer = setInterval(() => setRemaining(compute()), 1000)
    return () => clearInterval(timer)
  }, [compute])
  return remaining
}

function clock(seconds) {
  const minutes = Math.floor(seconds / 60)
  return `${minutes}:${String(seconds % 60).padStart(2, '0')}`
}

function Result({ attempt }) {
  return (
    <>
      <section className="card card--wide" aria-labelledby="resultado-titulo">
        <p className="page__note">
          <Link to={`/actividades/${attempt.activityId}`}>Volver al quiz</Link>
        </p>
        <h1 id="resultado-titulo">
          {attempt.studentName ? `Intento de ${attempt.studentName}` : 'Tu intento'} · número {attempt.number}
        </h1>
        <p className="score">{formatScore(attempt.score)}</p>
        <p>
          {attempt.correctCount} de {attempt.questionCount} respuestas correctas ({attempt.points} de {attempt.maxPoints} puntos) · tiempo{' '}
          {formatSeconds(attempt.durationSeconds)} · enviado {formatDateTime(attempt.finishedAt)}
        </p>
        {attempt.practice ? (
          <p className="page__note">Es de repaso: no cuenta para la nota del curso.</p>
        ) : (
          attempt.submissionId && (
            <p className="page__note">
              La nota quedó publicada en la planilla. <Link to={`/entregas/${attempt.submissionId}`}>Ver la entrega y su nota</Link>
            </p>
          )
        )}
        {!attempt.reviewAvailable && <p className="page__note">Las respuestas correctas se muestran cuando cierre el quiz.</p>}
      </section>
      {attempt.reviewAvailable && attempt.questions && <Review attempt={attempt} />}
    </>
  )
}

function Review({ attempt }) {
  return (
    <section className="card card--wide" aria-labelledby="revision-titulo">
      <h2 id="revision-titulo">Revisión</h2>
      <ol className="quiz-questions">
        {attempt.questions.map((question) => (
          <li key={question.id}>
            <p>
              <strong>{question.prompt}</strong>{' '}
              <span className={`badge badge--${question.correct ? 'ok' : 'danger'}`}>{question.correct ? 'Correcta' : 'Incorrecta'}</span>
            </p>
            {question.kind === 'SHORT' ? (
              <p>
                Respondiste: {question.text || '(sin respuesta)'}
                <br />
                <span className="page__note">Aceptadas: {question.acceptedAnswers.join(' · ')}</span>
              </p>
            ) : (
              <ul className="options">
                {question.options.map((option, index) => (
                  <li key={index} className={question.correctChoices.includes(index) ? 'option option--correct' : 'option'}>
                    {option}
                    {question.chosen.includes(index) && <strong> · tu respuesta</strong>}
                    {question.correctChoices.includes(index) && <span className="visually-hidden"> (correcta)</span>}
                  </li>
                ))}
              </ul>
            )}
            {question.explanation && <p className="page__note">{question.explanation}</p>}
          </li>
        ))}
      </ol>
    </section>
  )
}
