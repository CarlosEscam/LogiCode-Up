import { useState } from 'react'
import { apiDelete, apiPost, apiPut } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { AnswerKey } from './QuizPanel.jsx'
import { QUESTION_KIND_LABELS } from './labels.js'

const MAX_OPTIONS = 6

// Banco de preguntas del curso (RF-10): los quizzes eligen de aquí. Una pregunta ya respondida en
// un intento no se edita, para que las notas no cambien; sí se puede crear una nueva.
export default function QuestionBankPanel({ courseId, canManage }) {
  const bank = useQuery(`/courses/${courseId}/questions`)
  const [creating, setCreating] = useState(false)
  return (
    <section className="card card--wide" aria-labelledby="banco-titulo">
      <h2 id="banco-titulo">Banco de preguntas</h2>
      <FormStatus error={bank.error} />
      {canManage && (
        <>
          <button type="button" className="button button--small" onClick={() => setCreating(!creating)}>
            {creating ? 'Cancelar' : 'Nueva pregunta'}
          </button>
          {creating && (
            <QuestionForm
              prefix="nueva-pregunta"
              onSave={(body) => apiPost(`/courses/${courseId}/questions`, body)}
              onSaved={() => {
                setCreating(false)
                bank.reload()
              }}
            />
          )}
        </>
      )}
      {bank.data?.length === 0 && <p>El banco aún no tiene preguntas.</p>}
      {bank.data?.length > 0 && (
        <ol className="quiz-questions">
          {bank.data.map((question) => (
            <BankQuestion key={question.id} question={question} canManage={canManage} onChanged={bank.reload} />
          ))}
        </ol>
      )}
    </section>
  )
}

function BankQuestion({ question, canManage, onChanged }) {
  const [editing, setEditing] = useState(false)
  const archive = useSubmit(async () => {
    await apiDelete(`/questions/${question.id}`)
    onChanged()
    return true
  })
  return (
    <li>
      <strong>{question.prompt}</strong>{' '}
      <span className="page__note">
        {QUESTION_KIND_LABELS[question.kind]} · {question.points} {question.points === 1 ? 'punto' : 'puntos'}
      </span>
      {question.locked && <span className="badge">Ya respondida</span>}
      <AnswerKey question={question} />
      {question.explanation && <p className="page__note">Explicación: {question.explanation}</p>}
      {canManage && (
        <div className="toolbar">
          {!question.locked && (
            <button type="button" className="button button--secondary button--small" onClick={() => setEditing(!editing)}>
              {editing ? 'Cancelar' : 'Editar'}
            </button>
          )}
          <button type="button" className="button button--danger button--small" disabled={archive.pending} onClick={() => window.confirm('¿Archivar esta pregunta? Los quizzes ya no podrán elegirla.') && archive.submit()}>
            Archivar
          </button>
        </div>
      )}
      <FormStatus error={archive.error} />
      {editing && (
        <QuestionForm
          prefix={`pregunta-${question.id}`}
          question={question}
          onSave={(body) => apiPut(`/questions/${question.id}`, body)}
          onSaved={() => {
            setEditing(false)
            onChanged()
          }}
        />
      )}
    </li>
  )
}

let nextKey = 0
const blankOption = (correct = false) => ({ text: '', correct, key: nextKey++ })

