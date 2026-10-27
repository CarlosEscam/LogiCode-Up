import { useState } from 'react'
import { apiDelete, apiPatch, apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Link } from '../../shared/router/Link.jsx'
import { navigate } from '../../shared/router/router.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatDateTime } from '../activities/labels.js'
import { TOOL_LABELS } from '../content/labels.js'

// Un hilo del foro con sus respuestas. Moderan el docente y las cuentas Administrador (RF-27);
// el backend decide quién puede responder o moderar y lo informa en canReply y canModerate.
export default function ThreadPage({ id }) {
  const thread = useQuery(`/forum/threads/${id}`)
  const [feedback, setFeedback] = useState({ pending: false, error: null, success: null })
  async function moderate(action, success = null) {
    setFeedback({ pending: true, error: null, success: null })
    try {
      await action()
      setFeedback({ pending: false, error: null, success })
      thread.reload()
    } catch (error) {
      setFeedback({ pending: false, error: error.message, success: null })
    }
  }

  if (thread.status === 'loading' && !thread.data) return <p role="status">Cargando…</p>
  if (thread.status === 'error') {
    return (
      <section className="card">
        <h1>Hilo no disponible</h1>
        <FormStatus error={thread.error} />
        <p>
          <Link to="/foro">Volver al foro</Link>
        </p>
      </section>
    )
  }

  const data = thread.data
  return (
    <>
      <article className="card card--wide" aria-labelledby="hilo-titulo">
        <p className="page__note">
          <Link to="/foro">Foro</Link> · <span className="badge">{TOOL_LABELS[data.category]}</span>
          {data.pinned && <span className="badge badge--next">Fijado</span>}
          {data.closed && <span className="badge badge--private">Cerrado</span>}
        </p>
        <h1 id="hilo-titulo">{data.title}</h1>
        <p className="material__meta">
          {data.authorName ?? '—'} · {formatDateTime(data.createdAt)}
        </p>
        <p className="forum-text">{data.body}</p>
        {data.canModerate && (
          <div className="actions" aria-label="Moderación">
            <button
              type="button"
              className="button button--secondary button--small"
              disabled={feedback.pending}
              onClick={() => moderate(() => apiPatch(`/forum/threads/${id}`, { pinned: !data.pinned }))}
            >
              {data.pinned ? 'Soltar' : 'Fijar arriba'}
            </button>
            <button
              type="button"
              className="button button--secondary button--small"
              disabled={feedback.pending}
              onClick={() => moderate(() => apiPatch(`/forum/threads/${id}`, { closed: !data.closed }))}
            >
              {data.closed ? 'Reabrir' : 'Cerrar a respuestas'}
            </button>
            <ConfirmButton
              label="Eliminar hilo"
              onConfirm={() =>
                moderate(async () => {
                  await apiDelete(`/forum/threads/${id}`)
                  navigate('/foro')
                })
              }
            />
          </div>
        )}
        <FormStatus error={feedback.error} success={feedback.success} />
      </article>

      <section className="card card--wide" aria-labelledby="respuestas-titulo">
        <h2 id="respuestas-titulo">{data.posts.length === 1 ? '1 respuesta' : `${data.posts.length} respuestas`}</h2>
        <ol className="forum-posts">
          {data.posts.map((post) => (
            <li key={post.id} className="forum-post">
              <p className="material__meta">
                {post.authorName ?? '—'} · {formatDateTime(post.createdAt)}
              </p>
              <p className="forum-text">{post.body}</p>
              {data.canModerate && (
                <ConfirmButton
                  label="Eliminar respuesta"
                  onConfirm={() =>
                    moderate(() => apiDelete(`/forum/posts/${post.id}`), 'Respuesta eliminada.')
                  }
                />
              )}
            </li>
          ))}
        </ol>
        {data.canReply ? (
          <ReplyForm threadId={id} onSent={thread.reload} />
        ) : (
          <p className="page__note">
            {data.closed ? 'Este hilo está cerrado y no recibe más respuestas.' : <><Link to="/ingresar">Inicia sesión</Link> para responder.</>}
          </p>
        )}
      </section>
    </>
  )
}

// Acción destructiva en dos pasos, sin diálogos del navegador.
function ConfirmButton({ label, onConfirm }) {
  const [asking, setAsking] = useState(false)
  if (!asking) {
    return (
      <button type="button" className="button button--danger button--small" onClick={() => setAsking(true)}>
        {label}
      </button>
    )
  }
  return (
    <span role="group" aria-label={`Confirmar: ${label}`}>
      ¿Seguro?{' '}
      <button type="button" className="button button--danger button--small" onClick={onConfirm}>
        Sí, eliminar
      </button>{' '}
      <button type="button" className="button button--secondary button--small" onClick={() => setAsking(false)}>
        No
      </button>
    </span>
  )
}

function ReplyForm({ threadId, onSent }) {
  const [body, setBody] = useState('')
  const send = useSubmit(async () => {
    if (!body.trim()) throw new Error('Escribe tu respuesta.')
    const result = await apiPost(`/forum/threads/${threadId}/posts`, { body: body.trim() })
    setBody('')
    onSent()
    return result
  })
  return (
    <form onSubmit={send.submit} noValidate>
      <div className="field">
        <label htmlFor="respuesta">Tu respuesta</label>
        <textarea id="respuesta" rows={4} maxLength={10000} value={body} onChange={(event) => setBody(event.target.value)} />
      </div>
      <button type="submit" className="button" disabled={send.pending}>
        Responder
      </button>
      <FormStatus error={send.error} success={send.result && 'Respuesta publicada.'} />
    </form>
  )
}
