import { apiPost } from '../../shared/api/httpClient.js'
import { Link } from '../../shared/router/Link.jsx'
import { useLocation } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { PASSWORD_HINT, formValues, useSubmit } from '../../shared/ui/useSubmit.js'

// Destino del enlace de recuperación: /restablecer-contrasena?token=…
export default function PasswordResetPage() {
  const { query } = useLocation()
  const token = query.get('token')
  const { pending, error, fieldErrors, result, submit } = useSubmit(async (event) => {
    const { newPassword, confirmPassword } = formValues(event)
    if (newPassword !== confirmPassword) throw new Error('Las contraseñas no coinciden.')
    return apiPost('/auth/password-reset', { token, newPassword })
  })

  if (!token) {
    return (
      <section className="card card--narrow">
        <h1>Restablecer contraseña</h1>
        <p className="notice notice--error" role="alert">
          El enlace no es válido. <Link to="/recuperar-contrasena">Solicita uno nuevo</Link>.
        </p>
      </section>
    )
  }

  return (
    <section className="card card--narrow" aria-labelledby="restablecer-titulo">
      <h1 id="restablecer-titulo">Restablecer contraseña</h1>
      {result ? (
        <>
          <FormStatus success={result.message} />
          <p>
            <Link to="/ingresar">Iniciar sesión</Link>
          </p>
        </>
      ) : (
        <form onSubmit={submit} noValidate>
          <Field
            label="Nueva contraseña"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            hint={PASSWORD_HINT}
            required
            error={fieldErrors.newPassword}
          />
          <Field label="Confirmar contraseña" name="confirmPassword" type="password" autoComplete="new-password" required />
          <FormStatus error={error} />
          <button type="submit" className="button" disabled={pending}>
            {pending ? 'Guardando…' : 'Guardar contraseña'}
          </button>
        </form>
      )}
    </section>
  )
}
