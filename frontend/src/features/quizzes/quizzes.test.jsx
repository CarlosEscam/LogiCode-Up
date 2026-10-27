import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'

const STUDENT = {
  id: 's1',
  cedula: '600001',
  fullName: 'Ana Estudiante',
  role: 'STUDENT',
  fullPrivileges: false,
  email: 'ana@example.com',
  emailVerified: true,
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'ANSWER_ACTIVITIES'],
}
const TEACHER = { ...STUDENT, id: 'd1', fullName: 'Docente Curso', role: 'TEACHER', permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'GRADE'] }

const QUIZ_ACTIVITY = {
  id: 'q1',
  courseId: 'c1',
  title: 'Quiz de ciclos',
  kind: 'QUIZ',
  statement: 'Responde las preguntas.',
  acceptsText: false,
  acceptsFiles: false,
  acceptsPhotos: false,
  fileGroups: [],
  weightPercent: 20,
  opensAt: '2026-10-01T13:00:00Z',
  closesAt: '2026-10-08T04:59:00Z',
  maxAttempts: 1,
  status: 'PUBLISHED',
  canManage: false,
  canViewSubmissions: false,
  progress: { state: 'OPEN', attemptsUsed: 0, attemptsAllowed: 1, deadline: '2026-10-08T04:59:00Z', canSubmit: true, hasException: false },
}

const IN_PROGRESS = {
  id: 't1',
  activityId: 'q1',
  practice: false,
  number: 1,
  status: 'IN_PROGRESS',
  startedAt: '2026-10-05T15:00:00Z',
  deadline: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
  questionCount: 2,
  maxPoints: 3,
  reviewAvailable: false,
  questions: [
    { id: 'p1', kind: 'SINGLE', prompt: '¿Qué ciclo evalúa al final?', points: 1, options: ['Mientras', 'Repetir', 'Para'], chosen: [], text: null },
    { id: 'p2', kind: 'SHORT', prompt: '¿Qué instrucción muestra datos?', points: 2, options: [], chosen: [], text: null },
  ],
}

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

