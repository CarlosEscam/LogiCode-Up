// Línea del error en el mensaje del intérprete («Línea 4: …»), o null.
export function errorLineOf(error) {
  const match = /^Línea (\d+):/.exec(error ?? '')
  return match ? Number(match[1]) : null
}
