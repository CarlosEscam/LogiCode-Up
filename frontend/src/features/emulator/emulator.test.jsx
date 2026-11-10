import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App.jsx'
import { compileArduino, simulate } from './arduino.js'
import { checkFlow, newStep, parseProject, projectText, toPseint } from './flow.js'

const STUDENT = {
  id: 's1',
  cedula: '600001',
  fullName: 'Ana Estudiante',
  role: 'STUDENT',
  fullPrivileges: false,
  email: 'ana@example.com',
  emailVerified: true,
  permissions: ['VIEW_PRIVATE_COURSE_CONTENT', 'ANSWER_ACTIVITIES'],
}

function respond(status, body) {
  return { ok: status < 400, status, headers: new Headers({ 'Content-Type': 'application/json' }), json: async () => body }
}

function stubApi(routes) {
  const fetchMock = vi.fn(async (url, options = {}) => routes[`${options.method ?? 'GET'} ${url.split('?')[0]}`] ?? respond(404, {}))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

const compileMessage = (source) => {
  try {
    compileArduino(source)
    return null
  } catch (error) {
    return `${error.kind}: ${error.message}`
  }
}

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=t; path=/'
})

afterEach(() => {
  vi.unstubAllGlobals()
  window.localStorage.clear()
  window.history.replaceState(null, '', '/')
})

describe('Simulador de Arduino', () => {
  it('parpadea el LED y escribe en el Monitor Serie con el reloj simulado', () => {
    const source = `#define LED 13
int vueltas = 0;
void setup() { pinMode(LED, OUTPUT); Serial.begin(9600); }
void loop() {
  digitalWrite(LED, HIGH);
  delay(500);
  digitalWrite(LED, LOW);
  delay(500);
  vueltas++;
  Serial.println(vueltas);
}`
    expect(simulate(source, 250).board.values[13]).toBe(1)
    const { board } = simulate(source, 2600)
    expect(board.values[13]).toBe(0)
    expect(board.serial).toBe('1\n2\n')
    expect(board.warnings).toEqual([])
  })

  it('hace las cuentas como el Uno: int de 16 bits, división entera y float con dos decimales', () => {
    const { board } = simulate(
      `void setup() {
  Serial.begin(9600);
  int big = 300 * 300;
  float a = 5 / 2;
  float b = 5.0 / 2;
  Serial.println(big);
  Serial.println(a);
  Serial.println(b);
  Serial.println("Valor: " + String(7));
  Serial.println(map(512, 0, 1023, 0, 255));
  Serial.println(255, HEX);
  Serial.println('A');
}
void loop() {}`,
      10,
    )
    expect(board.serial).toBe('24464\n2.00\n2.50\nValor: 7\n127\nFF\nA\n')
  })

  it('ejecuta funciones, arreglos, switch y ciclos', () => {
    const { board } = simulate(
      `int notas[] = {3, 5, 4};
int factorial(int n) { if (n <= 1) return 1; return n * factorial(n - 1); }
void setup() {
  Serial.begin(9600);
  int suma = 0;
  for (int i = 0; i < sizeof(notas) / sizeof(notas[0]); i++) suma += notas[i];
  Serial.println(suma);
  Serial.println(factorial(5));
  int k = 0;
  do { k++; if (k == 2) continue; } while (k < 4);
  Serial.println(k);
  switch (2) { case 1: Serial.println("uno"); break; case 2: Serial.println("dos"); default: Serial.println("cae"); }
}
void loop() { delay(1000); }`,
      10,
    )
    expect(board.serial).toBe('12\n120\n4\ndos\ncae\n')
  })

  it('lee botones con INPUT_PULLUP y potenciómetros en A0', () => {
    const source = `void setup() { pinMode(2, INPUT_PULLUP); pinMode(9, OUTPUT); }
void loop() {
  if (digitalRead(2) == LOW) digitalWrite(8, HIGH);
  analogWrite(9, map(analogRead(A0), 0, 1023, 0, 255));
  delay(10);
}`
    const { board } = simulate(source, 50, { prepare: (b) => (b.press(2, true), b.turn(0, 1023)) })
    expect(board.values[8]).toBe(1)
    expect(board.values[9]).toBe(255)
    expect(board.warnings.map((item) => item.message)).toEqual([expect.stringContaining('sin pinMode(8, OUTPUT)')])
  })

  it('dice por qué no compila, con la línea', () => {
    expect(compileMessage('void setup() {\n  int a = 1\n}\nvoid loop() {}')).toBe('compile: Línea 2: falta «;» al final de la instrucción (antes de «}»).')
    expect(compileMessage('void setup() { x = 3; }\nvoid loop() {}')).toMatch(/^compile: Línea 1: «x» no está declarada/)
    expect(compileMessage('void setup() {}')).toMatch(/falta la función «void loop\(\)»/)
    expect(compileMessage('void setup() { pinmode(13, OUTPUT); }\nvoid loop() {}')).toMatch(/la función «pinmode» no existe/)
    expect(compileMessage('void setup() { digitalWrite(13); }\nvoid loop() {}')).toMatch(/digitalWrite\(\) recibe 2 valores y le diste 1/)
    expect(compileMessage('const int LED = 13;\nvoid setup() { LED = 4; }\nvoid loop() {}')).toMatch(/es constante/)
    expect(compileMessage('void setup() {\n  if (1) {\n}\nvoid loop() {}')).toMatch(/^compile: Línea 4: no se puede definir la función «loop» dentro de otra: seguramente falta una «}»/)
    expect(compileMessage('#include <Servo.h>\nServo motor;\nvoid setup() {}\nvoid loop() {}')).toMatch(/^compile: Línea 2:/)
    expect(compileMessage('void setup() { lcd.print(1); }\nvoid loop() {}')).toMatch(/^unsupported: .*aún no está disponible/)
    expect(compileMessage('void setup() {}\nvoid loop() {}')).toBeNull()
  })

  it('informa los errores al ejecutar sin colgar la página', () => {
    expect(() => simulate('void setup() { int a[3]; a[3] = 1; }\nvoid loop() {}', 10)).toThrow('Línea 1: la posición 3 no existe en «a» (va de 0 a 2).')
    expect(() => simulate('void setup() { int x = 0; int y = 4 / x; }\nvoid loop() {}', 10)).toThrow('división entre cero')
    expect(() => simulate('int f(int n) { return f(n + 1); }\nvoid setup() { f(1); }\nvoid loop() {}', 10)).toThrow('demasiadas llamadas anidadas')
    // Un ciclo sin fin solo gasta los pasos permitidos.
    const { machine } = simulate('void setup() { while (true) {} }\nvoid loop() {}', 1000, { maxSteps: 10000 })
    expect(machine.steps).toBeGreaterThanOrEqual(10000)
  })
})

