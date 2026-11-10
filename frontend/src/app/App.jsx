import { useEffect } from 'react'
import AccountPage from '../features/account/AccountPage.jsx'
import { roleLabel } from '../features/account/labels.js'
import ActivityPage from '../features/activities/ActivityPage.jsx'
import SubmissionPage from '../features/activities/SubmissionPage.jsx'
import AdminPanelPage from '../features/admin/AdminPanelPage.jsx'
import AuditPage from '../features/admin/AuditPage.jsx'
import EnablementsPage from '../features/admin/EnablementsPage.jsx'
import UsersPage from '../features/admin/UsersPage.jsx'
import EmailChangePage from '../features/auth/EmailChangePage.jsx'
import LoginPage from '../features/auth/LoginPage.jsx'
import PasswordRecoveryPage from '../features/auth/PasswordRecoveryPage.jsx'
import PasswordResetPage from '../features/auth/PasswordResetPage.jsx'
import RegistrationPage from '../features/auth/RegistrationPage.jsx'
import { landingFor } from '../features/auth/landing.js'
import { SessionProvider } from '../features/auth/SessionProvider.jsx'
import { useSession } from '../features/auth/useSession.js'
import VerifyEmailPage from '../features/auth/VerifyEmailPage.jsx'
import LibraryPage from '../features/content/LibraryPage.jsx'
import MaterialPage from '../features/content/MaterialPage.jsx'
import CoursePage from '../features/courses/CoursePage.jsx'
import CoursesPage from '../features/courses/CoursesPage.jsx'
import EmulatorPage from '../features/emulator/EmulatorPage.jsx'
import ForumPage from '../features/forum/ForumPage.jsx'
import ThreadPage from '../features/forum/ThreadPage.jsx'
import GamesPage from '../features/games/GamesPage.jsx'
import QuizAttemptPage from '../features/quizzes/QuizAttemptPage.jsx'
import HomePage from '../pages/HomePage.jsx'
import { Link } from '../shared/router/Link.jsx'
import { navigate, useLocation } from '../shared/router/router.js'
import { Icon, Logo } from '../shared/ui/Icon.jsx'

// access: 'public' (cualquiera), 'guest' (solo sin sesión), 'account' (con sesión) o un permiso.
// Ocultar una ruta es solo comodidad: el backend valida cada permiso.
// Un segmento ':id' recibe el valor de la ruta (p. ej. /cursos/123 → { id: '123' }).
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
  '/biblioteca': { access: 'public', render: () => <LibraryPage /> },
  '/materiales/:id': { access: 'public', render: ({ id }) => <MaterialPage key={id} id={id} /> },
  '/cursos': { access: ['VIEW_PRIVATE_COURSE_CONTENT'], render: () => <CoursesPage /> },
  '/cursos/:id': { access: ['VIEW_PRIVATE_COURSE_CONTENT'], render: ({ id }) => <CoursePage key={id} id={id} /> },
  '/actividades/:id': { access: ['VIEW_PRIVATE_COURSE_CONTENT'], render: ({ id }) => <ActivityPage key={id} id={id} /> },
  '/entregas/:id': { access: ['VIEW_PRIVATE_COURSE_CONTENT'], render: ({ id }) => <SubmissionPage key={id} id={id} /> },
  '/quiz-intentos/:id': { access: ['VIEW_PRIVATE_COURSE_CONTENT'], render: ({ id }) => <QuizAttemptPage key={id} id={id} /> },
  '/juegos': { access: 'public', render: () => <GamesPage /> },
  '/emulador': { access: 'account', render: () => <EmulatorPage /> },
  '/emulador/:tool': { access: 'account', render: ({ tool }) => <EmulatorPage tool={tool} /> },
  // Dirección anterior del editor de PSeInt (enlaces guardados).
  '/pseint': { access: 'account', render: () => <Redirect to="/emulador/pseint" /> },
  '/foro': { access: 'public', render: () => <ForumPage /> },
  '/foro/:id': { access: 'public', render: ({ id }) => <ThreadPage key={id} id={id} /> },
  '/admin': { access: ['MANAGE_USERS'], render: () => <AdminPanelPage /> },
  '/admin/usuarios': { access: ['MANAGE_USERS', 'APPROVE_TEACHERS'], render: () => <UsersPage /> },
  '/admin/habilitaciones': { access: ['MANAGE_STUDENT_ENABLEMENTS'], render: () => <EnablementsPage /> },
  '/admin/auditoria': { access: ['VIEW_AUDIT'], render: () => <AuditPage /> },
}

