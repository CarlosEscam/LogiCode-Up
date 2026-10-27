// Mensaje de nivel resuelto con el botón para seguir.
export default function Solved({ onNext, children }) {
  return (
    <div className="notice notice--ok" role="status">
      {children}{' '}
      {onNext ? (
        <button type="button" className="button button--small" onClick={onNext}>
          Siguiente nivel
        </button>
      ) : (
        '¡Completaste todos los niveles de este juego!'
      )}
    </div>
  )
}
