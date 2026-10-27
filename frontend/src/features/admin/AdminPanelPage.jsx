import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatDateTime } from '../activities/labels.js'
import { useRefreshWhile } from '../activities/useRefreshWhile.js'
import { useSession } from '../auth/useSession.js'
import { formatSize } from '../content/labels.js'

// Panel de administración (Etapa 8): cifras de la plataforma, accesos a las tareas administrativas y
// respaldos (RNF-09). El backend valida cada permiso.
export default function AdminPanelPage() {
  const session = useSession()
  const overview = useQuery('/admin/overview')
  const data = overview.data

  return (
    <>
      <section className="card card--wide" aria-labelledby="panel-titulo">
        <h1 id="panel-titulo">Administración</h1>
        <nav className="actions" aria-label="Tareas administrativas">
          {session.can('MANAGE_USERS') && <Link to="/admin/usuarios">Usuarios y docentes</Link>}
          {session.can('MANAGE_STUDENT_ENABLEMENTS') && <Link to="/admin/habilitaciones">Habilitar estudiantes</Link>}
          {session.can('VIEW_AUDIT') && <Link to="/admin/auditoria">Auditoría</Link>}
          <Link to="/biblioteca">Biblioteca</Link>
          <Link to="/foro">Foro</Link>
        </nav>
        <FormStatus error={overview.error} />
        {data && (
          <dl className="overview">
            <Figure label="Estudiantes activos" value={data.activeStudents} />
            <Figure label="Docentes activos" value={data.activeTeachers} />
            <Figure label="Docentes por aprobar" value={data.pendingTeachers} />
            <Figure label="Administradores" value={data.activeAdmins} />
            <Figure label="Cuentas desactivadas" value={data.disabledAccounts} />
            <Figure label="Cursos" value={data.courses} />
            <Figure label="Actividades publicadas" value={data.publishedActivities} />
            <Figure label="Entregas" value={data.submissions} />
            <Figure label="En revisión docente" value={data.gradesInReview} />
            <Figure label="Calificando con IA" value={data.gradesProcessing} />
            <Figure label="Materiales públicos" value={data.publicMaterials} />
            <Figure label="Materiales privados" value={data.privateMaterials} />
            <Figure label="Archivos cargados" value={formatSize(data.storedFileBytes)} />
            <Figure label="Hilos del foro" value={data.forumThreads} />
            <Figure label="Respuestas del foro" value={data.forumReplies} />
          </dl>
        )}
      </section>
      {session.can('MANAGE_BACKUPS') && <Backups />}
    </>
  )
}

function Figure({ label, value }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

const RESTORE_TEST = { OK: 'Restauración probada', ERROR: 'Falló la prueba de restauración' }

function Backups() {
  const backups = useQuery('/admin/backups')
  const request = useSubmit(async () => {
    const result = await apiPost('/admin/backups')
    backups.reload()
    return result
  })
  const data = backups.data
  useRefreshWhile(Boolean(data?.pendingRequest), backups.reload, 5000)

  return (
    <section className="card card--wide" aria-labelledby="respaldos-titulo">
      <h2 id="respaldos-titulo">Respaldos</h2>
      <FormStatus error={backups.error} />
      {data && (
        <>
          <p className="page__note">
            {data.service.running
              ? `Copia diaria de la base de datos y de los archivos a las ${data.service.dailyHour}:00 (hora de Colombia); se conservan ${data.service.retentionDays} días. Después de cada copia se prueba restaurarla.`
              : 'El servicio de respaldos no está corriendo. Revisa el contenedor "backup" de Docker Compose.'}
          </p>
          <button
            type="button"
            className="button"
            disabled={request.pending || data.pendingRequest || !data.service.running}
            onClick={() => request.submit()}
          >
            {data.pendingRequest ? 'Respaldo en curso…' : 'Crear respaldo ahora'}
          </button>
          <FormStatus error={request.error} />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Copia (hora de Colombia)</th>
                  <th scope="col">Base de datos</th>
                  <th scope="col">Archivos</th>
                  <th scope="col">Prueba de restauración</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((backup) => (
                  <tr key={backup.name}>
                    <td>
                      {formatDateTime(backup.createdAt)}
                      <small className="gradebook__weight">{backup.name}</small>
                    </td>
                    <td>{formatSize(backup.databaseBytes)}</td>
                    <td>{formatSize(backup.filesBytes)}</td>
                    <td>
                      <span className={backup.restoreTest === 'OK' ? 'badge badge--ok' : 'badge badge--warning'}>
                        {RESTORE_TEST[backup.restoreTest] ?? 'Sin probar'}
                      </span>
                      {backup.restoreTestDetail && <small className="gradebook__weight">{backup.restoreTestDetail}</small>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.items.length === 0 && <p>Aún no hay copias.</p>}
          <p className="page__note">Para restaurar una copia o guardarla fuera del equipo, sigue respaldos/README.md.</p>
        </>
      )}
    </section>
  )
}
