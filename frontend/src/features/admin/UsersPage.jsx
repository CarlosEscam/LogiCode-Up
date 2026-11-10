import { useState } from 'react'
import { apiPatch, apiPost } from '../../shared/api/httpClient.js'
import { Pager } from '../../shared/ui/Pager.jsx'
import { usePagedQuery } from '../../shared/api/usePagedQuery.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { ROLE_LABELS, STATUS_LABELS, roleLabel } from '../account/labels.js'
import { useSession } from '../auth/useSession.js'
import { useLocation } from '../../shared/router/router.js'
import { AdminCrumb } from './AdminCrumb.jsx'

export default function UsersPage() {
  const { account, can } = useSession()
  const { query } = useLocation()
  // ?rol=…&estado=… llega desde las cifras del panel de administración.
  const [filters, setFilters] = useState(() => ({ q: '', role: query.get('rol') ?? '', status: query.get('estado') ?? '', page: 0 }))
  const [search, setSearch] = useState('')
  const [feedback, setFeedback] = useState({ error: null, success: null })
  const users = usePagedQuery('/admin/users', { ...filters, size: 20 })

  async function run(action, success) {
    setFeedback({ error: null, success: null })
    try {
      await action()
      setFeedback({ error: null, success })
      users.reload()
    } catch (error) {
      setFeedback({ error: error.message, success: null })
    }
  }

  function changeFilter(name, value) {
    setFilters((previous) => ({ ...previous, [name]: value, page: 0 }))
  }

  return (
    <section className="card card--wide" aria-labelledby="usuarios-titulo">
      <AdminCrumb />
      <h1 id="usuarios-titulo">Usuarios</h1>
      <form
        className="filters"
        onSubmit={(event) => {
          event.preventDefault()
          changeFilter('q', search.trim())
        }}
      >
        <label>
          Buscar
          <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Cédula, nombre o correo" />
        </label>
        <label>
          Rol
          <select value={filters.role} onChange={(event) => changeFilter('role', event.target.value)}>
            <option value="">Todos</option>
            {Object.entries(ROLE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Estado
          <select value={filters.status} onChange={(event) => changeFilter('status', event.target.value)}>
            <option value="">Todos</option>
            {Object.entries(STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <button type="submit" className="button button--secondary">
          Buscar
        </button>
      </form>

      <FormStatus error={feedback.error ?? users.error} success={feedback.success} />

      <div className="table-wrap" tabIndex={0}>
        <table>
          <thead>
            <tr>
              <th scope="col">Cédula</th>
              <th scope="col">Nombre</th>
              <th scope="col">Correo</th>
              <th scope="col">Rol</th>
              <th scope="col">Estado</th>
              <th scope="col">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {users.data?.items.map((user) => (
              <tr key={user.id}>
                <td>{user.cedula}</td>
                <td>{user.fullName}</td>
                <td>
                  {user.email}
                  {!user.emailVerified && <span className="badge">sin verificar</span>}
                </td>
                <td>
                  {can('CHANGE_ROLES') && user.id !== account.id ? (
                    <select
                      aria-label={`Rol de ${user.fullName}`}
                      value={user.role}
                      onChange={(event) =>
                        run(() => apiPatch(`/admin/users/${user.id}/role`, { role: event.target.value }), 'Rol actualizado.')
                      }
                    >
                      {Object.entries(ROLE_LABELS).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  ) : (
                    roleLabel(user.role, user.fullPrivileges)
                  )}
                </td>
                <td>{STATUS_LABELS[user.status] ?? user.status}</td>
                <td>
                  <div className="actions">
                    {user.status === 'PENDING_APPROVAL' && user.role === 'TEACHER' && can('APPROVE_TEACHERS') && (
                      <button
                        type="button"
                        className="button button--small"
                        onClick={() => run(() => apiPost(`/admin/teachers/${user.id}/approve`), 'Docente aprobado.')}
                      >
                        Aprobar
                      </button>
                    )}
                    {can('MANAGE_USERS') && user.id !== account.id && user.status === 'ACTIVE' && (
                      <button
                        type="button"
                        className="button button--small button--danger"
                        onClick={() =>
                          window.confirm(`¿Desactivar la cuenta de ${user.fullName}? No podrá ingresar hasta que la actives de nuevo.`) &&
                          run(() => apiPatch(`/admin/users/${user.id}/status`, { status: 'DISABLED' }), 'Cuenta desactivada.')
                        }
                      >
                        Desactivar
                      </button>
                    )}
                    {can('MANAGE_USERS') && user.status === 'DISABLED' && (
                      <button
                        type="button"
                        className="button button--small button--secondary"
                        onClick={() =>
                          run(() => apiPatch(`/admin/users/${user.id}/status`, { status: 'ACTIVE' }), 'Cuenta activada.')
                        }
                      >
                        Activar
                      </button>
                    )}
                </div>
                </td>
              </tr>
            ))}
            {users.data?.items.length === 0 && (
              <tr>
                <td colSpan={6}>No hay usuarios con esos filtros.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {users.status === 'loading' && <p role="status">Cargando…</p>}
      <Pager data={users.data} onPage={(page) => setFilters((previous) => ({ ...previous, page }))} />
    </section>
  )
}
