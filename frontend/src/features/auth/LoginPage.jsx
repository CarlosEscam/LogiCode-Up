import { Link } from '../../shared/router/Link.jsx'
import { navigate, useLocation } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import { BrainMascot } from '../games/skins.jsx'
import { landingFor } from './landing.js'
import { useSession } from './useSession.js'

export default function LoginPage() {
  const { login } = useSession()
  const { query } = useLocation()
  const passwordChanged = query.get('motivo') === 'contrasena-cambiada'
  const { pending, error, submit } = useSubmit(async (event) => {
    const { cedula, password } = formValues(event)
    const account = await login(cedula.trim(), password)
    navigate(landingFor(account, query.get('volver')), { replace: true })
  })

  return (
    <section className="card auth-card" aria-labelledby="ingresar-titulo">
      <div className="auth-card__side">
        <div>
          <p className="eyebrow">Universidad de Pamplona</p>
          <p className="auth-card__title">Plataforma de Pensamiento Computacional</p>
          <p className="auth-card__note">Sin cuenta puedes usar la biblioteca pública, los juegos y leer el foro.</p>
        </div>
        <div className="mascot" aria-hidden="true">
          <BrainMascot />
        </div>
      </div>
      <div className="auth-card__form">
        <h1 id="ingresar-titulo">Iniciar sesión</h1>
        {passwordChanged && <FormStatus success="Tu contraseña cambió y se cerraron todas tus sesiones. Ingresa de nuevo." />}
        <form onSubmit={submit} noValidate>
          <Field label="Cédula" name="cedula" inputMode="numeric" autoComplete="username" required />
          <Field label="Contraseña" name="password" type="password" autoComplete="current-password" required />
          <FormStatus error={error} />
          <button type="submit" className="button" disabled={pending}>
            {pending ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
        <ul className="links">
          <li>
            <Link to="/recuperar-contrasena">¿Olvidaste tu contraseña?</Link>
          </li>
          <li>
            <Link to="/recuperar-correo">¿Ya no tienes acceso a tu correo?</Link>
          </li>
          <li>
            <Link to="/registro/estudiante">Registrarme como estudiante</Link> ·{' '}
            <Link to="/registro/docente">Solicitar cuenta de docente</Link>
          </li>
        </ul>
      </div>
    </section>
  )
}
