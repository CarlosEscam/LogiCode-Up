import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'

const TEACHER = {
  id: 'd1',
  cedula: '500001',
  fullName: 'Docente Curso',
  role: 'TEACHER',
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'GRADE'],
}
const COURSE = {
  id: 'c1',
  name: 'Lógica de programación',
  period: '2026-2',
  enrolledCount: 3,
  canManageContent: false,
  canManageEnrollments: false,
  canGrade: true,
  canViewCourseWork: true,
  enrolled: false,
}
const stats = (submitted, graded, inReview, overdue) => ({ submitted, graded, inReview, overdue })
const BOOK = {
  fullView: true,
  columns: [
    { activityId: 'a1', title: 'Tarea 1', closesAt: '2026-10-08T04:59:00Z', stats: stats(3, 2, 1, 0) },
    { activityId: 'a2', title: 'Taller 2', closesAt: '2026-11-20T04:59:00Z', stats: stats(1, 0, 1, 0) },
  ],
  rows: [{ cedula: '1' }, { cedula: '2' }, { cedula: '3' }],
}

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  window.history.replaceState(null, '', '/')
})

describe('Control de calificaciones', () => {
  it('muestra por actividad las calificadas y las pendientes, y manda todo a la IA', async () => {
    window.history.replaceState(null, '', '/cursos/c1?seccion=control')
    const routes = {
      'GET /api/v1/me': respond(200, TEACHER),
      'GET /api/v1/courses/c1': respond(200, COURSE),
      'GET /api/v1/courses/c1/gradebook': respond(200, BOOK),
      'POST /api/v1/courses/c1/ai-grading': respond(200, { queued: 1, unreadable: 1 }),
    }
    const fetchMock = vi.fn(async (url, options = {}) => routes[`${options.method ?? 'GET'} ${url.split('?')[0]}`] ?? respond(404, {}))
    vi.stubGlobal('fetch', fetchMock)
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    render(<App />)

    const table = await screen.findByRole('table', { name: 'Entregas calificadas y pendientes por actividad' })
    const tarea = within(table).getByRole('row', { name: /Tarea 1/ })
    expect(tarea).toHaveTextContent('2 de 3')
    const taller = within(table).getByRole('row', { name: /Taller 2/ })
    // 3 estudiantes, 1 entregó: quedan 2 sin entregar.
    expect(within(taller).getAllByRole('cell')[3]).toHaveTextContent('2')
    expect(screen.getByRole('link', { name: '2' })).toHaveAttribute('href', '/cursos/c1?seccion=revisar')

    await act(async () => fireEvent.click(screen.getByRole('button', { name: 'Calificar todo con IA' })))
    expect(await screen.findByText(/La IA está calificando 1 entrega\. 1 no las puede leer/)).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url, options]) => url.endsWith('/courses/c1/ai-grading') && options?.method === 'POST')).toBe(true)
  })
})