describe('Programa de DFD y Scratch', () => {
  it('se traduce a PSeInt y recuerda qué paso generó cada línea', () => {
    const steps = [
      newStep('leer', { vars: 'a, b' }),
      newStep('si', { cond: 'a > b', body: [{ type: 'escribir', expr: 'a' }], else: [{ type: 'escribir', expr: 'b' }] }),
      newStep('veces', { times: '2', body: [{ type: 'cambiar', var: 'a', expr: '1' }] }),
    ]
    const { code, lineSteps } = toPseint(steps)
    expect(code).toBe(
      'Algoritmo Programa\n    Leer a, b\n    Si a > b Entonces\n        Escribir a\n    SiNo\n        Escribir b\n    FinSi\n    Para vez_1 <- 1 Hasta 2 Hacer\n        a <- a + (1)\n    FinPara\nFinAlgoritmo\n',
    )
    expect(lineSteps[2]).toBe(steps[0].id)
    expect(lineSteps[4]).toBe(steps[1].body[0].id)
  })

  it('no compila con campos vacíos o nombres inválidos', () => {
    expect(checkFlow([newStep('si', { cond: '' })])).toMatchObject({ message: 'falta la condición.' })
    expect(checkFlow([newStep('leer', { vars: 'a, 2b' })])).toMatchObject({ message: expect.stringContaining('«2b» no sirve') })
    expect(checkFlow([newStep('asignar', { var: 'x', expr: '1' })])).toBeNull()
  })

  it('guarda y abre el proyecto como archivo', () => {
    const steps = [newStep('mientras', { cond: 'i < 3', body: [{ type: 'asignar', var: 'i', expr: 'i + 1' }] })]
    const text = projectText(steps, 'dfd')
    expect(toPseint(parseProject(text, 'dfd')).code).toBe(toPseint(steps).code)
    expect(() => parseProject(text, 'scratch')).toThrow('no es un proyecto de Scratch')
    expect(() => parseProject('hola', 'dfd')).toThrow('no es JSON válido')
  })
})

