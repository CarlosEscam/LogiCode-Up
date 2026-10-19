import { apiPost } from '../../shared/api/httpClient.js'
import { navigate } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { PASSWORD_HINT, formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import { useSession } from '../auth/useSession.js'
import { PERMISSION_LABELS, roleLabel } from './labels.js'

export default function AccountPage() {
  const { account, clear } = useSession()

  const verification = useSubmit(() => apiPost('/me/email-verification'))
  const password = useSubmit(async (event) => {
    const { currentPassword, newPassword, confirmPassword } = formValues(event)
    if (newPassword !== confirmPassword) throw new Error('Las contraseñas no coinciden.')
    await apiPost('/me/password', { currentPassword, newPassword })
    // El backend cierra todas las sesiones de la cuenta, incluida esta.
    clear()
    navigate('/ingresar?motivo=contrasena-cambiada', { replace: true })
  })

  return (
    <>
      <section className="card" aria-labelledby="cuenta-titulo">
        <h1 id="cuenta-titulo">Mi cuenta</h1>
        <dl className="details">
          <div>
            <dt>Nombre</dt>
            <dd>{account.fullName}</dd>
          </div>
          <div>
            <dt>Cédula</dt>
            <dd>{account.cedula}</dd>
          </div>
          <div>
            <dt>Rol</dt>
            <dd>{roleLabel(account.role, account.fullPrivileges)}</dd>
          </div>
          <div>
            <dt>Correo</dt>
            <dd>
              {account.email} · {account.emailVerified ? 'verificado' : 'sin verificar'}
            </dd>
          </div>
        </dl>
        {!account.emailVerified && (
          <>
            <FormStatus error={verification.error} success={verification.result?.message} />
            <button type="button" className="button button--secondary" onClick={verification.submit} disabled={verification.pending}>
              Reenviar correo de verificación
            </button>
          </>
        )}
      </section>

      <section className="card" aria-labelledby="permisos-titulo">
        <h2 id="permisos-titulo">Lo que puedes hacer</h2>
        <ul className="tags">
          {account.permissions.map((permission) => (
            <li key={permission}>{PERMISSION_LABELS[permission] ?? permission}</li>
          ))}
        </ul>
      </section>

      <section className="card" aria-labelledby="contrasena-titulo">
        <h2 id="contrasena-titulo">Cambiar contraseña</h2>
        <p className="page__note">Al cambiarla se cierran todas tus sesiones abiertas.</p>
        <form onSubmit={password.submit} noValidate>
          <Field label="Contraseña actual" name="currentPassword" type="password" autoComplete="current-password" required />
          <Field
            label="Nueva contraseña"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            hint={PASSWORD_HINT}
            required
            error={password.fieldErrors.newPassword}
          />
          <Field label="Confirmar nueva contraseña" name="confirmPassword" type="password" autoComplete="new-password" required />
          <FormStatus error={password.error} />
          <button type="submit" className="button" disabled={password.pending}>
            {password.pending ? 'Guardando…' : 'Cambiar contraseña'}
          </button>
        </form>
      </section>
    </>
  )
}
