import { apiPost } from '../../shared/api/httpClient.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'

// Recuperación del acceso al correo: se comparan el último correo registrado y la contraseña
// actual o inmediatamente anterior. El correo nuevo debe verificarse antes de reemplazar al actual.
export default function EmailChangePage() {
  const { pending, error, result, submit } = useSubmit(async (event) => {
    const form = event.currentTarget
    const values = formValues(event)
    const response = await apiPost('/auth/email-change-requests', {
      cedula: values.cedula.trim(),
      lastEmail: values.lastEmail.trim(),
      password: values.password,
      newEmail: values.newEmail.trim(),
    })
    form.reset()
    return response
  })

  return (
    <section className="card card--narrow" aria-labelledby="correo-titulo">
      <h1 id="correo-titulo">Recuperar el acceso a mi correo</h1>
      <p className="page__note">
        Si ya no puedes usar tu correo, confirma tu identidad con el último correo registrado y tu contraseña actual o
        la anterior. Enviaremos un enlace al correo nuevo; el cambio se aplica cuando lo abras. Máximo dos intentos por
        día.
      </p>
      <form onSubmit={submit} noValidate>
        <Field label="Cédula" name="cedula" inputMode="numeric" required />
        <Field label="Último correo registrado" name="lastEmail" type="email" required />
        <Field label="Contraseña actual o anterior" name="password" type="password" autoComplete="current-password" required />
        <Field label="Correo nuevo" name="newEmail" type="email" autoComplete="email" required />
        <FormStatus error={error} success={result?.message} />
        <button type="submit" className="button" disabled={pending}>
          {pending ? 'Enviando…' : 'Enviar verificación'}
        </button>
      </form>
    </section>
  )
}
