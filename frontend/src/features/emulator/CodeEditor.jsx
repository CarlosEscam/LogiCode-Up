import { useRef } from 'react'

// Editor de texto con números de línea y la línea del error resaltada. Tab escribe cuatro espacios; Esc y luego Tab
// sale del editor (así el teclado nunca queda atrapado). Highlight (opcional) pinta los colores de sintaxis debajo del
// área de texto, que es transparente; onCursor recibe la línea y la columna del cursor.
export default function CodeEditor({ id, label, value, onChange, errorLine, rows = 18, Highlight, onCursor, hideLabel = false }) {
  const gutter = useRef(null)
  const colors = useRef(null)
  const escaped = useRef(false)
  const count = value.split('\n').length

  function keyDown(event) {
    if (event.key === 'Escape') {
      escaped.current = true
      return
    }
    if (event.key === 'Tab' && !event.shiftKey && !escaped.current) {
      event.preventDefault()
      const area = event.currentTarget
      const { selectionStart: start, selectionEnd: end } = area
      onChange(`${value.slice(0, start)}    ${value.slice(end)}`)
      requestAnimationFrame(() => area.setSelectionRange(start + 4, start + 4))
      return
    }
    escaped.current = false
  }

  function cursor(event) {
    if (!onCursor) return
    const before = event.currentTarget.value.slice(0, event.currentTarget.selectionStart).split('\n')
    onCursor(before.length, before.at(-1).length + 1)
  }

  return (
    <div className="field code-editor-field">
      <label htmlFor={id} className={hideLabel ? 'visually-hidden' : undefined}>
        {label}
      </label>
      {/* Alto de las filas pedidas (línea de 1,35 rem) más el relleno: el texto se desplaza por dentro. */}
      <div className={`code-editor${Highlight ? ' code-editor--colors' : ''}`} style={{ height: `${rows * 1.35 + 1.25}rem` }}>
        <pre ref={gutter} className="code-editor__gutter" aria-hidden="true">
          {Array.from({ length: count }, (_, index) => (
            <span key={index} className={index + 1 === errorLine ? 'code-editor__error' : undefined}>
              {index + 1}
            </span>
          ))}
        </pre>
        <div className="code-editor__body">
          {Highlight && (
            <pre ref={colors} className="code code-editor__colors" aria-hidden="true">
              <Highlight text={value} />
              {'\n'}
            </pre>
          )}
          <textarea
            id={id}
            rows={rows}
            maxLength={20000}
            spellCheck={false}
            autoCapitalize="off"
            autoComplete="off"
            wrap="off"
            className="code code-editor__text"
            value={value}
            aria-describedby={`${id}-ayuda`}
            onChange={(event) => {
              onChange(event.target.value)
              cursor(event)
            }}
            onKeyDown={keyDown}
            onSelect={cursor}
            onScroll={(event) => {
              gutter.current.scrollTop = event.currentTarget.scrollTop
              if (colors.current) {
                colors.current.scrollTop = event.currentTarget.scrollTop
                colors.current.scrollLeft = event.currentTarget.scrollLeft
              }
            }}
          />
        </div>
      </div>
      <small id={`${id}-ayuda`} className="field__hint">
        Tab escribe una sangría; para salir del editor con el teclado pulsa Esc y luego Tab.
        {errorLine ? ` El error está en la línea ${errorLine}.` : ''}
      </small>
    </div>
  )
}