const NAV = [
  { to: '/cursos', label: 'Cursos', icon: 'cursos', access: ['VIEW_PRIVATE_COURSE_CONTENT'] },
  { to: '/admin', label: 'Administración', icon: 'admin', access: ['MANAGE_USERS'] },
  { to: '/admin/usuarios', label: 'Usuarios', icon: 'usuarios', access: ['MANAGE_USERS', 'APPROVE_TEACHERS'] },
  { to: '/admin/habilitaciones', label: 'Habilitar estudiantes', icon: 'usuarios', access: ['MANAGE_STUDENT_ENABLEMENTS'] },
  { to: '/admin/auditoria', label: 'Auditoría', icon: 'auditoria', access: ['VIEW_AUDIT'] },
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
  // En el celular el menú se desliza de lado: deja a la vista la opción de la página actual.
  useEffect(() => {
    document.querySelector('.topbar__nav [aria-current="page"]')?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
  }, [path, session.account])

  return (
    <>
      <header className="topbar">
        <Link to="/" className="topbar__brand">
          <Logo />
          LogiCode<span className="topbar__cursor" aria-hidden="true">_</span>
        </Link>
        <nav aria-label="Principal" className="topbar__nav">
          <NavLink to="/" icon="inicio" current={path === '/'}>
            Inicio
          </NavLink>
          {session.account &&
            NAV.filter((item) => item.to === '/cursos' && item.access.some(session.can)).map((item) => (
              <NavLink key={item.to} to={item.to} icon={item.icon} current={path.startsWith(item.to)}>
                {item.label}
              </NavLink>
            ))}
          <NavLink to="/biblioteca" icon="biblioteca" current={path === '/biblioteca' || path.startsWith('/materiales/')}>
            Biblioteca
          </NavLink>
          <NavLink to="/juegos" icon="juegos" current={path === '/juegos'}>
            Juegos
          </NavLink>
          {session.account && (
            <NavLink to="/emulador" icon="pseint" current={path.startsWith('/emulador')}>
              Emulador
            </NavLink>
          )}
          <NavLink to="/foro" icon="foro" current={path.startsWith('/foro')}>
            Foro
          </NavLink>
          {session.account &&
            NAV.filter((item) => item.to !== '/cursos' && item.access.some(session.can))
              // Quien entra al panel de administración llega desde allí a las demás páginas: una sola opción en el menú.
              .filter((item, _, items) => item.to === '/admin' || !items.some((other) => other.to === '/admin'))
              .map((item) => (
              <NavLink key={item.to} to={item.to} icon={item.icon} current={path === item.to || path.startsWith(`${item.to}/`)}>
                {item.label}
              </NavLink>
            ))}
        </nav>
        <div className="topbar__account">
          {session.account ? (
            <>
              <Link to="/mi-cuenta" className="topbar__who" aria-current={path === '/mi-cuenta' ? 'page' : undefined}>
                <strong>{session.account.fullName.split(' ')[0]}</strong>
                <span className="topbar__role"> · {roleLabel(session.account.role)}</span>
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
            session.status === 'ready' && (
              <Link to="/ingresar" className="button button--secondary button--small" aria-current={path === '/ingresar' ? 'page' : undefined}>
                Iniciar sesión
              </Link>
            )
          )}
        </div>
      </header>
      <main className="page">
        <Route path={path} session={session} />
      </main>
    </>
  )
}

function NavLink({ to, icon, current, children }) {
  return (
    <Link to={to} aria-current={current ? 'page' : undefined}>
      <Icon name={icon} />
      {children}
    </Link>
  )
}

function matchRoute(path) {
  for (const [pattern, route] of Object.entries(ROUTES)) {
    const names = []
    const regex = new RegExp(
      `^${pattern.replace(/:([a-zA-Z]+)/g, (_, name) => {
        names.push(name)
        return '([^/]+)'
      })}$`,
    )
    const match = path.match(regex)
    if (match) {
      const params = Object.fromEntries(names.map((name, index) => [name, decodeURIComponent(match[index + 1])]))
      return { route, params }
    }
  }
  return null
}

function Route({ path, session }) {
  const matched = matchRoute(path)
  if (!matched) {
    return (
      <section className="card">
        <h1>Página no encontrada</h1>
        <p>
          <Link to="/">Volver al inicio</Link>
        </p>
      </section>
    )
  }
  const { route, params } = matched
  if (route.access === 'public') return route.render(params)
  if (session.status === 'loading') return <p role="status">Cargando…</p>

  if (route.access === 'guest') {
    return session.account ? <Redirect to={landingFor(session.account, new URLSearchParams(window.location.search).get('volver'))} /> : route.render(params)
  }
  if (!session.account) return <Redirect to={`/ingresar?volver=${encodeURIComponent(path + window.location.search)}`} />
  if (Array.isArray(route.access) && !route.access.some(session.can)) {
    return (
      <section className="card">
        <h1>Sin permiso</h1>
        <p>Tu cuenta no tiene permiso para ver esta página.</p>
      </section>
    )
  }
  return route.render(params)
}

function Redirect({ to }) {
  useEffect(() => navigate(to, { replace: true }), [to])
  return null
}
