import { useRef } from 'react'

// Editor de texto con números de línea y la línea del error resaltada. Tab escribe cuatro espacios; Esc y luego Tab
// sale del editor (así el teclado nunca queda atrapado).
export default function CodeEditor({ id, label, value, onChange, errorLine, rows = 18 }) {
  const gutter = useRef(null)
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

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="code-editor">
        <pre ref={gutter} className="code-editor__gutter" aria-hidden="true">
          {Array.from({ length: count }, (_, index) => (
            <span key={index} className={index + 1 === errorLine ? 'code-editor__error' : undefined}>
              {index + 1}
            </span>
          ))}
        </pre>
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
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={keyDown}
          onScroll={(event) => {
            gutter.current.scrollTop = event.currentTarget.scrollTop
          }}
        />
      </div>
      <small id={`${id}-ayuda`} className="field__hint">
        Tab escribe una sangría; para salir del editor con el teclado pulsa Esc y luego Tab.
        {errorLine ? ` El error está en la línea ${errorLine}.` : ''}
      </small>
    </div>
  )
}
