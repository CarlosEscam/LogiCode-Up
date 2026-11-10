// Simulador de Arduino Uno para el Emulador: compila (lee y revisa) un subconjunto de C++ de Arduino y lo ejecuta paso a
// paso sobre una placa virtual. No es avr-gcc: cubre lo que se ve en el curso (setup/loop, pines, tiempo, Monitor Serie,
// variables, arreglos de una dimensión, funciones, if/switch/for/while/do). Lo que no cubre se informa como
// «aún no disponible», nunca se ignora. Es puro (sin React) para probarlo.
//
// La ejecución es un generador: cede el control en cada delay() y cada cierta cantidad de pasos, y la página lo reanuda
// según pasa el tiempo. El reloj es simulado: cada instrucción cuesta 1 µs y delay(ms) avanza el reloj.

export class ArduinoError extends Error {
  // kind: 'compile' (el programa no compila), 'unsupported' (válido en Arduino, pero el emulador no lo tiene) o 'runtime'.
  constructor(kind, line, message) {
    super(line ? `Línea ${line}: ${message}` : message)
    this.kind = kind
    this.line = line
  }
}

const compileError = (line, message) => new ArduinoError('compile', line, message)
const unsupported = (line, message) => new ArduinoError('unsupported', line, message)
const runtimeError = (line, message) => new ArduinoError('runtime', line, message)

export const PIN_COUNT = 20 // 0 a 13 digitales, 14 a 19 son A0 a A5
export const LIMITS = { callDepth: 200, arrayLength: 2000, serialChars: 20000 }

// ---------------------------------------------------------------- Léxico

const OPERATORS = [
  '<<=', '>>=', '++', '--', '+=', '-=', '*=', '/=', '%=', '&=', '|=', '^=', '==', '!=', '<=', '>=', '&&', '||', '<<', '>>', '::',
  '+', '-', '*', '/', '%', '<', '>', '=', '!', '&', '|', '^', '~', '?', ':', ';', ',', '.', '(', ')', '{', '}', '[', ']',
]