describe('Pestaña Emulador', () => {
  it('reúne un emulador por tema y ejecuta el DFD traducido a PSeInt', async () => {
    window.history.replaceState(null, '', '/')
    const fetchMock = stubApi({
      'GET /api/v1/me': respond(200, STUDENT),
      'POST /api/v1/pseint/runs': respond(200, { output: 'El mayor es 9\n', completed: true, errorKind: null, error: null, steps: 5, durationMs: 1 }),
    })
    render(<App />)
    fireEvent.click(await screen.findByRole('link', { name: 'Emulador' }))
    const tabs = within(screen.getByRole('tablist', { name: 'Lenguajes' })).getAllByRole('tab')
    expect(tabs.map((tab) => tab.textContent)).toEqual(['PSeInt', 'DFD', 'Scratch', 'Arduino'])

    fireEvent.click(screen.getByRole('tab', { name: 'DFD' }))
    expect(screen.getByRole('img', { name: /Diagrama de flujo: Inicio; Leer a, b; decisión ¿a > b\?/ })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Entradas para probar (una por línea)'), { target: { value: '9\n4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ejecutar' }))
    const result = await screen.findByRole('status', { name: 'Resultado de la ejecución' })
    expect(result).toHaveTextContent('✓ Compila y se ejecutó sin errores.')
    expect(result).toHaveTextContent('El mayor es 9')
    const call = fetchMock.mock.calls.find(([url]) => url === '/api/v1/pseint/runs')
    expect(JSON.parse(call[1].body).code).toContain('Si a > b Entonces')
  })

  it('en Scratch un bloque vacío no compila y no llama al servidor', async () => {
    window.history.replaceState(null, '', '/emulador/scratch')
    const fetchMock = stubApi({ 'GET /api/v1/me': respond(200, STUDENT) })
    render(<App />)
    fireEvent.click(await screen.findByRole('button', { name: '+ si … entonces' }))
    fireEvent.click(screen.getByRole('button', { name: 'Ejecutar' }))
    const result = await screen.findByRole('status', { name: 'Resultado de la ejecución' })
    expect(result).toHaveTextContent('✗ No compila')
    expect(result).toHaveTextContent('En «si entonces»: falta la condición.')
    expect(fetchMock.mock.calls.some(([url]) => url === '/api/v1/pseint/runs')).toBe(false)
  })

  it('Arduino verifica el programa en el navegador y marca la línea del error', async () => {
    window.history.replaceState(null, '', '/emulador/arduino')
    stubApi({ 'GET /api/v1/me': respond(200, STUDENT) })
    render(<App />)
    const editor = await screen.findByLabelText('Programa (.ino)')
    fireEvent.click(screen.getByRole('button', { name: '✓ Verificar' }))
    expect(screen.getByRole('status', { name: 'Resultado' })).toHaveTextContent('✓ Compila: el programa no tiene errores.')
    fireEvent.change(editor, { target: { value: 'void setup() {\n  pinMode(13, OUTPUT)\n}\nvoid loop() {}' } })
    fireEvent.click(screen.getByRole('button', { name: '✓ Verificar' }))
    expect(screen.getByRole('status', { name: 'Resultado' })).toHaveTextContent('✗ No compila')
    expect(screen.getByRole('status', { name: 'Resultado' })).toHaveTextContent('Línea 2: falta «;»')
    expect(screen.getByText(/El error está en la línea 2/)).toBeInTheDocument()
    expect(window.localStorage.getItem('logicode.emulador.arduino')).toContain('pinMode(13, OUTPUT)')
  })
  it('Arduino ejecuta en la placa virtual: el LED parpadea, el Monitor Serie escribe y un botón se puede presionar', async () => {
    window.history.replaceState(null, '', '/emulador/arduino')
    stubApi({ 'GET /api/v1/me': respond(200, STUDENT) })
    render(<App />)
    const editor = await screen.findByLabelText('Programa (.ino)')
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'performance'] })
    try {
      fireEvent.change(editor, {
        target: {
          value: [
            'void setup() { pinMode(13, OUTPUT); pinMode(2, INPUT_PULLUP); Serial.begin(9600); }',
            'void loop() {',
            '  digitalWrite(13, HIGH); delay(500); digitalWrite(13, LOW); delay(500);',
            '  Serial.println(digitalRead(2) == LOW ? "presionado" : "suelto");',
            '}',
          ].join('\n'),
        },
      })
      fireEvent.click(screen.getByRole('button', { name: '▶ Ejecutar' }))
      await act(async () => vi.advanceTimersByTime(300))
      expect(screen.getByRole('img', { name: 'LED del pin 13: HIGH (encendido)' })).toBeInTheDocument()
      await act(async () => vi.advanceTimersByTime(1000))
      expect(screen.getByLabelText('Monitor Serie')).toHaveTextContent('suelto')
      fireEvent.click(screen.getByRole('button', { name: 'Presionar botón' }))
      await act(async () => vi.advanceTimersByTime(1000))
      expect(screen.getByLabelText('Monitor Serie').textContent).toBe('suelto\npresionado\n')
      fireEvent.click(screen.getByRole('button', { name: '■ Detener' }))
      expect(screen.getByRole('status', { name: 'Resultado' })).toHaveTextContent('Ejecución detenida')
    } finally {
      vi.useRealTimers()
    }
  })
})
