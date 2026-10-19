import { render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'

const PDF_MATERIAL = {
  id: 'm1',
  topicId: 't1',
  topicTitle: 'Algoritmos con PSeInt',
  tool: 'PSEINT',
  courseId: 'c1',
  courseName: 'Lógica de programación',
  title: 'Guía de variables',
  description: null,
  kind: 'GUIDE',
  visibility: 'PUBLIC',
  position: 1,
  externalUrl: null,
  file: { id: 'f1', originalName: 'guia.pdf', format: 'PDF', contentType: 'application/pdf', sizeBytes: 2048, previewable: true },
  canEdit: false,
  canModerate: false,
}

const LINK_MATERIAL = {
  ...PDF_MATERIAL,
  id: 'm2',
  title: 'Descarga de PSeInt',
  kind: 'LINK',
  visibility: 'PRIVATE',
  externalUrl: 'https://pseint.sourceforge.net/',
  file: null,
}

const STUDENT = {
  id: 's1',
  cedula: '600001',
  fullName: 'Estudiante Uno',
  role: 'STUDENT',
  fullPrivileges: false,
  email: 'est@example.com',
  emailVerified: true,
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'PARTICIPATE_FORUM', 'ANSWER_ACTIVITIES'],
}

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

function stubApi(routes) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url, options = {}) => {
      const handler = routes[`${options.method ?? 'GET'} ${url.split('?')[0]}`]
      if (!handler) return respond(404, { title: 'No encontrado' })
      return typeof handler === 'function' ? handler(url, options) : handler
    }),
  )
}

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  window.history.replaceState(null, '', '/')
})

describe('Biblioteca y cursos', () => {
  it('muestra la biblioteca pública a un visitante y envía los filtros a la API', async () => {
    window.history.replaceState(null, '', '/biblioteca')
    stubApi({
      'GET /api/v1/me': respond(401, {}),
      'GET /api/v1/library': respond(200, { items: [PDF_MATERIAL], page: 0, size: 20, totalItems: 1, totalPages: 1 }),
      'GET /api/v1/library/topics': respond(200, [{ id: 't1', title: 'Algoritmos con PSeInt', tool: 'PSEINT', courseName: 'Curso' }]),
    })

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Guía de variables' })).toBeInTheDocument()
    expect(screen.getByText(/Inicia sesión para ver también/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Descargar Guía de variables' })).toHaveAttribute(
      'href',
      '/api/v1/materials/m1/download',
    )
    expect(screen.getByRole('option', { name: 'Algoritmos con PSeInt' })).toBeInTheDocument()
  })

  it('abre la vista previa de un PDF dentro de la página', async () => {
    window.history.replaceState(null, '', '/materiales/m1')
    stubApi({ 'GET /api/v1/me': respond(401, {}), 'GET /api/v1/materials/m1': respond(200, PDF_MATERIAL) })

    render(<App />)

    expect(await screen.findByTitle('Vista previa de Guía de variables')).toHaveAttribute('src', '/api/v1/materials/m1/preview')
  })

  it('muestra los temas del curso sin controles de edición a un estudiante', async () => {
    window.history.replaceState(null, '', '/cursos/c1')
    stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'GET /api/v1/courses/c1': respond(200, {
        id: 'c1',
        name: 'Lógica de programación',
        period: '2026-2',
        ownerName: 'Docente',
        enrolledCount: 30,
        canManageContent: false,
        canManageEnrollments: false,
      }),
      'GET /api/v1/courses/c1/topics': respond(200, [
        {
          id: 't1',
          courseId: 'c1',
          title: 'Algoritmos con PSeInt',
          tool: 'PSEINT',
          position: 1,
          nextClass: true,
          materials: [PDF_MATERIAL, LINK_MATERIAL],
        },
      ]),
    })

    render(<App />)

    const nextClass = await screen.findByRole('region', { name: 'Próxima clase' })
    expect(within(nextClass).getByRole('link', { name: 'Algoritmos con PSeInt' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Abrir enlace Descarga de PSeInt/ })).toHaveAttribute('rel', 'noopener noreferrer')
    expect(screen.queryByRole('button', { name: /Archivar/ })).not.toBeInTheDocument()
    expect(screen.queryByText('Agregar material a este tema')).not.toBeInTheDocument()
    expect(screen.queryByRole('tab', { name: 'Estudiantes' })).not.toBeInTheDocument()
  })
})
