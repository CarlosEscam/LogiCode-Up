import { useState } from 'react'
import { Pager } from '../../shared/ui/Pager.jsx'
import { usePagedQuery } from '../../shared/api/usePagedQuery.js'
import { FormStatus } from '../../shared/ui/forms.jsx'

const ACTION_LABELS = {
  LOGIN: 'Inicio de sesión',
  LOGOUT: 'Cierre de sesión',
  STUDENT_REGISTERED: 'Registro de estudiante',
  TEACHER_REQUESTED: 'Solicitud de docente',
  TEACHER_APPROVED: 'Docente aprobado',
  ROLE_CHANGED: 'Cambio de rol',
  STATUS_CHANGED: 'Cambio de estado',
  EMAIL_VERIFICATION_SENT: 'Verificación de correo enviada',
  EMAIL_VERIFIED: 'Correo verificado',
  PASSWORD_RECOVERY_REQUESTED: 'Recuperación de contraseña solicitada',
  PASSWORD_RESET: 'Contraseña restablecida',
  PASSWORD_CHANGED: 'Contraseña cambiada',
  EMAIL_CHANGE_REQUESTED: 'Cambio de correo solicitado',
  STUDENTS_ENABLED: 'Estudiantes habilitados',
  INITIAL_ADMIN_ENSURED: 'Administrador inicial verificado',
}

const OUTCOME_LABELS = { SUCCESS: 'Éxito', FAILURE: 'Fallo', DENIED: 'Denegado', LIMIT_REACHED: 'Límite alcanzado' }

const timeFormatter = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'short',
  timeStyle: 'medium',
  timeZone: 'America/Bogota',
})

export default function AuditPage() {
  const [page, setPage] = useState(0)
  const events = usePagedQuery('/admin/audit-events', { page, size: 50 })

  return (
    <section className="card card--wide" aria-labelledby="auditoria-titulo">
      <h1 id="auditoria-titulo">Auditoría</h1>
      <FormStatus error={events.error} />
      <div className="table-wrap" tabIndex={0}>
        <table>
          <thead>
            <tr>
              <th scope="col">Fecha (Colombia)</th>
              <th scope="col">Acción</th>
              <th scope="col">Resultado</th>
              <th scope="col">Sujeto</th>
              <th scope="col">Detalle</th>
            </tr>
          </thead>
          <tbody>
            {events.data?.items.map((event) => (
              <tr key={event.id}>
                <td>{timeFormatter.format(new Date(event.occurredAt))}</td>
                <td>{ACTION_LABELS[event.action] ?? event.action}</td>
                <td>{OUTCOME_LABELS[event.outcome] ?? event.outcome}</td>
                <td>{event.subject ?? '—'}</td>
                <td>{event.details ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {events.status === 'loading' && <p role="status">Cargando…</p>}
      <Pager data={events.data} onPage={setPage} />
    </section>
  )
}
