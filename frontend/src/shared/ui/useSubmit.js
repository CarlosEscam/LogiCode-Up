import { useState } from 'react'
import { ApiError } from '../api/httpClient.js'

// Envuelve el envío de un formulario: estado de carga, mensaje de éxito y errores de la API.
export function useSubmit(action) {
  const [state, setState] = useState({ pending: false, error: null, fieldErrors: {}, result: null })

  async function submit(event) {
    event?.preventDefault()
    setState({ pending: true, error: null, fieldErrors: {}, result: null })
    try {
      const result = await action(event)
      setState({ pending: false, error: null, fieldErrors: {}, result })
      return result
    } catch (error) {
      // TypeError = fallo de red de fetch; otros errores son validaciones propias del formulario.
      const message =
        error instanceof TypeError ? 'No fue posible conectar con el servidor. Intenta de nuevo.' : error.message
      setState({
        pending: false,
        error: message,
        fieldErrors: error instanceof ApiError ? error.fieldErrors : {},
        result: null,
      })
      return undefined
    }
  }

  return { ...state, submit }
}

// Datos del formulario como objeto plano.
export function formValues(event) {
  return Object.fromEntries(new FormData(event.currentTarget))
}

export const PASSWORD_HINT = 'Mínimo 8 caracteres, con al menos un número y un carácter especial.'
