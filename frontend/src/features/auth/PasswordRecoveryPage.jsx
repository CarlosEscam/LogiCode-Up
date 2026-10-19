import { apiPost } from '../../shared/api/httpClient.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'

export default function PasswordRecoveryPage() {
  const { pending, error, result, submit } = useSubmit(async (event) => {
    const { email } = formValues(event)
    return apiPost('/auth/password-recovery', { email: email.trim() })
  })

  return (
    <section className="card card--narrow" aria-labelledby="recuperar-titulo">
      <h1 id="recuperar-titulo">Recuperar contraseña</h1>
      <p className="page__note">
        Escribe el correo verificado de tu cuenta. El enlace vence en 60 minutos, sirve una sola vez y puedes pedirlo
        máximo dos veces por día.
      </p>
      <form onSubmit={submit} noValidate>
        <Field label="Correo electrónico" name="email" type="email" autoComplete="email" required />
        <FormStatus error={error} success={result?.message} />
        <button type="submit" className="button" disabled={pending}>
          {pending ? 'Enviando…' : 'Enviar enlace'}
        </button>
      </form>
    </section>
  )
}
