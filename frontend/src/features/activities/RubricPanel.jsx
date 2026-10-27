import { useState } from 'react'
import { apiPut } from '../../shared/api/httpClient.js'
import { useQuery } from '../../shared/api/usePagedQuery.js'
import { FormStatus } from '../../shared/ui/forms.jsx'
import { useSubmit } from '../../shared/ui/useSubmit.js'

// Rúbrica vigente (RN-20). Guardarla crea una versión nueva; las entregas ya enviadas conservan
// la versión con la que se evalúan. El estudiante ve los criterios, no la respuesta esperada.
export default function RubricPanel({ activity }) {
  const rubric = useQuery(`/activities/${activity.id}/rubric`)
  const [editing, setEditing] = useState(false)
  const data = rubric.data

  return (
    <section className="card card--wide" aria-labelledby="rubrica-titulo">
      <div className="topic__header">
        <h2 id="rubrica-titulo">Rúbrica</h2>
        {data && <span className="badge">Versión {data.number}</span>}
      </div>
      <FormStatus error={rubric.error} />
      {rubric.status === 'ready' && !data && (
        <p>{activity.canManage ? 'Define la rúbrica para poder publicar la actividad.' : 'La actividad aún no tiene rúbrica.'}</p>
      )}
      {data && <RubricTable rubric={data} />}
      {activity.canManage && rubric.status !== 'loading' && (
        <>
          <button type="button" className="button button--secondary button--small" onClick={() => setEditing(!editing)}>
            {editing ? 'Cancelar' : data ? 'Editar rúbrica' : 'Definir rúbrica'}
          </button>
          {editing && (
            <RubricEditor
              activityId={activity.id}
              rubric={data}
              onSaved={() => {
                setEditing(false)
                rubric.reload()
              }}
            />
          )}
        </>
      )}
    </section>
  )
}

export function RubricTable({ rubric }) {
  return (
    <>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">Criterio</th>
              <th scope="col">Puntos</th>
            </tr>
          </thead>
          <tbody>
            {rubric.criteria.map((criterion) => (
              <tr key={criterion.name}>
                <td>
                  <strong>{criterion.name}</strong>
                  {criterion.description && <div className="page__note">{criterion.description}</div>}
                </td>
                <td>{points(criterion.maxPoints)}</td>
              </tr>
            ))}
            <tr>
              <th scope="row">Total</th>
              <td>{points(rubric.totalPoints)}</td>
            </tr>
          </tbody>
        </table>
      </div>
      {rubric.expectedAnswer && (
        <p>
          <strong>Respuesta esperada:</strong> <span className="statement">{rubric.expectedAnswer}</span>
        </p>
      )}
      {rubric.testCaseCount > 0 && !rubric.testCases && (
        <p className="page__note">
          Se califica sola con {rubric.testCaseCount} casos de prueba de PSeInt: la nota es la parte de casos que tu programa resuelve.
        </p>
      )}
      {rubric.testCases?.length > 0 && <TestCaseTable cases={rubric.testCases} />}
    </>
  )
}

