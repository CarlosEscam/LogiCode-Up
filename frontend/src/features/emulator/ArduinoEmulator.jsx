import { useEffect, useRef, useState } from 'react'
import CodeEditor from './CodeEditor.jsx'
import { ArduinoHighlight } from './highlight.jsx'
import FileBar from './FileBar.jsx'
import { ArduinoError, PIN_COUNT, compileArduino, createBoard, createMachine } from './arduino.js'
import { useDraft } from './storage.js'

const EXAMPLE = `// Parpadeo: el LED de la placa (pin 13) se prende y se apaga cada medio segundo.
int vueltas = 0;

void setup() {
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  digitalWrite(LED_BUILTIN, HIGH);
  delay(500);
  digitalWrite(LED_BUILTIN, LOW);
  delay(500);
  vueltas++;
  Serial.print("Vueltas: ");
  Serial.println(vueltas);
}
`

const EXAMPLES = {
  parpadeo: EXAMPLE,
  boton: `// Botón en el pin 2 (con INPUT_PULLUP): mientras lo presionas se prende el LED del pin 8.
const int BOTON = 2;
const int LED = 8;

void setup() {
  pinMode(BOTON, INPUT_PULLUP);
  pinMode(LED, OUTPUT);
}

void loop() {
  if (digitalRead(BOTON) == LOW) {
    digitalWrite(LED, HIGH);
  } else {
    digitalWrite(LED, LOW);
  }
}
`,
  potenciometro: `// Potenciómetro en A0: su valor controla el brillo del LED del pin 9 (PWM).
void setup() {
  pinMode(9, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  int lectura = analogRead(A0);
  int brillo = map(lectura, 0, 1023, 0, 255);
  analogWrite(9, brillo);
  Serial.println(brillo);
  delay(200);
}
`,
  semaforo: `// Semáforo con tres LED.
int rojo = 4, amarillo = 3, verde = 2;

void setup() {
  pinMode(rojo, OUTPUT);
  pinMode(amarillo, OUTPUT);
  pinMode(verde, OUTPUT);
}

void encender(int pin, int ms) {
  digitalWrite(pin, HIGH);
  delay(ms);
  digitalWrite(pin, LOW);
}

void loop() {
  encender(verde, 3000);
  encender(amarillo, 1000);
  encender(rojo, 3000);
}
`,
}

const STEPS_PER_FRAME = 200000
// Con setTimeout y no requestAnimationFrame: la placa sigue corriendo aunque la pestaña no esté a la vista.
const FRAME_MS = 30
const pinName = (pin) => (pin >= 14 ? `A${pin - 14}` : String(pin))
const kindText = { compile: 'Error de compilación', unsupported: 'Aún no disponible en el emulador', runtime: 'Error al ejecutar' }

