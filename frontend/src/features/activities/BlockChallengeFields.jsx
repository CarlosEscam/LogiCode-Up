import { useEffect, useState } from 'react'
import BlocksGame from '../games/BlocksGame.jsx'
import { BLOCK_LABELS, boardProblem } from '../games/blocks.js'

const FACING_LABELS = { E: 'Derecha', S: 'Abajo', O: 'Izquierda', N: 'Arriba' }
const EXAMPLE = ['S..#', '##.#', '##.M']


// Reto de bloques de la rúbrica (RF-12). value es el reto guardado o null; onChange recibe el reto o null.
export default function BlockChallengeFields({ value, onChange }) {
  const [enabled, setEnabled] = useState(Boolean(value))
  const [gridText, setGridText] = useState((value?.grid ?? EXAMPLE).join('\n'))
  const [facing, setFacing] = useState(value?.facing ?? 'E')
  const [maxBlocks, setMaxBlocks] = useState(value?.maxBlocks ?? 8)
  const [allowed, setAllowed] = useState(value?.allowed ?? ['avanzar', 'izquierda', 'derecha'])
  const [useStart, setUseStart] = useState((value?.start ?? []).length > 0)
  const [program, setProgram] = useState(value?.start ?? [])

  const rows = gridText.split('\n').map((row) => row.trim()).filter(Boolean)
  const problem = boardProblem(rows)
  const level = { grid: rows, facing, maxBlocks: Number(maxBlocks) || 1, allowed, start: value?.start ?? [] }

  useEffect(() => {
    const grid = gridText.split('\n').map((row) => row.trim()).filter(Boolean)
    onChange(enabled ? { grid, facing, maxBlocks: Number(maxBlocks), allowed, start: useStart ? program : [] } : null)
  }, [enabled, gridText, facing, maxBlocks, allowed, useStart, program, onChange])

  return (
    <fieldset className="choices">
      <legend>Reto de bloques (opcional)</legend>
      <label className="choice">
        <input
          type="checkbox"
          checked={enabled}
          onChange={(event) => setEnabled(event.target.checked)}
        />{' '}
        Calificar con un reto de bloques: el estudiante arma el programa en la actividad y saca 5,0 si el robot llega a la meta
        (0,0 si no). La actividad debe recibir entregas en texto.
      </label>
      {enabled && (
        <>
          <div className="field">
            <label htmlFor="reto-tablero">Tablero (una fila por línea)</label>
            <textarea
              id="reto-tablero"
              className="code"
              rows={Math.max(3, rows.length + 1)}
              value={gridText}
              aria-describedby="reto-tablero-ayuda"
              onChange={(event) => setGridText(event.target.value)}
            />
            <small id="reto-tablero-ayuda" className="field__hint">
              # muro, . libre, S salida del robot, M meta. Hasta 12 × 12.
            </small>
          </div>
          <div className="field field--short">
            <label htmlFor="reto-mirando">El robot empieza mirando</label>
            <select
              id="reto-mirando"
              value={facing}
              onChange={(event) => setFacing(event.target.value)}
            >
              {Object.entries(FACING_LABELS).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="field field--short">
            <label htmlFor="reto-maximo">Máximo de bloques</label>
            <input
              id="reto-maximo"
              type="number"
              min="1"
              max="40"
              value={maxBlocks}
              onChange={(event) => setMaxBlocks(event.target.value)}
            />
          </div>
          <fieldset className="choices">
            <legend>Bloques que puede usar</legend>
            {Object.entries(BLOCK_LABELS).map(([type, label]) => (
              <label key={type} className="choice">
                <input
                  type="checkbox"
                  checked={allowed.includes(type)}
                  onChange={(event) => setAllowed(event.target.checked ? [...allowed, type] : allowed.filter((item) => item !== type))}
                />{' '}
                {label}
              </label>
            ))}
          </fieldset>
          {problem || allowed.length === 0 ? (
            <p className="notice notice--error" role="status">
              {problem ?? 'Elige por lo menos un bloque.'}
            </p>
          ) : (
            <>
              <h3>Vista previa</h3>
              <p className="page__note">Pruébalo como lo verá el estudiante. Para un reto de depuración, arma aquí el programa con el error.</p>
              <BlocksGame
                key={JSON.stringify([rows, facing, level.maxBlocks, allowed])}
                level={level}
                onProgramChange={setProgram}
              />
              <label className="choice">
                <input
                  type="checkbox"
                  checked={useStart}
                  onChange={(event) => setUseStart(event.target.checked)}
                />{' '}
                El estudiante empieza con el programa de la vista previa (reto de depuración)
              </label>
            </>
          )}
        </>
      )}
    </fieldset>
  )
}
