// Niveles de los juegos. Cada uno tiene un id estable (el avance guardado lo usa).

let blockId = 0
const b = (type, extra = {}) => ({ id: `inicial-${blockId++}`, type, ...extra })

// Juego de bloques (RF-22, RN-19): 20 niveles, cada uno un poco más difícil que el anterior. Va de secuencias
// y rutas a ciclos, ciclos anidados y condiciones, con niveles de depuración entre medio y un laberinto final.
// solution sirve a las pruebas para comprobar que cada nivel se puede resolver con maxBlocks.
const seq = (...types) => types.map((type) => b(type))
const rep = (times, body) => b('repetir', { times, body })
const si = (body, otherwise = []) => b('si', { body, else: otherwise })
const mientras = (body) => b('mientras', { body })
const MOVES = ['avanzar', 'izquierda', 'derecha']
// Sigue un pasillo que gira para los dos lados: si no puede avanzar, gira a la derecha y, si tampoco, da media vuelta hacia la izquierda.
const bothWays = (turnBack = ['izquierda', 'izquierda']) => [mientras([si(seq('avanzar'), [b('derecha'), si([], seq(...turnBack))])])]

export const BLOCK_LEVELS = [
  {
    id: 'secuencia-1',
    topic: 'Secuencia',
    title: 'Primeros pasos',
    goal: 'Lleva el robot hasta la meta avanzando en línea recta.',
    grid: ['S...M'],
    facing: 'E',
    maxBlocks: 4,
    allowed: ['avanzar'],
    solution: seq('avanzar', 'avanzar', 'avanzar', 'avanzar'),
  },
  {
    id: 'secuencia-2',
    topic: 'Secuencia',
    title: 'Doblar la esquina',
    goal: 'Avanza, gira y vuelve a avanzar. El orden de los bloques importa.',
    grid: ['S..#', '##.#', '##.M'],
    facing: 'E',
    maxBlocks: 7,
    allowed: MOVES,
    solution: seq('avanzar', 'avanzar', 'derecha', 'avanzar', 'avanzar', 'izquierda', 'avanzar'),
  },
  {
    id: 'secuencia-3',
    topic: 'Secuencia',
    title: 'Zigzag',
    goal: 'Ahora el robot empieza mirando hacia abajo. Izquierda y derecha dependen de hacia dónde mira: ponte en su lugar antes de cada giro.',
    grid: ['S##', '..#', '#..', '##M'],
    facing: 'S',
    maxBlocks: 9,
    allowed: MOVES,
    solution: seq('avanzar', 'izquierda', 'avanzar', 'derecha', 'avanzar', 'izquierda', 'avanzar', 'derecha', 'avanzar'),
  },
  {
    id: 'depuracion-1',
    topic: 'Depuración',
    title: 'Arregla los giros',
    goal: 'Este programa choca. Ejecútalo paso a paso, encuentra el error y corrígelo.',
    grid: ['S..#', '##.#', '##.M'],
    facing: 'E',
    maxBlocks: 7,
    allowed: MOVES,
    start: seq('avanzar', 'avanzar', 'izquierda', 'avanzar', 'avanzar', 'derecha', 'avanzar'),
    solution: seq('avanzar', 'avanzar', 'derecha', 'avanzar', 'avanzar', 'izquierda', 'avanzar'),
  },
  {
    id: 'ruta-1',
    topic: 'Ruta',
    title: 'Laberinto',
    goal: 'Planea la ruta completa antes de ejecutar: cuenta los pasos y los giros. Hay un solo camino.',
    grid: ['S.#...', '#.#.#.', '#...#M'],
    facing: 'E',
    maxBlocks: 16,
    allowed: MOVES,
    solution: seq('avanzar', 'derecha', 'avanzar', 'avanzar', 'izquierda', 'avanzar', 'avanzar', 'izquierda', 'avanzar', 'avanzar', 'derecha', 'avanzar', 'avanzar', 'derecha', 'avanzar', 'avanzar'),
  },
  {
    id: 'ruta-2',
    topic: 'Ruta',
    title: 'El camino corto',
    goal: 'Hay dos caminos a la meta, pero solo te alcanzan los bloques para el más corto. Cuenta antes de armar.',
    grid: ['S.....', '.####.', '.####.', '....M.'],
    facing: 'E',
    maxBlocks: 9,
    allowed: MOVES,
    solution: seq('derecha', 'avanzar', 'avanzar', 'avanzar', 'izquierda', 'avanzar', 'avanzar', 'avanzar', 'avanzar'),
  },
  {
    id: 'ciclo-1',
    topic: 'Ciclo',
    title: 'Pasillo largo',
    goal: 'Son diez pasos, pero solo tienes 2 bloques: usa Repetir.',
    grid: ['S.........M'],
    facing: 'E',
    maxBlocks: 2,
    allowed: ['avanzar', 'repetir'],
    solution: [rep(10, seq('avanzar'))],
  },
  {
    id: 'depuracion-2',
    topic: 'Depuración',
    title: 'Cuenta bien',
    goal: 'El robot se queda corto. Corrige el número de repeticiones.',
    grid: ['S......M'],
    facing: 'E',
    maxBlocks: 2,
    allowed: ['avanzar', 'repetir'],
    start: [rep(5, seq('avanzar'))],
    solution: [rep(7, seq('avanzar'))],
  },
  {
    id: 'ciclo-2',
    topic: 'Ciclo',
    title: 'Escalera',
    goal: 'La escalera repite el mismo patrón. Descúbrelo y repítelo.',
    grid: ['S.##', '#..#', '##..', '###M'],
    facing: 'E',
    maxBlocks: 5,
    allowed: [...MOVES, 'repetir'],
    solution: [rep(3, seq('avanzar', 'derecha', 'avanzar', 'izquierda'))],
  },
  {
    id: 'ciclo-3',
    topic: 'Ciclo',
    title: 'Almenas',
    goal: 'El muro sube y baja siempre igual. Encuentra el tramo que se repite: es más largo que el de la escalera.',
    grid: ['#...#...#', 'S.#...#.M'],
    facing: 'E',
    maxBlocks: 11,
    allowed: [...MOVES, 'repetir'],
    solution: [rep(2, seq('avanzar', 'izquierda', 'avanzar', 'derecha', 'avanzar', 'avanzar', 'derecha', 'avanzar', 'izquierda', 'avanzar'))],
  },
  {
    id: 'ciclo-4',
    topic: 'Ciclo',
    title: 'Cuadrado grande',
    goal: 'Con 4 bloques no alcanza un solo ciclo: pon un Repetir dentro de otro Repetir.',
    grid: ['S....', '####.', '####.', '####.', 'M....'],
    facing: 'E',
    maxBlocks: 4,
    allowed: [...MOVES, 'repetir'],
    solution: [rep(3, [rep(4, seq('avanzar')), b('derecha')])],
  },
  {
    id: 'ciclo-5',
    topic: 'Ciclo',
    title: 'Escalones grandes',
    goal: 'Cada escalón tiene tres pasos. Usa ciclos dentro de un ciclo para que te alcancen 7 bloques.',
    grid: ['S...###', '###.###', '###.###', '###....', '######.', '######.', '######M'],
    facing: 'E',
    maxBlocks: 7,
    allowed: [...MOVES, 'repetir'],
    solution: [rep(2, [rep(3, seq('avanzar')), b('derecha'), rep(3, seq('avanzar')), b('izquierda')])],
  },
  {
    id: 'depuracion-3',
    topic: 'Depuración',
    title: 'Escalones al revés',
    goal: 'Ahora la escalera baja hacia la izquierda y el programa choca. Uno de los ciclos cuenta mal: encuéntralo paso a paso.',
    grid: ['###...S', '###.###', '###.###', '....###', '.######', '.######', 'M######'],
    facing: 'O',
    maxBlocks: 7,
    allowed: [...MOVES, 'repetir'],
    start: [rep(2, [rep(2, seq('avanzar')), b('izquierda'), rep(3, seq('avanzar')), b('derecha')])],
    solution: [rep(2, [rep(3, seq('avanzar')), b('izquierda'), rep(3, seq('avanzar')), b('derecha')])],
  },
  {
    id: 'condicion-1',
    topic: 'Condición',
    title: 'Sigue el pasillo',
    goal: 'Mientras no llegues a la meta: si hay camino adelante, avanza; si no, gira a la derecha.',
    grid: ['S....', '####.', '#M##.', '#....'],
    facing: 'E',
    maxBlocks: 4,
    allowed: [...MOVES, 'si', 'mientras'],
    solution: [mientras([si(seq('avanzar'), seq('derecha'))])],
  },
  {
    id: 'condicion-2',
    topic: 'Condición',
    title: 'Caracol a la izquierda',
    goal: 'Este pasillo siempre dobla hacia el otro lado. Adapta la idea del nivel anterior.',
    grid: ['S#...', '.#.#.', '.#M#.', '.###.', '.....'],
    facing: 'S',
    maxBlocks: 4,
    allowed: [...MOVES, 'si', 'mientras'],
    solution: [mientras([si(seq('avanzar'), seq('izquierda'))])],
  },
  {
    id: 'depuracion-4',
    topic: 'Depuración',
    title: 'El ciclo que no para',
    goal: 'El robot va y viene sin llegar nunca. Mira en qué casilla se devuelve y corrige un solo bloque.',
    grid: ['S....', '####.', '..M#.', '.###.', '.....'],
    facing: 'E',
    maxBlocks: 4,
    allowed: [...MOVES, 'si', 'mientras'],
    start: [mientras([si(seq('avanzar'), seq('izquierda'))])],
    solution: [mientras([si(seq('avanzar'), seq('derecha'))])],
  },
  {
    id: 'condicion-3',
    topic: 'Condición',
    title: 'Cuenta las decisiones',
    goal: 'Sin Mientras: pon un Si dentro de Repetir. Cada vuelta el robot avanza o gira, así que cuenta cuántas vueltas necesita.',
    grid: ['S....', '####.', '####.', '#M...'],
    facing: 'E',
    maxBlocks: 4,
    allowed: [...MOVES, 'repetir', 'si'],
    solution: [rep(12, [si(seq('avanzar'), seq('derecha'))])],
  },
  {
    id: 'condicion-4',
    topic: 'Condición',
    title: 'Curvas para los dos lados',
    goal: 'Este pasillo dobla a la derecha y a la izquierda. Si no puedes avanzar, gira a la derecha; y si tampoco hay camino, da media vuelta. Necesitas un Si dentro de otro Si.',
    grid: ['S..##', '##.##', '##...', '####.', 'M....'],
    facing: 'E',
    maxBlocks: 7,
    allowed: [...MOVES, 'si', 'mientras'],
    solution: bothWays(),
  },
  {
    id: 'depuracion-5',
    topic: 'Depuración',
    title: 'Media vuelta',
    goal: 'El robot gira y gira en la primera esquina. Al programa le falta un bloque: ejecútalo paso a paso y mira hacia dónde queda mirando.',
    grid: ['S##.M', '.##.#', '....#'],
    facing: 'S',
    maxBlocks: 7,
    allowed: [...MOVES, 'si', 'mientras'],
    start: bothWays(['izquierda']),
    solution: bothWays(),
  },
  {
    id: 'ruta-3',
    topic: 'Desafío final',
    title: 'Gran laberinto',
    goal: 'Un pasillo largo con curvas para los dos lados y el robot empieza mirando a la pared. Tienes todos los bloques, pero solo 7.',
    grid: ['S...###', '###.###', '###....', '######.', '##.....', '##.####', 'M..####'],
    facing: 'N',
    maxBlocks: 7,
    allowed: [...MOVES, 'repetir', 'si', 'mientras'],
    solution: bothWays(),
  },
]