function stubApi(routes) {
  const fetchMock = vi.fn(async (url, options = {}) => {
    const handler = routes[`${options.method ?? 'GET'} ${url.split('?')[0]}`]
    if (!handler) return respond(404, { title: 'No encontrado' })
    return typeof handler === 'function' ? handler(url, options) : handler
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

describe('Quizzes', () => {
  it('el estudiante empieza el quiz desde la actividad, sin formulario de entrega', async () => {
    window.history.replaceState(null, '', '/actividades/q1')
    stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/activities/q1': respond(200, QUIZ_ACTIVITY),
      'GET /api/v1/activities/q1/quiz': respond(200, { activityId: 'q1', timeLimitMinutes: 10, questionCount: 2, maxPoints: 3, questions: null, hasAttempts: false }),
      'GET /api/v1/courses/c1/grades/mine': respond(200, []),
      'GET /api/v1/activities/q1/quiz/attempts/mine': respond(200, []),
      'GET /api/v1/activities/q1/quiz/ranking': respond(200, [
        { position: 1, studentName: 'Luis Pérez', score: 5, correctCount: 2, questionCount: 2, durationSeconds: 95, mine: false, attemptId: null },
      ]),
      'POST /api/v1/activities/q1/quiz/attempts': respond(200, IN_PROGRESS),
      'GET /api/v1/quiz-attempts/t1': respond(200, IN_PROGRESS),
    })
    render(<App />)

    expect(await screen.findByText(/2 preguntas · 3 puntos · 10 minutos en total/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar entrega' })).not.toBeInTheDocument()
    expect(screen.queryByText('Se entrega como')).not.toBeInTheDocument()
    expect(await screen.findByRole('cell', { name: '1 min 35 s' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Empezar el quiz' }))
    expect(await screen.findByRole('heading', { name: 'Quiz · intento 1' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/quiz-intentos/t1')
  })

  it('guarda las respuestas solas y al enviar muestra el resultado', async () => {
    window.history.replaceState(null, '', '/quiz-intentos/t1')
    let current = IN_PROGRESS
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/quiz-attempts/t1': () => respond(200, current),
      'PUT /api/v1/quiz-attempts/t1/answers': respond(204, null),
      'POST /api/v1/quiz-attempts/t1/finish': () => {
        current = { ...IN_PROGRESS, status: 'FINISHED', correctCount: 2, points: 3, score: 5, durationSeconds: 70, finishedAt: '2026-10-05T15:01:10Z', submissionId: 'e9' }
        return respond(200, current)
      },
    })
    render(<App />)

    expect(await screen.findByText(/Tiempo restante: (9|10):/)).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Repetir'))
    fireEvent.change(screen.getByLabelText('Respuesta a la pregunta 2'), { target: { value: 'Escribir' } })
    expect(screen.getByText('Cambios sin guardar')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Respuestas guardadas')).toBeInTheDocument(), { timeout: 3000 })
    const saved = fetchMock.mock.calls.find(([url, options]) => url.endsWith('/answers') && options.method === 'PUT')
    expect(JSON.parse(saved[1].body)).toEqual({
      answers: [
        { questionId: 'p1', choices: [1], text: null },
        { questionId: 'p2', choices: [], text: 'Escribir' },
      ],
    })

    fireEvent.click(screen.getByRole('button', { name: 'Enviar intento' }))
    expect(await screen.findByText('5,0')).toBeInTheDocument()
    expect(screen.getByText(/2 de 2 respuestas correctas \(3 de 3 puntos\)/)).toBeInTheDocument()
    expect(screen.getByText('Las respuestas correctas se muestran cuando cierre el quiz.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver la entrega y su nota' })).toHaveAttribute('href', '/entregas/e9')
  })

  it('el docente arma el quiz con preguntas del banco en el orden en que las marca', async () => {
    window.history.replaceState(null, '', '/actividades/q1')
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/activities/q1': respond(200, { ...QUIZ_ACTIVITY, status: 'DRAFT', canManage: true, canViewSubmissions: true, progress: null }),
      'GET /api/v1/activities/q1/quiz': respond(204, null),
      'GET /api/v1/activities/q1/submissions': respond(200, []),
      'GET /api/v1/activities/q1/grades': respond(200, []),
      'GET /api/v1/courses/c1/questions': respond(200, [
        { id: 'p1', kind: 'SINGLE', prompt: 'Pregunta uno', points: 1, options: [], acceptedAnswers: [], locked: false },
        { id: 'p2', kind: 'TRUE_FALSE', prompt: 'Pregunta dos', points: 2, options: [], acceptedAnswers: [], locked: false },
      ]),
      'PUT /api/v1/activities/q1/quiz': respond(200, { activityId: 'q1', timeLimitMinutes: 15, questionCount: 2, maxPoints: 3, questions: [], hasAttempts: false }),
    })
    render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: 'Armar el quiz' }))
    const bank = await screen.findByRole('group', { name: /Preguntas del banco/ })
    fireEvent.click(within(bank).getByLabelText(/Pregunta dos/))
    fireEvent.click(within(bank).getByLabelText(/Pregunta uno/))
    expect(within(bank).getByText(/2 elegidas, 3 puntos/)).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Tiempo total (minutos)'), { target: { value: '15' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar quiz' }))

    await waitFor(() => expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'PUT')).toBe(true))
    const put = fetchMock.mock.calls.find(([, options]) => options?.method === 'PUT')
    expect(JSON.parse(put[1].body)).toEqual({ timeLimitMinutes: 15, questionIds: ['p2', 'p1'] })
  })
})

