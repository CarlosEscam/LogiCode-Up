import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { navigate } from '../../shared/router/router.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { ActivityFields } from './ActivityForm.jsx'
import { activityBody } from './activityBody.js'
import { ACTIVITY_KIND_LABELS, STATUS_LABELS, formatDateTime, formatPercent, studentSituation } from './labels.js'

// Actividades del curso por fecha de cierre (RF-19). El estudiante ve su situación y su nota;
// el docente ve borradores y cuántos estudiantes han entregado.
export default function ActivitiesPanel({ course }) {
  const activities = useQuery(`/courses/${course.id}/activities`)
  const grades = useQuery(course.enrolled ? `/courses/${course.id}/grades/mine` : null)
  const gradeByActivity = Object.fromEntries((grades.data ?? []).map((grade) => [grade.activityId, grade]))
  // El estudiante ve primero lo que aún puede entregar (ya vienen por fecha de cierre).
  const all = activities.data ?? []
  const todo = all.filter((activity) => activity.progress?.state === 'OPEN')
  const rest = all.filter((activity) => !todo.includes(activity))

  return (
    <>
      <section className="card card--wide" aria-labelledby="actividades-titulo">
        <h2 id="actividades-titulo">Actividades</h2>
        <FormStatus error={activities.error} />
        {activities.status === 'loading' && !activities.data && <p role="status">Cargando…</p>}
        {activities.data?.length === 0 && <p>Este curso aún no tiene actividades.</p>}
        {todo.length > 0 && (
          <>
            <h3 className="activities__group">Por hacer</h3>
            <ActivityList activities={todo} grades={gradeByActivity} enrolledCount={course.enrolledCount} />
            {rest.length > 0 && <h3 className="activities__group">Las demás</h3>}
          </>
        )}
        {rest.length > 0 && <ActivityList activities={rest} grades={gradeByActivity} enrolledCount={course.enrolledCount} />}
      </section>
      {course.canManageContent && <NewActivity courseId={course.id} />}
    </>
  )
}

function ActivityList({ activities, grades, enrolledCount }) {
  return (
    <ul className="materials">
      {activities.map((activity) => (
        <ActivityRow key={activity.id} activity={activity} grade={grades[activity.id]} enrolledCount={enrolledCount} />
      ))}
    </ul>
  )
}

function ActivityRow({ activity, grade, enrolledCount }) {
  const situation = studentSituation(activity.progress, grade)
  return (
    <li className="material">
      <div className="material__body">
        <h3 className="material__title">
          <Link to={`/actividades/${activity.id}`}>{activity.title}</Link>
        </h3>
        <p className="material__meta">
          {ACTIVITY_KIND_LABELS[activity.kind]} · Cierra {formatDateTime(activity.progress?.deadline ?? activity.closesAt)} ·{' '}
          {formatPercent(activity.weightPercent)}
          {activity.status === 'DRAFT' && <span className="badge badge--private">{STATUS_LABELS.DRAFT}</span>}
          {situation && <span className={`badge badge--${situation.tone}`}>{situation.label}</span>}
        </p>
        {activity.submittedCount != null && (
          <p className="material__description">
            Entregaron {activity.submittedCount} de {enrolledCount} inscritos.
          </p>
        )}
      </div>
    </li>
  )
}

function NewActivity({ courseId }) {
  const create = useSubmit(async (event) => {
    const created = await apiPost(`/courses/${courseId}/activities`, activityBody(event.currentTarget))
    navigate(`/actividades/${created.id}`)
    return created
  })
  return (
    <section className="card card--wide" aria-labelledby="nueva-actividad-titulo">
      <details className="subform subform--first">
        <summary id="nueva-actividad-titulo">Nueva actividad</summary>
        <p className="page__note">Se crea como borrador. Para publicarla, define antes su rúbrica.</p>
        <form onSubmit={create.submit} noValidate>
          <ActivityFields prefix="nueva-actividad" />
          <button type="submit" className="button" disabled={create.pending}>
            Crear borrador
          </button>
        </form>
        <FormStatus error={create.error} />
      </details>
    </section>
  )
}