// ¿Qué imprime? Las respuestas se comprobaron con el intérprete de LogiCode.
export const OUTPUT_LEVELS = [
  {
    id: 'salida-1',
    title: 'Variables',
    code: 'Algoritmo Uno\n    a <- 3\n    b <- a * 2\n    Escribir a + b\nFinAlgoritmo',
    options: ['6', '9', '32', 'a + b'],
    answer: 1,
    explanation: 'b vale 3 × 2 = 6, y a + b = 3 + 6 = 9. Escribir muestra el valor, no el texto de la expresión.',
  },
  {
    id: 'salida-2',
    title: 'Si … SiNo',
    code: 'Algoritmo ParImpar\n    x <- 7\n    Si x MOD 2 = 0 Entonces\n        Escribir "par"\n    SiNo\n        Escribir "impar"\n    FinSi\nFinAlgoritmo',
    options: ['par', 'impar', 'par impar', '1'],
    answer: 1,
    explanation: '7 MOD 2 es el residuo de 7 / 2, que es 1. Como 1 no es 0, se ejecuta la rama SiNo.',
  },
  {
    id: 'salida-3',
    title: 'Acumulador en un Para',
    code: 'Algoritmo Suma\n    s <- 0\n    Para i <- 1 Hasta 4 Hacer\n        s <- s + i\n    FinPara\n    Escribir s\nFinAlgoritmo',
    options: ['4', '6', '10', '15'],
    answer: 2,
    explanation: 'El ciclo suma 1 + 2 + 3 + 4 = 10. El Para incluye el valor final.',
  },
  {
    id: 'salida-4',
    title: 'Mientras',
    code: 'Algoritmo Triple\n    n <- 1\n    Mientras n < 20 Hacer\n        n <- n * 3\n    FinMientras\n    Escribir n\nFinAlgoritmo',
    options: ['9', '20', '27', '81'],
    answer: 2,
    explanation: 'n pasa por 1, 3, 9 y 27. Con 27 la condición n < 20 ya es falsa y el ciclo termina.',
  },
  {
    id: 'salida-5',
    title: 'Repetir … Hasta Que',
    code: 'Algoritmo Pares\n    c <- 0\n    Repetir\n        c <- c + 2\n    Hasta Que c >= 5\n    Escribir c\nFinAlgoritmo',
    options: ['4', '5', '6', '7'],
    answer: 2,
    explanation: 'c vale 2, luego 4 y luego 6. La condición se revisa al final de cada vuelta: con 6 se cumple c >= 5.',
  },
  {
    id: 'salida-6',
    title: 'Para hacia atrás',
    code: 'Algoritmo Cuenta\n    Para i <- 3 Hasta 1 Con Paso -1 Hacer\n        Escribir Sin Saltar i, " "\n    FinPara\nFinAlgoritmo',
    options: ['1 2 3', '3 2 1', '3 2', '3 2 1 0'],
    answer: 1,
    explanation: 'Con Paso -1 el ciclo baja de 3 a 1, ambos incluidos. Sin Saltar deja todo en la misma línea.',
  },
]

