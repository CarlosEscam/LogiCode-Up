import SystemStatus from '../features/system/SystemStatus.jsx'
import { useSession } from '../features/auth/useSession.js'
import { Link } from '../shared/router/Link.jsx'

// Página inicial provisional. La página de inicio definitiva con misión, visión y explicación (RF-23)
// está pendiente.
export default function HomePage() {
  const { account } = useSession()

  return (
    <>
      <header className="page__header">
        <h1>LogiCode</h1>
        <p className="page__subtitle">
          Plataforma de Pensamiento Computacional · Universidad de Pamplona
        </p>
      </header>

      {!account && (
        <section aria-labelledby="acceso-titulo" className="card">
          <h2 id="acceso-titulo">Acceso</h2>
          <ul className="links">
            <li>
              <Link to="/ingresar">Iniciar sesión</Link>
            </li>
            <li>
              <Link to="/registro/estudiante">Registrarme como estudiante</Link> (tu cédula debe estar habilitada)
            </li>
            <li>
              <Link to="/registro/docente">Solicitar cuenta de docente</Link>
            </li>
          </ul>
        </section>
      )}

      <section aria-labelledby="estado-titulo" className="card">
        <h2 id="estado-titulo">Estado de la plataforma</h2>
        <SystemStatus />
      </section>

      <p className="page__note">
        Plataforma en construcción. Las funciones se habilitarán por etapas.
      </p>
    </>
  )
}