// Emulador de Arduino con aspecto del editor web de Arduino y los colores de LogiCode: barra con Verificar y Ejecutar
// (como Subir), placa elegida y archivo; código con colores a la izquierda, placa virtual (Arduino Uno) a la derecha y
// abajo la Salida de la compilación y el Monitor Serie. Compila en el navegador; nada se envía al servidor.
export default function ArduinoEmulator() {
  const [code, setCode] = useDraft('arduino', EXAMPLE)
  const [outcome, setOutcome] = useState(null) // { ok, kind, message, line, checked, summary }
  const [running, setRunning] = useState(false)
  const [, setFrame] = useState(0)
  const [serialText, setSerialText] = useState('')
  const [session, setSession] = useState(null) // { machine, board }: la placa se cambia en el sitio y frame repinta
  const [panel, setPanel] = useState('salida')
  const [position, setPosition] = useState([1, 1])
  const frame = useRef(null)
  const serialScreen = useRef(null)
  const board = session?.board ?? null

  const stop = () => {
    if (frame.current) clearTimeout(frame.current)
    frame.current = null
    setRunning(false)
  }
  useEffect(() => () => clearTimeout(frame.current), [])
  // El Monitor Serie baja solo, como en el Arduino IDE.
  useEffect(() => {
    if (serialScreen.current) serialScreen.current.scrollTop = serialScreen.current.scrollHeight
  })

  function fail(error) {
    if (!(error instanceof ArduinoError)) throw error
    setOutcome({ ok: false, kind: error.kind, message: error.message, line: error.line })
    setPanel('salida')
  }

  const summary = (program) =>
    `${program.functions.length} ${program.functions.length === 1 ? 'función' : 'funciones'} y ${program.globals.length} ${program.globals.length === 1 ? 'variable global' : 'variables globales'}.`

  function verify() {
    stop()
    try {
      setOutcome({ ok: true, checked: true, summary: summary(compileArduino(code)) })
      setPanel('salida')
    } catch (error) {
      fail(error)
    }
  }

  function start() {
    stop()
    let program
    try {
      program = compileArduino(code)
    } catch (error) {
      fail(error)
      return
    }
    const fresh = createBoard()
    const machine = createMachine(program, fresh)
    const started = performance.now()
    setSession({ machine, board: fresh })
    setOutcome({ ok: true, summary: summary(program) })
    setRunning(true)
    if (code.includes('Serial.')) setPanel('serial')
    const step = () => {
      try {
        machine.advance((performance.now() - started) * 1000, STEPS_PER_FRAME)
      } catch (error) {
        setFrame((n) => n + 1)
        setRunning(false)
        frame.current = null
        fail(error)
        return
      }
      setFrame((n) => n + 1)
      frame.current = setTimeout(step, FRAME_MS)
    }
    frame.current = setTimeout(step, FRAME_MS)
  }

  const edit = (next) => {
    setCode(next)
    if (!running) setOutcome(null)
  }

  const used = board
    ? Array.from({ length: PIN_COUNT }, (_, pin) => pin).filter((pin) => board.modes[pin] || board.values[pin] || board.tones[pin])
    : []
  const seconds = session ? (session.machine.time / 1e6).toLocaleString('es-CO', { maximumFractionDigits: 1 }) : null

  return (
    <>
      <p>
        Escribe tu programa de Arduino. <strong>✓ Verificar</strong> lo compila; <strong>→ Ejecutar</strong> lo compila y lo corre en una placa
        virtual (Arduino Uno) con LED, botones, potenciómetros y Monitor Serie. Los programas de verdad se suben a la placa con el Arduino IDE.
      </p>
      <div
        className="ide arduino-ide"
        onKeyDown={(event) => {
          if ((event.ctrlKey || event.metaKey) && (event.key === 'r' || event.key === 'u')) {
            event.preventDefault()
            if (event.key === 'r') verify()
            else start()
          }
        }}
      >
        <div className="ide__toolbar">
          <div className="arduino-ide__actions">
            <button type="button" className="arduino-ide__round" aria-label="✓ Verificar" title="Verificar (Ctrl+R)" onClick={verify}>
              ✓
            </button>
            <button type="button" className="arduino-ide__round arduino-ide__round--go" aria-label={running ? 'Reiniciar' : '→ Ejecutar'} title="Ejecutar en la placa virtual (Ctrl+U)" onClick={start}>
              →
            </button>
            <button type="button" className="arduino-ide__round arduino-ide__round--stop" aria-label="■ Detener" title="Detener" disabled={!running} onClick={stop}>
              ■
            </button>
            <span className="arduino-ide__board" title="Placa">
              <span className="arduino-ide__dot" aria-hidden="true" data-on={running || undefined} /> Arduino Uno (virtual)
            </span>
          </div>
          <FileBar idPrefix="arduino" extension=".ino" defaultName="mi-programa" getText={() => code} onOpen={(text) => edit(text)} onNew={() => edit('void setup() {\n\n}\n\nvoid loop() {\n\n}\n')} />
          <div className="field ide__examples">
            <label htmlFor="arduino-ejemplo">Ejemplos</label>
            <select
              id="arduino-ejemplo"
              value=""
              onChange={(event) => {
                const key = event.target.value
                if (key && (Object.values(EXAMPLES).includes(code) || window.confirm('El ejemplo reemplaza lo que hay en el editor. ¿Continuar?'))) {
                  stop()
                  edit(EXAMPLES[key])
                }
              }}
            >
              <option value="">Cargar…</option>
              <option value="parpadeo">Parpadeo del LED</option>
              <option value="boton">Botón y LED</option>
              <option value="potenciometro">Potenciómetro y brillo (PWM)</option>
              <option value="semaforo">Semáforo con una función</option>
            </select>
          </div>
        </div>
        <div className="ide__panes">
          <CodeEditor
            id="arduino-codigo"
            label="Programa (.ino)"
            hideLabel
            value={code}
            onChange={edit}
            errorLine={outcome?.line || null}
            rows={20}
            Highlight={ArduinoHighlight}
            onCursor={(line, column) => setPosition([line, column])}
          />
          <section className="arduino-board" aria-labelledby="arduino-placa">
            <h2 id="arduino-placa" className="ide-console__title">
              Placa virtual {seconds && <span className="arduino-board__time">· {seconds} s</span>}
            </h2>
            {!board && <p className="page__note arduino-board__empty">Pulsa → Ejecutar: aquí aparecen los LED, botones y potenciómetros que use tu programa.</p>}
            {board && used.length === 0 && board.analogUsed.size === 0 && (
              <p className="page__note arduino-board__empty">Los pines aparecen aquí cuando el programa los configura con pinMode o los usa.</p>
            )}
            {board && used.length > 0 && (
              <ul className="arduino-pins">
                {used.map((pin) => (
                  <Pin key={pin} pin={pin} board={board} onChange={() => setFrame((n) => n + 1)} />
                ))}
              </ul>
            )}
            {board &&
              [...board.analogUsed].sort().map((index) => (
                <div key={index} className="field arduino-board__pot">
                  <label htmlFor={`arduino-a${index}`}>
                    Potenciómetro en A{index}: {board.analog[index]}
                  </label>
                  <input
                    id={`arduino-a${index}`}
                    type="range"
                    min="0"
                    max="1023"
                    value={board.analog[index]}
                    onChange={(event) => {
                      board.turn(index, Number(event.target.value))
                      setFrame((n) => n + 1)
                    }}
                  />
                </div>
              ))}
          </section>
        </div>
        <div className="arduino-ide__bottom">
          <div className="tabs arduino-ide__tabs" role="tablist" aria-label="Paneles de abajo">
            <button type="button" role="tab" id="arduino-tab-salida" aria-controls="arduino-panel-salida" aria-selected={panel === 'salida'} onClick={() => setPanel('salida')}>
              Salida
            </button>
            <button type="button" role="tab" id="arduino-tab-serial" aria-controls="arduino-panel-serial" aria-selected={panel === 'serial'} onClick={() => setPanel('serial')}>
              Monitor Serie
            </button>
          </div>
          <div id="arduino-panel-salida" role="tabpanel" aria-labelledby="arduino-tab-salida" hidden={panel !== 'salida'}>
            <div role="status" aria-label="Resultado" className="arduino-ide__output">
              {!outcome && <p className="page__note">Pulsa ✓ Verificar para compilar.</p>}
              {outcome?.ok && (
                <p className="verdict verdict--ok">
                  {outcome.checked ? '✓ Compila: el programa no tiene errores.' : running ? '✓ Compila. El programa se está ejecutando en la placa.' : '✓ Compila. Ejecución detenida.'}
                </p>
              )}
              {outcome?.ok && <p className="page__note">Compilación terminada: {outcome.summary}</p>}
              {outcome && !outcome.ok && (
                <>
                  <p className={`verdict ${outcome.kind === 'runtime' ? 'verdict--warn' : 'verdict--error'}`}>
                    {outcome.kind === 'runtime' ? '✓ Compila, pero ✗ falló al ejecutarse.' : '✗ No compila: corrige el error de abajo.'}
                  </p>
                  <p className="notice notice--error">
                    <strong>{kindText[outcome.kind]}:</strong> {outcome.message}
                  </p>
                </>
              )}
              {board?.warnings.length > 0 && (
                <ul className="arduino-warnings">
                  {board.warnings.map((item) => (
                    <li key={item.message}>
                      ⚠ Línea {item.line}: {item.message}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
          <div id="arduino-panel-serial" role="tabpanel" aria-labelledby="arduino-tab-serial" hidden={panel !== 'serial'}>
            <pre ref={serialScreen} className="console arduino-serial" aria-label="Mensajes de la placa">
              {board?.serial || '(sin datos)'}
            </pre>
            <form
              className="toolbar arduino-ide__send"
              onSubmit={(event) => {
                event.preventDefault()
                board.send(`${serialText}\n`)
                setSerialText('')
              }}
            >
              <input aria-label="Texto para enviar por el Monitor Serie" placeholder="Mensaje para enviar a la placa" value={serialText} maxLength={200} onChange={(event) => setSerialText(event.target.value)} disabled={!running} />
              <button type="submit" className="button button--secondary button--small" disabled={!running}>
                Enviar
              </button>
              <span className="page__note">9600 baudios</span>
            </form>
          </div>
        </div>
        <div className="ide__status">
          <span>{running ? 'Ejecutando en la placa virtual' : outcome ? (outcome.ok ? 'Listo' : 'Con errores') : 'Listo para verificar'}</span>
          <span>
            Línea {position[0]}, Col {position[1]}
          </span>
          <span>Arduino Uno · LogiCode</span>
        </div>
      </div>
      <details>
        <summary>Qué entiende el emulador</summary>
        <p>
          setup() y loop(); variables int, long, unsigned, byte, char, bool, float y String; const y #define; arreglos de una
          dimensión; funciones con parámetros; if, else, switch, for, while, do while, break y continue; pinMode, digitalWrite,
          digitalRead, analogRead, analogWrite, delay, millis, tone, Serial.begin, Serial.print, Serial.println, Serial.available,
          Serial.read, map, constrain, min, max, abs, random y funciones matemáticas.
        </p>
        <p>
          Como en el Uno, int va de −32 768 a 32 767 (300 * 300 da 24464). Un botón en un pin INPUT se lee HIGH al presionarlo; con
          INPUT_PULLUP se lee LOW. Librerías (#include), punteros, clases y arreglos de dos dimensiones aún no están disponibles.
        </p>
      </details>
    </>
  )
}

function Pin({ pin, board, onChange }) {
  const pressed = board.pressed[pin]
  const mode = board.modes[pin] ?? 'sin pinMode'
  const value = board.values[pin]
  const output = mode === 'OUTPUT' || (mode === 'sin pinMode' && value)
  const brightness = board.pwm[pin] ? value / 255 : value ? 1 : 0
  const state = output ? (board.pwm[pin] ? `PWM ${value} de 255` : value ? 'HIGH (encendido)' : 'LOW (apagado)') : `entrada ${board.pressed[pin] ? 'presionada' : 'suelta'}`
  return (
    <li className="arduino-pin">
      <span className="arduino-pin__name">
        Pin {pinName(pin)}
        {pin === 13 ? ' (LED L)' : ''}
      </span>
      {output ? (
        <span className="arduino-led" style={{ opacity: 0.18 + brightness * 0.82 }} role="img" aria-label={`LED del pin ${pinName(pin)}: ${state}`} />
      ) : (
        <button
          type="button"
          className="button button--secondary button--small"
          aria-pressed={pressed}
          onClick={() => {
            board.press(pin, !pressed)
            onChange()
          }}
        >
          {pressed ? 'Soltar botón' : 'Presionar botón'}
        </button>
      )}
      <span className="page__note">
        {mode} · {state}
        {board.tones[pin] ? ` · ♪ ${board.tones[pin]} Hz` : ''}
      </span>
    </li>
  )
}
