// Textos de herramientas, tipos de material y visibilidad. Los valores vienen de la API.

export const TOOL_LABELS = {
  PSEINT: 'PSeInt',
  DFD: 'DFD',
  SCRATCH: 'Scratch',
  ARDUINO: 'Arduino',
  GENERAL: 'General',
}

export const KIND_LABELS = {
  GUIDE: 'Guía',
  DOCUMENT: 'Documento',
  PRESENTATION: 'Presentación',
  IMAGE: 'Imagen',
  VIDEO: 'Video',
  TOOL_FILE: 'Archivo de herramienta',
  LINK: 'Enlace',
}

// Formatos que acepta el backend para cada tipo (RNF-04); solo orientan el selector de archivos.
export const KIND_ACCEPT = {
  GUIDE: '.pdf,.doc,.docx',
  DOCUMENT: '.pdf,.doc,.docx',
  PRESENTATION: '.pdf,.ppt,.pptx',
  IMAGE: '.png,.jpg,.jpeg',
  VIDEO: '.mp4,.mov',
  TOOL_FILE: '.psc,.ino,.sb3',
}

export const VISIBILITY_LABELS = { PUBLIC: 'Público', PRIVATE: 'Privado' }

export function formatSize(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2).replace('.', ',')} GB`
}

export function previewUrl(material) {
  return `/api/v1/materials/${material.id}/preview`
}

export function downloadUrl(material) {
  return `/api/v1/materials/${material.id}/download`
}
