// Textos y formatos de actividades, entregas y notas. Los valores vienen de la API.

export const ACTIVITY_KIND_LABELS = { TASK: 'Tarea', WORKSHOP: 'Taller', EXAM: 'Evaluación' }

export const FILE_GROUP_LABELS = {
  DOCUMENTS: 'Documentos (PDF, Word)',
  PRESENTATIONS: 'Presentaciones (PDF, PowerPoint)',
  IMAGES: 'Imágenes (PNG, JPG)',
  VIDEOS: 'Videos (MP4, MOV)',
  TOOL_FILES: 'Archivos de PSeInt, Arduino o Scratch',
}

// Extensiones que orientan el selector de archivos; el backend valida el contenido real.
const FILE_GROUP_ACCEPT = {
  DOCUMENTS: '.pdf,.doc,.docx',
  PRESENTATIONS: '.pdf,.ppt,.pptx',
  IMAGES: '.png,.jpg,.jpeg',
  VIDEOS: '.mp4,.mov',
  TOOL_FILES: '.psc,.ino,.sb3',
}

export function acceptFor(activity) {
  const groups = new Set(activity.acceptsFiles ? activity.fileGroups : [])
  if (activity.acceptsPhotos) groups.add('IMAGES')
  return [...new Set([...groups].flatMap((group) => FILE_GROUP_ACCEPT[group].split(',')))].join(',')
}

export const STATUS_LABELS = { DRAFT: 'Borrador', PUBLISHED: 'Publicada' }

export const GRADE_STATUS_LABELS = { PENDING_REVIEW: 'En revisión', PUBLISHED: 'Calificada' }

export const GRADE_SOURCE_LABELS = { TEACHER: 'Docente', AI: 'IA' }

// Situación del estudiante combinada con su nota (RF-19).
export function studentSituation(progress, grade) {
  if (!progress) return null
  if (progress.state === 'SUBMITTED') {
    if (grade?.status === 'PUBLISHED') return { label: `Calificada: ${formatScore(grade.score)}`, tone: 'ok' }
    return { label: 'Entregada, en revisión', tone: 'info' }
  }
  if (progress.state === 'OVERDUE') return { label: 'Vencida sin entrega: 0,0', tone: 'danger' }
  if (progress.state === 'NOT_OPEN') return { label: 'Aún no abre', tone: 'muted' }
  return { label: 'Pendiente', tone: 'warning' }
}

export function formatScore(score) {
  return score == null ? '—' : Number(score).toFixed(1).replace('.', ',')
}

export function formatPercent(value) {
  return value == null ? 'Sin porcentaje' : `${String(value).replace('.', ',')} %`
}

// Las fechas se muestran y se escriben en hora de Colombia (UTC−5, sin horario de verano),
// sin importar la zona del equipo.
const BOGOTA_OFFSET_MS = 5 * 60 * 60 * 1000
const DATE_TIME = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' })

export function formatDateTime(instant) {
  return instant ? DATE_TIME.format(new Date(instant)) : '—'
}

// Instante ISO → valor de <input type="datetime-local"> en hora de Colombia.
export function toBogotaInput(instant) {
  if (!instant) return ''
  return new Date(new Date(instant).getTime() - BOGOTA_OFFSET_MS).toISOString().slice(0, 16)
}

// Valor de <input type="datetime-local"> (hora de Colombia) → instante ISO.
export function fromBogotaInput(value) {
  return value ? new Date(`${value}:00-05:00`).toISOString() : null
}

export function submissionFileUrl(submissionId, file, mode) {
  return `/api/v1/submissions/${submissionId}/files/${file.id}/${mode}`
}
