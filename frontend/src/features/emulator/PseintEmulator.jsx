import { useState } from 'react'
import { errorLineOf } from '../pseint/errorLine.js'
import CodeEditor from './CodeEditor.jsx'
import FileBar from './FileBar.jsx'
import PseintHighlight from './highlight.jsx'
import RunConsole from './RunConsole.jsx'
import { useDraft } from './storage.js'
import { statusText, useConsole } from './useConsole.js'

const EXAMPLES = {
  saludo: `Algoritmo Saludo
    Definir nombre Como Caracter
    Escribir "¿Cómo te llamas?"
    Leer nombre
    Escribir "Hola, ", nombre
FinAlgoritmo`,
  promedio: `Algoritmo Promedio
    Definir n, i Como Entero
    Definir nota, suma Como Real
    Leer n
    suma <- 0
    Para i <- 1 Hasta n Hacer
        Leer nota
        suma <- suma + nota
    FinPara
    Escribir "Promedio: ", suma / n
FinAlgoritmo`,
  mayor: `Algoritmo Mayor
    Definir a, b Como Entero
    Leer a, b
    Si a > b Entonces
        Escribir a
    SiNo
        Escribir b
    FinSi
FinAlgoritmo`,
  paraCada: `Algoritmo Notas
    Dimension notas[4]
    Para i <- 1 Hasta 4 Hacer
        Leer notas[i]
    FinPara
    suma <- 0
    Para Cada nota De notas Hacer
        suma <- suma + nota
    FinPara
    Escribir "Promedio: ", suma / 4
FinAlgoritmo`,
}

const EXAMPLE_LABELS = {
  saludo: 'Saludo (Leer y Escribir)',
  promedio: 'Promedio de notas (Para)',
  mayor: 'El mayor de dos números (Si)',
  paraCada: 'Promedio de un arreglo (Para Cada)',
}

// Emulador de PSeInt (RF-12) con aspecto de editor: código con colores a la izquierda y consola a la derecha, donde
// Leer pide el dato. Código libre, sin nota ni entrega. El servidor ejecuta con límites de tiempo, pasos, memoria y
// salida; el intérprete no tiene acceso a archivos ni a la red.
export default function PseintEmulator() {
  const [code, setCode] = useDraft('pseint', EXAMPLES.saludo)
  const [errorLine, setErrorLine] = useState(null)
  const [position, setPosition] = useState([1, 1])
  const run = useConsole(code, (result) => setErrorLine(errorLineOf(result.error)))

  const edit = (next) => {
    setCode(next)
    setErrorLine(null)
  }
  const start = () => {
    setErrorLine(null)
    run.start()
  }

  return (
    <>
      <p>
        Escribe tu algoritmo y pulsa <strong>▶ Ejecutar</strong> (o F9). Cuando el programa llegue a un <code>Leer</code>, escribe el dato en
        la consola y pulsa Enter. Es el mismo intérprete que califica las actividades con casos de prueba.
      </p>
      <div
        className="ide"
        onKeyDown={(event) => {
          if (event.key === 'F9' || (event.key === 'Enter' && (event.ctrlKey || event.metaKey))) {
            event.preventDefault()
            if (!run.busy) start()
          }
        }}
      >
        <div className="ide__toolbar">
          <FileBar
            idPrefix="pseint"
            extension=".psc"
            defaultName="mi-algoritmo"
            getText={() => code}
            onOpen={(text) => edit(text)}
            onNew={() => edit('Algoritmo SinTitulo\n    \nFinAlgoritmo\n')}
          />
          <div className="field ide__examples">
            <label htmlFor="pseint-ejemplo">Ejemplos</label>
            <select
              id="pseint-ejemplo"
              value=""
              onChange={(event) => {
                const key = event.target.value
                if (key && (Object.values(EXAMPLES).includes(code) || window.confirm('El ejemplo reemplaza lo que hay en el editor. ¿Continuar?'))) {
                  edit(EXAMPLES[key])
                }
              }}
            >
              <option value="">Cargar…</option>
              {Object.entries(EXAMPLE_LABELS).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="ide__run">
            {run.busy && (
              <button type="button" className="button button--secondary" onClick={run.stop}>
                ■ Detener
              </button>
            )}
            <button type="button" className="button ide__play" disabled={run.status === 'running'} onClick={start}>
              ▶ Ejecutar
            </button>
          </div>
        </div>
        <div className="ide__panes">
          <CodeEditor
            id="pseint-codigo"
            label="Programa (.psc)"
            hideLabel
            value={code}
            onChange={edit}
            errorLine={errorLine}
            rows={20}
            Highlight={PseintHighlight}
            onCursor={(line, column) => setPosition([line, column])}
          />
          <RunConsole run={run} idPrefix="pseint" hint="Pulsa ▶ Ejecutar para correr tu algoritmo." />
        </div>
        <div className="ide__status">
          <span>{statusText(run)}</span>
          <span>
            Línea {position[0]}, Col {position[1]}
          </span>
          <span>PSeInt · LogiCode</span>
        </div>
      </div>
      <details>
        <summary>Qué entiende el intérprete</summary>
        <p>
          Algoritmo/Proceso, Definir, Dimension (posiciones 0 a n), Leer, Escribir (con Sin Saltar), asignación con &lt;- o =, Si,
          Segun, Mientras, Repetir, Para con Paso, Para Cada (recorre un arreglo), SubProceso/Funcion con parámetros por valor o por referencia, operadores Y, O, NO,
          MOD y ^, y funciones como RC, ABS, TRUNC, REDON, AZAR, LONGITUD, SUBCADENA, MAYUSCULAS y CONVERTIRANUMERO.
        </p>
        <p>Límites por ejecución: 2 segundos, un millón de pasos, 20 000 caracteres de salida y 10 000 posiciones en arreglos.</p>
      </details>
    </>
  )
}
