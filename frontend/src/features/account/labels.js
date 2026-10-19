export const ROLE_LABELS = { STUDENT: 'Estudiante', TEACHER: 'Docente', ADMIN: 'Administrador' }

export const STATUS_LABELS = { PENDING_APPROVAL: 'Pendiente de aprobación', ACTIVE: 'Activa', DISABLED: 'Desactivada' }

export const PERMISSION_LABELS = {
  VIEW_PRIVATE_COURSE_CONTENT: 'Ver contenido privado del curso',
  PARTICIPATE_FORUM: 'Participar en foros',
  MODERATE_FORUM: 'Moderar foros',
  ANSWER_ACTIVITIES: 'Responder actividades',
  MANAGE_COURSE_CONTENT: 'Crear temas, materiales y actividades',
  GRADE: 'Calificar y ajustar notas',
  VIEW_COURSE_GRADES: 'Ver notas del curso',
  MANAGE_STUDENT_ENABLEMENTS: 'Habilitar cédulas de estudiantes',
  APPROVE_TEACHERS: 'Aprobar docentes',
  MANAGE_USERS: 'Activar y desactivar cuentas',
  MANAGE_LIBRARY: 'Administrar la biblioteca',
  MANAGE_BACKUPS: 'Administrar copias de seguridad',
  VIEW_AUDIT: 'Consultar la auditoría',
  CHANGE_ROLES: 'Cambiar roles',
}

export function roleLabel(role, fullPrivileges) {
  const label = ROLE_LABELS[role] ?? role
  return fullPrivileges ? `${label} (permisos completos)` : label
}
