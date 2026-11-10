import { apiPost } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'
import { formatDateTime } from './labels.js'

// Calibración de la IA del curso (T-90), opcional: mientras está abierta, la IA propone y el docente confirma; LogiCode
// muestra cuánto coinciden para decidir cuándo dejar que la IA publique sola con la regla del 80 %.
export default function CalibrationPanel({ courseId }) {
  const status = useQuery(`/courses/${courseId}/ai-calibration`)
  const action = (name) => async () => {
    const result = await apiPost(`/courses/${courseId}/ai-calibration/${name}`)
    status.reload()
    return result
  }
  const close = useSubmit(action('close'))
  const reopen = useSubmit(action('reopen'))
  const c = status.data
  if (!c) return <FormStatus error={status.error} />
  const missing = Math.max(0, c.minimum - c.compared)
  return (
    <section className="card card--wide" aria-labelledby="calibracion-titulo">
      <h2 id="calibracion-titulo">Calibración de la IA</h2>
      {c.calibrating ? (
        <p>
          <span className="badge badge--warning">En calibración</span> La IA propone y tú confirmas cada nota. Cuando
          tenga suficientes comparaciones contigo, puedes dejar que publique sola las notas con confianza de 80 % o más.
        </p>
      ) : (
        <p>
          <span className="badge badge--ok">La IA publica sola</span>{' '}
          {c.closedAt ? `Desde ${formatDateTime(c.closedAt)}, la IA publica` : 'La IA publica'} las notas legibles con
          confianza de 80 % o más; las demás llegan a Por revisar. Si quieres comparar sus notas con las tuyas antes de
          confiar, enciende la calibración.
        </p>
      )}
      <dl className="facts">
        <div>
          <dt>Notas comparadas</dt>
          <dd>{c.compared} de {c.minimum} necesarias</dd>
        </div>
        <div>
          <dt>Coinciden contigo (±0,5)</dt>
          <dd>{c.concordancePercent == null ? '—' : `${c.concordancePercent} % (${c.agreeing})`}</dd>
        </div>
        <div>
          <dt>Se habrían publicado mal</dt>
          <dd>{c.wouldHaveFailed}</dd>
        </div>
      </dl>
      {c.canChange && c.calibrating && (
        <>
          {/* El motivo del botón deshabilitado es su descripción accesible (II-04, D-2). */}
          <button
            type="button"
            className="button"
            disabled={close.pending || missing > 0}
            aria-describedby={missing > 0 ? 'calibracion-faltan' : undefined}
            onClick={() => close.submit()}
          >
            Dejar que la IA publique sola
          </button>
          {missing > 0 && <p id="calibracion-faltan" className="muted">Faltan {missing} notas comparadas para poder cerrar la calibración.</p>}
        </>
      )}
      {c.canChange && !c.calibrating && (
        <button type="button" className="button button--secondary" disabled={reopen.pending} onClick={() => reopen.submit()}>
          {c.closedAt ? 'Volver a calibrar' : 'Calibrar la IA'}
        </button>
      )}
      <FormStatus error={close.error ?? reopen.error} />
    </section>
  )
}
