import { apiPost } from '../../shared/api/httpClient.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { PASSWORD_HINT, formValues, useSubmit } from '../../shared/ui/useSubmit.js'

const VARIANTS = {
  student: {
    title: 'Registro de estudiante',
    intro: 'Tu cédula debe estar habilitada por un docente. Después de registrarte verifica tu correo.',
    path: '/auth/student-registrations',
    action: 'Registrarme',
  },
  teacher: {
    title: 'Solicitud de cuenta de docente',
    intro: 'Un administrador revisará tu solicitud. Mientras tanto, verifica tu correo.',
    path: '/auth/teacher-requests',
    action: 'Enviar solicitud',
  },
}

export default function RegistrationPage({ kind }) {
  const variant = VARIANTS[kind]
  const { pending, error, fieldErrors, result, submit } = useSubmit(async (event) => {
    const form = event.currentTarget
    const values = formValues(event)
    if (values.password !== values.confirmPassword) {
      throw new Error('Las contraseñas no coinciden.')
    }
    const response = await apiPost(variant.path, {
      cedula: values.cedula.trim(),
      fullName: values.fullName.trim(),
      email: values.email.trim(),
      password: values.password,
    })
    form.reset()
    return response
  })

  return (
    <section className="card card--narrow" aria-labelledby="registro-titulo">
      <h1 id="registro-titulo">{variant.title}</h1>
      <p className="page__note">{variant.intro}</p>
      <form onSubmit={submit} noValidate>
        <Field label="Cédula" name="cedula" inputMode="numeric" required error={fieldErrors.cedula} />
        <Field label="Nombre completo" name="fullName" autoComplete="name" required error={fieldErrors.fullName} />
        <Field label="Correo electrónico" name="email" type="email" autoComplete="email" required error={fieldErrors.email} />
        <Field
          label="Contraseña"
          name="password"
          type="password"
          autoComplete="new-password"
          hint={PASSWORD_HINT}
          required
          error={fieldErrors.password}
        />
        <Field label="Confirmar contraseña" name="confirmPassword" type="password" autoComplete="new-password" required />
        <FormStatus error={error} success={result?.message} />
        <button type="submit" className="button" disabled={pending}>
          {pending ? 'Enviando…' : variant.action}
        </button>
      </form>
    </section>
  )
}
