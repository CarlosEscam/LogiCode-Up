import { useState } from 'react'
import { usePagedQuery, useQuery } from '../../shared/api/usePagedQuery.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { Pager } from '../../shared/ui/Pager.jsx'
import { useSession } from '../auth/useSession.js'
import { KIND_LABELS, TOOL_LABELS, VISIBILITY_LABELS, formatSize } from './labels.js'
import { MaterialLinks } from './MaterialLinks.jsx'
import { MaterialModeration } from './MaterialModeration.jsx'

// Biblioteca por tema y herramienta (RF-24). Un visitante ve solo lo público; quien participa en un
// curso ve también lo privado de ese curso. El backend decide qué se muestra.
export default function LibraryPage() {
  const { account } = useSession()
  const [filters, setFilters] = useState({ q: '', tool: '', topicId: '', kind: '', page: 0 })
  const [search, setSearch] = useState('')
  const results = usePagedQuery('/library', { ...filters, size: 20 })
  const topics = useQuery('/library/topics')
  // Biblioteca administrable: quien modera (cuentas Administrador) publica, oculta o archiva desde aquí.
  const [feedback, setFeedback] = useState({ error: null, success: null })
  async function run(action, success) {
    try {
      await action()
      setFeedback({ error: null, success })
      results.reload()
      topics.reload()
    } catch (error) {
      setFeedback({ error: error.message, success: null })
    }
  }

  function changeFilter(name, value) {
    setFilters((previous) => ({ ...previous, [name]: value, page: 0 }))
  }

  const topicOptions = (topics.data ?? []).filter((topic) => !filters.tool || topic.tool === filters.tool)

  return (
    <>
      <section className="card card--wide page-head" aria-labelledby="biblioteca-titulo">
        <p className="eyebrow">Guías, presentaciones, videos y archivos</p>
        <h1 id="biblioteca-titulo">Biblioteca</h1>
        <p className="page__note">
          {account?.role === 'ADMIN'
            ? 'Todos los materiales, públicos y privados. Desde aquí los publicas, ocultas o archivas.'
            : account
            ? 'Materiales públicos y los privados de tus cursos.'
            : 'Materiales públicos. Inicia sesión para ver también los de tu curso.'}
        </p>
        <form
          className="filters"
          role="search"
          onSubmit={(event) => {
            event.preventDefault()
            changeFilter('q', search.trim())
          }}
        >
          <label>
            Buscar
            <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Título o tema" />
          </label>
          <label>
            Herramienta
            <select
              value={filters.tool}
              onChange={(event) => setFilters((previous) => ({ ...previous, tool: event.target.value, topicId: '', page: 0 }))}
            >
              <option value="">Todas</option>
              {Object.entries(TOOL_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Tema
            <select value={filters.topicId} onChange={(event) => changeFilter('topicId', event.target.value)}>
              <option value="">Todos</option>
              {topicOptions.map((topic) => (
                <option key={topic.id} value={topic.id}>
                  {topic.title}
                </option>
              ))}
            </select>
          </label>
          <label>
            Tipo
            <select value={filters.kind} onChange={(event) => changeFilter('kind', event.target.value)}>
              <option value="">Todos</option>
              {Object.entries(KIND_LABELS).map(([value, label]) => (
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

      <section className="page-body" aria-label="Materiales">
        <FormStatus error={results.error || feedback.error} success={feedback.success} />
        {results.status === 'loading' && !results.data && <p role="status">Cargando…</p>}
        {results.data && (
          <p className="page__note" role="status">
            {results.data.totalItems === 1 ? '1 material' : `${results.data.totalItems} materiales`}
          </p>
        )}
        <ul className="materials materials--grid">
          {results.data?.items.map((material) => (
            <li key={material.id} className="material">
              <div className="material__body">
                <p className="material__kind">
                  <span aria-hidden="true">{material.file?.format ?? 'URL'}</span>
                  {KIND_LABELS[material.kind]}
                </p>
                <h2 className="material__title">{material.title}</h2>
                <p className="material__meta">
                  <span className="badge">{TOOL_LABELS[material.tool]}</span> {material.topicTitle} · {KIND_LABELS[material.kind]}
                  {material.file && ` · ${material.file.format} · ${formatSize(material.file.sizeBytes)}`}
                  {material.visibility === 'PRIVATE' && <span className="badge badge--private">{VISIBILITY_LABELS.PRIVATE}</span>}
                </p>
                {material.description && <p className="material__description">{material.description}</p>}
              </div>
              <div className="material__actions">
                <MaterialLinks material={material} />
                <MaterialModeration material={material} run={run} />
              </div>
            </li>
          ))}
        </ul>
        {results.data?.items.length === 0 && <p>No hay materiales con estos filtros.</p>}
        <Pager data={results.data} onPage={(page) => setFilters((previous) => ({ ...previous, page }))} />
      </section>
    </>
  )
}
