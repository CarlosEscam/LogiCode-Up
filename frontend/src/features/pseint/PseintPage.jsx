import { useState } from 'react'
import PseintRunner from './PseintRunner.jsx'

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
}

const EXAMPLE_INPUTS = { saludo: 'Ana', promedio: '3\n4\n3.5\n5', mayor: '7\n12' }

// Editor de PSeInt para practicar sin instalar nada (RF-12). El servidor ejecuta el programa con límites
// de tiempo, pasos, memoria y salida; el intérprete no tiene acceso a archivos ni a la red.
export default function PseintPage() {
  const [code, setCode] = useState(EXAMPLES.saludo)
  const [example, setExample] = useState('saludo')
  return (
    <section className="card card--wide" aria-labelledby="pseint-titulo">
      <h1 id="pseint-titulo">Editor de PSeInt</h1>
      <p>
        Escribe tu algoritmo, pon las entradas que leería con <code>Leer</code> y ejecútalo. Es el mismo intérprete que califica las
        actividades con casos de prueba.
      </p>
      <div className="field">
        <label htmlFor="pseint-ejemplo">Ejemplo</label>
        <select
          id="pseint-ejemplo"
          value={example}
          onChange={(event) => {
            setExample(event.target.value)
            setCode(EXAMPLES[event.target.value])
          }}
        >
          <option value="saludo">Saludo (Leer y Escribir)</option>
          <option value="promedio">Promedio de notas (Para)</option>
          <option value="mayor">El mayor de dos números (Si)</option>
        </select>
      </div>
      <div className="field">
        <label htmlFor="pseint-codigo">Programa</label>
        <textarea
          id="pseint-codigo"
          rows={16}
          maxLength={20000}
          spellCheck={false}
          className="code"
          value={code}
          onChange={(event) => setCode(event.target.value)}
        />
      </div>
      <p className="page__note">Entradas de ejemplo: {EXAMPLE_INPUTS[example].replaceAll('\n', ', ')}</p>
      <PseintRunner code={code} idPrefix="pseint" />
      <details>
        <summary>Qué entiende el intérprete</summary>
        <p>
          Algoritmo/Proceso, Definir, Dimension (posiciones 0 a n), Leer, Escribir (con Sin Saltar), asignación con &lt;- o =, Si,
          Segun, Mientras, Repetir, Para con Paso, SubProceso/Funcion con parámetros por valor o por referencia, operadores Y, O, NO,
          MOD y ^, y funciones como RC, ABS, TRUNC, REDON, AZAR, LONGITUD, SUBCADENA, MAYUSCULAS y CONVERTIRANUMERO. Para Cada aún
          no está disponible.
        </p>
        <p>Límites por ejecución: 2 segundos, un millón de pasos, 20 000 caracteres de salida y 10 000 posiciones en arreglos.</p>
      </details>
    </section>
  )
}
