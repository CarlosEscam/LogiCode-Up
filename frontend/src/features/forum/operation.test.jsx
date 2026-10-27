import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'

// Etapa 8: foro, planilla y panel de administración. El backend decide permisos y cifras; aquí se
// comprueba que la interfaz muestra lo que recibe y solo ofrece lo permitido.

const STUDENT = {
  id: 's1',
  cedula: '600001',
  fullName: 'Ana Estudiante',
  role: 'STUDENT',
  fullPrivileges: false,
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'PARTICIPATE_FORUM', 'ANSWER_ACTIVITIES'],
}
const TEACHER = {
  ...STUDENT,
  id: 't1',
  fullName: 'Docente Curso',
  role: 'TEACHER',
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'PARTICIPATE_FORUM', 'MODERATE_FORUM', 'VIEW_COURSE_GRADES'],
}
const ADMIN = {
  ...STUDENT,
  id: 'a1',
  fullName: 'Diana Admin',
  role: 'ADMIN',
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'MANAGE_USERS', 'MANAGE_BACKUPS', 'VIEW_AUDIT'],
}

const THREAD = {
  id: 'h1',
  category: 'PSEINT',
  title: '¿Cómo uso Mientras?',
  body: 'No sé cuándo para.',
  authorName: 'Ana Estudiante',
  pinned: true,
  closed: false,
  createdAt: '2026-10-27T15:00:00Z',
  posts: [{ id: 'p1', authorName: 'Beto', body: 'Cuando la condición es falsa.', createdAt: '2026-10-27T16:00:00Z' }],
  canReply: false,
  canModerate: false,
}

const COLUMN = { activityId: 'a1', title: 'Taller 1', weightPercent: 50, effectiveWeight: 50, closesAt: '2026-10-28T04:59:00Z' }
const COLUMN2 = { activityId: 'a2', title: 'Taller 2', weightPercent: null, effectiveWeight: 50, closesAt: '2026-10-29T04:59:00Z' }
const BOOK = {
  courseId: 'c1',
  courseName: 'Lógica',
  period: '2026-2',
  columns: [
    { ...COLUMN, stats: { submitted: 2, graded: 1, inReview: 1, overdue: 0, average: 4, min: 4, max: 4, passing: 1 } },
    { ...COLUMN2, stats: { submitted: 0, graded: 0, inReview: 0, overdue: 2, average: 0, min: 0, max: 0, passing: 0 } },
  ],
  explicitWeightSum: 50,
  weightsComplete: true,
  weightsWarning: null,
  passingScore: 3,
  rows: [
    {
      cedula: '600001',
      fullName: 'Ana Estudiante',
      registered: true,
      cells: [
        { activityId: 'a1', status: 'GRADED', score: 4, submissionId: 'e1' },
        { activityId: 'a2', status: 'OVERDUE', score: 0, submissionId: null },
      ],
      partial: 2,
      definitive: 2,
    },
    {
      cedula: '600004',
      fullName: 'Beto Estudiante',
      registered: true,
      cells: [
        { activityId: 'a1', status: 'IN_REVIEW', score: null, submissionId: 'e2' },
        { activityId: 'a2', status: 'OVERDUE', score: 0, submissionId: null },
      ],
      partial: 0,
      definitive: null,
    },
  ],
  stats: { students: 2, withPartial: 2, averagePartial: 1, passingPartial: 0, withDefinitive: 1, averageDefinitive: 2, passingDefinitive: 0 },
  fullView: true,
}
const COURSE = { id: 'c1', name: 'Lógica', period: '2026-2', enrolledCount: 2, canManageContent: false, canManageEnrollments: false }

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

describe('Foro', () => {
  it('un visitante lee los hilos pero no ve el formulario para abrir uno', async () => {
    window.history.replaceState(null, '', '/foro')
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(401, {}),
      'GET /api/v1/forum/threads': respond(200, {
        items: [{ ...THREAD, replyCount: 1, lastActivityAt: THREAD.createdAt }],
        page: 0,
        size: 20,
        totalItems: 1,
        totalPages: 1,
      }),
    })

    render(<App />)

    expect(await screen.findByRole('link', { name: '¿Cómo uso Mientras?' })).toHaveAttribute('href', '/foro/h1')
    expect(screen.getByText('Fijado')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Abrir un hilo' })).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Herramienta'), { target: { value: 'DFD' } })
    await screen.findByRole('link', { name: '¿Cómo uso Mientras?' })
    expect(fetchMock.mock.calls.some(([url]) => url.includes('category=DFD'))).toBe(true)
  })

  it('el docente ve los controles de moderación y fija o suelta el hilo', async () => {
    window.history.replaceState(null, '', '/foro/h1')
    let pinned = true
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/forum/threads/h1': () => respond(200, { ...THREAD, pinned, canReply: true, canModerate: true }),
      'PATCH /api/v1/forum/threads/h1': (url, options) => {
        pinned = JSON.parse(options.body).pinned
        return respond(200, { ...THREAD, pinned })
      },
    })

    render(<App />)

    expect(await screen.findByRole('heading', { name: '¿Cómo uso Mientras?' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Eliminar respuesta' })).toBeInTheDocument()
    await act(async () => fireEvent.click(screen.getByRole('button', { name: 'Soltar' })))
    expect(await screen.findByRole('button', { name: 'Fijar arriba' })).toBeInTheDocument()
    const patch = fetchMock.mock.calls.find(([, options]) => options?.method === 'PATCH')
    expect(JSON.parse(patch[1].body)).toEqual({ pinned: false })
    expect(screen.getByLabelText('Tu respuesta')).toBeInTheDocument()
  })

  it('un estudiante no ve moderación y no responde un hilo cerrado', async () => {
    window.history.replaceState(null, '', '/foro/h1')
    stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/forum/threads/h1': respond(200, { ...THREAD, closed: true }),
    })

    render(<App />)

    expect(await screen.findByText('Este hilo está cerrado y no recibe más respuestas.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Eliminar/ })).not.toBeInTheDocument()
  })
})

