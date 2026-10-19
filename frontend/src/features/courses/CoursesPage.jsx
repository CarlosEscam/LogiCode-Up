import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { navigate } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import { useSession } from '../auth/useSession.js'

// Cursos en los que participa la cuenta (todos, para un Administrador). El docente puede crear cursos.
export default function CoursesPage() {
  const { can } = useSession()
  const courses = useQuery('/courses')

  const create = useSubmit(async (event) => {
    const { name, period, description } = formValues(event)
    const course = await apiPost('/courses', { name, period, description: description || null })
    navigate(`/cursos/${course.id}`)
    return course
  })

  return (
    <>
      <section className="card card--wide" aria-labelledby="cursos-titulo">
        <h1 id="cursos-titulo">Cursos</h1>
        <FormStatus error={courses.error} />
        {courses.status === 'loading' && <p role="status">Cargando…</p>}
        {courses.data?.length === 0 && <p>Aún no participas en ningún curso.</p>}
        <ul className="materials">
          {courses.data?.map((course) => (
            <li key={course.id} className="material">
              <div className="material__body">
                <h2 className="material__title">
                  <Link to={`/cursos/${course.id}`}>{course.name}</Link>
                </h2>
                <p className="material__meta">
                  Periodo {course.period} · Docente: {course.ownerName ?? '—'} · {course.enrolledCount} inscritos
                </p>
                {course.description && <p className="material__description">{course.description}</p>}
              </div>
            </li>
          ))}
        </ul>
      </section>

      {can('MANAGE_COURSE_CONTENT') && (
        <section className="card" aria-labelledby="nuevo-curso-titulo">
          <h2 id="nuevo-curso-titulo">Crear curso</h2>
          {courses.data?.length === 0 && (
            <p className="page__note">Al crear el primer curso, las cédulas ya habilitadas quedan inscritas en él.</p>
          )}
          <form onSubmit={create.submit} noValidate>
            <Field label="Nombre" name="name" required maxLength={150} error={create.fieldErrors.name} />
            <Field label="Periodo" name="period" required maxLength={20} placeholder="2026-2" error={create.fieldErrors.period} />
            <Field label="Descripción (opcional)" name="description" maxLength={2000} />
            <button type="submit" className="button" disabled={create.pending}>
              Crear curso
            </button>
          </form>
          <FormStatus error={create.error} />
        </section>
      )}
    </>
  )
}
