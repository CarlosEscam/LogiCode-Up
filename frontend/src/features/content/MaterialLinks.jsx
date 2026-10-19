import { Link } from '../../shared/router/Link.jsx'
import { downloadUrl } from './labels.js'

// Acciones para abrir un material: vista en página, descarga o enlace externo. El aria-label
// empieza con el texto visible y agrega el título, para distinguir los botones de cada material.
export function MaterialLinks({ material }) {
  if (material.kind === 'LINK') {
    return (
      <a
        className="button button--secondary button--small"
        href={material.externalUrl}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Abrir enlace ${material.title} (se abre en otra pestaña)`}
      >
        Abrir enlace
      </a>
    )
  }
  const view = material.file?.previewable ? 'Ver' : 'Detalles'
  return (
    <>
      <Link className="button button--secondary button--small" to={`/materiales/${material.id}`} aria-label={`${view} ${material.title}`}>
        {view}
      </Link>
      <a className="button button--secondary button--small" href={downloadUrl(material)} download aria-label={`Descargar ${material.title}`}>
        Descargar
      </a>
    </>
  )
}
