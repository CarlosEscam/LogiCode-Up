import { fromBogotaInput } from './labels.js'

// Cuerpo JSON para la API a partir del formulario.
export function activityBody(form) {
  const data = new FormData(form)
  const weight = data.get('weightPercent')
  return {
    title: data.get('title'),
    kind: data.get('kind'),
    statement: data.get('statement'),
    acceptsText: data.has('acceptsText'),
    acceptsFiles: data.has('acceptsFiles'),
    acceptsPhotos: data.has('acceptsPhotos'),
    fileGroups: data.has('acceptsFiles') ? data.getAll('fileGroups') : [],
    weightPercent: weight == null || weight === '' ? null : Number(weight),
    opensAt: fromBogotaInput(data.get('opensAt')),
    closesAt: fromBogotaInput(data.get('closesAt')),
    maxAttempts: Number(data.get('maxAttempts')),
  }
}
