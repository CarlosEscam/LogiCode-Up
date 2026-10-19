import { useState } from 'react'
import { apiDelete, apiPost, apiPut, apiSendForm } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { Field, FormStatus } from '../../shared/ui/forms.jsx'
import { formValues, useSubmit } from '../../shared/ui/useSubmit.js'
import { KIND_ACCEPT, KIND_LABELS, TOOL_LABELS, VISIBILITY_LABELS, formatSize } from '../content/labels.js'
import { MaterialLinks } from '../content/MaterialLinks.jsx'

// Temas del curso en orden, con sus materiales (RF-06 a RF-09). Los controles de edición solo se
// muestran a quien puede usarlos; el backend vuelve a validar cada acción.
export default function TopicsPanel({ courseId, canManage }) {
  const topics = useQuery(`/courses/${courseId}/topics`)
  const [feedback, setFeedback] = useState({ error: null, success: null })

  async function run(action, success) {
    setFeedback({ error: null, success: null })
    try {
      await action()
      setFeedback({ error: null, success })
      topics.reload()
      return true
    } catch (error) {
      setFeedback({ error: error.message, success: null })
      return false
    }
  }

  function move(index, delta) {
    const ids = topics.data.map((topic) => topic.id)
    ;[ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]
    return run(() => apiPut(`/courses/${courseId}/topics/order`, { ids }), 'Orden de los temas actualizado.')
  }

  const nextClass = topics.data?.find((topic) => topic.nextClass)

  return (
    <>
      {nextClass && (
        <section className="card card--wide next-class" aria-labelledby="proxima-titulo">
          <h2 id="proxima-titulo">Próxima clase</h2>
          <p>
            <a href={`#tema-${nextClass.id}`}>{nextClass.title}</a> · {TOOL_LABELS[nextClass.tool]}
          </p>
        </section>
      )}

      {/* Mensajes del panel; la tarjeta solo aparece cuando hay algo que decir. */}
      {(feedback.error || feedback.success || topics.error || !topics.data || topics.data.length === 0) && (
        <section className="card card--wide" aria-label="Estado de los temas">
          <FormStatus error={feedback.error ?? topics.error} success={feedback.success} />
          {topics.status === 'loading' && !topics.data && <p role="status">Cargando…</p>}
          {topics.data?.length === 0 && <p>Este curso aún no tiene temas.</p>}
        </section>
      )}

      {topics.data?.map((topic, index) => (
        <TopicCard
          key={topic.id}
          topic={topic}
          canManage={canManage}
          isFirst={index === 0}
          isLast={index === topics.data.length - 1}
          onMove={(delta) => move(index, delta)}
          run={run}
          reload={topics.reload}
          courseId={courseId}
        />
      ))}

      {canManage && <NewTopic courseId={courseId} onCreated={topics.reload} />}
    </>
  )
}