function lex(source) {
  const tokens = []
  const macros = new Map()
  let i = 0
  let line = 1
  const push = (token) => {
    if (token.kind === 'id' && macros.has(token.text)) macros.get(token.text).forEach((t) => tokens.push({ ...t, line: token.line }))
    else tokens.push(token)
  }
  while (i < source.length) {
    const ch = source[i]
    if (ch === '\n') {
      line += 1
      i += 1
    } else if (/\s/.test(ch)) i += 1
    else if (source.startsWith('//', i)) {
      while (i < source.length && source[i] !== '\n') i += 1
    } else if (source.startsWith('/*', i)) {
      const end = source.indexOf('*/', i + 2)
      if (end < 0) throw compileError(line, 'un comentario /* no se cerró con */.')
      line += source.slice(i, end).split('\n').length - 1
      i = end + 2
    } else if (ch === '#') {
      let end = source.indexOf('\n', i)
      if (end < 0) end = source.length
      const directive = source.slice(i, end).trim()
      const define = /^#define\s+([A-Za-z_]\w*)\s*(.*)$/.exec(directive)
      if (define) {
        if (define[2].startsWith('(')) throw unsupported(line, '#define con parámetros aún no está disponible en el emulador.')
        macros.set(define[1], lex(define[2]).filter((t) => t.kind !== 'end').map((t) => ({ ...t, line })))
      } else if (!/^#include\b/.test(directive)) throw unsupported(line, `la directiva «${directive.split(/\s/)[0]}» aún no está disponible en el emulador.`)
      i = end
    } else if (/[0-9]/.test(ch) || (ch === '.' && /[0-9]/.test(source[i + 1] ?? ''))) {
      const match = /^(0[xX][0-9a-fA-F]+|0[bB][01]+|[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?)([uUlLfF]*)/.exec(source.slice(i))
      const text = match[1]
      const isFloat = /[.eE]/.test(text) && !/^0[xX]/.test(text)
      const value = /^0[bB]/.test(text) ? parseInt(text.slice(2), 2) : Number(text)
      tokens.push({ kind: 'num', value, float: isFloat || /f/i.test(match[2]), long: /l/i.test(match[2]), line, text: match[0] })
      i += match[0].length
      if (/[A-Za-z_]/.test(source[i] ?? '')) throw compileError(line, `el número «${match[0]}${source[i]}» está mal escrito.`)
    } else if (/[A-Za-z_]/.test(ch)) {
      const match = /^[A-Za-z_]\w*/.exec(source.slice(i))
      push({ kind: 'id', text: match[0], line })
      i += match[0].length
    } else if (ch === '"' || ch === "'") {
      let j = i + 1
      let text = ''
      while (j < source.length && source[j] !== ch) {
        if (source[j] === '\n') throw compileError(line, `falta cerrar ${ch === '"' ? 'las comillas' : 'el apóstrofo'} del texto.`)
        if (source[j] === '\\') {
          const next = source[j + 1]
          text += { n: '\n', t: '\t', r: '\r', 0: '\0', '\\': '\\', '"': '"', "'": "'" }[next] ?? next
          j += 2
        } else {
          text += source[j]
          j += 1
        }
      }
      if (j >= source.length) throw compileError(line, `falta cerrar ${ch === '"' ? 'las comillas' : 'el apóstrofo'} del texto.`)
      if (ch === "'") {
        if (text.length !== 1) throw compileError(line, `un carácter entre apóstrofos debe ser uno solo ('${text}').`)
        tokens.push({ kind: 'char', value: text.charCodeAt(0), line, text: `'${text}'` })
      } else tokens.push({ kind: 'str', value: text, line, text: `"${text}"` })
      i = j + 1
    } else {
      const op = OPERATORS.find((candidate) => source.startsWith(candidate, i))
      if (!op) throw compileError(line, `el carácter «${ch}» no se esperaba aquí.`)
      tokens.push({ kind: 'op', text: op, line })
      i += op.length
    }
  }
  tokens.push({ kind: 'end', text: 'el fin del programa', line })
  return tokens
}

// ---------------------------------------------------------------- Tipos

const TYPE_WORDS = new Set([
  'void', 'int', 'long', 'short', 'unsigned', 'signed', 'byte', 'char', 'bool', 'boolean', 'float', 'double', 'String', 'word',
  'size_t', 'uint8_t', 'int8_t', 'uint16_t', 'int16_t', 'uint32_t', 'int32_t', 'const', 'static', 'volatile',
])
const INTEGER = new Set(['int', 'uint', 'long', 'ulong', 'byte', 'char', 'bool'])
const SIZES = { int: 2, uint: 2, long: 4, ulong: 4, byte: 1, char: 1, bool: 1, float: 4, string: 6 }
const TYPE_NAMES = { int: 'int', uint: 'unsigned int', long: 'long', ulong: 'unsigned long', byte: 'byte', char: 'char', bool: 'bool', float: 'float', string: 'String', void: 'void' }

function normalizeType(words, line) {
  const isConst = words.includes('const')
  const rest = words.filter((word) => !['const', 'static', 'volatile', 'signed'].includes(word))
  const unsigned = rest.includes('unsigned')
  const base = rest.filter((word) => word !== 'unsigned').join(' ') || 'int'
  const table = {
    void: 'void', int: 'int', short: 'int', 'short int': 'int', int16_t: 'int', word: 'uint', uint16_t: 'uint', size_t: 'uint',
    long: 'long', 'long int': 'long', int32_t: 'long', uint32_t: 'ulong', 'long long': 'long', byte: 'byte', uint8_t: 'byte',
    char: 'char', int8_t: 'char', bool: 'bool', boolean: 'bool', float: 'float', double: 'float', String: 'string',
  }
  let type = table[base]
  if (!type) throw compileError(line, `«${words.join(' ')}» no es un tipo válido.`)
  if (unsigned) type = { int: 'uint', long: 'ulong', char: 'byte' }[type] ?? (type === 'uint' || type === 'ulong' || type === 'byte' ? type : null)
  if (!type) throw compileError(line, `«${words.join(' ')}» no es un tipo válido.`)
  return { type, isConst }
}

const mod = (value, size) => ((value % size) + size) % size
function wrap(type, value) {
  if (type === 'float') return Number(value)
  if (type === 'string') return String(value)
  if (type === 'bool') return value ? 1 : 0
  const whole = Math.trunc(Number(value)) || 0
  switch (type) {
    case 'int':
      return mod(whole + 32768, 65536) - 32768
    case 'uint':
      return mod(whole, 65536)
    case 'long':
      return mod(whole + 2147483648, 4294967296) - 2147483648
    case 'ulong':
      return mod(whole, 4294967296)
    case 'byte':
      return mod(whole, 256)
    case 'char':
      return mod(whole + 128, 256) - 128
    default:
      return whole
  }
}

// ---------------------------------------------------------------- Sintaxis

function parse(tokens) {
  let pos = 0
  const peek = (offset = 0) => tokens[Math.min(pos + offset, tokens.length - 1)]
  const next = () => tokens[pos++]
  const is = (text, offset = 0) => {
    const token = peek(offset)
    return (token.kind === 'op' || token.kind === 'id') && token.text === text
  }
  const accept = (text) => (is(text) ? next() : null)
  const describe = (token) => (token.kind === 'end' ? token.text : `«${token.text}»`)
  function expect(text) {
    if (is(text)) return next()
    const found = peek()
    if (text === ';') {
      // Como el compilador: el «;» que falta es el de la línea anterior.
      const previous = tokens[pos - 1] ?? found
      throw compileError(previous.line, `falta «;» al final de la instrucción (antes de ${describe(found)}).`)
    }
    throw compileError(found.line, `se esperaba «${text}» y se encontró ${describe(found)}.`)
  }
  function identifier(what) {
    const token = peek()
    if (token.kind !== 'id' || TYPE_WORDS.has(token.text) || KEYWORDS.has(token.text)) {
      throw compileError(token.line, `se esperaba ${what} y se encontró ${describe(token)}.`)
    }
    return next()
  }
  const startsType = () => peek().kind === 'id' && TYPE_WORDS.has(peek().text)
  function typeWords() {
    const words = []
    const line = peek().line
    while (startsType()) words.push(next().text)
    return normalizeType(words, line)
  }

  function program() {
    const globals = []
    const functions = []
    while (peek().kind !== 'end') {
      if (!startsType()) throw compileError(peek().line, `se esperaba una declaración (por ejemplo «int x = 0;» o «void setup() {») y se encontró ${describe(peek())}.`)
      const line = peek().line
      const { type, isConst } = typeWords()
      const name = identifier('un nombre')
      if (is('(')) {
        next()
        const params = []
        if (!is(')') && !(is('void') && is(')', 1))) {
          do {
            const paramLine = peek().line
            if (!startsType()) throw compileError(paramLine, `se esperaba el tipo del parámetro y se encontró ${describe(peek())}.`)
            const param = typeWords()
            if (is('&') || is('*')) throw unsupported(paramLine, 'los parámetros por referencia (&) o punteros (*) aún no están disponibles en el emulador.')
            const paramName = identifier('el nombre del parámetro')
            if (is('[')) throw unsupported(paramLine, 'pasar arreglos a una función aún no está disponible en el emulador.')
            params.push({ type: param.type, name: paramName.text, line: paramLine })
          } while (accept(','))
        } else accept('void')
        expect(')')
        if (accept(';')) continue // prototipo
        if (!is('{')) throw compileError(peek().line, `se esperaba «{» para empezar la función ${name.text} y se encontró ${describe(peek())}.`)
        functions.push({ name: name.text, type, params, body: block(), line })
      } else {
        globals.push(...declarators(type, isConst, name))
        expect(';')
      }
    }
    return { globals, functions }
  }

  function declarators(type, isConst, first) {
    const list = []
    let name = first
    for (;;) {
      if (type === 'void') throw compileError(name.line, `la variable «${name.text}» no puede ser void.`)
      const decl = { kind: 'decl', type, isConst, name: name.text, line: name.line }
      if (accept('[')) {
        if (is(']')) decl.size = null
        else decl.size = expression()
        expect(']')
        if (is('[')) throw unsupported(name.line, 'los arreglos de dos dimensiones aún no están disponibles en el emulador.')
        decl.array = true
        if (accept('=')) {
          if (type === 'char' && peek().kind === 'str') throw unsupported(name.line, 'los textos como arreglo de char aún no están disponibles: usa String.')
          expect('{')
          decl.items = []
          if (!is('}')) {
            do {
              if (is('}')) break
              decl.items.push(assignment())
            } while (accept(','))
          }
          expect('}')
        } else if (decl.size === null) throw compileError(name.line, `el arreglo «${name.text}» necesita un tamaño o valores iniciales.`)
      } else if (accept('=')) decl.init = assignment()
      else if (is('(')) {
        next()
        decl.init = assignment()
        expect(')')
      }
      if (isConst && !decl.array && !decl.init) throw compileError(name.line, `la constante «${name.text}» necesita un valor inicial.`)
      list.push(decl)
      if (!accept(',')) return list
      name = identifier('un nombre')
    }
  }

  function block() {
    const line = expect('{').line
    const body = []
    while (!is('}')) {
      if (peek().kind === 'end') throw compileError(peek().line, 'falta «}» para cerrar un bloque.')
      body.push(statement())
    }
    next()
    return { kind: 'block', body, line }
  }

  function statement() {
    const token = peek()
    const line = token.line
    if (is('{')) return block()
    if (accept(';')) return { kind: 'empty', line }
    if (startsType() && !(is('(', 1))) {
      const { type, isConst } = typeWords()
      const name = identifier('un nombre')
      if (is('(')) throw compileError(line, `no se puede definir la función «${name.text}» dentro de otra: seguramente falta una «}» antes de la línea ${line}.`)
      const decls = declarators(type, isConst, name)
      expect(';')
      return { kind: 'decls', decls, line }
    }
    if (accept('if')) {
      expect('(')
      const cond = expression()
      expect(')')
      const then = statement()
      const otherwise = accept('else') ? statement() : null
      return { kind: 'if', cond, then, otherwise, line }
    }
    if (accept('while')) {
      expect('(')
      const cond = expression()
      expect(')')
      return { kind: 'while', cond, body: statement(), line }
    }
    if (accept('do')) {
      const body = statement()
      if (!accept('while')) throw compileError(peek().line, `se esperaba «while» después del cuerpo de do y se encontró ${describe(peek())}.`)
      expect('(')
      const cond = expression()
      expect(')')
      expect(';')
      return { kind: 'do', cond, body, line }
    }
    if (accept('for')) {
      expect('(')
      let init = null
      if (startsType()) {
        const { type, isConst } = typeWords()
        init = { kind: 'decls', decls: declarators(type, isConst, identifier('un nombre')), line }
      } else if (!is(';')) init = { kind: 'expr', expr: expression(), line }
      expect(';')
      const cond = is(';') ? null : expression()
      expect(';')
      const update = is(')') ? null : expression()
      expect(')')
      return { kind: 'for', init, cond, update, body: statement(), line }
    }
    if (accept('switch')) {
      expect('(')
      const value = expression()
      expect(')')
      expect('{')
      const cases = []
      while (!is('}')) {
        const caseLine = peek().line
        if (accept('case')) {
          const test = expression()
          expect(':')
          cases.push({ test, body: [], line: caseLine })
        } else if (accept('default')) {
          expect(':')
          cases.push({ test: null, body: [], line: caseLine })
        } else {
          if (cases.length === 0) throw compileError(caseLine, 'dentro de switch las instrucciones van después de un «case …:».')
          if (peek().kind === 'end') throw compileError(caseLine, 'falta «}» para cerrar el switch.')
          cases.at(-1).body.push(statement())
        }
      }
      next()
      return { kind: 'switch', value, cases, line }
    }
    if (accept('break')) {
      expect(';')
      return { kind: 'break', line }
    }
    if (accept('continue')) {
      expect(';')
      return { kind: 'continue', line }
    }
    if (accept('return')) {
      const value = is(';') ? null : expression()
      expect(';')
      return { kind: 'return', value, line }
    }
    if (['else', 'case', 'default'].includes(token.text) && token.kind === 'id') throw compileError(line, `«${token.text}» sin el ${token.text === 'else' ? 'if' : 'switch'} que le corresponde.`)
    const expr = expression()
    expect(';')
    return { kind: 'expr', expr, line }
  }

  const expression = () => {
    const first = assignment()
    if (!is(',')) return first
    const list = [first]
    while (accept(',')) list.push(assignment())
    return { kind: 'comma', list, line: first.line }
  }

  const ASSIGN = new Set(['=', '+=', '-=', '*=', '/=', '%=', '&=', '|=', '^=', '<<=', '>>='])
  function assignment() {
    const target = ternary()
    const token = peek()
    if (token.kind === 'op' && ASSIGN.has(token.text)) {
      next()
      return { kind: 'assign', op: token.text, target, value: assignment(), line: token.line }
    }
    return target
  }

  function ternary() {
    const cond = binary(0)
    if (!accept('?')) return cond
    const yes = assignment()
    expect(':')
    return { kind: 'ternary', cond, yes, no: assignment(), line: cond.line }
  }

  const LEVELS = [['||'], ['&&'], ['|'], ['^'], ['&'], ['==', '!='], ['<', '>', '<=', '>='], ['<<', '>>'], ['+', '-'], ['*', '/', '%']]
  function binary(level) {
    if (level === LEVELS.length) return unary()
    let left = binary(level + 1)
    for (;;) {
      const token = peek()
      if (token.kind !== 'op' || !LEVELS[level].includes(token.text)) return left
      next()
      left = { kind: 'binary', op: token.text, left, right: binary(level + 1), line: token.line }
    }
  }

  function unary() {
    const token = peek()
    if (token.kind === 'op' && ['!', '-', '+', '~'].includes(token.text)) {
      next()
      return { kind: 'unary', op: token.text, value: unary(), line: token.line }
    }
    if (token.kind === 'op' && (token.text === '++' || token.text === '--')) {
      next()
      return { kind: 'update', op: token.text, prefix: true, target: unary(), line: token.line }
    }
    if (token.kind === 'op' && (token.text === '&' || token.text === '*')) throw unsupported(token.line, 'los punteros (& y *) aún no están disponibles en el emulador.')
    // (int) x
    if (is('(') && peek(1).kind === 'id' && TYPE_WORDS.has(peek(1).text)) {
      next()
      const { type } = typeWords()
      expect(')')
      return { kind: 'cast', type, value: unary(), line: token.line }
    }
    return postfix()
  }

  function postfix() {
    let node = primary()
    for (;;) {
      const token = peek()
      if (is('(')) {
        next()
        const args = []
        if (!is(')')) {
          do args.push(assignment())
          while (accept(','))
        }
        expect(')')
        if (node.kind !== 'name' && node.kind !== 'member') throw compileError(token.line, 'solo se puede llamar a una función por su nombre.')
        node = { kind: 'call', name: node.name, args, line: node.line }
      } else if (is('[')) {
        next()
        const index = expression()
        expect(']')
        node = { kind: 'index', target: node, index, line: token.line }
      } else if (is('.')) {
        next()
        const member = identifier('el nombre de una función')
        if (node.kind !== 'name') throw unsupported(token.line, 'esa forma de usar «.» aún no está disponible en el emulador.')
        node = { kind: 'member', name: `${node.name}.${member.text}`, line: token.line }
      } else if (token.kind === 'op' && (token.text === '++' || token.text === '--')) {
        next()
        node = { kind: 'update', op: token.text, prefix: false, target: node, line: token.line }
      } else return node
    }
  }

  function primary() {
    const token = next()
    const { line } = token
    if (token.kind === 'num') return { kind: 'num', value: token.value, type: token.float ? 'float' : token.long || token.value > 32767 ? 'long' : 'int', line }
    if (token.kind === 'char') return { kind: 'num', value: token.value, type: 'char', line }
    if (token.kind === 'str') return { kind: 'str', value: token.value, line }
    if (token.kind === 'op' && token.text === '(') {
      const inner = expression()
      expect(')')
      return inner
    }
    if (token.kind === 'id') {
      if (token.text === 'true' || token.text === 'false') return { kind: 'num', value: token.text === 'true' ? 1 : 0, type: 'bool', line }
      if (token.text === 'sizeof') {
        expect('(')
        if (startsType()) {
          const { type } = typeWords()
          expect(')')
          return { kind: 'num', value: SIZES[type] ?? 2, type: 'uint', line }
        }
        const target = expression()
        expect(')')
        return { kind: 'sizeof', target, line }
      }
      // int(x), String(x): conversión con forma de función.
      if (TYPE_WORDS.has(token.text) && is('(')) {
        const { type } = normalizeType([token.text], line)
        return { kind: 'name', name: `@${type}`, line }
      }
      if (KEYWORDS.has(token.text) || TYPE_WORDS.has(token.text)) throw compileError(line, `«${token.text}» no se esperaba aquí.`)
      return { kind: 'name', name: token.text, line }
    }
    throw compileError(line, token.kind === 'end' ? 'el programa terminó antes de tiempo: revisa que cada «(» y «{» se cierre.' : `${describe(token)} no se esperaba aquí.`)
  }

  return program()
}

const KEYWORDS = new Set(['if', 'else', 'while', 'do', 'for', 'switch', 'case', 'default', 'break', 'continue', 'return', 'sizeof', 'true', 'false', 'class', 'struct', 'new', 'delete', 'goto'])

// ---------------------------------------------------------------- Revisión (lo que avisa el compilador)

const CONSTANTS = {
  HIGH: ['int', 1], LOW: ['int', 0], INPUT: ['int', 0], OUTPUT: ['int', 1], INPUT_PULLUP: ['int', 2], LED_BUILTIN: ['int', 13],
  A0: ['int', 14], A1: ['int', 15], A2: ['int', 16], A3: ['int', 17], A4: ['int', 18], A5: ['int', 19],
  DEC: ['int', 10], HEX: ['int', 16], BIN: ['int', 2], OCT: ['int', 8], PI: ['float', Math.PI], HALF_PI: ['float', Math.PI / 2], TWO_PI: ['float', Math.PI * 2],
}

const NUMERIC = (types) => (types.includes('float') ? 'float' : types.some((t) => t === 'long' || t === 'ulong') ? 'long' : 'int')
// [mínimo, máximo] de argumentos y tipo del resultado (o función de los tipos de los argumentos).
const BUILTINS = {
  pinMode: [2, 2, 'void'], digitalWrite: [2, 2, 'void'], digitalRead: [1, 1, 'int'], analogRead: [1, 1, 'int'], analogWrite: [2, 2, 'void'],
  delay: [1, 1, 'void'], delayMicroseconds: [1, 1, 'void'], millis: [0, 0, 'ulong'], micros: [0, 0, 'ulong'],
  tone: [2, 3, 'void'], noTone: [1, 1, 'void'],
  'Serial.begin': [1, 2, 'void'], 'Serial.print': [1, 2, 'void'], 'Serial.println': [0, 2, 'void'], 'Serial.available': [0, 0, 'int'], 'Serial.read': [0, 0, 'int'],
  map: [5, 5, 'long'], constrain: [3, 3, NUMERIC], min: [2, 2, NUMERIC], max: [2, 2, NUMERIC], abs: [1, 1, NUMERIC], sq: [1, 1, NUMERIC],
  sqrt: [1, 1, 'float'], pow: [2, 2, 'float'], sin: [1, 1, 'float'], cos: [1, 1, 'float'], tan: [1, 1, 'float'], floor: [1, 1, 'float'], ceil: [1, 1, 'float'], round: [1, 1, 'long'],
  random: [1, 2, 'long'], randomSeed: [1, 1, 'void'],
  '@int': [1, 1, 'int'], '@uint': [1, 1, 'uint'], '@long': [1, 1, 'long'], '@ulong': [1, 1, 'ulong'], '@byte': [1, 1, 'byte'], '@char': [1, 1, 'char'], '@bool': [1, 1, 'bool'], '@float': [1, 1, 'float'], '@string': [1, 2, 'string'],
}

function check(program) {
  const functions = new Map()
  for (const fn of program.functions) {
    if (functions.has(fn.name)) throw compileError(fn.line, `la función «${fn.name}» está definida dos veces.`)
    if (BUILTINS[fn.name] || CONSTANTS[fn.name]) throw compileError(fn.line, `«${fn.name}» ya es parte de Arduino; usa otro nombre.`)
    functions.set(fn.name, fn)
  }
  for (const name of ['setup', 'loop']) {
    const fn = functions.get(name)
    if (!fn) throw compileError(0, `falta la función «void ${name}()». Todo programa de Arduino necesita setup() y loop().`)
    if (fn.params.length > 0 || fn.type !== 'void') throw compileError(fn.line, `${name} debe escribirse «void ${name}()», sin parámetros.`)
  }

  const scopes = [new Map()]
  const lookup = (name) => {
    for (let i = scopes.length - 1; i >= 0; i -= 1) if (scopes[i].has(name)) return scopes[i].get(name)
    return null
  }
  const declare = (decl) => {
    const scope = scopes.at(-1)
    if (scope.has(decl.name)) throw compileError(decl.line, `«${decl.name}» ya está declarada en esta parte del programa.`)
    if (functions.has(decl.name) || BUILTINS[decl.name] || CONSTANTS[decl.name]) throw compileError(decl.line, `«${decl.name}» ya es el nombre de una función o constante; usa otro nombre.`)
    scope.set(decl.name, decl)
  }
  let fn = null
  let loops = 0
  let switches = 0

  function checkDecl(decl) {
    if (decl.array) {
      if (decl.size) {
        expr(decl.size)
        if (!INTEGER.has(decl.size.type)) throw compileError(decl.line, 'el tamaño de un arreglo debe ser un número entero.')
      }
      decl.items?.forEach((item) => {
        expr(item)
        assignable(decl.type, item, decl.line)
      })
      if (decl.size?.kind === 'num' && decl.items && decl.items.length > decl.size.value) throw compileError(decl.line, `el arreglo «${decl.name}» tiene más valores que posiciones.`)
    } else if (decl.init) {
      expr(decl.init)
      assignable(decl.type, decl.init, decl.line)
    }
    declare(decl)
  }

  function assignable(type, value, line) {
    if (value.type === 'void') throw compileError(line, 'esa función no devuelve un valor para guardar.')
    if (value.type === 'array') throw compileError(line, 'un arreglo completo no se puede guardar así: usa una posición, por ejemplo a[0].')
    if (value.type === 'string' && type !== 'string') throw compileError(line, `no se puede guardar un texto en una variable ${TYPE_NAMES[type]}.`)
    if (value.type === 'text' && type !== 'string') throw compileError(line, `no se puede guardar un texto en una variable ${TYPE_NAMES[type]}; para textos usa String.`)
  }

  function lvalue(node, line) {
    if (node.kind === 'name') {
      const decl = lookup(node.name)
      if (!decl) return expr(node)
      if (decl.isConst) throw compileError(line, `«${node.name}» es constante: no se puede cambiar.`)
      if (decl.array) throw compileError(line, `«${node.name}» es un arreglo: cambia una posición, por ejemplo ${node.name}[0].`)
      node.type = decl.type
      return node.type
    }
    if (node.kind === 'index') {
      expr(node)
      if (lookup(node.target.name)?.isConst) throw compileError(line, `«${node.target.name}» es constante: no se puede cambiar.`)
      return node.type
    }
    throw compileError(line, 'a la izquierda de «=» debe ir una variable.')
  }

  function expr(node) {
    switch (node.kind) {
      case 'num':
        return node.type
      case 'str':
        node.type = 'text'
        return node.type
      case 'name': {
        const decl = lookup(node.name)
        if (decl) {
          node.type = decl.array ? 'array' : decl.type
          return node.type
        }
        if (CONSTANTS[node.name]) {
          node.constant = CONSTANTS[node.name][1]
          node.type = CONSTANTS[node.name][0]
          return node.type
        }
        if (functions.has(node.name) || BUILTINS[node.name]) throw compileError(node.line, `«${node.name}» es una función: para usarla escribe ${node.name}(…).`)
        throw compileError(node.line, `«${node.name}» no está declarada. Declárala antes de usarla (por ejemplo «int ${node.name} = 0;») o revisa cómo está escrita.`)
      }
      case 'member':
        throw compileError(node.line, `«${node.name}» es una función: escribe ${node.name}(…).`)
      case 'index': {
        if (node.target.kind !== 'name' || !lookup(node.target.name)?.array) throw compileError(node.line, 'solo se pueden usar corchetes [ ] con un arreglo.')
        expr(node.index)
        if (!INTEGER.has(node.index.type)) throw compileError(node.line, 'la posición de un arreglo debe ser un número entero.')
        node.type = lookup(node.target.name).type
        return node.type
      }
      case 'sizeof': {
        if (node.target.kind === 'name' && lookup(node.target.name)) {
          const decl = lookup(node.target.name)
          node.decl = decl
        } else expr(node.target)
        node.type = 'uint'
        return node.type
      }
      case 'call': {
        const builtin = BUILTINS[node.name]
        const user = functions.get(node.name)
        if (!builtin && !user) {
          if (node.name.includes('.')) throw unsupported(node.line, `«${node.name}» aún no está disponible en el emulador.`)
          throw compileError(node.line, `la función «${node.name}» no existe. Revisa cómo está escrita (Arduino distingue mayúsculas y minúsculas).`)
        }
        const [least, most] = builtin ?? [user.params.length, user.params.length]
        if (node.args.length < least || node.args.length > most) {
          const expected = least === most ? `${least}` : `de ${least} a ${most}`
          throw compileError(node.line, `${node.name.replace(/^@/, '')}() recibe ${expected} ${most === 1 ? 'valor' : 'valores'} y le diste ${node.args.length}.`)
        }
        const types = node.args.map((arg) => expr(arg))
        node.args.forEach((arg, index) => {
          if (arg.type === 'void') throw compileError(node.line, 'una función sin valor (void) no puede usarse como dato.')
          if (arg.type === 'array') throw compileError(node.line, 'pasar un arreglo completo aún no está disponible en el emulador.')
          const printing = node.name === 'Serial.print' || node.name === 'Serial.println' || node.name === '@string'
          if (!user && (arg.type === 'text' || arg.type === 'string') && !(printing && index === 0)) throw compileError(node.line, `${node.name.replace(/^@/, '')}() necesita un número, no un texto.`)
          if (user) assignable(user.params[index].type, arg, node.line)
        })
        node.type = builtin ? (typeof builtin[2] === 'function' ? builtin[2](types) : builtin[2]) : user.type
        return node.type
      }
      case 'assign': {
        const type = lvalue(node.target, node.line)
        expr(node.value)
        if (node.op === '=') assignable(type, node.value, node.line)
        else if (type === 'string' ? node.op !== '+=' : node.value.type === 'string' || node.value.type === 'text') throw compileError(node.line, `«${node.op}» no sirve con textos.`)
        node.type = type
        return type
      }
      case 'update': {
        const type = lvalue(node.target, node.line)
        if (type === 'string') throw compileError(node.line, `«${node.op}» no sirve con textos.`)
        node.type = type
        return type
      }
      case 'unary': {
        const type = expr(node.value)
        if (type === 'string' || type === 'text') throw compileError(node.line, `«${node.op}» no sirve con textos.`)
        node.type = node.op === '!' ? 'bool' : type === 'float' ? 'float' : NUMERIC([type])
        return node.type
      }
      case 'cast':
        if (expr(node.value) === 'text' || node.value.type === 'string') throw compileError(node.line, 'un texto no se puede convertir así a número.')
        return node.type
      case 'binary': {
        const left = expr(node.left)
        const right = expr(node.right)
        if (left === 'void' || right === 'void') throw compileError(node.line, 'una función sin valor (void) no puede usarse como dato.')
        const texts = [left, right].filter((t) => t === 'string' || t === 'text').length
        if (texts > 0) {
          if (node.op === '+' && (left === 'string' || right === 'string')) node.type = 'string'
          else if (node.op === '+' && texts === 2) throw compileError(node.line, 'para unir dos textos entre comillas, convierte uno con String(…).')
          else if ((node.op === '==' || node.op === '!=') && left === 'string' && right !== 'float') node.type = 'bool'
          else throw compileError(node.line, `«${node.op}» no sirve con textos${node.op === '+' ? '; para unir texto y número usa String(…), por ejemplo "Valor: " + String(x)' : ''}.`)
          return node.type
        }
        if (['&&', '||', '==', '!=', '<', '>', '<=', '>='].includes(node.op)) node.type = 'bool'
        else if (['%', '&', '|', '^', '<<', '>>'].includes(node.op)) {
          if (left === 'float' || right === 'float') throw compileError(node.line, `«${node.op}» solo sirve con números enteros.`)
          node.type = NUMERIC([left, right])
        } else node.type = NUMERIC([left, right])
        // Si alguno es unsigned long, la cuenta es unsigned long (millis() - inicio).
        if (node.type === 'long' && (left === 'ulong' || right === 'ulong')) node.type = 'ulong'
        return node.type
      }
      case 'ternary': {
        expr(node.cond)
        const yes = expr(node.yes)
        const no = expr(node.no)
        node.type = yes === no ? yes : yes === 'string' || no === 'string' ? 'string' : NUMERIC([yes, no])
        return node.type
      }
      case 'comma':
        node.list.forEach(expr)
        node.type = node.list.at(-1).type
        return node.type
      default:
        throw compileError(node.line, 'expresión desconocida.')
    }
  }

  function stmt(node) {
    switch (node.kind) {
      case 'block':
        scopes.push(new Map())
        node.body.forEach(stmt)
        scopes.pop()
        break
      case 'decls':
        node.decls.forEach(checkDecl)
        break
      case 'expr':
        expr(node.expr)
        break
      case 'if':
        expr(node.cond)
        stmt(node.then)
        if (node.otherwise) stmt(node.otherwise)
        break
      case 'while':
      case 'do':
        expr(node.cond)
        loops += 1
        stmt(node.body)
        loops -= 1
        break
      case 'for':
        scopes.push(new Map())
        if (node.init) stmt(node.init)
        if (node.cond) expr(node.cond)
        if (node.update) expr(node.update)
        loops += 1
        stmt(node.body)
        loops -= 1
        scopes.pop()
        break
      case 'switch':
        if (!INTEGER.has(expr(node.value))) throw compileError(node.line, 'switch solo funciona con números enteros o caracteres.')
        switches += 1
        scopes.push(new Map())
        node.cases.forEach((item) => {
          if (item.test) {
            expr(item.test)
            if (!['num', 'name'].includes(item.test.kind) && !(item.test.kind === 'unary' && item.test.value.kind === 'num')) throw compileError(item.line, 'cada case lleva un valor fijo, por ejemplo «case 1:» o «case \'a\':».')
          }
          item.body.forEach(stmt)
        })
        scopes.pop()
        switches -= 1
        break
      case 'break':
        if (loops === 0 && switches === 0) throw compileError(node.line, 'break solo puede ir dentro de un ciclo o un switch.')
        break
      case 'continue':
        if (loops === 0) throw compileError(node.line, 'continue solo puede ir dentro de un ciclo.')
        break
      case 'return':
        if (node.value) {
          expr(node.value)
          if (fn.type === 'void') throw compileError(node.line, `${fn.name}() es void: su return no lleva valor.`)
          assignable(fn.type, node.value, node.line)
        } else if (fn.type !== 'void') throw compileError(node.line, `${fn.name}() debe devolver un valor ${TYPE_NAMES[fn.type]}.`)
        break
      case 'empty':
        break
      default:
        throw compileError(node.line, 'instrucción desconocida.')
    }
  }

  program.globals.forEach(checkDecl)
  for (const item of program.functions) {
    fn = item
    scopes.push(new Map())
    item.params.forEach((param) => declare({ ...param, kind: 'decl' }))
    item.body.body.forEach(stmt)
    scopes.pop()
  }
  return program
}

// Compila el código: devuelve el programa listo para ejecutar o lanza ArduinoError ('compile' o 'unsupported').
export function compileArduino(source) {
  if (!source.trim()) throw compileError(0, 'el programa está vacío.')
  return check(parse(lex(source)))
}

// ---------------------------------------------------------------- Ejecución

// Placa virtual: modo y valor de cada pin, entradas que pone la página (botones y potenciómetros) y el Monitor Serie.
export function createBoard() {
  const board = {
    modes: Array(PIN_COUNT).fill(null), // null, 'INPUT', 'OUTPUT', 'INPUT_PULLUP'
    values: Array(PIN_COUNT).fill(0), // salida: 0/1 digital o 0..255 con analogWrite
    pwm: Array(PIN_COUNT).fill(false),
    tones: Array(PIN_COUNT).fill(0),
    pressed: Array(PIN_COUNT).fill(false), // botón conectado al pin (a 5 V si INPUT, a tierra si INPUT_PULLUP)
    analog: Array(6).fill(512), // potenciómetros en A0..A5
    analogUsed: new Set(),
    serial: '',
    serialInput: '',
    serialBegun: false,
    warnings: [],
    // Lo que hace la persona en la placa mientras corre el programa.
    press: (pin, down) => {
      board.pressed[pin] = down
    },
    turn: (index, value) => {
      board.analog[index] = value
    },
    send: (text) => {
      board.serialInput += text
    },
  }
  return board
}

const FORMAT_INT = (value, base) => (base && base !== 10 ? (value < 0 ? value >>> 0 : value).toString(base).toUpperCase() : String(value))

function display(value, type, extra) {
  if (type === 'float') {
    if (!Number.isFinite(value)) return Number.isNaN(value) ? 'nan' : value > 0 ? 'inf' : '-inf'
    return value.toFixed(extra ?? 2)
  }
  if (type === 'char') return String.fromCharCode(mod(value, 256))
  if (type === 'string' || type === 'text') return value
  return FORMAT_INT(value, extra)
}

// Crea la ejecución: un generador que cede en cada delay y cada YIELD_EVERY pasos. machine.time es el reloj en µs.
export function createMachine(program, board) {
  const YIELD_EVERY = 2000
  const machine = { time: 0, steps: 0, board }
  const globals = new Map()
  const functions = new Map(program.functions.map((fn) => [fn.name, fn]))
  let depth = 0
  let seed = 1

  const random = () => {
    seed = (seed * 1103515245 + 12345) % 2147483648
    return seed / 2147483648
  }
  const warn = (line, message) => {
    if (!board.warnings.some((item) => item.message === message)) board.warnings.push({ line, message })
  }
  const pinOf = (value, line) => {
    const pin = Math.trunc(value)
    if (pin < 0 || pin >= PIN_COUNT) throw runtimeError(line, `el pin ${pin} no existe en el Arduino Uno (pines 0 a 13 y A0 a A5).`)
    return pin
  }
  const writeSerial = (text, line) => {
    if (!board.serialBegun) warn(line, 'Usaste Serial sin Serial.begin(9600) en setup(): en una placa real no se vería nada.')
    board.serial = (board.serial + text).slice(-LIMITS.serialChars)
  }

  function* tick() {
    machine.steps += 1
    machine.time += 1
    if (machine.steps % YIELD_EVERY === 0) yield 'steps'
  }

  function makeVar(decl, value) {
    return { type: decl.type, isConst: decl.isConst, array: decl.array, value }
  }

  function* declare(scope, decl) {
    if (decl.array) {
      const size = decl.size ? yield* evaluate(scope, decl.size) : decl.items.length
      if (size < 1 || size > LIMITS.arrayLength) throw runtimeError(decl.line, `el arreglo «${decl.name}» debe tener entre 1 y ${LIMITS.arrayLength} posiciones (tiene ${size}).`)
      if (decl.items && decl.items.length > size) throw runtimeError(decl.line, `el arreglo «${decl.name}» tiene más valores que posiciones.`)
      const items = Array(size).fill(decl.type === 'string' ? '' : 0)
      for (let i = 0; i < (decl.items?.length ?? 0); i += 1) items[i] = wrap(decl.type, yield* evaluate(scope, decl.items[i]))
      scope.set(decl.name, makeVar(decl, items))
    } else {
      // Las variables locales sin valor inicial empiezan en 0 (en una placa real tendrían basura).
      const value = decl.init ? yield* evaluate(scope, decl.init) : decl.type === 'string' ? '' : 0
      scope.set(decl.name, makeVar(decl, wrap(decl.type, value)))
    }
  }

  const find = (scope, name) => {
    for (let current = scope; current; current = current.parent) if (current.vars.has(name)) return current.vars.get(name)
    return globals.get(name)
  }
  const makeScope = (parent) => {
    const scope = { vars: new Map(), parent }
    scope.set = (name, variable) => scope.vars.set(name, variable)
    return scope
  }

  function* reference(scope, node) {
    if (node.kind === 'name') {
      const variable = find(scope, node.name)
      return { get: () => variable.value, set: (value) => (variable.value = wrap(variable.type, value)), type: variable.type }
    }
    const variable = find(scope, node.target.name)
    const index = Math.trunc(yield* evaluate(scope, node.index))
    if (index < 0 || index >= variable.value.length) {
      throw runtimeError(node.line, `la posición ${index} no existe en «${node.target.name}» (va de 0 a ${variable.value.length - 1}).`)
    }
    return { get: () => variable.value[index], set: (value) => (variable.value[index] = wrap(variable.type, value)), type: variable.type }
  }

  function arithmetic(op, a, b, node) {
    const integer = node.type !== 'float' && node.type !== 'bool'
    switch (op) {
      case '+':
        return a + b
      case '-':
        return a - b
      case '*':
        return a * b
      case '/':
        if (integer) {
          if (b === 0) throw runtimeError(node.line, 'división entre cero.')
          return Math.trunc(a / b)
        }
        return a / b
      case '%':
        if (b === 0) throw runtimeError(node.line, 'residuo (%) entre cero.')
        return a % b
      case '&':
        return a & b
      case '|':
        return a | b
      case '^':
        return a ^ b
      case '<<':
        return a * 2 ** b
      case '>>':
        return Math.floor(a / 2 ** b)
      default:
        throw runtimeError(node.line, `operador ${op} desconocido.`)
    }
  }

  function* evaluate(scope, node) {
    yield* tick()
    switch (node.kind) {
      case 'num':
        return node.value
      case 'str':
        return node.value
      case 'name':
        if (node.constant !== undefined) return node.constant
        return find(scope, node.name).value
      case 'index': {
        const ref = yield* reference(scope, node)
        return ref.get()
      }
      case 'sizeof': {
        if (node.decl) {
          const variable = find(scope, node.decl.name)
          return SIZES[variable.type] * (variable.array ? variable.value.length : 1)
        }
        return SIZES[node.target.type] ?? 2
      }
      case 'assign': {
        const ref = yield* reference(scope, node.target)
        let value = yield* evaluate(scope, node.value)
        if (node.op !== '=') {
          const op = node.op.slice(0, -1)
          if (ref.type === 'string') value = ref.get() + display(value, node.value.type)
          else value = arithmetic(op, ref.get(), value, { ...node, type: ref.type === 'float' || node.value.type === 'float' ? 'float' : ref.type })
        } else if (ref.type === 'string') value = display(value, node.value.type)
        ref.set(value)
        return ref.get()
      }
      case 'update': {
        const ref = yield* reference(scope, node.target)
        const before = ref.get()
        ref.set(before + (node.op === '++' ? 1 : -1))
        return node.prefix ? ref.get() : before
      }
      case 'unary': {
        const value = yield* evaluate(scope, node.value)
        if (node.op === '!') return value ? 0 : 1
        if (node.op === '~') return wrap(node.type, ~value)
        return node.op === '-' ? wrap(node.type, -value) : value
      }
      case 'cast':
        return wrap(node.type, yield* evaluate(scope, node.value))
      case 'binary': {
        if (node.op === '&&') return (yield* evaluate(scope, node.left)) && (yield* evaluate(scope, node.right)) ? 1 : 0
        if (node.op === '||') return (yield* evaluate(scope, node.left)) || (yield* evaluate(scope, node.right)) ? 1 : 0
        const a = yield* evaluate(scope, node.left)
        const b = yield* evaluate(scope, node.right)
        if (node.type === 'string') return display(a, node.left.type) + display(b, node.right.type)
        switch (node.op) {
          case '==':
            return a === b ? 1 : 0
          case '!=':
            return a !== b ? 1 : 0
          case '<':
            return a < b ? 1 : 0
          case '>':
            return a > b ? 1 : 0
          case '<=':
            return a <= b ? 1 : 0
          case '>=':
            return a >= b ? 1 : 0
          default:
            return wrap(node.type, arithmetic(node.op, a, b, node))
        }
      }
      case 'ternary':
        return (yield* evaluate(scope, node.cond)) ? yield* evaluate(scope, node.yes) : yield* evaluate(scope, node.no)
      case 'comma': {
        let last
        for (const item of node.list) last = yield* evaluate(scope, item)
        return last
      }
      case 'call':
        return yield* call(scope, node)
      default:
        throw runtimeError(node.line, 'expresión desconocida.')
    }
  }

  function* call(scope, node) {
    const args = []
    for (const arg of node.args) args.push(yield* evaluate(scope, arg))
    const user = functions.get(node.name)
    if (user) {
      depth += 1
      if (depth > LIMITS.callDepth) throw runtimeError(node.line, `demasiadas llamadas anidadas a funciones (más de ${LIMITS.callDepth}): revisa si una función se llama a sí misma sin fin.`)
      const local = makeScope(null)
      user.params.forEach((param, index) => local.set(param.name, { type: param.type, value: wrap(param.type, args[index]) }))
      const outcome = yield* run(local, user.body)
      depth -= 1
      if (user.type === 'void') return undefined
      if (outcome?.type !== 'return') throw runtimeError(user.line, `${user.name}() terminó sin devolver un valor con return.`)
      return wrap(user.type, outcome.value)
    }
    const { line } = node
    const [a, b, c, d, e] = args
    switch (node.name) {
      case 'pinMode': {
        const pin = pinOf(a, line)
        const modeName = { 0: 'INPUT', 1: 'OUTPUT', 2: 'INPUT_PULLUP' }[b]
        if (!modeName) throw runtimeError(line, 'el modo de pinMode debe ser INPUT, OUTPUT o INPUT_PULLUP.')
        board.modes[pin] = modeName
        return undefined
      }
      case 'digitalWrite': {
        const pin = pinOf(a, line)
        if (board.modes[pin] !== 'OUTPUT') warn(line, `digitalWrite(${pin}, …) sin pinMode(${pin}, OUTPUT) en setup(): en una placa real el LED casi no encendería.`)
        board.values[pin] = b ? 1 : 0
        board.pwm[pin] = false
        return undefined
      }
      case 'analogWrite': {
        const pin = pinOf(a, line)
        if (![3, 5, 6, 9, 10, 11].includes(pin)) warn(line, `el pin ${pin} no tiene PWM en el Uno (solo 3, 5, 6, 9, 10 y 11): analogWrite solo lo prende o lo apaga.`)
        const value = Math.max(0, Math.min(255, Math.trunc(b)))
        board.values[pin] = [3, 5, 6, 9, 10, 11].includes(pin) ? value : value >= 128 ? 1 : 0
        board.pwm[pin] = [3, 5, 6, 9, 10, 11].includes(pin)
        return undefined
      }
      case 'digitalRead': {
        const pin = pinOf(a, line)
        if (board.modes[pin] === 'OUTPUT') return board.values[pin] ? 1 : 0
        if (board.modes[pin] === 'INPUT_PULLUP') return board.pressed[pin] ? 0 : 1
        return board.pressed[pin] ? 1 : 0
      }
      case 'analogRead': {
        const pin = a >= 14 ? a - 14 : a
        if (pin < 0 || pin > 5) throw runtimeError(line, `analogRead(${a}): las entradas analógicas son A0 a A5.`)
        board.analogUsed.add(pin)
        return board.analog[pin]
      }
      case 'delay':
        if (a < 0) throw runtimeError(line, 'delay() no recibe tiempos negativos.')
        machine.time += Math.trunc(a) * 1000
        yield 'delay'
        return undefined
      case 'delayMicroseconds':
        machine.time += Math.max(0, Math.trunc(a))
        yield 'delay'
        return undefined
      case 'millis':
        return wrap('ulong', Math.floor(machine.time / 1000))
      case 'micros':
        return wrap('ulong', machine.time)
      case 'tone':
        board.tones[pinOf(a, line)] = Math.trunc(b)
        return undefined
      case 'noTone':
        board.tones[pinOf(a, line)] = 0
        return undefined
      case 'Serial.begin':
        board.serialBegun = true
        return undefined
      case 'Serial.print':
      case 'Serial.println': {
        const text = node.args.length ? display(a, node.args[0].type, b) : ''
        writeSerial(node.name === 'Serial.println' ? `${text}\n` : text, line)
        return undefined
      }
      case 'Serial.available':
        return board.serialInput.length
      case 'Serial.read': {
        if (!board.serialInput) return -1
        const code = board.serialInput.charCodeAt(0)
        board.serialInput = board.serialInput.slice(1)
        return code
      }
      case 'map':
        if (c === b) throw runtimeError(line, 'map() con el rango de entrada vacío (los dos primeros límites son iguales).')
        return wrap('long', Math.trunc(((a - b) * (e - d)) / (c - b)) + d)
      case 'constrain':
        return Math.min(Math.max(a, b), c)
      case 'min':
        return Math.min(a, b)
      case 'max':
        return Math.max(a, b)
      case 'abs':
        return Math.abs(a)
      case 'sq':
        return a * a
      case 'sqrt':
        return Math.sqrt(a)
      case 'pow':
        return a ** b
      case 'sin':
        return Math.sin(a)
      case 'cos':
        return Math.cos(a)
      case 'tan':
        return Math.tan(a)
      case 'floor':
        return Math.floor(a)
      case 'ceil':
        return Math.ceil(a)
      case 'round':
        return Math.round(a)
      case 'random': {
        const [low, high] = node.args.length === 1 ? [0, a] : [a, b]
        if (high <= low) return low
        return low + Math.floor(random() * (high - low))
      }
      case 'randomSeed':
        seed = Math.abs(Math.trunc(a)) % 2147483648 || 1
        return undefined
      case '@string':
        return display(a, node.args[0].type, b)
      default:
        if (node.name.startsWith('@')) return wrap(node.name.slice(1), a)
        throw runtimeError(line, `«${node.name}» aún no está disponible en el emulador.`)
    }
  }

  // Ejecuta una instrucción. Devuelve undefined o { type: 'break' | 'continue' | 'return', value }.
  function* run(scope, node) {
    yield* tick()
    switch (node.kind) {
      case 'block': {
        const inner = makeScope(scope)
        for (const item of node.body) {
          const outcome = yield* run(inner, item)
          if (outcome) return outcome
        }
        return undefined
      }
      case 'decls':
        for (const decl of node.decls) yield* declare(scope, decl)
        return undefined
      case 'expr':
        yield* evaluate(scope, node.expr)
        return undefined
      case 'if':
        if (yield* evaluate(scope, node.cond)) return yield* run(scope, node.then)
        return node.otherwise ? yield* run(scope, node.otherwise) : undefined
      case 'while':
        while (yield* evaluate(scope, node.cond)) {
          const outcome = yield* run(scope, node.body)
          if (outcome?.type === 'break') break
          if (outcome?.type === 'return') return outcome
        }
        return undefined
      case 'do':
        do {
          const outcome = yield* run(scope, node.body)
          if (outcome?.type === 'break') break
          if (outcome?.type === 'return') return outcome
        } while (yield* evaluate(scope, node.cond))
        return undefined
      case 'for': {
        const inner = makeScope(scope)
        if (node.init) yield* run(inner, node.init)
        while (!node.cond || (yield* evaluate(inner, node.cond))) {
          const outcome = yield* run(inner, node.body)
          if (outcome?.type === 'break') break
          if (outcome?.type === 'return') return outcome
          if (node.update) yield* evaluate(inner, node.update)
        }
        return undefined
      }
      case 'switch': {
        const value = yield* evaluate(scope, node.value)
        let start = -1
        for (let i = 0; i < node.cases.length && start < 0; i += 1) {
          if (node.cases[i].test && (yield* evaluate(scope, node.cases[i].test)) === value) start = i
        }
        if (start < 0) start = node.cases.findIndex((item) => !item.test)
        if (start < 0) return undefined
        const inner = makeScope(scope)
        for (let i = start; i < node.cases.length; i += 1) {
          for (const item of node.cases[i].body) {
            const outcome = yield* run(inner, item)
            if (outcome?.type === 'break') return undefined
            if (outcome) return outcome
          }
        }
        return undefined
      }
      case 'break':
        return { type: 'break' }
      case 'continue':
        return { type: 'continue' }
      case 'return':
        return { type: 'return', value: node.value ? yield* evaluate(scope, node.value) : undefined }
      case 'empty':
        return undefined
      default:
        throw runtimeError(node.line, 'instrucción desconocida.')
    }
  }

  function* main() {
    const scope = { vars: globals, parent: null, set: (name, variable) => globals.set(name, variable) }
    for (const decl of program.globals) yield* declare(scope, decl)
    const setup = { kind: 'call', name: 'setup', args: [], line: 0 }
    const loop = { kind: 'call', name: 'loop', args: [], line: 0 }
    yield* call(scope, setup)
    for (;;) {
      yield* call(scope, loop)
      yield 'loop'
    }
  }

  machine.generator = main()
  // Avanza hasta que el reloj llegue a untilTime (µs) o se gasten maxSteps pasos. Lanza ArduinoError si falla.
  machine.advance = (untilTime, maxSteps) => {
    const limit = machine.steps + maxSteps
    while (machine.time < untilTime && machine.steps < limit) machine.generator.next()
  }
  return machine
}

// Para las pruebas: compila, ejecuta durante ms milisegundos simulados y devuelve la placa.
export function simulate(source, ms, { maxSteps = 5_000_000, prepare } = {}) {
  const board = createBoard()
  prepare?.(board)
  const machine = createMachine(compileArduino(source), board)
  machine.advance(ms * 1000, maxSteps)
  return { board, machine }
}
