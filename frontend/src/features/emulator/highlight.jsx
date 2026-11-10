// Colores de sintaxis del Emulador (PSeInt y Arduino). Divide el texto en piezas (comentario, texto, número, palabra
// clave, función) sin cambiar ni un carácter, para que el resaltado quede exactamente debajo del área de texto.

const words = (text) => new Set(text.split(/\s+/).filter(Boolean))

const PSEINT = {
  keywords: words(`algoritmo finalgoritmo proceso finproceso subproceso finsubproceso funcion función finfuncion finfunción definir como
  entero real caracter carácter logico lógico cadena texto numero número numerico numérico dimension dimensión leer escribir
  imprimir mostrar sin saltar sinsaltar si entonces sino finsi segun según hacer de otro modo finsegun finsegún mientras
  finmientras repetir hasta que para con paso finpara cada verdadero falso y o no mod por valor referencia es`),
  functions: words(`rc raiz raíz abs trunc redon azar aleatorio longitud subcadena mayusculas mayúsculas minusculas minúsculas concatenar
  convertiranumero convertiratexto sen cos tan asen acos atan ln exp`),
  constants: new Set(),
  ignoreCase: true,
  token: /(\/\/[^\n]*)|("[^"\n]*"?|'[^'\n]*'?)|(\b\d+(?:\.\d+)?\b)|([A-Za-zÁÉÍÓÚÜÑáéíóúüñ_][\wÁÉÍÓÚÜÑáéíóúüñ]*)|(<-|←|<=|>=|<>|[-+*/^=<>&|~%])/g,
}

const ARDUINO = {
  keywords: words(`void int long short unsigned signed byte char bool boolean float double String word size_t uint8_t int8_t uint16_t
  int16_t uint32_t int32_t const static volatile if else while do for switch case default break continue return sizeof`),
  functions: words(`setup loop pinMode digitalWrite digitalRead analogRead analogWrite delay delayMicroseconds millis micros tone noTone
  map constrain min max abs sq sqrt pow sin cos tan floor ceil round random randomSeed Serial begin print println available read`),
  constants: words('HIGH LOW INPUT OUTPUT INPUT_PULLUP LED_BUILTIN A0 A1 A2 A3 A4 A5 true false DEC HEX BIN OCT PI HALF_PI TWO_PI'),
  ignoreCase: false,
  token: /(\/\/[^\n]*|\/\*[\s\S]*?(?:\*\/|$)|^[ \t]*#[^\n]*)|("(?:[^"\\\n]|\\.)*"?|'(?:[^'\\\n]|\\.)*'?)|(\b(?:0[xX][\da-fA-F]+|\d+(?:\.\d+)?)[uUlLfF]*\b)|([A-Za-z_]\w*)|([-+*/%=<>!&|^~?:]+)/gm,
}

function tokens(text, language) {
  const parts = []
  let last = 0
  for (const match of text.matchAll(language.token)) {
    if (match.index > last) parts.push(['', text.slice(last, match.index)])
    const [word, comment, string, number, name] = match
    let kind = 'op'
    if (comment) kind = comment.trimStart().startsWith('#') ? 'keyword' : 'comment'
    else if (string) kind = 'string'
    else if (number) kind = 'number'
    else if (name) {
      const key = language.ignoreCase ? name.toLowerCase() : name
      kind = language.keywords.has(key) ? 'keyword' : language.functions.has(key) ? 'function' : language.constants.has(key) ? 'number' : 'name'
    }
    parts.push([kind, word])
    last = match.index + word.length
  }
  if (last < text.length) parts.push(['', text.slice(last)])
  return parts
}

function render(text, language) {
  return tokens(text, language).map(([kind, piece], index) =>
    kind ? (
      <span key={index} className={`tok tok--${kind}`}>
        {piece}
      </span>
    ) : (
      piece
    ),
  )
}

export default function PseintHighlight({ text }) {
  return render(text, PSEINT)
}

export function ArduinoHighlight({ text }) {
  return render(text, ARDUINO)
}
