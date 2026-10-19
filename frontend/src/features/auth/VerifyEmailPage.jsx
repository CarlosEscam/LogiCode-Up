import { useEffect, useRef, useState } from 'react'
import { apiPost } from '../../shared/api/httpClient.js'
import { Link } from '../../shared/router/Link.jsx'
import { useLocation } from '../../shared/router/router.js'

// Destino del enlace enviado por correo: /verificar-correo?token=…
export default function VerifyEmailPage() {
  const { query } = useLocation()
  const token = query.get('token')
  const [state, setState] = useState({ status: token ? 'loading' : 'error', message: 'El enlace no es válido.' })
  const sent = useRef(false)

  useEffect(() => {
    // El token es de un solo uso: se evita el doble envío del modo estricto de React.
    if (!token || sent.current) return
    sent.current = true
    apiPost('/auth/email-verifications/confirm', { token })
      .then((result) => setState({ status: 'ok', ...result }))
      .catch((error) => setState({ status: 'error', message: error.message }))
  }, [token])

  return (
    <section className="card card--narrow" aria-labelledby="verificar-titulo">
      <h1 id="verificar-titulo">Verificación de correo</h1>
      {state.status === 'loading' && <p role="status">Verificando tu correo…</p>}
      {state.status === 'error' && (
        <p className="notice notice--error" role="alert">
          {state.message}
        </p>
      )}
      {state.status === 'ok' && (
        <>
          <p className="notice notice--ok" role="status">
            {state.message}
          </p>
          {state.passwordSetupRequired ? (
            <p>
              Tu cuenta aún no tiene contraseña. <Link to="/recuperar-contrasena">Crea una aquí</Link> con el correo{' '}
              <strong>{state.email}</strong>.
            </p>
          ) : (
            <p>
              <Link to="/ingresar">Iniciar sesión</Link>
            </p>
          )}
        </>
      )}
    </section>
  )
}
