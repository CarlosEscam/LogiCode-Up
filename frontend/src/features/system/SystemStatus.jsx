import { useSystemInfo } from './useSystemInfo.js'

const timeFormatter = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'long',
  timeStyle: 'short',
  timeZone: 'America/Bogota',
})

// Muestra si la interfaz logra comunicarse con la API y la base de datos.
export default function SystemStatus() {
  const { status, data } = useSystemInfo()

  if (status === 'loading') {
    return (
      <p className="status" role="status">
        Verificando la conexión con el servidor…
      </p>
    )
  }

  if (status === 'error') {
    return (
      <p className="status status--error" role="alert">
        No fue posible conectar con el servidor. Revisa que el backend esté en ejecución.
      </p>
    )
  }

  return (
    <dl className="status status--ok" aria-label="Estado del sistema">
      <div>
        <dt>Servidor</dt>
        <dd>En línea · versión {data.version} · entorno {data.environment}</dd>
      </div>
      <div>
        <dt>Base de datos</dt>
        <dd>{data.schemaVersion ? `Esquema en versión ${data.schemaVersion}` : 'Sin migraciones aplicadas'}</dd>
      </div>
      <div>
        <dt>Hora del servidor</dt>
        <dd>{timeFormatter.format(new Date(data.serverTime))} ({data.timeZone})</dd>
      </div>
    </dl>
  )
}
