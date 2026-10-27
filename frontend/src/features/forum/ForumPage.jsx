import { useState } from 'react'
import { apiPost } from '../../shared/api/httpClient.js'
import { usePagedQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { navigate } from '../../shared/router/router.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { Pager } from '../../shared/ui/Pager.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import { formatDateTime } from '../activities/labels.js'
import { useSession } from '../auth/useSession.js'
import { TOOL_LABELS } from '../content/labels.js'

// Foro por herramienta (RF-26): cualquiera lee; con sesión se abren hilos y se responde.
export default function ForumPage() {
  const session = useSession()
  const [filters, setFilters] = useState({ category: '', q: '', page: 0 })
  const [search, setSearch] = useState('')
  const threads = usePagedQuery('/forum/threads', { ...filters, size: 20 })
  const canWrite = session.can('PARTICIPATE_FORUM')

  return (
    <>
      <section className="card card--wide page-head" aria-labelledby="foro-titulo">
        <p className="eyebrow">Responden compañeros y docentes</p>
        <h1 id="foro-titulo">Foro</h1>
        <p className="page__note">
          Preguntas y respuestas sobre PSeInt, DFD, Scratch, Arduino y temas generales.{' '}
          {!session.account && session.status === 'ready' && (
            <>
              <Link to="/ingresar">Inicia sesión</Link> para participar.
            </>
          )}
        </p>
        <form
          className="filters"
          role="search"
          onSubmit={(event) => {
            event.preventDefault()
            setFilters((previous) => ({ ...previous, q: search.trim(), page: 0 }))
          }}
        >
          <label>
            Buscar
            <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Título o mensaje" />
          </label>
          <label>
            Herramienta
            <select
              value={filters.category}
              onChange={(event) => setFilters((previous) => ({ ...previous, category: event.target.value, page: 0 }))}
            >
              <option value="">Todas</option>
              {Object.entries(TOOL_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="button">
            Buscar
          </button>
        </form>
      </section>

      <div className={canWrite ? 'forum-layout' : undefined}>
        <section className="card card--wide forum-threads" aria-label="Hilos">
          <FormStatus error={threads.error} />
          {threads.status === 'loading' && !threads.data && <p role="status">Cargando…</p>}
          <ul className="materials">
            {threads.data?.items.map((thread) => (
              <li key={thread.id} className="material forum-thread">
                <span className="forum-thread__avatar" aria-hidden="true">
                  {initials(thread.authorName)}
                </span>
                <div className="material__body">
                  <h2 className="material__title">
                    <Link to={`/foro/${thread.id}`}>{thread.title}</Link>
                  </h2>
                  <p className="material__meta">
                    <span className="badge">{TOOL_LABELS[thread.category]}</span>
                    {thread.pinned && <span className="badge badge--next">Fijado</span>}
                    {thread.closed && <span className="badge badge--private">Cerrado</span>} {thread.authorName ?? '—'} · última
                    actividad {formatDateTime(thread.lastActivityAt)}
                  </p>
                </div>
                <p className="forum-thread__replies">
                  <strong>{thread.replyCount}</strong> {thread.replyCount === 1 ? 'respuesta' : 'respuestas'}
                </p>
              </li>
            ))}
          </ul>
          {threads.data?.items.length === 0 && <p>No hay hilos con estos filtros.</p>}
          <Pager data={threads.data} onPage={(page) => setFilters((previous) => ({ ...previous, page }))} />
        </section>
        {canWrite && <NewThread defaultCategory={filters.category || 'GENERAL'} />}
      </div>
    </>
  )
}

// Iniciales del autor para el círculo de cada hilo; es solo decoración.
function initials(name) {
  return (name ?? '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0])
    .join('')
    .toUpperCase()
}

function NewThread({ defaultCategory }) {
  const create = useSubmit(async (event) => {
    const { category, title, body } = formValues(event)
    const thread = await apiPost('/forum/threads', { category, title: title.trim(), body: body.trim() })
    navigate(`/foro/${thread.id}`)
    return thread
  })
  return (
    <section className="card card--wide" aria-labelledby="nuevo-hilo">
      <h2 id="nuevo-hilo">Abrir un hilo</h2>
      <form onSubmit={create.submit} noValidate>
        <div className="field">
          <label htmlFor="hilo-categoria">Herramienta</label>
          <select id="hilo-categoria" name="category" defaultValue={defaultCategory} key={defaultCategory}>
            {Object.entries(TOOL_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <Field label="Título" name="title" required minLength={3} maxLength={150} error={create.fieldErrors.title} />
        <div className="field">
          <label htmlFor="hilo-mensaje">Mensaje</label>
          <textarea id="hilo-mensaje" name="body" rows={5} required maxLength={10000} />
        </div>
        <button type="submit" className="button" disabled={create.pending}>
          Publicar
        </button>
      </form>
      <FormStatus error={create.error} />
    </section>
  )
}
