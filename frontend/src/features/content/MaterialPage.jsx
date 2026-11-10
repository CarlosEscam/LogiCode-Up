import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { useLocation } from '../../shared/router/router.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { KIND_LABELS, TOOL_LABELS, VISIBILITY_LABELS, downloadUrl, formatSize, previewUrl } from './labels.js'
import { MaterialLinks } from './MaterialLinks.jsx'

// Detalle de un material con vista en página para PDF, imágenes y videos (RF-25).
export default function MaterialPage({ id }) {
  const material = useQuery(`/materials/${id}`)
  const fromCourse = useLocation().query.get('desde') === 'curso'

  if (material.status === 'loading') return <p role="status">Cargando…</p>
  if (material.status === 'error') {
    return (
      <section className="card">
        <h1>Material no disponible</h1>
        <FormStatus error={material.error} />
        <p>
          <Link to="/biblioteca">Volver a la biblioteca</Link>
        </p>
      </section>
    )
  }

  const item = material.data
  return (
    <section className="card card--wide" aria-labelledby="material-titulo">
      <p className="page__note">
        {fromCourse && item.courseId ? (
          <Link to={`/cursos/${item.courseId}`}>{item.courseName}</Link>
        ) : (
          <Link to="/biblioteca">Biblioteca</Link>
        )}{' '}
        · {TOOL_LABELS[item.tool]} · {item.topicTitle}
      </p>
      <h1 id="material-titulo">{item.title}</h1>
      <p className="material__meta">
        {KIND_LABELS[item.kind]} · {VISIBILITY_LABELS[item.visibility]}
        {item.file && ` · ${item.file.originalName} (${formatSize(item.file.sizeBytes)})`}
      </p>
      {item.description && <p>{item.description}</p>}
      <Preview material={item} />
      <div className="material__actions">
        <MaterialLinks material={item} />
      </div>
    </section>
  )
}

function Preview({ material }) {
  if (!material.file) return null
  if (!material.file.previewable) {
    return (
      <p className="notice">
        Este formato no se puede ver dentro de la página. <a href={downloadUrl(material)}>Descárgalo</a> para abrirlo.
      </p>
    )
  }
  const src = previewUrl(material)
  if (material.file.format === 'PDF') {
    // Muchos navegadores móviles no muestran PDF dentro de un iframe: se ofrece abrirlo aparte.
    return (
      <>
        <iframe className="preview preview--pdf" src={src} title={`Vista previa de ${material.title}`} />
        <p>
          <a href={src} target="_blank" rel="noopener">
            Abrir el PDF en una pestaña nueva
          </a>
        </p>
      </>
    )
  }
  if (material.file.format === 'PNG' || material.file.format === 'JPG') {
    return <img className="preview" src={src} alt={material.description || material.title} />
  }
  return (
    <video className="preview" src={src} controls preload="metadata">
      Tu navegador no puede reproducir este video. <a href={downloadUrl(material)}>Descárgalo</a>.
    </video>
  )
}
