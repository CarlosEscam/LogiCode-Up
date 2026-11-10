import { useRef, useState } from 'react'

// Práctica libre (Emulador): nada se envía al servidor ni cuenta como entrega. El trabajo se guarda solo en el equipo:
// un borrador en el navegador mientras se escribe y, si el estudiante quiere, un archivo descargado.

const MAX_OPEN_BYTES = 512 * 1024

function download(fileName, text) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

// Barra de archivo: nombre, guardar en el equipo, abrir un archivo guardado y empezar de nuevo.
// onOpen recibe el texto del archivo y devuelve un mensaje de error si no lo pudo usar.
export default function FileBar({ idPrefix, extension, getText, onOpen, onNew, defaultName }) {
  const [name, setName] = useState(defaultName)
  const [message, setMessage] = useState(null)
  const input = useRef(null)
  const fileName = `${(name.trim() || defaultName).replace(/[\\/:*?"<>|]+/g, '-')}${extension}`

  async function open(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (file.size > MAX_OPEN_BYTES) {
      setMessage({ error: true, text: 'El archivo es demasiado grande (máximo 512 KB).' })
      return
    }
    const problem = onOpen(await file.text())
    if (problem) {
      setMessage({ error: true, text: problem })
      return
    }
    setName(file.name.replace(/\.[^.]+(\.json)?$/i, ''))
    setMessage({ error: false, text: `Se abrió ${file.name}.` })
  }

  return (
    <div className="file-bar">
      <div className="field">
        <label htmlFor={`${idPrefix}-archivo`}>Nombre del archivo</label>
        <input id={`${idPrefix}-archivo`} value={name} maxLength={60} onChange={(event) => setName(event.target.value)} />
      </div>
      <div className="toolbar">
        <button
          type="button"
          className="button button--secondary button--small"
          onClick={() => {
            download(fileName, getText())
            setMessage({ error: false, text: `Se descargó ${fileName} en tu equipo.` })
          }}
        >
          Guardar en mi equipo
        </button>
        <button type="button" className="button button--secondary button--small" onClick={() => input.current.click()}>
          Abrir archivo…
        </button>
        <input ref={input} type="file" accept={extension === '.json' ? '.json' : `${extension},.txt`} hidden onChange={open} aria-label="Archivo para abrir" />
        <button
          type="button"
          className="button button--secondary button--small"
          onClick={() => {
            if (window.confirm('¿Empezar de nuevo? Se borra lo que hay en el editor (guárdalo antes si lo quieres).')) {
              onNew()
              setMessage(null)
            }
          }}
        >
          Nuevo
        </button>
      </div>
      {message && (
        <p className={`page__note${message.error ? ' field__error' : ''}`} role="status">
          {message.text}
        </p>
      )}
    </div>
  )
}
