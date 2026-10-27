import { apiPut } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { navigate, useLocation } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import ActivitiesPanel from '../activities/ActivitiesPanel.jsx'
import ReviewPanel from '../activities/ReviewPanel.jsx'
import GradebookPanel from '../gradebook/GradebookPanel.jsx'
import QuestionBankPanel from '../quizzes/QuestionBankPanel.jsx'
import StudentsPanel from './StudentsPanel.jsx'
import TopicsPanel from './TopicsPanel.jsx'

// Página de un curso: temas con sus materiales, actividades y, para el docente o un Administrador,
// la bandeja Por revisar, la planilla y los inscritos; el estudiante ve sus notas.
export default function CoursePage({ id }) {
  const course = useQuery(`/courses/${id}`)
  const { query } = useLocation()

  if (course.status === 'loading' && !course.data) return <p role="status">Cargando…</p>
  if (course.status === 'error') {
    return (
      <section className="card">
        <h1>Curso no disponible</h1>
        <FormStatus error={course.error} />
        <p>
          <Link to="/cursos">Volver a cursos</Link>
        </p>
      </section>
    )
  }

  const data = course.data
  const tabs = [
    ['temas', 'Temas'],
    ['actividades', 'Actividades'],
    data.canViewCourseWork && ['revisar', 'Por revisar'],
    data.canViewCourseWork ? ['planilla', 'Planilla'] : data.enrolled && ['planilla', 'Mis notas'],
    data.canViewCourseWork && ['preguntas', 'Banco de preguntas'],
    data.canManageEnrollments && ['estudiantes', 'Estudiantes'],
  ].filter(Boolean)
  // La sección va en la dirección (?seccion=actividades) para volver a ella desde otras páginas.
  const tab = tabs.some(([key]) => key === query.get('seccion')) ? query.get('seccion') : 'temas'
  const setTab = (key) => navigate(key === 'temas' ? `/cursos/${id}` : `/cursos/${id}?seccion=${key}`, { replace: true })
  return (
    <>
      <section className="card card--wide page-head" aria-labelledby="curso-titulo">
        <p className="page__note">
          <Link to="/cursos">Cursos</Link> · Periodo {data.period}
        </p>
        <h1 id="curso-titulo">{data.name}</h1>
        <p className="material__meta">
          Docente: {data.ownerName ?? '—'} · {data.enrolledCount} inscritos
        </p>
        {data.description && <p>{data.description}</p>}
        {data.canManageContent && <EditCourse course={data} onSaved={course.reload} />}
        <div className="tabs" role="tablist" aria-label="Secciones del curso">
          {tabs.map(([key, label]) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </div>
      </section>
      {tab === 'temas' && <TopicsPanel courseId={id} canManage={data.canManageContent} />}
      {tab === 'actividades' && <ActivitiesPanel course={data} />}
      {tab === 'revisar' && <ReviewPanel courseId={id} canGrade={data.canGrade} />}
      {tab === 'planilla' && <GradebookPanel courseId={id} />}
      {tab === 'preguntas' && <QuestionBankPanel courseId={id} canManage={data.canManageContent} />}
      {tab === 'estudiantes' && <StudentsPanel courseId={id} onChange={course.reload} />}
    </>
  )
}

function EditCourse({ course, onSaved }) {
  const save = useSubmit(async (event) => {
    const { name, period, description } = formValues(event)
    const result = await apiPut(`/courses/${course.id}`, { name, period, description: description || null })
    onSaved()
    return result
  })
  return (
    <details>
      <summary>Editar datos del curso</summary>
      <form onSubmit={save.submit} noValidate>
        <Field label="Nombre" name="name" defaultValue={course.name} required maxLength={150} />
        <Field label="Periodo" name="period" defaultValue={course.period} required maxLength={20} />
        <Field label="Descripción" name="description" defaultValue={course.description ?? ''} maxLength={2000} />
        <button type="submit" className="button" disabled={save.pending}>
          Guardar
        </button>
      </form>
      <FormStatus error={save.error} success={save.result && 'Curso actualizado.'} />
    </details>
  )
}