function TestCaseTable({ cases }) {
  return (
    <div className="table-wrap">
      <table>
        <caption>Casos de prueba (la nota es 5 × casos aprobados / total)</caption>
        <thead>
          <tr>
            <th scope="col">Caso</th>
            <th scope="col">Entradas</th>
            <th scope="col">Salida esperada</th>
          </tr>
        </thead>
        <tbody>
          {cases.map((testCase, index) => (
            <tr key={index}>
              <td>{index + 1}</td>
              <td>
                <pre className="console">{testCase.input || '(ninguna)'}</pre>
              </td>
              <td>
                <pre className="console">{testCase.expectedOutput}</pre>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function points(value) {
  return String(Number(value)).replace('.', ',')
}

let nextKey = 0

function RubricEditor({ activityId, rubric, onSaved }) {
  const [criteria, setCriteria] = useState(() =>
    (rubric?.criteria ?? [{ name: '', description: '', maxPoints: 5 }]).map((criterion) => ({ ...criterion, key: nextKey++ })),
  )
  const [cases, setCases] = useState(() => (rubric?.testCases ?? []).map((testCase) => ({ ...testCase, key: nextKey++ })))

  function updateCase(key, field, value) {
    setCases((current) => current.map((testCase) => (testCase.key === key ? { ...testCase, [field]: value } : testCase)))
  }

  function update(key, field, value) {
    setCriteria((current) => current.map((criterion) => (criterion.key === key ? { ...criterion, [field]: value } : criterion)))
  }

  const save = useSubmit(async (event) => {
    const expectedAnswer = new FormData(event.currentTarget).get('expectedAnswer')
    const result = await apiPut(`/activities/${activityId}/rubric`, {
      expectedAnswer: expectedAnswer || null,
      criteria: criteria.map(({ name, description, maxPoints }) => ({
        name,
        description: description || null,
        maxPoints: maxPoints === '' ? null : Number(maxPoints),
      })),
      testCases: cases.map(({ input, expectedOutput }) => ({ input, expectedOutput })),
    })
    onSaved()
    return result
  })

  return (
    <form onSubmit={save.submit} noValidate className="subform">
      <div className="field">
        <label htmlFor="rubrica-respuesta">Respuesta esperada (solo la ven el docente y los Administradores)</label>
        <textarea id="rubrica-respuesta" name="expectedAnswer" rows={4} maxLength={20000} defaultValue={rubric?.expectedAnswer ?? ''} />
      </div>
      <fieldset className="choices">
        <legend>Criterios</legend>
        {criteria.map((criterion, index) => (
          <div key={criterion.key} className="criterion">
            <div className="field">
              <label htmlFor={`criterio-${criterion.key}-nombre`}>Criterio {index + 1}</label>
              <input
                id={`criterio-${criterion.key}-nombre`}
                value={criterion.name}
                maxLength={150}
                required
                onChange={(event) => update(criterion.key, 'name', event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor={`criterio-${criterion.key}-descripcion`}>Descripción (opcional)</label>
              <input
                id={`criterio-${criterion.key}-descripcion`}
                value={criterion.description ?? ''}
                maxLength={2000}
                onChange={(event) => update(criterion.key, 'description', event.target.value)}
              />
            </div>
            <div className="field field--short">
              <label htmlFor={`criterio-${criterion.key}-puntos`}>Puntos</label>
              <input
                id={`criterio-${criterion.key}-puntos`}
                type="number"
                min="0.01"
                step="0.01"
                value={criterion.maxPoints}
                onChange={(event) => update(criterion.key, 'maxPoints', event.target.value)}
              />
            </div>
            <button
              type="button"
              className="button button--danger button--small"
              disabled={criteria.length === 1}
              aria-label={`Quitar criterio ${index + 1}`}
              onClick={() => setCriteria((current) => current.filter((item) => item.key !== criterion.key))}
            >
              Quitar
            </button>
          </div>
        ))}
        <button
          type="button"
          className="button button--secondary button--small"
          disabled={criteria.length >= 20}
          onClick={() => setCriteria((current) => [...current, { name: '', description: '', maxPoints: 1, key: nextKey++ }])}
        >
          Agregar criterio
        </button>
      </fieldset>
      <fieldset className="choices">
        <legend>Casos de prueba de PSeInt (opcional)</legend>
        <p className="page__note">
          Con casos de prueba, cada entrega con un programa de PSeInt (respuesta escrita o archivo .psc) se califica sola: la nota es 5 × casos
          aprobados / total. Cada línea esperada debe aparecer en la salida, en orden; no importan mayúsculas ni espacios de más. Si el
          programa tiene un error de sintaxis, la entrega pasa a tu revisión.
        </p>
        {cases.map((testCase, index) => (
          <div key={testCase.key} className="criterion">
            <div className="field">
              <label htmlFor={`caso-${testCase.key}-entrada`}>Caso {index + 1}: entradas (una por línea)</label>
              <textarea
                id={`caso-${testCase.key}-entrada`}
                className="code"
                rows={3}
                maxLength={5000}
                value={testCase.input ?? ''}
                onChange={(event) => updateCase(testCase.key, 'input', event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor={`caso-${testCase.key}-salida`}>Salida esperada</label>
              <textarea
                id={`caso-${testCase.key}-salida`}
                className="code"
                rows={3}
                maxLength={5000}
                required
                value={testCase.expectedOutput}
                onChange={(event) => updateCase(testCase.key, 'expectedOutput', event.target.value)}
              />
            </div>
            <button
              type="button"
              className="button button--danger button--small"
              aria-label={`Quitar caso ${index + 1}`}
              onClick={() => setCases((current) => current.filter((item) => item.key !== testCase.key))}
            >
              Quitar
            </button>
          </div>
        ))}
        <button
          type="button"
          className="button button--secondary button--small"
          disabled={cases.length >= 20}
          onClick={() => setCases((current) => [...current, { input: '', expectedOutput: '', key: nextKey++ }])}
        >
          Agregar caso de prueba
        </button>
      </fieldset>
      <button type="submit" className="button" disabled={save.pending}>
        Guardar rúbrica
      </button>
      <FormStatus error={save.error} />
    </form>
  )
}
