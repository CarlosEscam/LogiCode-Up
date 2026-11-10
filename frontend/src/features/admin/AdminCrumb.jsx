import { Link } from '../../shared/router/Link.jsx'
import { useSession } from '../auth/useSession.js'

// El menú principal solo muestra "Administración"; desde cada subpágina se vuelve al panel.
export function AdminCrumb() {
  const { can } = useSession()
  if (!can('MANAGE_USERS')) return null
  return (
    <p className="page__note">
      <Link to="/admin">Administración</Link>
    </p>
  )
}
