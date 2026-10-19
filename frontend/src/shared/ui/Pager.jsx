export function Pager({ data, onPage }) {
  if (!data || data.totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="Paginación">
      <button type="button" className="button button--secondary" disabled={data.page === 0} onClick={() => onPage(data.page - 1)}>
        Anterior
      </button>
      <span>
        Página {data.page + 1} de {data.totalPages}
      </span>
      <button
        type="button"
        className="button button--secondary"
        disabled={data.page + 1 >= data.totalPages}
        onClick={() => onPage(data.page + 1)}
      >
        Siguiente
      </button>
    </nav>
  )
}
