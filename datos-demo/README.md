# Datos de demostración y prueba

`cargar-datos-demo.mjs` llena una instalación de LogiCode con datos ficticios para probar y presentar. Usa la API como lo haría una persona, así que pasa por las mismas reglas y permisos que la aplicación. **Cada etapa agrega su parte a este mismo script**; al final del proyecto queda una instancia completa de presentación.

Todas las personas, cédulas y correos son ficticios (dominio `demo.logicode.test`, que no existe). No se guardan contraseñas reales en el repositorio.

## Cómo cargarlos

Con la aplicación corriendo (`docker compose up -d --wait`) y Node 20 o superior:

```powershell
node datos-demo/cargar-datos-demo.mjs
```

El script pide la cédula y la contraseña de una **cuenta inicial** (privilegio completo), porque aprobar docentes y cambiar roles lo exige. No guarda esos datos. También se pueden pasar como variables de entorno:

```powershell
$env:LOGICODE_ADMIN_CEDULA = "<tu cédula>"
$env:LOGICODE_ADMIN_PASSWORD = "<tu contraseña>"
node datos-demo/cargar-datos-demo.mjs
```

Si el frontend no está en `http://localhost:3000`, indica la dirección con `$env:LOGICODE_URL`.

- Si los datos ya están cargados, el script lo detecta y no cambia nada.
- Las fechas se calculan desde el momento de la carga (por ejemplo, "cerró hace 3 días"); para una presentación conviene cargarlos el mismo día o el anterior.
- Para empezar de cero: `docker compose down -v` borra **toda** la base de datos y los archivos (también tu contraseña de la cuenta inicial, que se vuelve a crear con el correo de verificación). Después, `docker compose up -d --wait` y se vuelve a cargar.
- Si en tu base ya hay cédulas habilitadas y el curso de demostración es el primero que se crea, esas cédulas quedan inscritas en él (regla T-25). En una base limpia no pasa.

## Cuentas

Contraseña de todas las cuentas de demostración: **`Demo#2026`** (solo para estos datos ficticios).

| Cédula | Nombre | Rol | Para qué sirve |
| --- | --- | --- | --- |
| 1000000101 | Laura Méndez Ríos | Docente (aprobada) | Dueña del curso: crea contenido, califica, da excepciones |
| 1000000102 | Jorge Pabón Silva | Docente (pendiente) | Solicitud por aprobar en Usuarios; no puede ingresar aún |
| 1000000103 | Diana Rojas Castro | Administradora ordinaria | Ve todo en solo lectura; no crea ni califica (RN-04) |
| 1000000201 | Ana Sofía Torres | Estudiante del curso | Tiene notas publicadas (una corregida) y una foto por revisar |
| 1000000202 | Bruno Díaz León | Estudiante del curso | Nota 3,0 y una entrega en texto por revisar |
| 1000000203 | Camila Peña Gómez | Estudiante del curso | Nota 5,0 |
| 1000000204 | David Suárez Mora | Estudiante del curso | Entrega de la Tarea 1 en Por revisar |
| 1000000205 | Elena Vargas Ruiz | Estudiante del curso | Tarea 1 vencida (0,0); segundo intento del Taller 2 |
| 1000000206 | Felipe Ortiz Cano | Estudiante del curso | Tarea 1 vencida pero con plazo ampliado: puede entregar |
| 1000000207 | Gabriela Ruiz Peña | Inscrita sin registrarse | Aparece como "Aún no se registra" |
| 1000000208 | Hugo Lara Medina | Estudiante fuera del curso | Solo ve la biblioteca pública |

## Contenido por etapa

### Etapa 3 · Acceso y permisos
Las cuentas de la tabla: docente aprobada, solicitud pendiente, administradora ordinaria (cambio de rol hecho por la cuenta inicial), estudiantes registrados, una cédula inscrita sin cuenta y un estudiante habilitado fuera del curso.

### Etapa 4 · Curso, temas y biblioteca
- Curso **Pensamiento Computacional · Grupo A** (2026-2), de Laura, con 7 inscritos.
- Temas: PSeInt (próxima clase), DFD, Scratch y Arduino.
- Materiales públicos (biblioteca abierta): Guía 1 en PDF, enlace de descarga de PSeInt y editor de Scratch.
- Materiales privados del curso: ejemplo `promedio.psc`, diagrama PNG y `led.ino`.