function QuestionForm({ prefix, question, onSave, onSaved }) {
  const [kind, setKind] = useState(question?.kind ?? 'SINGLE')
  const [options, setOptions] = useState(() =>
    question?.options.length ? question.options.map((option) => ({ ...option, key: nextKey++ })) : [blankOption(true), blankOption()],
  )
  const [trueIsCorrect, setTrueIsCorrect] = useState(question?.kind === 'TRUE_FALSE' ? question.options[0].correct : true)

  const save = useSubmit(async (event) => {
    const data = new FormData(event.currentTarget)
    const body = {
      kind,
      prompt: data.get('prompt'),
      points: Number(data.get('points') || 1),
      explanation: data.get('explanation') || null,
      options: [],
      acceptedAnswers: [],
    }
    if (kind === 'TRUE_FALSE') {
      body.options = [
        { text: 'Verdadero', correct: trueIsCorrect },
        { text: 'Falso', correct: !trueIsCorrect },
      ]
    } else if (kind === 'SHORT') {
      body.acceptedAnswers = String(data.get('acceptedAnswers'))
        .split('\n')
        .map((answer) => answer.trim())
        .filter(Boolean)
    } else {
      body.options = options.map(({ text, correct }) => ({ text, correct }))
    }
    const result = await onSave(body)
    onSaved()
    return result
  })

  const setOption = (key, change) =>
    setOptions((current) =>
      current.map((option) => {
        if (option.key === key) return { ...option, ...change }
        // En selección única, marcar una desmarca las demás.
        return kind === 'SINGLE' && change.correct ? { ...option, correct: false } : option
      }),
    )

  return (
    <form onSubmit={save.submit} noValidate className="subform">
      <div className="form-row">
        <div className="field">
          <label htmlFor={`${prefix}-tipo`}>Tipo</label>
          <select id={`${prefix}-tipo`} value={kind} onChange={(event) => setKind(event.target.value)}>
            {Object.entries(QUESTION_KIND_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <Field id={`${prefix}-puntos`} label="Puntos" name="points" type="number" min="1" max="10" defaultValue={question?.points ?? 1} />
      </div>
      <div className="field">
        <label htmlFor={`${prefix}-enunciado`}>Pregunta</label>
        <textarea id={`${prefix}-enunciado`} name="prompt" rows={3} required maxLength={2000} defaultValue={question?.prompt} />
      </div>
      {kind === 'TRUE_FALSE' && (
        <fieldset className="choices">
          <legend>La afirmación es</legend>
          <label>
            <input type="radio" name={`${prefix}-vf`} checked={trueIsCorrect} onChange={() => setTrueIsCorrect(true)} /> Verdadera
          </label>
          <label>
            <input type="radio" name={`${prefix}-vf`} checked={!trueIsCorrect} onChange={() => setTrueIsCorrect(false)} /> Falsa
          </label>
        </fieldset>
      )}
      {kind === 'SHORT' && (
        <div className="field">
          <label htmlFor={`${prefix}-aceptadas`}>Respuestas aceptadas (una por línea, hasta 5)</label>
          <textarea
            id={`${prefix}-aceptadas`}
            name="acceptedAnswers"
            rows={3}
            defaultValue={question?.acceptedAnswers?.join('\n')}
            aria-describedby={`${prefix}-aceptadas-ayuda`}
          />
          <small id={`${prefix}-aceptadas-ayuda`} className="field__hint">
            No importan mayúsculas, tildes ni espacios de más.
          </small>
        </div>
      )}
      {(kind === 'SINGLE' || kind === 'MULTIPLE') && (
        <fieldset className="choices">
          <legend>Opciones (marca {kind === 'SINGLE' ? 'la correcta' : 'las correctas'})</legend>
          {options.map((option, index) => (
            <div key={option.key} className="criterion">
              <input
                type="checkbox"
                aria-label={`Opción ${index + 1} correcta`}
                checked={option.correct}
                onChange={(event) => setOption(option.key, { correct: event.target.checked })}
              />
              <input
                aria-label={`Opción ${index + 1}`}
                value={option.text}
                maxLength={500}
                onChange={(event) => setOption(option.key, { text: event.target.value })}
              />
              <button
                type="button"
                className="button button--danger button--small"
                disabled={options.length <= 2}
                aria-label={`Quitar opción ${index + 1}`}
                onClick={() => setOptions((current) => current.filter((item) => item.key !== option.key))}
              >
                Quitar
              </button>
            </div>
          ))}
          <button
            type="button"
            className="button button--secondary button--small"
            disabled={options.length >= MAX_OPTIONS}
            onClick={() => setOptions((current) => [...current, blankOption()])}
          >
            Agregar opción
          </button>
        </fieldset>
      )}
      <Field
        id={`${prefix}-explicacion`}
        label="Explicación para la revisión (opcional)"
        name="explanation"
        maxLength={2000}
        defaultValue={question?.explanation ?? ''}
      />
      <button type="submit" className="button" disabled={save.pending}>
        Guardar pregunta
      </button>
      <FormStatus error={save.error} />
    </form>
  )
}
