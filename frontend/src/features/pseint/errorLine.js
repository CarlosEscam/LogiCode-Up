// Línea del error en el mensaje del intérprete («Línea 4: …»), o null.
export function errorLineOf(error) {
  const match = /^Línea (\d+):/.exec(error ?? '')
  return match ? Number(match[1]) : null
}

export const ERROR_KIND_LABELS = {
  SYNTAX: 'Error de sintaxis',
  UNSUPPORTED: 'Aún no disponible en LogiCode',
  RUNTIME: 'Error al ejecutar',
  LIMIT: 'Límite superado',
}
