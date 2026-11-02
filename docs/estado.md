# Estado del proyecto

Última actualización: 2 de noviembre de 2026 · Etapa en curso: **ampliaciones después de la Etapa 10: retos de bloques (RF-12), Excel y PDF (P-07, P-08), juegos con más niveles (bloques con personajes), juego nuevo "Caza el error", rediseño visual con el Canva, juegos como catálogo y juego de Arduino con niveles de matrices, textos y ciclos en DFD entregados; documentación completa del proyecto al día con el commit `344801c`; archivos en MinIO (P-05) y correo real con Brevo (P-06) funcionando desde el 1 de noviembre; ensayo de la IA con imágenes sintéticas (1 de noviembre); dependencias revisadas otra vez (2 de noviembre); documentación completa regenerada el 2 de noviembre; maquetas de la Etapa 1 entregadas el 2 de noviembre**

Leyenda: **Implementado** = existe en el código y tiene prueba o verificación. **Pendiente** = no existe todavía. **Decisión aprobada** = definido en la especificación, sin implementar.

## Etapas

| Etapa | Estado | Notas |
| --- | --- | --- |
| 0 · Aprobación de alcance | Aprobada (especificación) | |
| 1 · Análisis y diseño | Diseño técnico hecho; casos de uso, historias de usuario y maquetas entregados | [Casos de uso](casos-de-uso.md) e [historias de usuario](historias-de-usuario.md) con criterios Dado/Cuando/Entonces, escritos sobre lo implementado (1 de noviembre); [maquetas](maquetas.md) de baja fidelidad de las 18 pantallas principales (2 de noviembre). Siguen **pendientes**: rúbricas de referencia del docente y la prueba de lectura de código manuscrito (pruebas de aceptación de Qwen en `ia-pruebas/`) |
| 2 · Fundaciones técnicas | Implementada | Detalle en la sección de la Etapa 2 |
| 3 · Acceso y permisos | Implementada y aprobada | Correo real con Brevo funcionando desde el 1 de noviembre (P-06). La carga desde Excel llegó con P-07 |
| 4 · Curso, temas y biblioteca | Implementada y aprobada | Desde el 1 de noviembre los archivos van en MinIO (P-05) |
| 5 · Actividades y entregas | Implementada y aprobada | Calificación manual del docente |
| 6 · Calificación determinista e IA | Implementada y aprobada; faltan las pruebas de aceptación de Qwen en el equipo de presentación | La parte determinista (PSeInt y quizzes, RF-16) se hizo en la Etapa 7 (T-48) |
| 7 · Quizzes, PSeInt y juegos | Implementada y aprobada | Falta que Carlos revise niveles y notas automáticas de los datos demo |
| 8 · Foro y operación docente | Implementada y aprobada | Carlos pidió la Etapa 9 el 27 de octubre |
| 9 · Calidad y seguridad | Implementada y aprobada | Carlos pidió la Etapa 10 el 27 de octubre. R9-01, R9-02 y R9-05 se cerraron en la Etapa 10 |
| 10 · Piloto y entrega | Aprobada; pendiente el piloto con el docente | Detalle abajo, en [piloto.md](piloto.md) y en los [manuales](manuales/) |
| Ampliación · retos de bloques (RF-12) | Implementada, pendiente de revisión | Carlos la pidió el 27 de octubre al aprobar la Etapa 10. Detalle abajo |
| Ampliación · Excel y PDF (P-07, P-08) | Implementada, pendiente de revisión | Aprobada el 27 de octubre junto con RF-12. Detalle abajo |
| Ampliación · juegos con más niveles y personajes del robot | Implementada; Carlos aprobó los 20 niveles y los personajes y pidió más niveles en todos los juegos (27 de octubre) | Detalle abajo |
| Ampliación · rediseño visual con el Canva | Implementada, pendiente de revisión | Carlos aprobó la maqueta el 27 de octubre. Detalle abajo |
| Ampliación · archivos en MinIO (P-05) y remitente de Brevo (P-06) | Implementada y en uso | Carlos decidió las dos y las instaló en su equipo el 1 de noviembre; los correos de Brevo llegan. Detalle abajo |
| Ampliación · juegos como catálogo (estilo Biblioteca) | Implementada, pendiente de revisión | Carlos aprobó la maqueta el 28 de octubre. Detalle abajo |
| Ampliación · juego de Arduino, matrices y textos en PSeInt, ciclos en DFD | Implementada, **pendiente de revisión de Carlos** | Carlos lo pidió el 28 de octubre para cubrir los temas más flojos. Detalle abajo |
| Ampliación · juego "Prueba de escritorio" | Implementada, **pendiente de revisión de Carlos** | Carlos pidió el 2 de noviembre uno de los dos juegos opcionales; se eligió este. El traductor DFD↔PSeInt sigue **pendiente**. Detalle abajo |
| Documentación completa del proyecto | Entregada y revisada con Carlos el 2 de noviembre | Índice, visión general, casos de uso, historias, instalación, manual del administrador, arquitectura, modelo de datos, API y manuales revisados contra el commit `344801c` (1 de noviembre) y de nuevo contra `85dca33` (2 de noviembre). Detalle abajo |

## Datos de demostración

Implementado: `datos-demo/cargar-datos-demo.mjs` carga por la API datos ficticios de las Etapas 3 a 8 (cuentas de cada rol, curso con temas y materiales, actividades en distintos estados, entregas, notas, historial, excepciones, un taller con fotos, texto, `.psc` y PDF para la IA, banco de preguntas, un quiz calificable cerrado con ranking, uno de repaso, un taller de PSeInt con casos de prueba, hilos del foro en cada herramienta, uno fijado, uno cerrado y uno eliminado por moderación, y un reto de bloques calificable). Se amplía en cada etapa. Si ya hay datos de etapas anteriores, solo agrega los que faltan. Verificado en Docker sobre una base limpia (Etapas 3 a 8, en la Etapa 10 por HTTPS; con el reto de bloques, versión 8 de la base; el 1 de noviembre, con los archivos en MinIO); volver a ejecutarlo no duplica datos. Desde la Etapa 10 se niega a cargar en una instalación en modo piloto, salvo `LOGICODE_DEMO_EN_PILOTO=si`. Detalle en [datos-demo/README.md](../datos-demo/README.md).

## Archivos en MinIO (P-05) y remitente de Brevo (P-06) · detalle

Sin cambio de base de datos (sigue en la versión 8) ni de interfaz.

| Entregable | Estado | Dónde |
| --- | --- | --- |
| Servicio `minio` en Docker Compose, sin puerto publicado, con usuario y contraseña en `.env` | Implementado | `docker-compose.yml`, `.env.example` (T-85) |
| Adaptador de `BinaryStorage` para MinIO: la carga se valida en `file-storage` y se sube con su SHA-256; vista previa y descarga pasan por el backend con los mismos permisos; los `Range` piden a MinIO desde esa posición | Implementado | `files/infrastructure/S3BinaryStorage.java`, `S3ObjectClient.java` |
| Paso automático a MinIO de los archivos que había en el disco, sin cortar el servicio | Implementado | `S3BinaryStorage.migrateLocalFiles()` |
| Respaldos con los datos de MinIO; restauración de copias nuevas y de copias de antes de MinIO | Implementado | `respaldos/respaldo.sh`, `respaldos/README.md` |
| Remitente `logicode.notificaciones@gmail.com` en `BREVO_SENDER_EMAIL` | Implementado | `docker-compose.brevo.yml`, `.env.example` (T-86) |
| Remitente verificado en Brevo, API key y envío real de prueba | Hecho por Carlos el 1 de noviembre | En su cuenta de Brevo ([instalación](instalacion.md), sección 8) |

### Verificación realizada (MinIO y Brevo)

- Backend: todas las pruebas anteriores en verde, más `S3ObjectClientTest` (la firma coincide con la de botocore, la biblioteca oficial de AWS) y `MinioStorageIT` contra la imagen real de MinIO: guardar, leer desde una posición, borrar, rechazo de una carga con SHA-256 distinto, rechazo de credenciales equivocadas y paso de archivos locales.
- Actualización real: una instalación de la versión anterior (`1d08059`) con los datos demo (14 archivos en el disco) se actualizó a esta versión; el backend pasó 14 de 14 archivos a MinIO, dejó vacía la carpeta y las descargas por la API dieron el mismo SHA-256 guardado en la base.
- Video de 60 MB: carga en unos 4 s, petición `Range` del último kilobyte en 37 ms (`206`, bytes correctos) y descarga completa idéntica.
- Respaldos: la copia nueva pasó la prueba de restauración ("15 archivos legibles"); `restaurar` se negó con MinIO en marcha; se restauraron una copia de antes de MinIO (el backend volvió a pasar los 14 archivos) y una copia nueva (15 objetos de vuelta, video idéntico).
- Instalación limpia con datos demo: carga completa sin errores; 14 archivos en MinIO y 14 en la base.
- No se probó en este entorno: el envío real con Brevo (necesita la API key) ni Windows con Docker Desktop. Carlos instaló esta versión en su equipo con Windows el 1 de noviembre: MinIO y los correos de Brevo funcionan.

## Documentación del proyecto · detalle