### Etapa 5 · Actividades y entregas
| Actividad | Situación |
| --- | --- |
| Tarea 1: algoritmo del promedio (30 %) | Cerrada hace 3 días. Ana 4,2 (corregida desde 4,5, con historial), Bruno 3,0, Camila 5,0, David en Por revisar, Elena vencida 0,0, Felipe con plazo ampliado 2 días |
| Taller 2: tabla de multiplicar (30 %, fotos, 2 intentos) | Abierto 5 días más. Ana (foto) y Bruno (texto) por revisar; Elena calificada 3,5 en el intento 1 y su intento 2 por revisar |
| Evaluación 1: condicionales (40 %) | Publicada; abre en 3 días |
| Proyecto: animación en Scratch | Borrador sin rúbrica (solo lo ve la docente) |

### Etapa 6 · Calificación con IA

**Taller 3: mayor de dos números (foto o archivo)**, sin porcentaje, 2 intentos, rúbrica de 1 + 2,5 + 1,5 puntos. Las fotos son imágenes sintéticas de cuaderno en `muestras/` (no son de estudiantes reales).

| Estudiante | Entrega | Qué se espera con la IA encendida |
| --- | --- | --- |
| Ana | Foto legible y correcta | Nota alta; se publica sola si la confianza es de 80 % o más |
| Bruno | Foto legible con la condición invertida | Nota baja en "Condición correcta" |
| David | Foto borrosa y oscura | Ilegible: pasa al docente sin nota |
| Camila | Respuesta escrita | Correcta con `>=` |
| Elena | Archivo `.psc` guardado en Windows (latin-1) | Incompleta: no muestra el mayor cuando `b > a` |
| Felipe | PDF | La IA no lee PDF (P-08): va directo a Por revisar como "Formato que la IA no lee" |

Lo que diga la IA depende del modelo; la tabla es lo que el caso busca mostrar, no un resultado medido.

- **Con la IA apagada** (por defecto), las seis quedan en Por revisar como "Calificación manual". Al encenderla, "Reintentar con IA" en cada entrega las manda a la cola (la de Felipe no, porque es PDF).
- **Con la IA encendida al cargar**, cinco entran a la cola y se procesan de a una; en el equipo de referencia cada foto puede tardar varios minutos.
- Si las etapas anteriores ya estaban cargadas, el script agrega solo el Taller 3.
- `ia-pruebas/` usa a Laura y a Felipe en un curso aparte, "Pruebas de aceptación IA", para no mezclar las pruebas con esta demostración.

### Etapa 7 · Quizzes, PSeInt y juegos

**Banco de preguntas** del curso: 9 preguntas de los cuatro tipos (selección única, múltiple, falso o verdadero y respuesta corta), con explicación para la revisión.

**Quiz 1: ciclos y condiciones** (calificable, 6 preguntas, 7 puntos, 15 minutos, un intento, sin porcentaje porque el curso ya suma 100 %). Se responde abierto y luego el script lo cierra, así que ya se ven la revisión y el ranking.

| Estudiante | Qué hizo | Resultado |
| --- | --- | --- |
| Ana | Todo correcto | 5,0 automática |
| Bruno | Falla la de selección múltiple (le falta Repetir); escribe "escribir" en minúscula, que cuenta | 3,6 |
| Camila | Falla la de MOD y la de asignación | 3,6; en el ranking desempata el menor tiempo |
| David | Empieza, guarda dos respuestas y abandona | El servidor envía el intento solo cuando se acaban sus 15 minutos: 1,4 |
| Elena | No lo responde | Vencida: 0,0 |
| Felipe | No lo ha respondido | Tiene el plazo ampliado 2 días (excepción con motivo): puede entrar y responderlo |

**Repaso: tipos de datos** (quiz de repaso, 4 preguntas, 5 minutos, abierto una semana): Ana lo hizo dos veces (2 de 4 y luego 4 de 4) y Bruno una (3 de 4). El ranking cuenta el mejor intento de cada uno. No da nota.

**Taller 4: suma de 1 a n (casos de prueba)**, sin porcentaje, 2 intentos. Tres casos: n = 5 → 15, n = 10 → 55 y n = 0 → 0. Se califica al recibir la entrega, sin IA:

| Estudiante | Entrega | Resultado |
| --- | --- | --- |
| Ana | Respuesta escrita con `Para` y mensaje "La suma es" | 5,0 automática (el mensaje no estorba) |
| Bruno | `Repetir`, que corre una vez aunque n = 0 | 3,3: falla el caso n = 0 |
| Camila | Le falta `FinPara` | Por revisar como "El programa no se pudo ejecutar" |
| David | Archivo `suma.psc` con la fórmula n × (n + 1) / 2 | 5,0 automática |

En la entrega de Camila (o cualquier otra), el docente puede usar "Ejecutar casos de prueba" para ver caso por caso. El editor está en **PSeInt** del menú (con sesión).

**Juegos** (`/juegos`, sin sesión): no cargan datos en el servidor; el avance se guarda en el navegador de cada quien.

- Si las etapas anteriores ya estaban cargadas, el script agrega solo lo de la Etapa 7 (y lo de la 6 si faltaba).
- Los tiempos de los intentos que hace el script son de un segundo o menos, así que el desempate por tiempo del ranking se ve mejor cuando los estudiantes responden en vivo.

### Etapa 8 · Foro y operación docente

**Foro** (`/foro`, se lee sin sesión):

| Hilo | Herramienta | Qué muestra |
| --- | --- | --- |
| Bienvenidos al foro de Pensamiento Computacional (Laura) | General | Fijado arriba |
| ¿Por qué mi ciclo Mientras no termina? (Ana) | PSeInt | Responden Bruno y la docente |
| ¿Qué figura uso para leer datos? (Camila) | DFD | Responde la docente |
| Mi gato no se mueve con las flechas (David) | Scratch | Responde Elena |
| ¿El LED necesita resistencia? (Felipe) | Arduino | La docente responde y lo cierra: ya no recibe respuestas |
| Cambio respuestas del Quiz 1 (Bruno) | General | Diana, administradora ordinaria, lo eliminó: no aparece, pero queda en la base quién lo hizo |

Para probar la moderación, entra con Laura o Diana y usa "Fijar arriba", "Cerrar a respuestas" o "Eliminar" dentro de un hilo; con un estudiante esos botones no aparecen.

**Planilla**: pestaña "Planilla" del curso (Laura, Diana o una cuenta inicial) y "Mis notas" para cada estudiante. No agrega notas nuevas: muestra las de las etapas anteriores. Como el curso ya tenía porcentajes que suman 100 % (Tarea 1 30 %, Taller 2 30 % y Evaluación 1 40 %), Quiz 1, Taller 3 y Taller 4 quedan en 0 % y la planilla lo avisa; para verlas pesar, quita o baja un porcentaje desde la actividad. "Exportar a Excel (CSV)" descarga la misma tabla.

**Panel de administración**: `/admin` con Diana o una cuenta inicial: cifras de la plataforma y respaldos. El primer respaldo diario lo hace el servicio `backup` a la hora de `BACKUP_HOUR`; con "Crear respaldo ahora" se ve uno en un minuto.

- Si las etapas anteriores ya estaban cargadas, el script agrega solo lo de la Etapa 8.

### Etapa 9 · Calidad y seguridad

No agrega datos: la etapa revisa lo que ya existe. Los datos de las Etapas 3 a 8 alcanzan para las dos demostraciones del punto de control, descritas paso a paso en [docs/calidad.md](../docs/calidad.md#demostraciones):

| Demostración | Con quién | Qué hacer |
| --- | --- | --- |
| Nota automática de PSeInt | Elena (`1000000205`) | Entregar su programa en "Taller 4: suma de 1 a n"; la nota sale sola |
| Nota manual | Camila (`1000000203`) y Laura (`1000000101`) | Camila entrega en texto "Taller 2"; Laura la califica desde "Por revisar"; Camila la ve |
| Restauración | Una cuenta inicial o Diana (`1000000103`) | Respaldo desde `/admin`, un cambio cualquiera y restauración por consola |

Cada demostración usa un intento de esos estudiantes. Para repetirlas desde cero, restaura el respaldo hecho antes de empezar.

## Para las etapas siguientes

Cada etapa nueva agrega una función `stageN(...)` al script y una sección aquí, sin cambiar lo anterior salvo que la etapa lo requiera.