// Ordena el algoritmo: las líneas se muestran revueltas; la primera y la última quedan fijas.
export const ORDER_LEVELS = [
  {
    id: 'orden-1',
    title: 'Sumar dos números',
    first: 'Algoritmo Suma',
    last: 'FinAlgoritmo',
    lines: ['Escribir "Escribe dos números"', 'Leer a, b', 'suma <- a + b', 'Escribir "La suma es ", suma'],
  },
  {
    id: 'orden-2',
    title: '¿Mayor de edad?',
    first: 'Algoritmo Edad',
    last: 'FinAlgoritmo',
    lines: ['Leer edad', 'Si edad >= 18 Entonces', '    Escribir "Mayor de edad"', 'SiNo', '    Escribir "Menor de edad"', 'FinSi'],
  },
  {
    id: 'orden-3',
    title: 'Tabla del 5',
    first: 'Algoritmo Tabla',
    last: 'FinAlgoritmo',
    lines: ['Escribir "Tabla del 5"', 'Para i <- 1 Hasta 10 Hacer', '    Escribir "5 x ", i, " = ", 5 * i', 'FinPara'],
  },
  {
    id: 'orden-4',
    title: 'Adivina el número',
    first: 'Algoritmo Adivina',
    last: 'FinAlgoritmo',
    lines: ['Definir intento Como Entero', 'Repetir', '    Escribir "Adivina el número"', '    Leer intento', 'Hasta Que intento = 7', 'Escribir "¡Correcto!"'],
  },
]

