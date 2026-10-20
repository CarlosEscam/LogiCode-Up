import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'
import { fromBogotaInput, toBogotaInput } from './labels.js'

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

const TEACHER = { ...STUDENT, id: 'd1', cedula: '500001', fullName: 'Docente Curso', role: 'TEACHER', permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'GRADE'] }

const COURSE = {
  id: 'c1',
  name: 'Lógica de programación',
  period: '2026-2',
  ownerName: 'Docente Curso',
  enrolledCount: 2,
  canManageContent: false,
  canManageEnrollments: false,
  canGrade: false,
  canViewCourseWork: false,
  enrolled: true,
}

const ACTIVITY = {
  id: 'a1',
  courseId: 'c1',
  title: 'Algoritmo del promedio',
  kind: 'TASK',
  statement: 'Escribe el pseudocódigo.',
  acceptsText: true,
  acceptsFiles: true,
  acceptsPhotos: false,
  fileGroups: ['DOCUMENTS'],
  weightPercent: 60,
  opensAt: '2026-10-01T13:00:00Z',
  closesAt: '2026-10-08T04:59:00Z',
  maxAttempts: 1,
  status: 'PUBLISHED',
  canManage: false,
  canGrade: false,
  canViewSubmissions: false,
  submittedCount: null,
  progress: {
    state: 'OPEN',
    attemptsUsed: 0,
    attemptsAllowed: 1,
    deadline: '2026-10-08T04:59:00Z',
    canSubmit: true,
    lastSubmissionId: null,
    hasException: false,
  },
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

describe('Actividades y entregas', () => {
  it('convierte las fechas a la hora de Colombia sin importar la zona del equipo', () => {
    expect(toBogotaInput('2026-10-08T04:59:00Z')).toBe('2026-10-07T23:59')
    expect(fromBogotaInput('2026-10-07T23:59')).toBe('2026-10-08T04:59:00.000Z')
  })

  it('muestra al estudiante su situación y su nota en cada actividad', async () => {
    window.history.replaceState(null, '', '/cursos/c1?seccion=actividades')
    stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/courses/c1': respond(200, COURSE),
      'GET /api/v1/courses/c1/activities': respond(200, [
        { ...ACTIVITY, progress: { ...ACTIVITY.progress, state: 'SUBMITTED', canSubmit: false, attemptsUsed: 1 } },
        { ...ACTIVITY, id: 'a2', title: 'Ciclos', progress: { ...ACTIVITY.progress, state: 'OVERDUE', canSubmit: false } },
        { ...ACTIVITY, id: 'a3', title: 'Foto del cuaderno', progress: { ...ACTIVITY.progress, state: 'SUBMITTED', canSubmit: false, attemptsUsed: 1 } },
      ]),
      'GET /api/v1/courses/c1/grades/mine': respond(200, [
        { activityId: 'a1', submissionId: 'e1', attempt: 1, status: 'PUBLISHED', score: 4, feedback: 'Bien', gradedAt: '2026-10-09T15:00:00Z' },
        { activityId: 'a3', submissionId: 'e3', attempt: 1, status: 'PROCESSING', score: null, feedback: null, gradedAt: null },
      ]),
    })

    render(<App />)

    expect(await screen.findByText('Calificada: 4,0')).toBeInTheDocument()
    expect(screen.getByText('Vencida sin entrega: 0,0')).toBeInTheDocument()
    expect(screen.getByText('Entregada, calificando con IA')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Actividades' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.queryByRole('tab', { name: 'Por revisar' })).not.toBeInTheDocument()
    expect(screen.queryByText('Nueva actividad')).not.toBeInTheDocument()
  })

  it('envía la entrega del estudiante como formulario con su respuesta', async () => {
    window.history.replaceState(null, '', '/actividades/a1')
    stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/activities/a1': respond(200, ACTIVITY),
      'GET /api/v1/activities/a1/rubric': respond(200, {
        id: 'r1',
        number: 1,
        statement: ACTIVITY.statement,
        expectedAnswer: null,
        criteria: [{ name: 'Cálculo correcto', description: null, maxPoints: 5 }],
        totalPoints: 5,
      }),
      'GET /api/v1/courses/c1/grades/mine': respond(200, []),
      'GET /api/v1/activities/a1/submissions/mine': respond(200, []),
    })
    const sent = []
    vi.stubGlobal(
      'XMLHttpRequest',
      class {
        upload = {}
        open(method, url) {
          this.url = url
        }
        setRequestHeader() {}
        send(body) {
          sent.push({ url: this.url, text: body.get('text') })
          this.status = 201
          this.responseText = JSON.stringify({ id: 'e1', attempt: 1, late: false })
          this.onload()
        }
      },
    )

    render(<App />)

    expect(await screen.findByRole('cell', { name: /Cálculo correcto/ })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Enviar entrega' }))
    expect(await screen.findByText('La entrega está vacía: escribe tu respuesta o adjunta un archivo.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Respuesta'), { target: { value: 'prom <- (a+b+c)/3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar entrega' }))
    expect(await screen.findByText('Entrega recibida (intento 1).')).toBeInTheDocument()
    expect(sent).toEqual([{ url: '/api/v1/activities/a1/submissions', text: 'prom <- (a+b+c)/3' }])
  })

  it('permite al docente publicar la nota con coma decimal y muestra el historial', async () => {
    window.history.replaceState(null, '', '/entregas/e1')
    const pending = {
      grade: { id: 'g1', submissionId: 'e1', status: 'PENDING_REVIEW', score: null, feedback: null, rubricVersion: 1 },
      rubric: { id: 'r1', number: 1, statement: 'x', expectedAnswer: 'Promedio de tres notas', criteria: [{ name: 'Cálculo', maxPoints: 5 }], totalPoints: 5 },
      history: [],
      aiEvaluations: [],
      canGrade: true,
      canRetryWithAi: false,
    }
    let current = pending
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/submissions/e1': respond(200, {
        id: 'e1',
        activityId: 'a1',
        activityTitle: 'Algoritmo del promedio',
        studentName: 'Ana Estudiante',
        attempt: 1,
        textAnswer: 'prom <- (a+b+c)/3',
        files: [],
        submittedAt: '2026-10-05T15:00:00Z',
        late: false,
      }),
      'GET /api/v1/submissions/e1/grade': () => respond(200, current),
      'POST /api/v1/submissions/e1/grade': () => {
        current = {
          ...pending,
          grade: { ...pending.grade, status: 'PUBLISHED', score: 4.5, gradedByName: 'Docente Curso' },
          history: [{ previousScore: null, newScore: 4.5, source: 'TEACHER', changedByName: 'Docente Curso', changedAt: '2026-10-09T15:00:00Z' }],
        }
        return respond(200, current)
      },
    })

    render(<App />)

    expect(await screen.findByText('Esta entrega espera tu calificación.')).toBeInTheDocument()
    expect(screen.getByText('Promedio de tres notas')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Nota (0,0 a 5,0)'), { target: { value: '6' } })
    fireEvent.click(screen.getByRole('button', { name: 'Publicar nota' }))
    expect(await screen.findByText('Escribe una nota entre 0,0 y 5,0 con un decimal.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Nota (0,0 a 5,0)'), { target: { value: '4,5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Publicar nota' }))
    expect(await screen.findByText('Nota publicada.')).toBeInTheDocument()
    const post = fetchMock.mock.calls.find(([, options]) => options?.method === 'POST')
    expect(JSON.parse(post[1].body)).toEqual({ score: 4.5, feedback: null })
    const nota = screen.getByRole('region', { name: 'Nota' })
    expect(await within(nota).findByText('Calificada')).toBeInTheDocument()
    expect(within(nota).getByText('4,5')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Historial de la nota' })).toBeInTheDocument()
  })

  it('muestra al docente la propuesta de la IA para confirmarla o reintentar', async () => {
    window.history.replaceState(null, '', '/entregas/e2')
    const review = {
      grade: { id: 'g2', submissionId: 'e2', status: 'PENDING_REVIEW', reviewReason: 'LOW_CONFIDENCE', score: null, feedback: null, rubricVersion: 1 },
      rubric: { id: 'r1', number: 1, statement: 'x', expectedAnswer: null, criteria: [{ name: 'Cálculo', maxPoints: 4 }], totalPoints: 4 },
      history: [],
      aiEvaluations: [
        {
          id: 'ia1',
          model: 'qwen2.5vl:7b',
          outcome: 'LOW_CONFIDENCE',
          legible: true,
          confidence: 72,
          proposedScore: 3.8,
          feedback: 'Revisa la división.',
          transcription: 'prom <- a + b / 2',
          error: null,
          criteria: [{ name: 'Cálculo', points: 3, maxPoints: 4, comment: 'Falta paréntesis' }],
          durationMs: 95000,
          createdAt: '2026-10-09T15:00:00Z',
        },
        { id: 'ia0', model: 'qwen2.5vl:7b', outcome: 'FAILED', error: 'No fue posible conectar con el servicio de IA (Ollama) o no respondió a tiempo.', criteria: [], durationMs: 10, createdAt: '2026-10-09T14:00:00Z' },
      ],
      canGrade: true,
      canRetryWithAi: true,
    }
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/submissions/e2': respond(200, {
        id: 'e2',
        activityId: 'a1',
        activityTitle: 'Algoritmo del promedio',
        studentName: 'Beto Estudiante',
        attempt: 1,
        textAnswer: 'prom <- a + b / 2',
        files: [],
        submittedAt: '2026-10-05T15:00:00Z',
        late: false,
      }),
      'GET /api/v1/submissions/e2/grade': respond(200, review),
      'POST /api/v1/submissions/e2/grade': respond(200, review),
      'POST /api/v1/submissions/e2/grade/ai-retry': respond(200, review),
    })

    render(<App />)

    expect(await screen.findByText('Confianza menor al 80 %.')).toBeInTheDocument()
    const ia = screen.getByRole('region', { name: 'Resultado de la IA' })
    expect(within(ia).getByText('72 %')).toBeInTheDocument()
    expect(within(ia).getByText('3 de 4')).toBeInTheDocument()
    expect(within(ia).getByText('Evaluaciones anteriores (1)')).toBeInTheDocument()
    expect(screen.getByLabelText('Nota (0,0 a 5,0)')).toHaveValue('3,8')

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar nota' }))
    expect(await screen.findByText('Nota publicada.')).toBeInTheDocument()
    const post = fetchMock.mock.calls.find(([url, options]) => options?.method === 'POST' && url.endsWith('/grade'))
    expect(JSON.parse(post[1].body)).toEqual({ score: 3.8, feedback: 'Revisa la división.' })

    fireEvent.click(screen.getByRole('button', { name: 'Reintentar con IA' }))
    await vi.waitFor(() => expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/ai-retry'))).toBe(true))
  })
})