describe('PSeInt', () => {
  it('ejecuta el programa con las entradas y muestra la salida y el error', async () => {
    window.history.replaceState(null, '', '/pseint')
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'POST /api/v1/pseint/runs': respond(200, {
        output: 'Hola, Ana\n',
        completed: false,
        errorKind: 'RUNTIME',
        error: 'Línea 5: división por cero.',
        steps: 4,
        durationMs: 1,
      }),
    })
    render(<App />)

    fireEvent.change(await screen.findByLabelText('Entradas para probar (una por línea)'), { target: { value: 'Ana' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ejecutar' }))
    const result = await screen.findByRole('status', { name: 'Resultado de la ejecución' })
    expect(result).toHaveTextContent('Hola, Ana')
    expect(result).toHaveTextContent('Error al ejecutar: Línea 5: división por cero.')
    const call = fetchMock.mock.calls.find(([url]) => url === '/api/v1/pseint/runs')
    expect(JSON.parse(call[1].body)).toMatchObject({ input: 'Ana', code: expect.stringContaining('Algoritmo Saludo') })
  })

  it('el docente guarda casos de prueba en la rúbrica y los vuelve a ejecutar en una entrega', async () => {
    window.history.replaceState(null, '', '/actividades/a1')
    const task = { ...QUIZ_ACTIVITY, id: 'a1', kind: 'TASK', acceptsText: true, canManage: true, canViewSubmissions: true, progress: null }
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/activities/a1': respond(200, task),
      'GET /api/v1/activities/a1/rubric': respond(204, null),
      'GET /api/v1/activities/a1/submissions': respond(200, []),
      'GET /api/v1/activities/a1/grades': respond(200, []),
      'PUT /api/v1/activities/a1/rubric': respond(200, { id: 'r1', number: 1, criteria: [], totalPoints: 5, testCases: [], testCaseCount: 1 }),
    })
    const { unmount } = render(<App />)

    fireEvent.click(await screen.findByRole('button', { name: 'Definir rúbrica' }))
    fireEvent.change(screen.getByLabelText('Criterio 1'), { target: { value: 'Funciona' } })
    fireEvent.click(screen.getByRole('button', { name: 'Agregar caso de prueba' }))
    fireEvent.change(screen.getByLabelText('Caso 1: entradas (una por línea)'), { target: { value: '7\n12' } })
    fireEvent.change(screen.getByLabelText('Salida esperada'), { target: { value: '12' } })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar rúbrica' }))
    await waitFor(() => expect(fetchMock.mock.calls.some(([, options]) => options?.method === 'PUT')).toBe(true))
    const put = fetchMock.mock.calls.find(([, options]) => options?.method === 'PUT')
    expect(JSON.parse(put[1].body).testCases).toEqual([{ input: '7\n12', expectedOutput: '12' }])
    unmount()

    window.history.replaceState(null, '', '/entregas/e1')
    stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/submissions/e1': respond(200, {
        id: 'e1', activityId: 'a1', activityTitle: 'Mayor', studentName: 'Ana', attempt: 1, textAnswer: 'Algoritmo x', files: [], submittedAt: '2026-10-05T15:00:00Z', late: false,
      }),
      'GET /api/v1/submissions/e1/grade': respond(200, {
        grade: { id: 'g1', status: 'PENDING_REVIEW', reviewReason: 'CODE_ERROR' },
        rubric: { id: 'r1', number: 1, criteria: [{ name: 'Funciona', maxPoints: 5 }], totalPoints: 5, testCases: [{ input: '7\n12', expectedOutput: '12' }], testCaseCount: 1 },
        history: [],
        aiEvaluations: [],
        canGrade: true,
        canRetryWithAi: false,
        canRunTests: true,
      }),
      'POST /api/v1/submissions/e1/test-run': respond(200, {
        cases: [{ number: 1, input: '7\n12', expectedOutput: '12', output: '7\n', passed: false, error: null }],
        programError: null,
        passed: 0,
        total: 1,
        score: 0,
      }),
    })
    render(<App />)

    expect(await screen.findByText('El programa no se pudo ejecutar.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ejecutar casos de prueba' }))
    expect(await screen.findByText('0 de 1 casos aprobados · nota 0,0')).toBeInTheDocument()
    expect(screen.getByText('No aprobado')).toBeInTheDocument()
  })
})
