import { useState } from 'react'
import { Field } from '../../shared/ui/forms.jsx'
import { ACTIVITY_KIND_LABELS, FILE_GROUP_LABELS, toBogotaInput } from './labels.js'

// Campos de una actividad, para crearla o editarla (RF-10, RF-11). El backend valida de nuevo
// fechas, formatos, intentos y que los porcentajes del curso no superen 100 % (RN-07).
export function ActivityFields({ prefix, activity }) {
  const [acceptsFiles, setAcceptsFiles] = useState(activity?.acceptsFiles ?? false)
  const groups = new Set(activity?.fileGroups ?? ['DOCUMENTS'])
  return (
    <>
      <Field id={`${prefix}-title`} label="Título" name="title" defaultValue={activity?.title} required maxLength={150} />
      <div className="field">
        <label htmlFor={`${prefix}-statement`}>Enunciado</label>
        <textarea id={`${prefix}-statement`} name="statement" rows={5} required maxLength={20000} defaultValue={activity?.statement} />
      </div>
      <div className="form-row">
        <div className="field">
          <label htmlFor={`${prefix}-kind`}>Tipo</label>
          <select id={`${prefix}-kind`} name="kind" defaultValue={activity?.kind ?? 'TASK'}>
            {Object.entries(ACTIVITY_KIND_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <Field
          id={`${prefix}-weight`}
          label="Porcentaje de la nota (opcional)"
          name="weightPercent"
          type="number"
          min="0.01"
          max="100"
          step="0.01"
          defaultValue={activity?.weightPercent ?? ''}
        />
        <Field
          id={`${prefix}-attempts`}
          label="Intentos"
          name="maxAttempts"
          type="number"
          min="1"
          max="10"
          required
          defaultValue={activity?.maxAttempts ?? 1}
        />
      </div>
      <div className="form-row">
        <Field
          id={`${prefix}-opens`}
          label="Apertura (hora de Colombia)"
          name="opensAt"
          type="datetime-local"
          required
          defaultValue={toBogotaInput(activity?.opensAt)}
        />
        <Field
          id={`${prefix}-closes`}
          label="Cierre (hora de Colombia)"
          name="closesAt"
          type="datetime-local"
          required
          defaultValue={toBogotaInput(activity?.closesAt)}
        />
      </div>
      <fieldset className="choices">
        <legend>Formas de entrega</legend>
        <label>
          <input type="checkbox" name="acceptsText" defaultChecked={activity?.acceptsText ?? true} /> Texto
        </label>
        <label>
          <input
            type="checkbox"
            name="acceptsFiles"
            checked={acceptsFiles}
            onChange={(event) => setAcceptsFiles(event.target.checked)}
          />{' '}
          Archivos
        </label>
        <label>
          <input type="checkbox" name="acceptsPhotos" defaultChecked={activity?.acceptsPhotos ?? false} /> Fotos del
          trabajo hecho a mano
        </label>
      </fieldset>
      {acceptsFiles && (
        <fieldset className="choices">
          <legend>Tipos de archivo aceptados</legend>
          {Object.entries(FILE_GROUP_LABELS).map(([value, label]) => (
            <label key={value}>
              <input type="checkbox" name="fileGroups" value={value} defaultChecked={groups.has(value)} /> {label}
            </label>
          ))}
        </fieldset>
      )}
    </>
  )
}
