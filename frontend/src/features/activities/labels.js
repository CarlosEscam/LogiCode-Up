// Textos y formatos de actividades, entregas y notas. Los valores vienen de la API.

export const ACTIVITY_KIND_LABELS = {
  TASK: 'Tarea',
  WORKSHOP: 'Taller',
  EXAM: 'Evaluación',
  QUIZ: 'Quiz calificable',
  PRACTICE_QUIZ: 'Quiz de repaso',
}

export const isQuizKind = (kind) => kind === 'QUIZ' || kind === 'PRACTICE_QUIZ'

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

export const GRADE_STATUS_LABELS = { PROCESSING: 'Calificando con IA', PENDING_REVIEW: 'En revisión', PUBLISHED: 'Calificada' }

export const GRADE_STATUS_TONES = { PROCESSING: 'info', PENDING_REVIEW: 'warning', PUBLISHED: 'ok' }

// Por qué una entrega espera al docente (solo lo ve quien califica).
export const REVIEW_REASON_LABELS = {
  AI_DISABLED: 'Calificación manual',
  UNSUPPORTED: 'Formato que la IA no lee',
  ILLEGIBLE: 'Foto ilegible',
  LOW_CONFIDENCE: 'Confianza menor al 90 %',
  AI_FAILED: 'Falló la IA',
  CODE_ERROR: 'El programa no se pudo ejecutar',
  CALIBRATION: 'Calibración de la IA',
}

export const REVIEW_REASON_HELP = {
  AI_DISABLED: 'La calificación con IA estaba desactivada cuando llegó la entrega.',
  UNSUPPORTED:
    'La entrega trae archivos que la IA no lee (Word, PowerPoint, video, Scratch o un PDF con contraseña, dañado o de más de 5 páginas). La IA lee respuestas escritas, fotos PNG o JPG, PDF cortos y archivos de PSeInt o Arduino.',
  ILLEGIBLE: 'La IA indicó que no puede leer la entrega con seguridad. Revísala y pon la nota.',
  LOW_CONFIDENCE: 'La IA propuso una nota, pero su confianza quedó por debajo del 90 %. Revísala y confírmala o ajústala.',
  AI_FAILED: 'La IA no pudo procesar la entrega después de varios intentos. No se asignó nota; puedes reintentar o calificar tú.',
  CODE_ERROR:
    'El programa tiene un error de sintaxis o usa algo que el intérprete de LogiCode aún no ejecuta, así que no se corrieron los casos de prueba. Revísalo y pon la nota.',
  CALIBRATION:
    'El curso está en calibración: la IA propuso una nota con confianza de 90 % o más, pero no la publica sola hasta que compares suficientes propuestas con tu nota. Confírmala si estás de acuerdo o pon la tuya.',
}

export const AI_OUTCOME_LABELS = {
  PUBLISHED: 'Confianza de 90 % o más',
  ILLEGIBLE: 'Ilegible',
  LOW_CONFIDENCE: 'Confianza baja',
  FAILED: 'Fallo técnico',
}

export const AI_OUTCOME_TONES = { PUBLISHED: 'ok', ILLEGIBLE: 'warning', LOW_CONFIDENCE: 'warning', FAILED: 'danger' }

export const GRADE_SOURCE_LABELS = { TEACHER: 'Docente', AI: 'IA', AUTOMATIC: 'Automática' }

// Situación del estudiante combinada con su nota (RF-19).
export function studentSituation(progress, grade) {
  if (!progress) return null
  if (progress.state === 'SUBMITTED') {
    if (grade?.status === 'PUBLISHED') return { label: `Calificada: ${formatScore(grade.score)}`, tone: 'ok' }
    if (grade?.status === 'PROCESSING') return { label: 'Entregada, calificando con IA', tone: 'info' }
    return { label: 'Entregada, en revisión', tone: 'info' }
  }
  if (progress.state === 'OVERDUE') return { label: 'Vencida sin entrega: 0,0', tone: 'danger' }
  if (progress.state === 'NOT_OPEN') return { label: 'Aún no abre', tone: 'muted' }
  if (progress.state === 'CLOSED') return { label: 'Cerrado', tone: 'muted' }
  const left = timeLeft(progress.deadline)
  return { label: left ? `Pendiente · ${left}` : 'Pendiente', tone: 'warning' }
}

// Tiempo que queda para entregar, en palabras: "faltan 3 días", "cierra en 5 h".
export function timeLeft(deadline, now = Date.now()) {
  const hours = (new Date(deadline) - now) / 36e5
  if (!(hours >= 0)) return null
  if (hours < 1) return 'cierra en menos de una hora'
  if (hours < 24) return `cierra en ${Math.floor(hours)} h`
  const days = Math.floor(hours / 24)
  return days === 1 ? 'falta 1 día' : `faltan ${days} días`
}

export function formatScore(score) {
  return score == null ? '—' : Number(score).toFixed(1).replace('.', ',')
}

export function formatPoints(value) {
  return value == null ? '—' : String(Number(value)).replace('.', ',')
}

export function formatDuration(ms) {
  if (ms == null) return '—'
  const seconds = Math.round(ms / 1000)
  return seconds < 60 ? `${seconds} s` : `${Math.floor(seconds / 60)} min ${seconds % 60} s`
}

export function formatSeconds(seconds) {
  return seconds == null ? '—' : formatDuration(seconds * 1000)
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
