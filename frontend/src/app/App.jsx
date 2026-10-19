import { useEffect } from 'react'
import AccountPage from '../features/account/AccountPage.jsx'
import { roleLabel } from '../features/account/labels.js'
import AuditPage from '../features/admin/AuditPage.jsx'
import EnablementsPage from '../features/admin/EnablementsPage.jsx'
import UsersPage from '../features/admin/UsersPage.jsx'
import EmailChangePage from '../features/auth/EmailChangePage.jsx'
import LoginPage from '../features/auth/LoginPage.jsx'
import PasswordRecoveryPage from '../features/auth/PasswordRecoveryPage.jsx'
import PasswordResetPage from '../features/auth/PasswordResetPage.jsx'
import RegistrationPage from '../features/auth/RegistrationPage.jsx'
import { SessionProvider } from '../features/auth/SessionProvider.jsx'
import { useSession } from '../features/auth/useSession.js'
import VerifyEmailPage from '../features/auth/VerifyEmailPage.jsx'
import HomePage from '../pages/HomePage.jsx'
import { Link } from '../shared/router/Link.jsx'
import { navigate, useLocation } from '../shared/router/router.js'

// access: 'public' (cualquiera), 'guest' (solo sin sesión), 'account' (con sesión) o un permiso.
// Ocultar una ruta es solo comodidad: el backend valida cada permiso.
const ROUTES = {
  '/': { access: 'public', render: () => <HomePage /> },
  '/ingresar': { access: 'guest', render: () => <LoginPage /> },
  '/registro/estudiante': { access: 'guest', render: () => <RegistrationPage kind="student" /> },
  '/registro/docente': { access: 'guest', render: () => <RegistrationPage kind="teacher" /> },
  '/verificar-correo': { access: 'public', render: () => <VerifyEmailPage /> },
  '/recuperar-contrasena': { access: 'public', render: () => <PasswordRecoveryPage /> },
  '/restablecer-contrasena': { access: 'public', render: () => <PasswordResetPage /> },
  '/recuperar-correo': { access: 'guest', render: () => <EmailChangePage /> },
  '/mi-cuenta': { access: 'account', render: () => <AccountPage /> },
  '/admin/usuarios': { access: ['MANAGE_USERS', 'APPROVE_TEACHERS'], render: () => <UsersPage /> },
  '/admin/habilitaciones': { access: ['MANAGE_STUDENT_ENABLEMENTS'], render: () => <EnablementsPage /> },
  '/admin/auditoria': { access: ['VIEW_AUDIT'], render: () => <AuditPage /> },
}

const NAV = [
  { to: '/admin/usuarios', label: 'Usuarios', access: ['MANAGE_USERS', 'APPROVE_TEACHERS'] },
  { to: '/admin/habilitaciones', label: 'Habilitar estudiantes', access: ['MANAGE_STUDENT_ENABLEMENTS'] },
  { to: '/admin/auditoria', label: 'Auditoría', access: ['VIEW_AUDIT'] },
]

export default function App() {
  return (
    <SessionProvider>
      <Layout />
    </SessionProvider>
  )
}

function Layout() {
  const session = useSession()
  const { path } = useLocation()

  return (
    <>
      <header className="topbar">
        <Link to="/" className="topbar__brand">
          LogiCode
        </Link>
        <nav aria-label="Principal" className="topbar__nav">
          {session.account ? (
            <>
              {NAV.filter((item) => item.access.some(session.can)).map((item) => (
                <Link key={item.to} to={item.to} aria-current={path === item.to ? 'page' : undefined}>
                  {item.label}
                </Link>
              ))}
              <Link to="/mi-cuenta" aria-current={path === '/mi-cuenta' ? 'page' : undefined}>
                {session.account.fullName.split(' ')[0]} · {roleLabel(session.account.role)}
              </Link>
              <button
                type="button"
                className="button button--secondary button--small"
                onClick={() => session.logout().finally(() => navigate('/'))}
              >
                Cerrar sesión
              </button>
            </>
          ) : (
            session.status === 'ready' && <Link to="/ingresar">Iniciar sesión</Link>
          )}
        </nav>
      </header>
      <main className="page">
        <Route path={path} session={session} />
      </main>
    </>
  )
}

function Route({ path, session }) {
  const route = ROUTES[path]
  if (!route) {
    return (
      <section className="card">
        <h1>Página no encontrada</h1>
        <p>
          <Link to="/">Volver al inicio</Link>
        </p>
      </section>
    )
  }
  if (route.access === 'public') return route.render()
  if (session.status === 'loading') return <p role="status">Cargando…</p>

  if (route.access === 'guest') {
    return session.account ? <Redirect to="/mi-cuenta" /> : route.render()
  }
  if (!session.account) return <Redirect to="/ingresar" />
  if (Array.isArray(route.access) && !route.access.some(session.can)) {
    return (
      <section className="card">
        <h1>Sin permiso</h1>
        <p>Tu cuenta no tiene permiso para ver esta página.</p>
      </section>
    )
  }
  return route.render()
}

function Redirect({ to }) {
  useEffect(() => navigate(to, { replace: true }), [to])
  return null
}