describe('Planilla', () => {
  it('el docente ve pesos, vencidas, parcial y definitiva por separado, y el enlace de exportación', async () => {
    window.history.replaceState(null, '', '/cursos/c1?seccion=planilla')
    stubApi({
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/courses/c1': respond(200, { ...COURSE, canViewCourseWork: true }),
      'GET /api/v1/courses/c1/gradebook': respond(200, BOOK),
    })

    render(<App />)

    const table = within(await screen.findByRole('table', { name: 'Notas por estudiante y actividad' }))
    expect(table.getByText('50 % (reparto)')).toBeInTheDocument()
    const ana = within(table.getByRole('row', { name: /Ana Estudiante/ }))
    expect(ana.getByRole('link', { name: '4,0' })).toHaveAttribute('href', '/entregas/e1')
    expect(ana.getByTitle('Vencida sin entrega')).toHaveTextContent('0,0*')
    expect(within(table.getByRole('row', { name: /Beto Estudiante/ })).getByText('En revisión')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Exportar a Excel (.xlsx)' })).toHaveAttribute(
      'href',
      '/api/v1/courses/c1/gradebook/export/xlsx',
    )
    expect(screen.getByRole('link', { name: 'Exportar a CSV' })).toHaveAttribute('href', '/api/v1/courses/c1/gradebook/export')
  })

  it('el estudiante ve solo sus notas, con la definitiva aparte del avance parcial', async () => {
    window.history.replaceState(null, '', '/cursos/c1?seccion=planilla')
    const mine = { ...BOOK, fullView: false, stats: null, rows: [{ ...BOOK.rows[1], definitive: null }] }
    stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/courses/c1': respond(200, { ...COURSE, enrolled: true }),
      'GET /api/v1/courses/c1/gradebook': respond(200, mine),
    })

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Mis notas' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Mis notas' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('Aparece cuando todas las actividades tengan resultado.')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Exportar/ })).not.toBeInTheDocument()
  })
})

describe('Panel de administración', () => {
  it('muestra cifras y respaldos, y pide un respaldo inmediato', async () => {
    window.history.replaceState(null, '', '/admin')
    let pending = false
    const backups = () =>
      respond(200, {
        service: { running: true, dailyHour: 2, retentionDays: 30 },
        pendingRequest: pending,
        items: [
          {
            name: 'logicode-20261027-020000',
            createdAt: '2026-10-27T07:00:00Z',
            databaseBytes: 2048,
            filesBytes: 1048576,
            restoreTest: 'OK',
            restoreTestDetail: 'Base restaurada en una copia temporal.',
          },
        ],
      })
    stubApi({
      'GET /api/v1/me': respond(200, ADMIN),
      'GET /api/v1/admin/overview': respond(200, {
        activeStudents: 6,
        activeTeachers: 1,
        activeAdmins: 2,
        pendingTeachers: 1,
        disabledAccounts: 0,
        courses: 1,
        publishedActivities: 8,
        submissions: 20,
        gradesInReview: 3,
        gradesProcessing: 0,
        publicMaterials: 4,
        privateMaterials: 2,
        storedFileBytes: 4096,
        forumThreads: 5,
        forumReplies: 9,
      }),
      'GET /api/v1/admin/backups': backups,
      'POST /api/v1/admin/backups': () => {
        pending = true
        return respond(202, {})
      },
    })

    render(<App />)

    expect(await screen.findByText('Docentes por aprobar')).toBeInTheDocument()
    expect(await screen.findByText('Restauración probada')).toBeInTheDocument()
    expect(screen.getByText(/a las 2:00 \(hora de Colombia\); se conservan 30 días/)).toBeInTheDocument()
    await act(async () => fireEvent.click(screen.getByRole('button', { name: 'Crear respaldo ahora' })))
    expect(await screen.findByRole('button', { name: 'Respaldo en curso…' })).toBeDisabled()
  })
})