function TopicCard({ topic, canManage, isFirst, isLast, onMove, run, reload, courseId }) {
  const [editing, setEditing] = useState(false)

  function moveMaterial(index, delta) {
    const ids = topic.materials.map((material) => material.id)
    ;[ids[index], ids[index + delta]] = [ids[index + delta], ids[index]]
    return run(() => apiPut(`/topics/${topic.id}/materials/order`, { ids }), 'Orden de los materiales actualizado.')
  }

  return (
    <section id={`tema-${topic.id}`} className="card card--wide topic" aria-labelledby={`tema-${topic.id}-titulo`}>
      <div className="topic__header">
        <h2 id={`tema-${topic.id}-titulo`}>{topic.title}</h2>
        <span className="badge">{TOOL_LABELS[topic.tool]}</span>
        {topic.nextClass && <span className="badge badge--next">Próxima clase</span>}
      </div>
      {topic.description && <p>{topic.description}</p>}

      {canManage && (
        <div className="toolbar" aria-label={`Acciones del tema ${topic.title}`}>
          <button
            type="button"
            className="button button--secondary button--small"
            disabled={isFirst}
            onClick={() => onMove(-1)}
            aria-label={`Subir tema ${topic.title}`}
          >
            Subir
          </button>
          <button
            type="button"
            className="button button--secondary button--small"
            disabled={isLast}
            onClick={() => onMove(1)}
            aria-label={`Bajar tema ${topic.title}`}
          >
            Bajar
          </button>
          <button
            type="button"
            className="button button--secondary button--small"
            onClick={() =>
              run(
                () => apiPut(`/courses/${courseId}/next-class`, { topicId: topic.nextClass ? null : topic.id }),
                topic.nextClass ? 'Se quitó la marca de próxima clase.' : `"${topic.title}" es el tema de la próxima clase.`,
              )
            }
          >
            {topic.nextClass ? 'Quitar próxima clase' : 'Marcar como próxima clase'}
          </button>
          <button type="button" className="button button--secondary button--small" onClick={() => setEditing(!editing)}>
            {editing ? 'Cancelar edición' : 'Editar tema'}
          </button>
          <button
            type="button"
            className="button button--danger button--small"
            onClick={() =>
              window.confirm(`¿Archivar el tema "${topic.title}"? Dejará de mostrarse con sus materiales.`) &&
              run(() => apiDelete(`/topics/${topic.id}`), 'Tema archivado.')
            }
          >
            Archivar tema
          </button>
        </div>
      )}
      {editing && <EditTopic topic={topic} onSaved={() => setEditing(false)} run={run} />}

      {topic.materials.length === 0 ? (
        <p className="page__note">Sin materiales.</p>
      ) : (
        <ul className="materials">
          {topic.materials.map((material, index) => (
            <MaterialRow
              key={material.id}
              material={material}
              run={run}
              isFirst={index === 0}
              isLast={index === topic.materials.length - 1}
              onMove={(delta) => moveMaterial(index, delta)}
            />
          ))}
        </ul>
      )}

      {canManage && <NewMaterial topic={topic} onCreated={reload} />}
    </section>
  )
}

function MaterialRow({ material, run, isFirst, isLast, onMove }) {
  const otherVisibility = material.visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC'
  return (
    <li className="material">
      <div className="material__body">
        <h3 className="material__title">{material.title}</h3>
        <p className="material__meta">
          {KIND_LABELS[material.kind]}
          {material.file && ` · ${material.file.format} · ${formatSize(material.file.sizeBytes)}`}
          <span className={material.visibility === 'PRIVATE' ? 'badge badge--private' : 'badge'}>
            {VISIBILITY_LABELS[material.visibility]}
          </span>
        </p>
        {material.description && <p className="material__description">{material.description}</p>}
      </div>
      <div className="material__actions">
        <MaterialLinks material={material} />
        {material.canEdit && (
          <>
            <button
              type="button"
              className="button button--secondary button--small"
              disabled={isFirst}
              onClick={() => onMove(-1)}
              aria-label={`Subir ${material.title}`}
            >
              Subir
            </button>
            <button
              type="button"
              className="button button--secondary button--small"
              disabled={isLast}
              onClick={() => onMove(1)}
              aria-label={`Bajar ${material.title}`}
            >
              Bajar
            </button>
          </>
        )}
        {material.canModerate && (
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
        )}
      </div>
    </li>
  )
}

function EditTopic({ topic, onSaved, run }) {
  function submit(event) {
    event.preventDefault()
    const { title, description, tool } = formValues(event)
    run(() => apiPut(`/topics/${topic.id}`, { title, description: description || null, tool }), 'Tema actualizado.').then(
      (saved) => saved && onSaved(),
    )
  }
  return (
    <form onSubmit={submit} noValidate className="subform">
      <Field id={`tema-${topic.id}-titulo-campo`} label="Título" name="title" defaultValue={topic.title} required maxLength={150} />
      <Field
        id={`tema-${topic.id}-descripcion`}
        label="Descripción"
        name="description"
        defaultValue={topic.description ?? ''}
        maxLength={2000}
      />
      <ToolSelect id={`tema-${topic.id}-herramienta`} defaultValue={topic.tool} />
      <button type="submit" className="button">
        Guardar tema
      </button>
    </form>
  )
}

