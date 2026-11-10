import { useRef, useState } from 'react'
import { apiPost } from '../../shared/api/httpClient.js'

const NEEDS_INPUT = /faltan datos de entrada/
const MAX_READS = 200

// Corre un programa de PSeInt en el servidor como si la consola fuera interactiva. El servidor necesita todas las entradas
// de una vez, así que cuando el programa llega a un Leer sin dato se muestra lo que alcanzó a escribir, la consola pide el
// dato y se vuelve a ejecutar con todos los datos dados. El intérprete es determinista (AZAR tiene semilla fija), así que
// cada ejecución repite la salida anterior y sigue desde ahí. onDone recibe el resultado final.
export function useConsole(code, onDone) {
  const [state, setState] = useState({ status: 'idle', reads: [], result: null, error: null })
  const turn = useRef(0) // descarta la respuesta de una ejecución que se detuvo o se reemplazó

  async function execute(reads, fresh) {
    const mine = ++turn.current
    setState((current) => ({ ...current, ...(fresh && { result: null }), status: 'running', reads, error: null }))
    try {
      const result = await apiPost('/pseint/runs', { code, input: reads.map((read) => read.value).join('\n') })
      if (mine !== turn.current) return
      const waiting = result.errorKind === 'RUNTIME' && NEEDS_INPUT.test(result.error ?? '') && reads.length < MAX_READS
      setState({ status: waiting ? 'waiting' : 'done', reads, result, error: null })
      if (!waiting) onDone?.(result)
    } catch (error) {
      if (mine !== turn.current) return
      setState({ status: 'idle', reads, result: null, error: error instanceof TypeError ? 'No fue posible conectar con el servidor. Intenta de nuevo.' : error.message })
    }
  }

  return {
    ...state,
    busy: state.status === 'running' || state.status === 'waiting',
    // problem: error de compilación ya conocido (DFD con un campo vacío); se informa sin llamar al servidor.
    start: (problem) => {
      if (problem) {
        turn.current += 1
        const result = { output: '', completed: false, errorKind: 'SYNTAX', error: problem, steps: 0, durationMs: 0 }
        setState({ status: 'done', reads: [], result, error: null })
        onDone?.(result)
      } else if (!code.trim()) setState({ status: 'idle', reads: [], result: null, error: 'Escribe el programa antes de ejecutarlo.' })
      else execute([], true)
    },
    answer: (value) => execute([...state.reads, { value, at: state.result.output.length }]),
    stop: () => {
      turn.current += 1
      setState((current) => ({ ...current, status: current.result ? 'stopped' : 'idle' }))
    },
  }
}

export function statusText(run) {
  const result = run.result
  return {
    idle: 'Intérprete listo',
    running: 'Ejecutando…',
    waiting: 'Esperando un dato en la consola',
    done: result && !result.errorKind ? `Ejecución exitosa · ${result.steps} pasos · ${result.durationMs} ms` : 'El programa se detuvo por un error',
    stopped: 'Ejecución detenida',
  }[run.status]
}
