// Colores de sintaxis de PSeInt para el editor del Emulador. Divide el texto en piezas (comentario, texto, número,
// palabra clave, función) sin cambiar ni un carácter, para que el resaltado quede exactamente debajo del área de texto.

const KEYWORDS = new Set(
  `algoritmo finalgoritmo proceso finproceso subproceso finsubproceso funcion función finfuncion finfunción definir como
  entero real caracter carácter logico lógico cadena texto numero número numerico numérico dimension dimensión leer escribir
  imprimir mostrar sin saltar sinsaltar si entonces sino finsi segun según hacer de otro modo finsegun finsegún mientras
  finmientras repetir hasta que para con paso finpara cada verdadero falso y o no mod por valor referencia es`
    .split(/\s+/)
    .filter(Boolean),
)
const FUNCTIONS = new Set(
  `rc raiz raíz abs trunc redon azar aleatorio longitud subcadena mayusculas mayúsculas minusculas minúsculas concatenar
  convertiranumero convertiratexto sen cos tan asen acos atan ln exp`
    .split(/\s+/)
    .filter(Boolean),
)

const TOKEN = /(\/\/[^\n]*)|("[^"\n]*"?|'[^'\n]*'?)|(\b\d+(?:\.\d+)?\b)|([A-Za-zÁÉÍÓÚÜÑáéíóúüñ_][\wÁÉÍÓÚÜÑáéíóúüñ]*)|(<-|←|<=|>=|<>|[-+*/^=<>&|~%])/g

function pseintTokens(text) {
  const parts = []
  let last = 0
  for (const match of text.matchAll(TOKEN)) {
    if (match.index > last) parts.push(['', text.slice(last, match.index)])
    const [word, comment, string, number, name] = match
    let kind = 'op'
    if (comment) kind = 'comment'
    else if (string) kind = 'string'
    else if (number) kind = 'number'
    else if (name) kind = KEYWORDS.has(name.toLowerCase()) ? 'keyword' : FUNCTIONS.has(name.toLowerCase()) ? 'function' : 'name'
    parts.push([kind, word])
    last = match.index + word.length
  }
  if (last < text.length) parts.push(['', text.slice(last)])
  return parts
}

export default function PseintHighlight({ text }) {
  return pseintTokens(text).map(([kind, piece], index) =>
    kind ? (
      <span key={index} className={`tok tok--${kind}`}>
        {piece}
      </span>
    ) : (
      piece
    ),
  )
}