Solo documentación: no cambia código de la aplicación. Todo se revisó contra el commit `344801c` (base de datos en la versión 8).

| Documento | Estado | Notas |
| --- | --- | --- |
| [docs/README.md](README.md) | Nuevo | Índice por tipo de lector (directivo, docente, estudiante, administrador, desarrollador) |
| [Visión general](vision-general.md) | Nuevo | Problema, propuesta, roles, funciones, recorrido de un curso, tecnología, estado y glosario |
| [Casos de uso](casos-de-uso.md) | Nuevo | Actores y casos CU-01 a CU-32 con su estado; estados de una entrega |
| [Historias de usuario](historias-de-usuario.md) | Nuevo | HU-01 a HU-37 en diez épicas, con criterios Dado/Cuando/Entonces |
| [Instalación](instalacion.md) | Nuevo | Windows y Linux desde cero, primer ingreso, datos demo, IA, Brevo, piloto, respaldos, problemas frecuentes y pasar LogiCode a otro PC |
| [Manual del administrador](manuales/manual-administrador.md) | Nuevo | Cuentas iniciales frente a las demás cuentas Administrador; panel, usuarios, habilitaciones, auditoría, biblioteca, foro y respaldos |
| [`migracion/`](../migracion/) | Nuevo | Copia de `exportar.ps1` e `importar.ps1` y su guía, sin rutas personales |
| [Diagramas](diagramas/) | Nuevo | DFD de contexto y de nivel 1, modelo de datos, secuencia y estados de la entrega (Mermaid e imagen). Se corrigieron el modelo (falta el origen `AUTOMATIC` de la nota) y los estados (faltaban la publicación automática y la nota del docente mientras la IA procesa) |
| Arquitectura, modelo de datos y API | Actualizados | Doce módulos y sus reglas, procesos en segundo plano, migraciones `V1` a `V8` con diagramas entidad-relación, todos los endpoints y los errores con sus títulos reales |
| Manuales del docente, del estudiante y técnico | Actualizados | Barra con iconos, importar desde Excel, "Definir rúbrica", PDF leídos por la IA, catálogo de juegos con filtros y niveles, juego de Arduino, ciclos en DFD, cambio de correo con cédula. Capturas nuevas en `manuales/img/` |
| Textos de la interfaz en otros documentos | Corregidos | "Crear respaldo ahora" en `piloto.md` y `calidad.md` (decía "Hacer un respaldo ahora"); "Exportar a Excel (.xlsx)" y "Exportar a CSV" y "Pendiente" en Estudiantes en `datos-demo/README.md` |
| [Maquetas](maquetas.md) (Etapa 1) | Nuevo, 2 de noviembre | 18 pantallas principales en baja fidelidad (11 de PC y 7 de celular) con notas numeradas, mapa de navegación y los casos de uso e historias que cubre cada una. Dibujadas en `docs/maquetas/fuente.html` a partir de las pantallas reales; imágenes con `node docs/maquetas/capturar.mjs`. Validarlas con el docente queda para el piloto |
| Rúbricas de referencia y prueba de lectura manuscrita (Etapa 1) | **Pendiente** | No se pueden escribir sin el docente ni sin correr las pruebas de Qwen en el equipo de referencia |
| Revisión del 2 de noviembre | Hecha con Carlos | Contra el commit `85dca33`: Brevo pasa a probado, la visión general y el índice muestran el ensayo de la IA, se agrega el acceso temporal con ngrok ([piloto.md](piloto.md#8-mostrar-logicode-fuera-de-la-red-demostraciones)) y el documento único (`.docx` y `.pdf`) se genera con `docs/documento-completo/generar.py`: índice lleno, imágenes ajustadas a la hoja, enlaces internos y anexos con el ensayo de la IA y la migración |

### Verificación realizada (documentación)

- Textos de botones, pestañas y mensajes comparados con el código del frontend; permisos con `AccessPolicy`; tablas con las migraciones `V1` a `V8`; endpoints con los controladores.
- Todos los diagramas Mermaid se dibujaron con mermaid-cli sin errores, y los enlaces relativos entre documentos apuntan a archivos que existen.
- No se ejecutaron de nuevo la aplicación, los scripts de migración ni la instalación en Linux para esta entrega. Algunas capturas son anteriores al rediseño o usan un modelo simulado, y su pie lo dice.

## Rediseño visual con el Canva · detalle

| Elemento | Estado | Notas |
| --- | --- | --- |
| Colores, letras y botones | Implementado | Fondo crema, franja lavanda bajo la barra superior, títulos con serifa (Fraunces), texto en DM Sans, código en JetBrains Mono y morado como color de acento, tomados del diseño de Canva "Página web educativa para estudiantes". Las tres fuentes van en `frontend/public/fonts/` (licencia OFL): la CSP solo permite archivos propios y el piloto puede no tener internet (T-82). Todo en `styles/global.css` |
| Barra superior | Implementado | Logo, menú con iconos (`shared/ui/Icon.jsx`) y la opción actual en morado; agrega "Inicio". En el celular el menú es una fila que se desliza de lado y deja a la vista la opción actual. Los permisos de cada opción no cambian |
| Encabezado de página | Implementado | La primera sección del curso, la biblioteca, el foro y los juegos es una franja lavanda de borde a borde (`page-head`). Las pestañas del curso cierran la franja; los niveles de cada juego son pastillas |
| Inicio | Implementado | Presentación con el título, los botones de ingreso y registro, un algoritmo de PSeInt en un arco y el cerebro del juego de bloques como mascota (`BrainMascot`). En lugar de la foto del Canva va el arco, porque no hay una foto con permiso de uso |
| Iniciar sesión | Implementado | Tarjeta con un lado lavanda y la mascota; los campos y enlaces son los mismos |
| Biblioteca y foro | Implementado | Los filtros van en una barra dentro de la franja. La biblioteca muestra cada material como tarjeta con su formato; el foro muestra las iniciales del autor y el número de respuestas a la derecha, y el formulario "Abrir un hilo" al lado de la lista en el computador |
| Mis notas | Implementado | El avance parcial resalta en morado y la nota definitiva queda aparte |
| Textos y campos | Sin cambios | Se conservaron los nombres de campos, botones y mensajes de LogiCode |

### Verificación realizada (rediseño)

- Interfaz: 65 pruebas, oxlint sin hallazgos y compilación de Vite.
- Con backend y datos demo en una base limpia: axe sin hallazgos (WCAG 2.1 AA y buenas prácticas) en 24 pantallas (inicio, ingreso, registro, biblioteca, foro, hilo, juegos, Caza el error, mi cuenta, las seis pestañas del curso, Mis notas, PSeInt, una actividad, panel de administración, usuarios y auditoría) a 1366 px y a 390 px, sin desborde lateral. Capturas en `capturas-rediseno/`.
- Solo se pudo ver la parte de arriba del diseño de Canva (está en otra cuenta); el resto de las pantallas siguen el mismo estilo.

## Juego "Prueba de escritorio" · detalle

| Elemento | Estado | Notas |
| --- | --- | --- |
| Juego "Prueba de escritorio" (12 niveles) | Implementado | Tarjeta nueva en `/juegos` (`games/DeskGame.jsx`, niveles en `DESK_LEVELS`). El estudiante sigue un programa de PSeInt y llena la tabla: cada fila es una línea o una vuelta del ciclo, con el valor de cada variable después de ella, Verdadero o Falso en la columna de una condición y lo que sale en Pantalla. La primera fila viene llena como ejemplo y "—" marca lo que no se llena. **Comprobar** pinta en rojo las casillas equivocadas y dice en qué fila empezar; al acertar todo se ve la explicación. En el teléfono la tabla se desplaza de lado y la columna Paso queda fija (T-87) |
| Niveles | Implementado | Intercambio, una variable que cambia de valor, precio con descuento, el mayor de dos (con `Leer`), Si dentro de otro Si, sumar con Para, Mientras que se triplica, contar pares, Repetir hasta que (factorial), el mayor de un arreglo, palabra al revés y una función que se usa dentro de un Para |
| Traductor DFD↔PSeInt | **Pendiente** | El otro juego opcional; no se hizo |

### Verificación realizada (prueba de escritorio)

- Los 12 programas se ejecutaron con el intérprete de PSeInt del backend (`pseint/domain`, compilado aparte): lo que sale en la columna Pantalla coincide con la salida del programa en todos. Los valores intermedios de la tabla se revisaron a mano.
- `games.test.jsx`: cada fila tiene una casilla por columna, la primera trae ejemplo y no hay casillas vacías por llenar; la comparación acepta 3,5, V/F, tildes y comillas y rechaza lo que no coincide; una prueba de interfaz llena un nivel con una casilla mala, ve el aviso y la casilla marcada, la corrige y guarda el avance. Interfaz: 72 pruebas, oxlint sin hallazgos y `vite build`. El backend no cambió.
- Recorrido en Chromium con Vite (sin backend): catálogo con siete juegos y 111 niveles, un nivel con casillas por revisar a 1280 px y un nivel resuelto a 390 px sin desborde lateral; axe sin hallazgos (WCAG 2.1 AA y buenas prácticas) con casillas marcadas en rojo a 1366 y 390 px. Capturas en `capturas-prueba-escritorio/`.

## Juegos como catálogo · detalle

| Elemento | Estado | Notas |
| --- | --- | --- |
| Catálogo de juegos | Implementado | `/juegos` muestra los juegos (siete desde el 2 de noviembre) como tarjetas, igual que la biblioteca muestra materiales: dibujo del juego, herramienta (Scratch, PSeInt, DFD o Arduino), temas, descripción y "N de M niveles resueltos". El botón dice "Jugar" o "Seguir jugando" y abre el primer nivel sin resolver |
| Filtros | Implementado | Buscar por nombre o descripción, Herramienta y Tema (Secuencias, Variables, Condiciones, Ciclos, Arreglos, Matrices, Textos, Funciones, Rutas, Depuración, Sensores y actuadores), en la franja lavanda como en la biblioteca. Si ningún juego coincide lo dice |
| Página de cada juego | Implementado | `/juegos?juego=<clave>&nivel=<id>`: enlace "← Juegos", herramienta y temas, avance y los niveles resueltos marcados con ✓. Los juegos y sus niveles no cambian |
| Agregar juegos | Implementado | Un juego nuevo es una entrada más en la lista `GAMES` de `GamesPage.jsx` (clave, nombre, herramienta, temas, niveles, componente y descripción); la tarjeta y los filtros salen solos (T-83) |

### Verificación realizada (juegos como catálogo)

- Interfaz: 66 pruebas (una nueva para el catálogo y sus filtros), oxlint sin hallazgos y compilación de Vite.
- axe sin hallazgos en el catálogo, Bloques y Caza el error a 1366 px y a 390 px, sin desborde lateral. Capturas en `capturas-juegos-catalogo/`.

## Excel y PDF (P-07 y P-08) · detalle

| Entregable | Estado | Dónde |
| --- | --- | --- |
| Importar estudiantes desde Excel (RF-05, P-07) | Implementado | "Importar lista" en la pestaña Estudiantes del curso y en "Habilitar estudiantes": `.xlsx` (primera hoja, cédula en A y nombre en B) o `.csv`, con las mismas reglas (T-75). `StudentListReader` |
| Exportar la planilla a Excel (RF-18, P-07) | Implementado | "Exportar a Excel (.xlsx)" en la Planilla: notas y porcentajes como números (T-76). "Exportar a CSV" sigue |
| La IA lee entregas en PDF (P-08) | Implementado | Texto del PDF y cada página como imagen, hasta 5 páginas (T-77). `PdfContent`. Probado con un PDF real y Qwen el 1 de noviembre: notas iguales a la del docente, 8 a 9 min por PDF de 3 páginas (`ia-pruebas/README.md`) |
| La IA lee Word y PowerPoint | Pendiente | No aprobado: siguen con el docente como "Formato que la IA no lee" |
| Pruebas de aceptación de la IA con PDF en el equipo de referencia | Pendiente | El modelo no corre en el entorno de Claude; la prueba de integración usa un proveedor simulado y comprueba lo que recibe el modelo |

### Verificación realizada (Excel y PDF)

- `StudentListReaderTest` (CSV, `.xlsx` con cédulas como número y como texto, encabezado, repetidas, filas con error, archivo dañado o `.xls`), `PdfContentTest` (texto y una imagen por página, más de 5 páginas, PDF dañado, escaneo sin texto, página gigante), `ContentFlowsIT` (inscripción con `.xlsx`), `ForumAndOperationFlowsIT` (cada cifra del `.xlsx` contra la pantalla; el estudiante recibe `403`) y `AiGradingFlowsIT` (un PDF llega al modelo con su texto y su página; uno dañado pasa al docente sin llamar al modelo; un `.docx` sigue yendo directo al docente). Backend: 126 pruebas sin fallas.
- Revisión de dependencias (R9-05) con POI, PDFBox y sus dependencias contra la base OSV de Maven: 0 avisos.
- Docker por HTTPS (modo piloto) sobre una base limpia con los datos demo, con la IA encendida y un servidor que imita a Ollama (el modelo real no corre en el entorno de Claude): la entrega en PDF de Felipe llegó al "modelo" con su texto y la imagen de su página, dibujada dentro del contenedor del backend; un segundo intento en PDF enviado desde el teléfono quedó calificado. La lista de Excel inscribió a Iván y Juliana desde la interfaz y la planilla se descargó en `.xlsx` con las cifras de la pantalla. Capturas en `capturas-excel-pdf/` (las de la nota de la IA dicen "modelo simulado").

### Punto de control

Pendiente: revisión de Carlos (importar `datos-demo/muestras/lista-estudiantes.xlsx`, abrir la planilla exportada en Excel) y una prueba con PDF real en el equipo de referencia con la IA encendida.

## Juego de Arduino, matrices y textos, ciclos en DFD · detalle

| Elemento | Estado | Detalle |
|---|---|---|
| Juego nuevo "¿Qué hace este código?" (Arduino, 12 niveles) | Implementado | Tarjeta Arduino en `/juegos` (`ARDUINO_LEVELS`, usa el componente de "¿Qué imprime?" con una pregunta propia por nivel). Se lee el programa y se elige lo que pasa en la placa: LED fijo, parpadeo, `for` en `setup`, Monitor Serie, variable global contra variable dentro de `loop`, botón con `INPUT` y con `INPUT_PULLUP`, sensor de luz con `analogRead`, brillo con `analogWrite`, potenciómetro con `map` y semáforo (T-84) |
| Matrices y textos en PSeInt | Implementado | "¿Qué imprime?" pasa a 21 niveles (textos con `Subcadena`, `Mayusculas` y `Longitud`, recorrer un texto, palabra al revés, matrices y diagonal), "Caza el error" a 14 (palabra al revés, fila contra columna de una matriz) y "Ordena el algoritmo" a 14 (palabra al revés, sumar las columnas de una matriz). Solo usan posiciones de 1 en adelante, que funcionan igual en el intérprete de LogiCode y en PSeInt |
| Ciclos en "Completa el DFD" | Implementado | 14 niveles. Figura nueva `while`: rombo con el cuerpo debajo por el Sí, flecha de regreso por la izquierda y salida por el No a la derecha. Niveles: contar del 1 al 5, suma del 1 al n, adivina el número y contar aprobados (una decisión dentro del ciclo, con la rama No vacía). El texto alternativo del diagrama dice dónde empieza y termina el ciclo |
| Niveles aceptados por Carlos | **Pendiente: revisión de Carlos** | Capturas en `capturas-arduino-matrices-dfd/` |

### Verificación realizada (Arduino, matrices y textos, ciclos en DFD)

- Programas de PSeInt: todos los niveles de "¿Qué imprime?", "Ordena" y "Caza el error" (los nuevos y los anteriores) se ejecutaron con el intérprete de LogiCode compilado aparte: cada respuesta coincide y ninguna otra opción da lo mismo; en Caza el error, con el error sale lo que dice el nivel, con la corrección correcta lo esperado y con las otras no.
- Programas de Arduino: cada uno se compiló con g++ y funciones de Arduino simuladas (pines, tiempo, `analogRead`, `map` y Monitor Serie), con el botón presionado y suelto y con varias lecturas del sensor; la traza coincide con la respuesta de cada nivel. No se probaron en una placa real.
- Interfaz: 69 pruebas (nuevas: casillas de un DFD con ciclo, un DFD con ciclo completo, el juego de Arduino y conteos), oxlint sin hallazgos y compilación de Vite.
- axe sin hallazgos en el catálogo, Arduino, un DFD con ciclo y los niveles nuevos de PSeInt a 1366 px y a 390 px, sin desborde lateral. Se corrigió un hallazgo anterior: en el teléfono las líneas largas de "Ordena" creaban barras de desplazamiento sin teclado; ahora bajan de renglón.

## Juegos ampliados: más niveles y personajes del robot · detalle

| Entregable | Estado | Dónde |
| --- | --- | --- |
| Bloques: 24 niveles, cada uno un poco más difícil | Implementado | `/juegos`, pestaña Bloques (`frontend/src/features/games/levels.js`). Orden: secuencias (1 a 3, el 3 empieza mirando hacia abajo), rutas (5 y 6, el 6 con dos caminos y bloques solo para el corto), ciclos (7 a 13, con ciclos anidados del 11 al 13; el 13 tiene dos ciclos dentro de otro), condiciones (15 a 19 y 21: girar a la derecha, a la izquierda, Si dentro de Repetir, un Si dentro de otro Si para pasillos que doblan a los dos lados, y ese Si doble dentro de Repetir) y el desafío final (24). Hay 7 niveles de depuración (4, 8, 14, 17, 20, 22 y 23) que empiezan con un programa que falla: giros, conteo de un Repetir, ciclos anidados, un giro equivocado, un bloque que falta y un Si con las ramas cambiadas. Se conservan los ids de los niveles anteriores, así que el avance guardado no se pierde (T-78) |
| ¿Qué imprime?: 16 niveles | Implementado | Mismo tipo de ejercicio, de variables a funciones: intercambio, división entera y residuo (`trunc`, `MOD`), Si … SiNo, Y / O, acumulador, contador con condición, Mientras, Repetir … Hasta Que, Para hacia atrás, Según, contar dígitos, Fibonacci, ciclos anidados, arreglos y funciones. Las respuestas se comprobaron ejecutando cada programa con el intérprete de LogiCode (T-80) |
| Ordena el algoritmo: 12 niveles | Implementado | De 4 a 11 líneas: área del círculo, promedio con aprobación, contar positivos (Si dentro de Para), sumar hasta el cero (Mientras), Si anidado, validar con Repetir, tablas con Para anidado y el mayor de un arreglo. Cada nivel tiene un solo orden correcto (no hay pasos que se puedan intercambiar) y todos se ejecutaron con el intérprete (T-80) |
| Completa el DFD: 10 niveles | Implementado | Mismas figuras y reglas del dibujo: minutos a horas, el mayor de dos, precio con descuento, promedio con aprobación, dos decisiones seguidas (aprobó y con honores) y cajero automático, con 1 a 4 casillas y opciones que se parecen a la respuesta |
| Personaje del robot (flecha, lápiz, cerebro o fantasma) | Implementado | Selector "Personaje del robot" debajo del tablero, también en los retos de bloques de una actividad. Dibujos propios en SVG inspirados en las imágenes que envió Carlos (`games/skins.jsx`). El lápiz gira hacia donde mira; el cerebro y el fantasma siguen de pie, miran con los ojos hacia ese lado y llevan un triángulo en el borde de la casilla. El texto "Robot en la fila…, mirando…" no cambia. Se recuerda en el navegador (T-79) |
| Juego nuevo "Caza el error" (12 niveles) | Implementado | Pestaña "Caza el error" en `/juegos` (`games/ErrorGame.jsx`, niveles en `ERROR_LEVELS`). Cada nivel muestra qué debería mostrar el programa y qué muestra: el estudiante marca la línea con el error y, cuando la encuentra, elige entre tres correcciones; al acertar ve la explicación. Temas de fácil a difícil: operaciones, texto contra variable, paréntesis, límite de un Si, límite de un Para, contador contra acumulador, ciclo que no termina, Y contra O, arreglo con valor inicial equivocado, ciclos anidados y función que no devuelve. Carlos lo eligió el 27 de octubre entre tres propuestas (T-81) |
| Niveles nuevos aceptados por Carlos | **Pendiente: revisión de Carlos** | Capturas en `capturas-bloques-20-niveles/`, `capturas-mas-niveles/` y `capturas-caza-el-error/` |

### Verificación realizada (más niveles y personajes)

- `games.test.jsx`: cada nivel se resuelve con su solución dentro del límite de bloques; los de depuración empiezan fallando; los ids de cada juego y las opciones de cada nivel no se repiten; y ningún nivel que permite ciclos o Mientras se puede resolver con una secuencia simple de Avanzar y giros dentro del límite (búsqueda del camino más corto). Una prueba cambia el personaje, comprueba que el texto de posición y dirección sigue igual y que se recuerda al volver. Interfaz: 63 pruebas, `oxlint` limpio y `vite build`. El backend no cambió.
- Los 16 programas de "¿Qué imprime?" y los 12 de "Ordena el algoritmo" (armados en orden, con entradas de ejemplo) se ejecutaron con el intérprete de PSeInt del backend (`pseint/domain`, compilado aparte): cada salida coincide con la respuesta y todos los programas de ordenar corren sin errores.
- Recorrido en Chromium con `vite preview` (sin backend): los cuatro personajes en las cuatro direcciones, un nivel de ciclos anidados resuelto paso a paso y un nivel de depuración en pantalla de teléfono (390 px). Capturas en `capturas-bloques-20-niveles/`. Con los niveles nuevos: una respuesta de "¿Qué imprime?", un algoritmo de 11 líneas, dos DFD completos (uno en el teléfono, para ver que el texto cabe en las figuras) y el nivel "El Si al revés". Capturas en `capturas-mas-niveles/`.

- Caza el error: cada programa se ejecutó con el intérprete tres veces más: con el error muestra lo que dice el nivel, con la corrección correcta muestra lo esperado y con cada una de las otras dos opciones no. Prueba de interfaz: línea equivocada, línea correcta, corrección equivocada y corrección correcta con el avance guardado. Interfaz: 65 pruebas. axe sin hallazgos (WCAG 2.1 AA) en el juego a 1366 px y a 390 px. Capturas en `capturas-caza-el-error/`.

## Retos de bloques que configura el docente (RF-12) · detalle

| Entregable | Estado | Dónde |
| --- | --- | --- |
| El docente configura un reto de bloques en la rúbrica | Implementado | "Editar rúbrica" → "Reto de bloques (opcional)": tablero de hasta 12 × 12 (`#` muro, `.` libre, `S` salida, `M` meta), hacia dónde mira el robot, máximo de bloques (1 a 40), bloques permitidos y, opcional, un programa inicial con un error (reto de depuración). Vista previa con el mismo juego que verá el estudiante. Se guarda en la rúbrica versionada (columna `rubric_version.block_challenge`, migración `V8`, T-72) |
| El estudiante arma y envía el programa | Implementado | En la actividad, el juego de bloques reemplaza la respuesta escrita; el programa viaja como texto JSON. La actividad debe recibir entregas en texto y no puede tener a la vez casos de prueba (T-73) |
| Calificación automática del reto | Implementado | El servidor vuelve a ejecutar el programa con las mismas reglas del juego (`BlockChallenge`): 5,0 si el robot llega a la meta y 0,0 si choca, termina antes o pasa de 400 pasos, publicada como "Automática" con la razón. Un programa con bloques no permitidos, con más bloques del máximo o que no es un programa va a Por revisar como "El programa no se pudo ejecutar" (T-74) |
| El docente ve el programa entregado | Implementado | La entrega muestra el programa con sangría (Repetir 3 veces / Avanzar …) y la rúbrica con el tablero para probarlo |
| Notas parciales por número de bloques o por pasos | Pendiente | No pedido: la nota es todo o nada (T-74) |

### Verificación realizada (retos de bloques)

- `BlockChallengeTest` (tablero, validación y ejecución, también con los niveles de `/juegos`) y `QuizAndPseintFlowsIT` (reto inválido o sin entregas en texto → 422, reto con casos de prueba → 422, programa que llega 5,0, que choca 0,0, con bloques no permitidos o texto suelto → Por revisar). Backend: 120 pruebas sin fallas ni omitidas. Interfaz: 44 pruebas, `oxlint` limpio y `vite build`.
- Docker por HTTPS (modo piloto) sobre una base limpia con los datos demo: las tres entregas del reto quedaron en 5,0, 0,0 (choca) y 0,0 (termina antes). En el teléfono, un estudiante armó un programa equivocado (0,0) y en el segundo intento llegó a la meta (5,0). Capturas en `capturas-rf12/`. axe sin hallazgos en la actividad del estudiante (390 px) ni en el editor de la rúbrica (1280 px).

### Punto de control

Pendiente: revisión de Carlos (crear un reto propio con "Editar rúbrica" y resolverlo con una cuenta de estudiante).

## Etapa 10 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Despliegue local inicial con Docker Compose | Implementado | `docker-compose.piloto.yml` sobre el archivo principal: HTTPS en Nginx (puerto 443, el 80 redirige), cookies `Secure`, documentación de la API apagada, proyecto aparte para no mezclar con los datos demo (T-67, T-68). Guía en [piloto.md](piloto.md) |
| HTTPS (RNF-02, R9-01) | Implementado | Certificado propio o, si no hay, autoridad local + certificado creados al arrancar (`frontend/nginx/https.sh`); se instala `logicode-ca.crt` en los equipos para no ver advertencias |
| Respaldos restaurables | Demostrado en modo piloto | Respaldo pedido desde el panel, hilo creado después, restauración por consola con los mismos comandos: el hilo desapareció |
| Manual técnico, docente y estudiante | Implementado | [manuales/](manuales/): técnico (instalación, operación, administración, respaldos, actualización, seguridad, problemas), docente y estudiante con capturas |
| Docente acepta el piloto | **Pendiente** | Plan, registro de observaciones y acta de aceptación en [piloto.md](piloto.md#7-acta-de-aceptación-del-piloto). Lo hacen el docente y el equipo durante el piloto |
| Página de inicio (RF-23) | Implementado | Misión, visión, qué se puede hacer y cómo empezar |
| Dependencias Java contra vulnerabilidades (R9-05) | Implementado | Spring Boot 4.1.2 (T-70); detalle en [calidad.md](calidad.md) |
| Proveedor en línea de alojamiento y almacenamiento | Pendiente (en observación) | P-09: se decide antes de publicar. Todo despliegue futuro será con Docker |
| Entrega objetivo | 10 de diciembre de 2026 | Versión 1.0.0 (T-71) |

### Verificación realizada (Etapa 10)

- Backend: `./mvnw verify` con Spring Boot 4.1.2 → 116 pruebas en verde (70 unitarias y 46 de integración).
- Frontend: `npm run lint`, `npm test` (42 pruebas, una nueva para la página de inicio) y `npm run build` en verde; `npm audit` en 0.
- Docker Compose en modo piloto sobre una base limpia: los cuatro servicios `healthy`; el certificado se creó solo con las IP y nombres pedidos y Chromium y `curl` lo aceptaron con la autoridad local; HTTP redirige a HTTPS; HTTP/2; las cookies de sesión y CSRF salen con `Secure`; `/swagger-ui.html` y `/api/v1/openapi` responden 404; la página de inicio muestra "versión 1.0.0 · entorno piloto".
- Datos de demostración cargados por HTTPS (Etapas 3 a 8) y recorrido en Chromium a 1280 y 390 px para las capturas de los manuales; axe-core en la página de inicio nueva: 0 hallazgos y sin desborde a lo ancho.
- Restauración demostrada en el modo piloto (detalle en la tabla).
- No verificado en el entorno de Claude: la línea `apk add openssl` del Dockerfile del frontend (el entorno no llega a los repositorios de Alpine; se probó con el mismo `openssl` tomado de otra imagen Alpine), la instalación de la autoridad en teléfonos reales y el firewall de Windows.

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Docente acepta el piloto | Acta de [piloto.md](piloto.md#7-acta-de-aceptación-del-piloto) firmada después del piloto. **Pendiente** |
| Respaldos restaurables | Demostrado en modo piloto; repetirlo en el equipo servidor antes del piloto (piloto.md, sección 5). **Pendiente: revisión de Carlos** |
| Manual técnico, docente y estudiante entregados | [manuales/](manuales/). **Pendiente: revisión de Carlos** |

## Etapa 9 · detalle

Lista de defectos, riesgos aceptados, revisión de seguridad y accesibilidad, observabilidad y guías de demostración en [calidad.md](calidad.md).

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Pruebas unitarias, de integración y de API | Implementado | 116 pruebas del backend y 41 de la interfaz; nuevas en esta etapa: `ApiSecurityIT` y el docente de otro curso en `ActivityFlowsIT` |
| Permisos | Implementado | `ApiSecurityIT` recorre todos los endpoints sin sesión y sin CSRF; las pruebas por rol y por curso de las Etapas 3 a 8 siguen en verde |
| Carga de archivos | Revisado, sin cambios | Pruebas de las Etapas 4 y 5 (tipo real, tamaño, nombre, formatos no aceptados) |
| Móvil y accesibilidad (RNF-01, RNF-11) | Implementado | axe-core en 37 vistas a 1280 y 390 px: 0 hallazgos tras corregir D9-01; ninguna vista se desborda a lo ancho |
| Seguridad | Implementado | Cabeceras de seguridad y CSP en Nginx (D9-02); `npm audit` en 0; dependencias Java sin avisos en la base OSV (R9-05, repetida el 2 de noviembre); HTTPS en el modo piloto (R9-01, Etapa 10) |
| Restauración | Demostrado | Respaldo pedido desde el panel, cambio posterior, restauración por consola: el cambio desaparece |
| Observabilidad | Implementado | Línea de log por solicitud con estado, duración e identificador (D9-03); `/actuator/metrics` solo desde el PC |

### Verificación realizada (Etapa 9)

- Backend: `./mvnw verify` → 116 pruebas en verde (70 unitarias y 46 de integración).
- Frontend: `npm run lint`, `npm test` (41 pruebas) y `npm run build` en verde.
- Docker Compose sobre una base limpia (versión 7) con los datos de demostración de las Etapas 3 a 8: auditoría con axe en Chromium, CSP sin errores en la consola (también la vista previa de PDF), métricas y líneas de log comprobadas, restauración demostrada, y el flujo de calificación hecho en la interfaz: nota automática de PSeInt (Elena, 5,0) y nota manual desde Por revisar (Camila, 4,2) vista por la estudiante en el teléfono.

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Lista de defectos críticos resuelta | [calidad.md](calidad.md): no hubo críticos; D9-01 a D9-03 corregidos. **Pendiente: revisión de Carlos** |
| Restauración y flujo de calificación demostrados | Hechos en Docker (detalle arriba); la guía para repetirlos está en [calidad.md](calidad.md#demostraciones). **Pendiente: revisión de Carlos** |

## Etapa 8 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Foro por herramienta (RF-26) | Implementado | Módulo `forum`. Página pública `/foro` con hilos de PSeInt, DFD, Scratch, Arduino y General, filtro por herramienta y búsqueda por título o texto; los fijados van primero y luego los de actividad más reciente. Leer no pide sesión; abrir un hilo o responder pide sesión (`PARTICIPATE_FORUM`). Se muestra el nombre del autor, nunca su cédula ni su correo (T-66) |
| Moderación del foro (RF-27, RN-15) | Implementado | Docente y cuentas Administrador (`MODERATE_FORUM`) fijan, cierran o reabren un hilo y eliminan hilos o respuestas. Eliminar oculta y deja quién y cuándo (T-65). Un hilo cerrado no recibe respuestas (`409`) |
| Planilla del curso (RF-18, RN-07) | Implementado | Pestaña "Planilla" del curso: una columna por actividad calificable publicada (sin quizzes de repaso ni borradores, T-60) con su porcentaje, el reparto de lo que falta y el estado de cada nota (calificada, vencida sin entrega = 0,0, en revisión o pendiente). Avance parcial y nota definitiva por separado (T-61). Avisa cuando los porcentajes no permiten cerrar la definitiva |
| Exportación a Excel (RF-18) | Implementado como CSV y, desde P-07, como `.xlsx` | "Exportar a Excel (CSV)" (hoy "Exportar a CSV"): UTF-8 con BOM, `;` y coma decimal para que Excel en español lo abra directo; la exportación usa el mismo cálculo que la pantalla (T-64) |
| Notas del estudiante (RF-20) | Implementado | El estudiante inscrito ve en "Mis notas" solo su fila: nota por actividad, avance parcial y definitiva. La retroalimentación sigue en cada entrega (Etapa 5) |
| Estadísticas básicas | Implementado | Por actividad: entregas, calificadas, en revisión, vencidas, promedio, mínima, máxima y aprobados (≥ 3,0). Por curso: estudiantes con avance o definitiva, promedios y aprobados. Panel de administración con cifras de toda la plataforma (T-62) |
| Biblioteca administrable (RF-28) | Implementado | En `/biblioteca`, las cuentas Administrador cambian la visibilidad y archivan materiales desde la misma página; el docente sigue publicando lo suyo desde el curso |
| Panel de administración | Implementado | Página `/admin` (`MANAGE_USERS`): cuentas por rol y estado, docentes por aprobar, cursos, actividades, entregas, notas en revisión o con la IA, materiales, espacio usado y actividad del foro, con enlaces a usuarios, habilitación y auditoría |
| Respaldos (RF-28) | Implementado | Servicio `backup` de Docker Compose con `respaldos/respaldo.sh`: copia diaria a la hora de `BACKUP_HOUR` (hora de Colombia) de la base y los archivos, prueba de restauración en una base aparte después de cada copia, y retención de `BACKUP_RETENTION_DAYS` días (T-63). Desde el panel (`MANAGE_BACKUPS`) se ve el estado y se pide un respaldo inmediato. Restaurar se hace por consola, documentado en [respaldos/README.md](../respaldos/README.md) |
| Editar mensajes propios del foro | Pendiente | No pedido en RF-26/27; el autor no edita ni borra lo que escribió, lo hace un moderador |
| Descargar respaldos desde la web | Pendiente (a propósito) | Los respaldos tienen datos de todos; se copian con `docker compose cp` por quien maneja el servidor |

### Verificación realizada (Etapa 8)

- Backend: `./mvnw verify` → 113 pruebas en verde (70 unitarias y 43 de integración). `GradebookRulesTest` cubre el reparto de porcentajes, la suma incompleta, el avance parcial y la definitiva. `ForumAndOperationFlowsIT` usa PostgreSQL real con reloj controlado: lectura del foro sin sesión, escritura con sesión, validación, permisos de moderación, hilo cerrado; planilla con porcentaje explícito y repartido, borrador excluido, vencida sin entrega que pasa a 0,0 y vuelve definitiva la nota, estudiante que solo ve su fila; CSV conciliado con la pantalla; permisos del panel y de los respaldos con copias falsas en una carpeta temporal.
- Frontend: `npm run lint`, `npm test` (41 pruebas) y `npm run build` en verde.
- Docker Compose sobre una base limpia (migraciones hasta la versión 7) con los datos de demostración de las Etapas 3 a 8, y un recorrido en Chromium: el visitante lee el foro, un estudiante responde desde el teléfono, el docente ve las acciones de moderación (fijar, cerrar y eliminar se hicieron por la API al cargar los datos demo), la planilla y su CSV coinciden, el estudiante ve "Mis notas" en pantalla de teléfono, el panel muestra las cifras y un respaldo pedido desde la web termina con la restauración probada. También se restauró un respaldo de verdad sobre la base en uso (lo creado después desapareció).

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Permisos administrativos revisados | Matriz de [permisos.md](permisos.md) (nota de la Etapa 8). `ForumAndOperationFlowsIT` comprueba que el estudiante no modera, que el administrador ordinario modera y exporta la planilla (calificar le sigue negado, como se prueba desde la Etapa 5), y que solo `MANAGE_USERS`/`MANAGE_BACKUPS` abren el panel y los respaldos. **Pendiente: revisión de Carlos** |
| Exportación conciliada con la escala y los pesos | La prueba compara cada celda del CSV con la planilla de la pantalla (escala 0,0 a 5,0, un decimal, porcentajes explícitos y repartidos). **Pendiente: revisión de Carlos** con el curso de demostración: sus porcentajes explícitos ya suman 100 % (30 + 40 + 30), así que Quiz 1, Taller 3 y Taller 4 quedan en 0 % y la planilla lo avisa |

## Etapa 7 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Editor e intérprete de PSeInt con aislamiento (RNF-05) | Implementado | Módulo `pseint`: intérprete propio en Java (perfil flexible) que no tiene instrucciones de archivos, red ni sistema, así que un programa no puede tocar el equipo (T-49). Límites por ejecución: 2 s, 1 000 000 de pasos, 20 000 caracteres de código y de salida, 10 000 de entrada, 10 000 posiciones de arreglos, textos de 1 000 caracteres y 200 llamadas anidadas; a lo sumo 4 ejecuciones a la vez (T-50). Página `/pseint` con ejemplos y `POST /pseint/runs` (con sesión) |
| Casos de prueba por actividad (RF-12) | Implementado | Hasta 20 casos (entradas y salida esperada) en la rúbrica versionada (RN-20). Cada línea esperada debe aparecer en la salida, en orden y como palabra completa, sin importar mayúsculas ni espacios de más (T-51) |
| Calificación determinista de PSeInt (RF-16) | Implementado | Al recibir una entrega con código (respuesta escrita o primer `.psc`) en una actividad con casos: nota = 5 × casos aprobados ÷ total, publicada como "Automática" con el detalle por caso. Error de sintaxis o instrucción no disponible → Por revisar como "El programa no se pudo ejecutar" (T-52). El docente puede volver a correr los casos en la entrega |
| Banco de preguntas (RF-10) | Implementado | Sección "Banco de preguntas" del curso: selección única, múltiple, falso o verdadero y respuesta corta (hasta 5 respuestas aceptadas; sin importar mayúsculas, tildes ni espacios). Una pregunta ya respondida no se edita (T-55) |
| Quizzes calificables y de repaso (RF-10, RF-21) | Implementado | Tipos de actividad "Quiz calificable" y "Quiz de repaso" (T-53): mismas fechas, publicación, excepciones y planilla que las demás actividades. Las preguntas elegidas y su clave quedan como la rúbrica versionada del quiz (T-54). El de repaso no da nota, se repite libremente y muestra la revisión al terminar |
| Tiempo total, envío automático e intentos (RN-13) | Implementado | El tiempo corre desde que empieza el intento; la página guarda las respuestas solas y lo envía al acabarse. Si el navegador se cierra, el servidor lo envía con lo guardado (revisión cada 30 s, margen de 30 s, T-56). Un intento por defecto; el docente da otro con "Plazo e intentos" |
| Nota por aciertos y ranking (RN-14, RF-16) | Implementado | Nota = 5 × puntos obtenidos ÷ puntos totales, con un decimal; se publica como "Automática" y entra a la planilla como una entrega más. Ranking por nota y luego por menor tiempo; en el calificable cuenta el último intento y en el de repaso el mejor (T-57). La revisión del calificable se abre cuando cierra el quiz (T-58) |
| Juegos de salida, orden, DFD y bloques (RF-22, RN-19) | Implementado | Página pública `/juegos`: "¿Qué imprime?" (21 niveles, respuestas comprobadas con el intérprete), "Caza el error" (14), "Ordena el algoritmo" (14), "Completa el DFD" (14, diagrama dibujado en SVG, con ciclos), "¿Qué hace este código?" de Arduino (12) y "Bloques" (24 niveles desde el 27 de octubre: secuencia, ruta, ciclo, ciclos anidados, condición y depuración, con ejecución animada, paso a paso, límite de bloques y personaje del robot a elegir). Avance guardado solo en el navegador (T-59) |
| Niveles de bloques configurados por el docente (RF-12, "criterios de bloques") | Implementado después de la Etapa 10 | Los niveles de `/juegos` siguen fijos en el código; el docente configura retos calificables en la rúbrica (ver "Retos de bloques" arriba) |
| Ciclo `Para Cada` de PSeInt | Implementado (2 de noviembre de 2026) | `Para Cada elemento De arreglo Hacer … FinPara` (también con `En`) recorre todas las posiciones en orden, por filas si el arreglo tiene varias dimensiones; lo que se asigne al elemento queda guardado en el arreglo, como en PSeInt. Como LogiCode acepta índices 0 a n, recorre 1 a n, o 0 a n-1 si el programa ya usó alguna posición 0. Ejemplo "Promedio de un arreglo" en el editor |
| Instrucciones de PSeInt no cubiertas | Pendiente | El perfil estricto (declarar todo, `;` obligatorio). Un programa que las use va a revisión del docente, nunca recibe un 0 automático |

### Verificación realizada (Etapa 7)

- Backend: `./mvnw verify` → 101 pruebas en verde (64 unitarias y 37 de integración). `PseintTest` cubre el lenguaje (ciclos, Segun, subprocesos por referencia, arreglos, funciones, errores con línea) y los límites (ciclo infinito, recursión sin fin, salida y arreglos enormes). `QuizAndPseintFlowsIT` usa PostgreSQL real con reloj controlado: banco y permisos, quiz que no se publica sin preguntas ni recibe archivos, intento con nota automática sin que el estudiante vea la clave, intento abandonado que el servidor envía al vencer y segundo intento habilitado por el docente, ranking, revisión al cerrar, quiz de repaso, taller de PSeInt con nota automática, nota parcial, error de sintaxis a revisión, "ejecutar casos" y los límites y la sesión del editor.
- Frontend: `npm run lint`, `npm test` (35 pruebas) y `npm run build` en verde. Las pruebas cubren que cada nivel de bloques se resuelve dentro de su límite, que los de depuración empiezan fallando, el choque y el tope de pasos, los juegos sin sesión y sin almacenamiento, el intento con guardado automático y el armado del quiz.
- Docker Compose sobre una base limpia (migraciones hasta la versión 6) con los datos de demostración de las Etapas 3 a 7, y un recorrido en Chromium: un estudiante responde el quiz con el plazo ampliado, recarga la página sin perder respuestas y ve su nota; el editor de PSeInt ejecuta un programa y corta un ciclo infinito; los juegos se juegan sin sesión, también en pantalla de teléfono.

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Ninguna ejecución accede al equipo ni excede límites | El lenguaje no tiene instrucciones de archivos, red ni sistema (T-49); `PseintTest` y `QuizAndPseintFlowsIT` prueban el corte por pasos, salida, tamaño de arreglos, anidamiento y recursión. El tope de 2 s está en el código pero no tiene prueba propia: el de pasos se alcanza antes |
| Calificaciones aceptadas por el docente | **Pendiente: revisión de Carlos.** Datos de demostración: Quiz 1 (cerrado), Repaso de tipos de datos y Taller 4 con casos de prueba |
| Niveles aceptados por el docente | **Pendiente: revisión de Carlos** en `/juegos`. Los niveles se cambian en `frontend/src/features/games/levels.js` |

## Etapa 6 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Calificación con IA de fotos y archivos compatibles (RF-14, RN-08) | Implementado | Al llegar una entrega se crea la nota en evaluación y un trabajo en la cola (`ai_grading_job`). El worker envía a Ollama el nombre del estudiante, el enunciado, la respuesta esperada, la rúbrica y el contenido (texto, fotos PNG/JPG, archivos `.psc`/`.ino`). Nunca cédula, correo ni nombres de archivo (T-44) |
| Publicación automática con contenido legible y confianza ≥ 80 % (RN-09, RN-11) | Implementado | `AiGradingPolicy`: la nota sale de los puntos por criterio (T-39). Con 80 % o más se publica con su retroalimentación y queda en el historial como "IA" |
| Foto ilegible o confianza < 80 % a confirmación docente (RF-15) | Implementado | La entrega pasa a Por revisar con el motivo y la propuesta de la IA; el docente la confirma o la ajusta |
| Fallo técnico sin nota y con reintento (RF-15, RNF-06, RNF-07) | Implementado | 3 intentos automáticos con espera creciente; después, "Falló la IA" en Por revisar. Botón "Reintentar con IA" y calificación manual siempre disponibles. La entrega nunca se bloquea ni se pierde |
| Trabajo en segundo plano con estado visible (RNF-08) | Implementado | Estado "Calificando con IA" en la bandeja, la entrega y la vista del estudiante; se actualiza solo |
| Resultado de la IA e historial para el docente (RF-17) | Implementado | Lectura del modelo, legibilidad, confianza, puntos y comentario por criterio, retroalimentación propuesta, duración y evaluaciones anteriores. El estudiante no ve nada de esto |
| Scratch y Arduino (RN-10) | Parcial | `.ino` se evalúa como texto. `.sb3`, Word, PowerPoint y video pasan al docente ("Formato que la IA no lee"). PDF: lo lee desde P-08 (T-77) |
| Calificación determinista de PSeInt y quizzes (RF-16) | Implementado en la Etapa 7 | Ver la sección de la Etapa 7 |
| Pruebas de aceptación con Qwen2.5-VL 7B en el equipo Ryzen 7 5700U | **Pendiente con fotos reales** | Arnés en [ia-pruebas/](../ia-pruebas/README.md). Ensayo del 1 de noviembre en el PC de Carlos con las 3 imágenes sintéticas: ver [ia-pruebas/README.md](../ia-pruebas/README.md#ensayo-del-1-de-noviembre-de-2026-imágenes-sintéticas). Decisión de Carlos: la regla del 80 % no cambia hasta medir con fotos manuscritas reales |

### Verificación realizada (Etapa 6)

- Backend: `./mvnw verify` → 87 pruebas en verde (58 unitarias y 29 de integración). `AiGradingFlowsIT` usa PostgreSQL real con un proveedor simulado: publicación automática con 92 %, confianza de 79 % a revisión con propuesta y confirmación docente, foto ilegible sin nota, fallo técnico reintentado con reloj controlado hasta "Falló la IA", respuesta incoherente con la rúbrica tratada como fallo, reintento del docente, la IA nunca pisa una nota puesta por el docente, PDF directo a revisión, archivo `.psc` enviado como texto, y que al modelo no llegan cédula, correo ni nombres de archivo. `OllamaGradingAdapterTest` prueba el protocolo contra un servidor HTTP simulado (esquema, imágenes, errores 404/500 y tiempo agotado); `ImagePreparerTest`, `AiGradingPolicyTest` y `AiGradingJobTest` cubren las reglas.
- Frontend: `npm run lint`, `npm test` (17 pruebas) y `npm run build` en verde.
- Docker Compose con perfil `ia` y **Ollama 0.35.1 real**, sobre la base de la Etapa 5 (la migración a la versión 5 se aplicó sin problemas). Como Qwen2.5-VL no se puede descargar aquí, se usó un modelo de texto pequeño (Qwen2.5 0,5B) solo para comprobar el protocolo: la salida estructurada con el esquema funciona, una respuesta de texto se calificó y publicó de punta a punta, y las fotos con ese modelo sin visión fallaron como fallo técnico y pasaron a "Falló la IA". Las notas de ese modelo no significan nada: la calidad de lectura y calificación solo la miden las pruebas de aceptación con Qwen2.5-VL.

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Confianza ≥ 80 % y contenido legible publica la nota | `AiGradingPolicyTest`, `AiGradingFlowsIT` |
| Foto ilegible o confianza inferior requiere confirmación docente | `AiGradingFlowsIT` (79 %, ilegible) y pantallas de Por revisar y de la entrega |
| Un fallo técnico no asigna nota y permite reintento | `AiGradingFlowsIT`, `OllamaGradingAdapterTest` y el recorrido con Ollama real |
| Lectura manuscrita, coincidencia de notas, confianza, legibilidad, latencia y RAM con Qwen2.5-VL 7B Q4_K_M en el Ryzen 7 5700U | **Pendiente con fotos reales** (ensayo sintético hecho el 1 de noviembre). `node ia-pruebas/ejecutar-pruebas.mjs ia-pruebas/muestras/muestras.json` en el equipo de presentación; el resumen queda en `ia-pruebas/resultados/` |

## Etapa 5 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Crear actividad (RF-10) | Implementado | `POST /courses/{id}/activities` crea un borrador; `PUT /activities/{id}` edita; `DELETE` archiva. Pestaña Actividades del curso y página `/actividades/{id}` |
| Apertura, cierre, porcentaje, tipo de entrega e intentos (RF-11) | Implementado | Tipo (tarea, taller, evaluación), texto, archivos (grupos de formatos) o fotos, porcentaje opcional, 1 a 10 intentos. Las fechas se escriben y muestran en hora de Colombia |
| Suma de porcentajes ≤ 100 % (RN-07) | Implementado | Crear o editar responde `422` con el disponible si se pasa |
| Rúbrica versionada (RN-20) | Implementado | `PUT /activities/{id}/rubric` (criterios con puntos y respuesta esperada) crea una versión; cambiar el enunciado también. Sin rúbrica no se publica. Cada entrega queda ligada a la versión vigente al enviarla |
| Entrega por texto, archivo o foto (RF-13) | Implementado | `POST /activities/{id}/submissions` (multipart, hasta 5 archivos, con avance de carga). Se valida el formato real con el módulo `files` |
| Historial de entregas (RF-17, RF-19) | Implementado | Intentos del estudiante (`/submissions/mine`), lista de inscritos con su situación (`/submissions`), detalle `/entregas/{id}` con vista previa y descarga con permisos |
| Bandeja Por revisar | Implementado | `GET /courses/{id}/review-queue`: último intento de cada estudiante sin nota. Pestaña Por revisar del curso |
| Calificación e historial de nota (RN-06, RN-09) | Implementado (manual) | `POST /submissions/{id}/grade`: nota 0,0 a 5,0 con un decimal y retroalimentación; corregir deja historial. La calificación con IA es de la Etapa 6 |
| Vencida sin entrega = 0,0 (RN-05) | Implementado | Estado `OVERDUE` calculado; se muestra 0,0 al estudiante y al docente |
| Excepciones de plazo y segundo intento (RF-11, RN-05) | Implementado | `PUT/DELETE /activities/{id}/exceptions/{studentId}`: nuevo plazo (también para reabrir después del cierre) e intentos adicionales. La entrega fuera del plazo original queda marcada "tarde" |
| Administrador ordinario (RN-04) | Implementado | Ve actividades, entregas, notas y la bandeja; crear, calificar o dar excepciones responde `403` |
| Estudiante ve pendientes, entregadas, calificadas y en revisión (RF-19) | Implementado | Situación en cada actividad; la nota y la retroalimentación solo cuando están publicadas |

### Verificación realizada (Etapa 5)

- Backend: `./mvnw verify` → 66 pruebas en verde (43 unitarias y 23 de integración). `ActivityFlowsIT` recorre con PostgreSQL real y un reloj que se adelanta: borrador invisible para el estudiante, publicar sin rúbrica (`422`), porcentajes sobre 100 % (`422`), entrega con PDF, foto no aceptada, sin intentos, estudiante externo (`404`), docente intentando entregar (`403`), bandeja Por revisar, calificación, corrección con historial, nota visible para el estudiante sin la respuesta esperada, nueva versión de rúbrica al cambiar el enunciado, cierre vencido (0,0), fecha de excepción en el pasado (`422`), reapertura y segundo intento marcados como tarde, archivo de la actividad. `StudentProgressTest` y `GradeTest` cubren las reglas de fechas, intentos y escala.
- Frontend: `npm run lint`, `npm test` (16 pruebas) y `npm run build` en verde.
- Docker Compose (base de datos en versión 4), recorrido por Nginx: el docente crea la actividad en la interfaz, la publicación sin rúbrica se rechaza, define la rúbrica y publica; la estudiante entrega texto y PDF desde el celular; el docente califica 4,5 desde Por revisar, ve el historial y da un segundo intento; la estudiante ve su nota y puede volver a entregar. Escritorio y 390 px sin desplazamiento horizontal.

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Un ciclo completo de creación y entrega funciona sin IA | `ActivityFlowsIT` y recorrido en Docker: crear, rúbrica, publicar, entregar, Por revisar, calificar |
| Fechas y excepciones de plazo verificadas | Reloj controlado en `ActivityFlowsIT`: cierre, vencida 0,0, reapertura, intento adicional, marca de tarde; `StudentProgressTest` |

## Etapa 4 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Curso con docente responsable | Implementado | `GET/POST /courses`, `GET/PUT /courses/{id}`; pantallas `/cursos` y `/cursos/{id}`. Quien crea el curso queda como docente responsable |
| Inscripción de estudiantes al curso (RN-03) | Implementado | Por cédula, individual o con lista CSV (y `.xlsx` desde P-07): `/courses/{id}/enrollments` y `/import`; pestaña Estudiantes. Inscribir también habilita la cédula. El primer curso inscribe las cédulas ya habilitadas (T-25) |
| Temas con herramienta y orden (RF-06) | Implementado | PSeInt, DFD, Scratch, Arduino o General; crear, editar, reordenar, archivar |
| Próxima clase (RF-09) | Implementado | `PUT /courses/{id}/next-class`; un solo tema por curso (índice único) |
| Materiales: guías, documentos, presentaciones, imágenes, videos, archivos de herramienta y enlaces (RF-07) | Implementado | `POST /topics/{id}/materials` (multipart, con avance de carga en la interfaz) |
| Público o privado (RF-08, RN-03, RN-17) | Implementado | Lo privado solo lo ven inscritos, el docente responsable y las cuentas Administrador; lo demás responde `404` |
| Biblioteca con búsqueda y filtros por tema, herramienta y tipo (RF-24) | Implementado | `GET /library`, `GET /library/topics`; pantalla pública `/biblioteca` |
| Previsualización y descarga con permisos (RF-25) | Implementado | `/materials/{id}/preview` (PDF, imágenes y videos; admite `Range` para adelantar videos) y `/download`; pantalla `/materiales/{id}` |
| Formatos, límites y validación del tipo real (RNF-04, RN-21) | Implementado | Lista de formatos aprobada; 500 MB y 1 GB para videos; se revisa la firma del archivo, no solo la extensión |
| Retención mínima de seis meses (RNF-12) | Implementado en datos | Cada archivo guarda `retain_until`; nada se borra, los temas y materiales se archivan. La limpieza automática después de la retención está pendiente |
| Administrador ordinario: publicar biblioteca, sin crear temas ni materiales (RN-04) | Implementado | Puede cambiar visibilidad y archivar; crear o editar responde `403` |
| Almacenamiento de archivos | Implementado en MinIO (P-05, T-85) | Servicio `minio` detrás del puerto `BinaryStorage`; los archivos que había en `file-storage` se pasan solos a MinIO al arrancar |

### Verificación realizada (Etapa 4)

- Backend: `./mvnw verify` → 50 pruebas en verde. `ContentFlowsIT` recorre con PostgreSQL real el punto de control: visitante (solo lo público, `404` en lo privado, vista previa y descarga del PDF público, petición parcial `Range`), estudiante inscrito (ve lo privado y no puede modificar), estudiante de fuera del curso (solo lo público), administrador ordinario (no crea, sí publica y archiva), archivos falsos rechazados (PDF que no es PDF, imagen como guía, `.exe`, enlace `javascript:`), orden de temas, próxima clase, filtros de la biblioteca, archivado, inscripción por CSV y retiro. `FileFormatTest` cubre la detección de formatos.
- Frontend: `npm run lint`, `npm test` (12 pruebas) y `npm run build` en verde.
- Docker Compose (tres servicios `healthy`, base de datos en versión 3), recorrido por Nginx: el docente crea curso y temas, un PDF falso es rechazado, sube PDF público, imagen privada y enlace privado, marca la próxima clase e inscribe por CSV; un visitante ve 1 material y abre la vista previa; una estudiante inscrita ve 3 materiales y la imagen privada. Revisado en escritorio y en móvil (390 px) sin desplazamiento horizontal.

### Punto de control

| Criterio | Cómo se comprueba |
| --- | --- |
| Visitante y usuario de curso ven exactamente los recursos permitidos | `ContentFlowsIT` (visitante, inscrito, externo, administrador ordinario) y recorrido en Docker |
| Móvil revisado | Capturas a 390 px de biblioteca, curso y vista previa; sin desbordes |

## Etapa 3 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Registro de estudiante con cédula habilitada (RF-01, RN-01) | Implementado | `POST /auth/student-registrations`; pantalla `/registro/estudiante` |
| Solicitud de cuenta docente y aprobación (RF-02, RN-02) | Implementado | `POST /auth/teacher-requests`, `POST /admin/teachers/{id}/approve`; pantallas `/registro/docente` y `/admin/usuarios` |
| Inicio y cierre de sesión con cédula (RF-03) | Implementado | Sesión en PostgreSQL (D-01), cookie `LOGICODE_SESSION` HttpOnly, CSRF; tras 5 intentos fallidos en 15 min se responde `429` |
| Confirmación de correo (RF-29, RN-24) | Implementado | Enlace de un solo uso, vence en 24 h (T-13); `/verificar-correo` |
| Recuperación de contraseña (RF-04, RN-23) | Implementado | Solo correo verificado, token de LogiCode de un uso y 60 min, respuesta genérica, 2 por día en Colombia, cierra todas las sesiones; `/recuperar-contrasena`, `/restablecer-contrasena` |
| Cambio asistido de correo (RF-30, RN-25) | Implementado | Último correo + contraseña actual o anterior; el correo nuevo se verifica antes de reemplazar; 2 por día con contador propio (D-04); `/recuperar-correo` |
| Cambio de contraseña con cierre de sesiones (RNF-02) | Implementado | `POST /me/password`; solo se guardan el hash actual y el anterior (BCrypt, D-02) |
| Habilitación de estudiantes individual y por lista (RF-05) | Implementado | `POST /student-enablements` y `/import` (CSV o `.xlsx` desde P-07, hasta 2000 filas); pantalla `/admin/habilitaciones` |
| Roles y autorización (RF-28, RN-04, RN-15, RN-22) | Implementado | `AccessPolicy` + verificación en cada caso de uso; matriz en [permisos.md](permisos.md) |
| Privilegio completo solo para las 2 cuentas iniciales (D-03, RN-16) | Implementado | Se crean desde `INITIAL_ADMIN_*` en `.env`; nadie más puede tenerlo; no se puede quedar la plataforma sin una activa |
| Auditoría de acceso | Implementado | Tabla `audit_event`; `GET /admin/audit-events`; pantalla `/admin/auditoria` |
| Envío de correo con Brevo | Implementado y probado con la cuenta real (1 de noviembre) | `docker-compose.brevo.yml`; remitente `logicode.notificaciones@gmail.com` en `BREVO_SENDER_EMAIL` (P-06, T-86) y API key como secreto. En desarrollo se usa la bandeja local `.local/mail-outbox` |

### Verificación realizada (Etapa 3)

- Backend: `./mvnw verify` → 40 pruebas en verde, entre ellas `IdentityFlowsIT`, que recorre con PostgreSQL real: alta de las cuentas iniciales, solicitud y aprobación docente, habilitación y registro de estudiantes (individual y CSV), búsqueda de usuarios, administrador ordinario sin cambio de roles (`403`), RN-16, cierre de todas las sesiones al cambiar contraseña, recuperación (correo verificado, un uso, límite diario), cambio asistido de correo con la contraseña anterior, bloqueo por intentos fallidos y CSRF obligatorio.
- Pruebas de arquitectura: capas y, desde esta etapa, fronteras entre módulos.
- Frontend: `npm run lint`, `npm test` (9 pruebas) y `npm run build` en verde.
- Docker Compose (tres servicios `healthy`), recorrido completo a través de Nginx con cookies reales: verificación de correo de la cuenta inicial → creación de contraseña → inicio de sesión → habilitación individual y CSV → registro de estudiante → búsqueda → cierre de sesión. Las pantallas se revisaron en escritorio y en móvil (390 px).

### Criterios de aceptación de la especificación

| Criterio | Cómo se comprueba |
| --- | --- |
| Permisos completos y cambio de rol solo en las dos cuentas iniciales | `AccessPolicyTest`, `IdentityFlowsIT.ordinaryAdminCannotChangeRolesAndOnlyInitialAccountsCan` |
| Un administrador ordinario no responde actividades, no crea temas/materiales/actividades, no califica ni ajusta notas | `AccessPolicyTest` (permisos ausentes). Los endpoints de esas acciones llegan en las Etapas 4 a 6 y usarán estos mismos permisos |
| No quedarse sin cuenta inicial activa con privilegios completos | `IdentityFlowsIT.platformNeverLosesItsLastActiveFullAdmin` y el arranque, que restablece el privilegio (D-03) |

## Etapa 2 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Repositorio Git y ramas | Implementado | Rama `main`; flujo en README |
| Estructura React | Implementado | `frontend/` |
| Estructura Spring Boot modular por capas | Implementado | `backend/src/main/java/co/edu/unipamplona/logicode/` |
| API REST JSON con prefijo `/api/v1` | Implementado | Ver [api/README.md](api/README.md) |
| PostgreSQL y migraciones versionadas | Implementado | Flyway, `V1` a `V3` |
| Docker Compose | Implementado | `docker-compose.yml` (db, backend, frontend) |
| Configuración por entorno | Implementado | `.env.example`, `application.yml` |
| Almacenamiento de archivos | Implementado en la Etapa 4 en disco local (T-22); en MinIO desde el 1 de noviembre (T-85) | Servicio `minio`, volumen `minio-data` |
| CI básico | Implementado (sin ejecutar en GitHub) | `.github/workflows/ci.yml`; el repositorio aún no está publicado en GitHub |
| Health checks y OpenAPI | Implementado | `/actuator/health/*`, `/api/v1/openapi`, `/swagger-ui.html` |

## Requerimientos funcionales

| Requerimientos | Estado |
| --- | --- |
| RF-01, RF-02, RF-03, RF-04, RF-29, RF-30 | Implementados |
| RF-05 | Implementado con CSV y Excel `.xlsx` (P-07) |
| RF-06, RF-07, RF-08, RF-09, RF-24, RF-25 | Implementados (Etapa 4) |
| RF-28 | Implementado: usuarios, roles, aprobación docente, auditoría, biblioteca, moderación del foro y respaldos |
| RF-10, RF-11, RF-13, RF-19 | Implementados (Etapa 5) |
| RF-14, RF-15, RF-17 | Implementados (Etapa 6): calificación con IA en segundo plano, confirmación docente y registro de cada evaluación. Pruebas de aceptación del modelo pendientes en el equipo de referencia |
| RF-12, RF-16, RF-21, RF-22 | Implementados (Etapa 7): casos de prueba y calificación automática de PSeInt, quizzes con banco de preguntas, quiz de repaso con revisión y juegos. Retos de bloques que configura el docente: implementados después de la Etapa 10 (BD versión 8) |
| RF-18, RF-20, RF-26, RF-27 | Implementados (Etapa 8): planilla con exportación CSV y, desde P-07, `.xlsx`, notas del estudiante y foro con moderación |
| RF-23 | Implementado (Etapa 10): página de inicio con misión, visión y explicación de la plataforma. El texto de misión y visión es una propuesta a partir del propósito de la especificación |
