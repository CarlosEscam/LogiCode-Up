import { useState } from 'react'
import PseintRunner from '../pseint/PseintRunner.jsx'
import { errorLineOf } from '../pseint/errorLine.js'
import CodeEditor from './CodeEditor.jsx'
import FileBar from './FileBar.jsx'
import { useDraft } from './storage.js'

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

const EXAMPLE_INPUTS = { saludo: 'Ana', promedio: '3\n4\n3.5\n5', mayor: '7\n12', paraCada: '3\n4.5\n5\n2.5' }

// Emulador de PSeInt (RF-12): código libre, sin nota ni entrega. El servidor ejecuta el programa con límites de tiempo,
// pasos, memoria y salida; el intérprete no tiene acceso a archivos ni a la red.
export default function PseintEmulator() {
  const [code, setCode] = useDraft('pseint', EXAMPLES.saludo)
  const [example, setExample] = useState('')
  const [errorLine, setErrorLine] = useState(null)
  const edit = (next) => {
    setCode(next)
    setErrorLine(null)
  }
  return (
    <>
      <p>
        Escribe tu algoritmo, pon las entradas que leería con <code>Leer</code> y ejecútalo. Es el mismo intérprete que califica las
        actividades con casos de prueba.
      </p>
      <FileBar
        idPrefix="pseint"
        extension=".psc"
        defaultName="mi-algoritmo"
        getText={() => code}
        onOpen={(text) => edit(text)}
        onNew={() => edit('Algoritmo SinTitulo\n    \nFinAlgoritmo\n')}
      />
      <div className="field field--short">
        <label htmlFor="pseint-ejemplo">Cargar un ejemplo</label>
        <select
          id="pseint-ejemplo"
          value={example}
          onChange={(event) => {
            const key = event.target.value
            if (key && (Object.values(EXAMPLES).includes(code) || window.confirm('El ejemplo reemplaza lo que hay en el editor. ¿Continuar?'))) {
              setExample(key)
              edit(EXAMPLES[key])
            }
          }}
        >
          <option value="">Elige…</option>
          <option value="saludo">Saludo (Leer y Escribir)</option>
          <option value="promedio">Promedio de notas (Para)</option>
          <option value="mayor">El mayor de dos números (Si)</option>
          <option value="paraCada">Promedio de un arreglo (Para Cada)</option>
        </select>
      </div>
      <CodeEditor id="pseint-codigo" label="Programa (.psc)" value={code} onChange={edit} errorLine={errorLine} />
      {example && <p className="page__note">Entradas de ejemplo: {EXAMPLE_INPUTS[example].replaceAll('\n', ', ')}</p>}
      <PseintRunner code={code} idPrefix="pseint" onResult={(result) => setErrorLine(errorLineOf(result.error))} />
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