// Completa el diagrama de flujo (DFD). Cada casilla en blanco tiene su respuesta; las opciones son las mismas para todas.
// Un nodo 'decision' tiene dos ramas (yes, no) que se unen en el siguiente nodo.
export const DFD_LEVELS = [
  {
    id: 'dfd-1',
    title: 'Área del rectángulo',
    nodes: [
      { shape: 'start', text: 'Inicio' },
      { shape: 'io', text: 'Leer base, altura' },
      { shape: 'process', blank: 'A', answer: 'área ← base * altura' },
      { shape: 'io', text: 'Escribir área' },
      { shape: 'end', text: 'Fin' },
    ],
    options: ['área ← base + altura', 'área ← base * altura', 'Leer área', '¿base > altura?'],
  },
  {
    id: 'dfd-2',
    title: 'Promedio de tres notas',
    nodes: [
      { shape: 'start', text: 'Inicio' },
      { shape: 'io', text: 'Leer n1, n2, n3' },
      { shape: 'process', blank: 'A', answer: 'prom ← (n1 + n2 + n3) / 3' },
      { shape: 'io', blank: 'B', answer: 'Escribir prom' },
      { shape: 'end', text: 'Fin' },
    ],
    options: ['prom ← n1 + n2 + n3 / 3', 'prom ← (n1 + n2 + n3) / 3', 'Escribir prom', 'Leer prom'],
  },
  {
    id: 'dfd-3',
    title: '¿Aprobó?',
    nodes: [
      { shape: 'start', text: 'Inicio' },
      { shape: 'io', text: 'Leer nota' },
      {
        shape: 'decision',
        blank: 'A',
        answer: '¿nota >= 3?',
        yes: { shape: 'io', text: 'Escribir "Aprobó"' },
        no: { shape: 'io', text: 'Escribir "Reprobó"' },
      },
      { shape: 'end', text: 'Fin' },
    ],
    options: ['¿nota >= 3?', '¿nota < 3?', 'nota ← 3', 'Leer 3'],
  },
  {
    id: 'dfd-4',
    title: 'Par o impar',
    nodes: [
      { shape: 'start', text: 'Inicio' },
      { shape: 'io', text: 'Leer n' },
      {
        shape: 'decision',
        blank: 'A',
        answer: '¿n MOD 2 = 0?',
        yes: { shape: 'io', blank: 'B', answer: 'Escribir "par"' },
        no: { shape: 'io', text: 'Escribir "impar"' },
      },
      { shape: 'end', text: 'Fin' },
    ],
    options: ['¿n / 2 = 0?', '¿n MOD 2 = 0?', 'Escribir "par"', 'n ← n MOD 2'],
  },
]

// Casillas en blanco de un DFD, en orden (A, B, …).
export function dfdBlanks(level) {
  return level.nodes.flatMap((node) => [node, node.yes, node.no].filter((item) => item?.blank))
}

// Revuelve las líneas sin dejarlas en el orden correcto.
export function shuffle(lines) {
  if (lines.length < 2) return [...lines]
  let order = [...lines]
  while (order.every((line, index) => line === lines[index])) {
    order = [...lines]
    for (let i = order.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[order[i], order[j]] = [order[j], order[i]]
    }
  }
  return order
}
