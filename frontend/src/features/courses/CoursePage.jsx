import { useState } from 'react'
import { apiPut } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import StudentsPanel from './StudentsPanel.jsx'
import TopicsPanel from './TopicsPanel.jsx'

// Página de un curso: temas con sus materiales y, para el docente o un Administrador, los inscritos.
export default function CoursePage({ id }) {
  const course = useQuery(`/courses/${id}`)
  const [tab, setTab] = useState('temas')

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
  return (
    <>
      <section className="card card--wide" aria-labelledby="curso-titulo">
        <p className="page__note">
          <Link to="/cursos">Cursos</Link> · Periodo {data.period}
        </p>
        <h1 id="curso-titulo">{data.name}</h1>
        <p className="material__meta">
          Docente: {data.ownerName ?? '—'} · {data.enrolledCount} inscritos
        </p>
        {data.description && <p>{data.description}</p>}
        {data.canManageContent && <EditCourse course={data} onSaved={course.reload} />}
        {data.canManageEnrollments && (
          <div className="tabs" role="tablist" aria-label="Secciones del curso">
            <button type="button" role="tab" aria-selected={tab === 'temas'} onClick={() => setTab('temas')}>
              Temas
            </button>
            <button type="button" role="tab" aria-selected={tab === 'estudiantes'} onClick={() => setTab('estudiantes')}>
              Estudiantes
            </button>
          </div>
        )}
      </section>
      {tab === 'temas' ? (
        <TopicsPanel courseId={id} canManage={data.canManageContent} />
      ) : (
        <StudentsPanel courseId={id} onChange={course.reload} />
      )}
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
