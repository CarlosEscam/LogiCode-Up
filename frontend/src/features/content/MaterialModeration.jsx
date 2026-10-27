import { apiDelete, apiPut } from '../../shared/api/httpClient.js'
import { VISIBILITY_LABELS } from './labels.js'

// Publicar u ocultar y archivar un material (RN-17): su docente y las cuentas Administrador (MANAGE_LIBRARY).
// run(acción, mensaje) ejecuta la llamada y recarga la lista de quien lo usa.
export function MaterialModeration({ material, run }) {
  if (!material.canModerate) return null
  const otherVisibility = material.visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC'
  return (
    <>
      <button
        type="button"
        className="button button--secondary button--small"
        aria-label={`Hacer ${VISIBILITY_LABELS[otherVisibility].toLowerCase()} ${material.title}`}
        onClick={() =>
          run(
            () => apiPut(`/materials/${material.id}/visibility`, { visibility: otherVisibility }),
            `"${material.title}" ahora es ${VISIBILITY_LABELS[otherVisibility].toLowerCase()}.`,
          )
        }
      >
        Hacer {VISIBILITY_LABELS[otherVisibility].toLowerCase()}
      </button>
      <button
        type="button"
        className="button button--danger button--small"
        aria-label={`Archivar ${material.title}`}
        onClick={() =>
          window.confirm(`¿Archivar "${material.title}"? Dejará de mostrarse.`) &&
          run(() => apiDelete(`/materials/${material.id}`), 'Material archivado.')
        }
      >
        Archivar
      </button>
    </>
  )
}