function NewTopic({ courseId, onCreated }) {
  const create = useSubmit(async (event) => {
    const form = event.currentTarget
    const { title, description, tool } = formValues(event)
    const result = await apiPost(`/courses/${courseId}/topics`, { title, description: description || null, tool })
    form.reset()
    onCreated()
    return result
  })
  return (
    <section className="card card--wide" aria-labelledby="nuevo-tema-titulo">
      <h2 id="nuevo-tema-titulo">Nuevo tema</h2>
      <form onSubmit={create.submit} noValidate>
        <Field label="Título" name="title" required maxLength={150} error={create.fieldErrors.title} />
        <Field label="Descripción (opcional)" name="description" maxLength={2000} />
        <ToolSelect id="nuevo-tema-herramienta" defaultValue="PSEINT" />
        <button type="submit" className="button" disabled={create.pending}>
          Crear tema
        </button>
      </form>
      <FormStatus error={create.error} success={create.result && `Tema "${create.result.title}" creado.`} />
    </section>
  )
}

function ToolSelect({ id, defaultValue }) {
  return (
    <div className="field">
      <label htmlFor={id}>Herramienta</label>
      <select id={id} name="tool" defaultValue={defaultValue}>
        {Object.entries(TOOL_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>
    </div>
  )
}

function NewMaterial({ topic, onCreated }) {
  const [kind, setKind] = useState('GUIDE')
  const [progress, setProgress] = useState(null)
  const prefix = `material-${topic.id}`

  const create = useSubmit(async (event) => {
    const form = event.currentTarget
    const data = new FormData(form)
    if (kind === 'LINK') data.delete('file')
    else {
      data.delete('url')
      const file = data.get('file')
      if (!file || !file.name) throw new Error('Selecciona el archivo del material.')
    }
    setProgress(kind === 'LINK' ? null : 0)
    try {
      const result = await apiSendForm(`/topics/${topic.id}/materials`, data, { onProgress: setProgress })
      form.reset()
      setKind('GUIDE')
      onCreated()
      return result
    } finally {
      setProgress(null)
    }
  })

  return (
    <details className="subform">
      <summary>Agregar material a este tema</summary>
      <form onSubmit={create.submit} noValidate>
        <Field id={`${prefix}-title`} label="Título" name="title" required maxLength={150} />
        <Field id={`${prefix}-description`} label="Descripción (opcional)" name="description" maxLength={2000} />
        <div className="form-row">
          <div className="field">
            <label htmlFor={`${prefix}-kind`}>Tipo</label>
            <select id={`${prefix}-kind`} name="kind" value={kind} onChange={(event) => setKind(event.target.value)}>
              {Object.entries(KIND_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor={`${prefix}-visibility`}>Visibilidad</label>
            <select id={`${prefix}-visibility`} name="visibility" defaultValue="PRIVATE">
              <option value="PRIVATE">Privado (solo el curso)</option>
              <option value="PUBLIC">Público (biblioteca abierta)</option>
            </select>
          </div>
        </div>
        {kind === 'LINK' ? (
          <Field id={`${prefix}-url`} label="Dirección del enlace" name="url" type="url" placeholder="https://" required />
        ) : (
          <div className="field">
            <label htmlFor={`${prefix}-file`}>Archivo</label>
            <input id={`${prefix}-file`} type="file" name="file" accept={KIND_ACCEPT[kind]} aria-describedby={`${prefix}-file-ayuda`} />
            <small id={`${prefix}-file-ayuda`} className="field__hint">
              Formatos: {KIND_ACCEPT[kind].replaceAll(',', ', ')}. Máximo {kind === 'VIDEO' ? '1 GB' : '500 MB'}.
            </small>
          </div>
        )}
        {progress != null && (
          <p role="status">
            <progress max="100" value={progress} aria-label="Avance de la carga" /> {progress} %
          </p>
        )}
        <button type="submit" className="button" disabled={create.pending}>
          {create.pending ? 'Subiendo…' : 'Agregar material'}
        </button>
      </form>
      <FormStatus error={create.error} success={create.result && `Material "${create.result.title}" agregado.`} />
    </details>
  )
}
