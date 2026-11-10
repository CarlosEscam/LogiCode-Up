# Registro de decisiones

Tres categorías, para no confundir lo aprobado con lo propuesto:

- **Decisión aprobada**: viene de la especificación. No se cambia sin un cambio de alcance registrado.
- **Decisión técnica tomada (Etapa 2)**: elección de implementación hecha al construir la fundación, dentro de lo aprobado. Revisable.
- **Pendiente de decidir**: se necesita una respuesta antes de la etapa indicada.

## Decisiones aprobadas (especificación)

| Tema | Decisión |
| --- | --- |
| Tecnologías | React · API REST JSON · Java con Spring Boot · PostgreSQL · Docker |
| Arquitectura | Monolito modular con capas api, application, domain, infrastructure; sin microservicios ni Kubernetes |
| Despliegue | Desarrollo, pruebas y presentación en local con Docker Compose; publicación en línea en observación |
| Administradores con privilegios completos | Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi; las demás cuentas Administrador no cambian roles, no responden actividades, no crean temas/materiales/actividades, no califican ni ajustan notas |
| Contraseñas | Mínimo 8 caracteres, al menos un número y un carácter especial; BCrypt o Argon2id; solo hash actual e inmediatamente anterior |
| Recuperación | Solo con correo verificado; token generado y validado por LogiCode, aleatorio, un uso, 60 minutos; Brevo solo envía; máximo 2 solicitudes por cuenta por día calendario en Colombia; respuesta genérica; al cambiar contraseña se cierran todas las sesiones |
| Cambio asistido de correo | Último correo registrado + contraseña actual o anterior; el correo nuevo se verifica antes de reemplazar al actual |
| IA | Ollama + Qwen2.5-VL 7B (Q4_K_M) local en Docker; publicación automática con contenido legible y confianza reportada ≥ 80 %; si no, confirmación docente; un fallo técnico no genera nota |
| Privacidad IA | Se envía nombre, archivo, enunciado, rúbrica y escala; nunca cédula ni correo |
| Archivos | 500 MB por archivo, videos hasta 1 GB, retención mínima de seis meses; formatos PDF, DOC/DOCX, PPT/PPTX, PNG/JPG/JPEG, MP4/MOV, PSC, INO, SB3 |
| Notas | Escala 0,0 a 5,0; vencida sin entrega = 0,0 salvo reapertura; definitiva separada del avance parcial |
| Juegos | Bloques con secuencias, rutas, ciclos, condiciones y depuración, inspirados en Scratch |
| Migraciones | Flyway |
| Tareas lentas | Worker Spring Boot + tabla de trabajos en PostgreSQL |

## Decisiones técnicas tomadas en la Etapa 2

| # | Decisión | Motivo |
| --- | --- | --- |
| T-01 | Java 21 (LTS) y Spring Boot 4.1 | Versiones con soporte vigente a la fecha de entrega (dic. 2026). La línea 3.5 de Spring Boot ya no recibe soporte libre |
| T-02 | Maven con wrapper (`mvnw`) | No exige instalar Maven; es el estándar de Spring Boot |
| T-03 | Paquete raíz `co.edu.unipamplona.logicode`; código en inglés, interfaz y documentación en español | Coincide con los ejemplos de nombres de la especificación (ActivityController, SubmitWork) |
| T-04 | React con **JavaScript** (no TypeScript) y Vite como herramienta de compilación | TypeScript no está en las tecnologías aprobadas. Vite es la herramienta de compilación recomendada para React sin framework adicional |
| T-05 | Pruebas: JUnit 5, Testcontainers (PostgreSQL real), ArchUnit; Vitest + Testing Library; lint con oxlint | Solo en pruebas, no afectan la ejecución. ArchUnit hace cumplir las capas aprobadas |
| T-06 | Nginx sirve el frontend y reenvía `/api` | La especificación menciona Nginx como proxy reverso; evita CORS |
| T-07 | PostgreSQL 17 (imagen `postgres:17-alpine`) | Versión estable con soporte largo |
| T-08 | Errores en formato Problem Details (RFC 9457) | Estándar soportado por Spring; formato único para la interfaz |
| T-09 | Fechas en UTC en base de datos; reglas de calendario con `America/Bogota` | La regla de "día calendario en Colombia" necesita una zona explícita |
| T-10 | La contraseña de la base de datos local va en `.env` (ignorado por Git) | Es solo para el entorno local. Las credenciales de Brevo (Etapa 3) se manejarán como secretos de Docker, como pide la especificación |
| T-11 | Base de datos y backend publicados solo en `127.0.0.1`; el frontend en el puerto 3000 | PostgreSQL no se expone a la red. El frontend queda accesible para presentar desde otro equipo de la red local |
| T-12 | Flujo Git: rama `main` estable + ramas `feature/<etapa>-<tema>`; integración por pull request con CI en verde | Ver README |

## Decisiones aprobadas para la Etapa 3 (19 de octubre de 2026)

| # | Decisión | Detalle |
| --- | --- | --- |
| D-01 | Sesiones guardadas en el servidor (PostgreSQL) | Spring Session JDBC con cookie HttpOnly y protección CSRF. Cerrar todas las sesiones = borrar las sesiones de la cuenta. Reemplaza la pregunta P-01 |
| D-02 | Contraseñas con BCrypt | Incluido en Spring Security, sin dependencias adicionales. Reemplaza P-02 |
| D-03 | Cuentas iniciales cargadas desde configuración local | Cédula, nombre y correo de las dos administradoras se leen de variables de entorno (`INITIAL_ADMIN_*` en `.env`, ignorado por Git). Nunca se escriben en el código, en migraciones ni en el repositorio. No se configura contraseña: cada cuenta la define con el flujo de verificación de correo. Reemplaza P-03 |
| D-04 | Límite de 2 solicitudes diarias contado por separado | Un contador para recuperación de contraseña y otro para cambio asistido de correo, por cuenta y por día calendario en Colombia. Reemplaza P-04 |

## Decisiones técnicas tomadas en la Etapa 3

| # | Decisión | Motivo |
| --- | --- | --- |
| T-13 | El enlace de verificación de correo vence en 24 horas | La especificación fija 60 minutos solo para la recuperación de contraseña. 24 h da margen a quien se registra y no abre el correo de inmediato; sigue siendo de un solo uso |
| T-14 | En desarrollo los correos se guardan como archivos `.html` en `.local/mail-outbox` (proveedor `outbox`); con `docker-compose.brevo.yml` se envían con Brevo | Permite probar todos los flujos sin cuenta de Brevo. La carpeta está fuera de Git porque contiene enlaces de un solo uso |
| T-15 | Los correos se envían después de confirmar la transacción y en segundo plano | Un correo nunca sale si la operación falló, y la respuesta no espera a Brevo. Si el envío falla queda en el log y el usuario puede pedir otro enlace |
| T-16 | Tras 5 intentos fallidos de inicio de sesión en 15 minutos para una cédula, se responde `429` hasta que pase la ventana | Protección básica contra adivinar contraseñas (RNF de seguridad). Se cuenta en la auditoría, sin tablas nuevas |
| T-17 | Al arrancar, el backend sincroniza las cuentas iniciales con `INITIAL_ADMIN_*`: las crea si faltan, les restablece el privilegio completo, se lo retira a cualquier otra cuenta y reenvía la verificación mientras su correo no esté verificado | Aplica D-03 y RN-16 aunque alguien edite la base de datos a mano. Si se pierde el correo de verificación basta con reiniciar el backend |
| T-18 | El correo vive en `user_account` (columna `email` + `email_verified_at`); un correo nuevo espera en `one_time_token.target_email` hasta verificarse | Más simple que la tabla `user_email` propuesta: solo existe un correo vigente por cuenta y el pendiente solo vive mientras el enlace es válido |
| T-19 | Las entidades del dominio usan anotaciones `jakarta.persistence`; los puertos de `application` los implementan adaptadores con Spring Data en `infrastructure` | Evita duplicar cada entidad en un modelo JPA aparte. El dominio sigue sin depender de Spring (lo verifica ArchUnit) |
| T-20 | Enrutador propio mínimo en React (History API) en vez de una librería | Pocas rutas planas; no agrega dependencias sin aprobación. Se puede reemplazar si la navegación crece |
| T-21 | Se desactiva el usuario en memoria que Spring Security crea por defecto | Las cuentas viven en PostgreSQL; evita una contraseña generada en el log |

## Decisiones técnicas tomadas en la Etapa 4

| # | Decisión | Motivo |
| --- | --- | --- |
| T-22 | Archivos en un volumen local de Docker (`file-storage`) detrás del puerto `BinaryStorage`, en vez de MinIO | Opción por defecto mientras se respondía P-05: no agregaba un servicio más al equipo de presentación. **Reemplazada por T-85** el 1 de noviembre de 2026; el adaptador local sigue para ejecutar el backend fuera de Docker y para las pruebas |
| T-23 | Módulo `files` aparte: valida extensión, tamaño y firma real, guarda solo metadatos en `stored_file` y usa claves aleatorias | Las Etapas 5 a 8 (entregas, fotos) reutilizan la misma validación. El nombre original nunca se usa como ruta |
| T-24 | Inscripción por cédula en `course_enrollment` | El docente puede inscribir antes de que el estudiante se registre; al registrarse ya ve el contenido privado |
| T-25 | El primer curso que se crea inscribe todas las cédulas ya habilitadas | Los estudiantes habilitados en la Etapa 3 no pierden acceso al crear el curso |
| T-26 | Temas y materiales se archivan, no se borran | Cumple la retención de seis meses (RNF-12) y permite recuperar contenido. La limpieza de archivos vencidos se agregó después (T-88) |
| T-27 | Lo que alguien no puede ver responde `404`, no `403` | No revela la existencia de cursos o materiales privados |
| T-28 | El administrador ordinario puede cambiar visibilidad y archivar materiales (publicar biblioteca), no crearlos ni editarlos | Une "publicar biblioteca" de RF-28 con RN-04 |
| T-29 | `Role` y `Permission` pasan a `shared.domain`; la anotación `@CurrentActor` pasa a `identity.application` | Otros módulos los necesitan y la regla de módulos solo permite usar la capa `application` de otro módulo |

## Decisiones técnicas tomadas en la Etapa 5

| # | Decisión | Motivo |
| --- | --- | --- |
| T-30 | Dos módulos: `activities` (actividades, entregas, excepciones) y `grading` (rúbricas, notas, historial). `activities` define el puerto `GradingGateway` y `grading` lo implementa | La dependencia va en un solo sentido (grading → activities); la Etapa 6 agrega la IA dentro de `grading` sin tocar las entregas |
| T-31 | En la Etapa 5 el docente pone una nota global de 0,0 a 5,0 con retroalimentación; los criterios de la rúbrica orientan, no se puntúan uno a uno | Cumple el punto de control "sin IA" sin adelantar la calificación por criterio, que define la Etapa 6 con el modelo |
| T-32 | "Vencida sin entrega = 0,0" se calcula al consultar (estado `OVERDUE`), no se guarda una nota | Si el docente reabre el plazo, el 0,0 desaparece solo; no hay tareas programadas que puedan fallar |
| T-33 | Cuenta el último intento de cada estudiante: es el que entra a Por revisar y el que ve el estudiante como su nota. Los anteriores conservan su nota en el historial | Regla simple y predecible para el segundo intento. La nota definitiva aparte del avance parcial llega con la planilla (Etapa 8) |
| T-34 | Una excepción por estudiante y actividad, con plazo nuevo y/o intentos adicionales; el plazo nunca se acorta y la entrega posterior al cierre original se marca "tarde" | Cubre reabrir (RN-05) y segundo intento con un solo concepto |
| T-35 | Hasta 5 archivos por entrega, y las fotos aceptan PNG o JPG | Límite razonable para tareas; la carga total por petición está limitada a 1,1 GB por Nginx y Spring |
| T-36 | El borrador solo lo ve quien administra el curso; la respuesta esperada solo el docente y las cuentas Administrador | Evita que el estudiante vea la solución en la rúbrica |
| T-37 | Las fechas de actividades se escriben y muestran en hora de Colombia (UTC−5) sin importar la zona del equipo | Colombia no tiene horario de verano; evita errores si un navegador está en otra zona |

## Decisiones técnicas tomadas en la Etapa 6

| # | Decisión | Motivo |
| --- | --- | --- |
| T-38 | Cola de evaluación en PostgreSQL (`ai_grading_job`) y worker dentro del backend: toma un trabajo con `FOR UPDATE SKIP LOCKED` y un token de toma, llama al modelo fuera de toda transacción y guarda el resultado en otra; procesa de a uno | Es el diseño de la especificación (sin RabbitMQ). El token impide aplicar dos veces un resultado si un trabajo bloqueado se retoma; en CPU no sirve evaluar en paralelo |
| T-39 | El modelo devuelve puntos por criterio y LogiCode calcula la nota: 5 × puntos obtenidos ÷ puntos totales, con un decimal. Puntos fuera de rango o criterios que no cuadran con la rúbrica cuentan como fallo técnico | La nota siempre corresponde a la rúbrica vigente y nunca se publica una nota que el modelo "inventó" aparte |
| T-40 | El umbral de 80 % es fijo en el dominio, no configurable | Es una decisión aprobada; cambiarlo es un cambio de alcance |
| T-41 | La IA lee respuestas escritas, fotos PNG/JPG y archivos `.psc`/`.ino`. Si la entrega trae otro formato, va completa al docente; no se califica a medias | Word, PowerPoint, video y Scratch necesitan conversión. Desde P-08 la IA sí lee PDF (T-77) |
| T-42 | Fallo técnico: hasta 3 intentos con espera creciente (2 y 4 minutos); después la entrega pasa a Por revisar como "Falló la IA", sin nota. Cada intento queda registrado | RF-15 y RNF-06: reintento controlado y estado visible |
| T-43 | El docente puede calificar en cualquier momento, también mientras la IA trabaja. Si la IA termina después, su resultado se guarda pero no cambia la nota | La revisión humana manda (RN-09) y nunca queda bloqueada por el modelo |
| T-44 | Al modelo se envía el nombre del estudiante, el título, el enunciado y la respuesta esperada de la versión de rúbrica, los criterios y el contenido con etiquetas neutras ("Imagen 1", "Archivo 2"). Nunca cédula, correo ni el nombre original de los archivos. Fotos enderezadas según EXIF y reducidas a 1280 px; temperatura 0; respuesta JSON con esquema; tope de 3072 tokens | RN-08 y RN-18; el nombre de un archivo puede contener la cédula. Resultados reproducibles y sin respuestas sin fin |
| T-45 | La IA viene apagada (`LOGICODE_AI_ENABLED=false`) y Ollama corre como servicio opcional de Docker Compose con el perfil `ia` (imagen `ollama/ollama:0.35.1`, modelo en el volumen `ollama-models`). Apagada, las entregas pasan a Por revisar como "Calificación manual", y los trabajos que hubieran quedado en cola pasan al docente al arrancar | Quien no tenga el modelo descargado puede usar todo lo demás; el modelo pesa unos 6 GB |
| T-46 | La entrega sigue respondiendo `201` con el intento; el estado de la calificación se consulta en `GET /submissions/{id}/grade` (`PROCESSING` mientras la IA trabaja) y la interfaz lo actualiza cada 10 s | La entrega sí queda creada al responder; reemplaza la nota de la Etapa 5 que anunciaba `202` |
| T-47 | El estudiante ve "Calificando con IA" o "En revisión" y su nota cuando se publica; no ve el motivo, la confianza ni el resultado de la IA | La confirmación docente es un trámite interno; evita que el estudiante dispute la propuesta antes de la revisión |
| T-48 | La calificación determinista (RF-16: casos de prueba de PSeInt y claves de quizzes) se implementa en la Etapa 7 | Los casos de prueba, el intérprete aislado y los quizzes se crean en esa etapa; hoy no hay contra qué comparar. Hecho en la Etapa 7 (T-52, T-57) |

## Decisiones técnicas tomadas en la Etapa 7

| # | Decisión | Motivo |
| --- | --- | --- |
| T-49 | Intérprete de PSeInt propio en Java (módulo `pseint`, perfil flexible), dentro del backend. El aislamiento viene del lenguaje: no tiene instrucciones de archivos, red, procesos ni reflexión, y `AZAR` usa una semilla fija | No hay un intérprete de PSeInt reutilizable para Java; ejecutar el PSeInt oficial exigiría un contenedor por ejecución. Así ningún programa toca el equipo y las notas se repiten igual |
| T-50 | Límites por ejecución: 2 s, 1 000 000 de pasos, código y salida de 20 000 caracteres, entrada de 10 000, 10 000 posiciones de arreglos, textos de 1 000, 200 llamadas anidadas y 100 bloques anidados; a lo sumo 4 ejecuciones a la vez (si no hay turno en 10 s, responde 429) | RNF-05: CPU, memoria y tiempo acotados; sobran para los ejercicios del curso |
| T-51 | Comparación de salida: cada línea no vacía de la salida esperada debe aparecer en la salida, en orden, como palabra completa y sin importar mayúsculas ni espacios de más. "5" no coincide dentro de "15", "-5" ni "5.5" | Los mensajes que pide el programa ("Escribe un número") no deben costar el caso; un valor equivocado sí |
| T-52 | Nota de PSeInt = 5 × casos aprobados ÷ total, con un decimal, publicada como "Automática" con el detalle por caso. Si el programa no se puede leer (error de sintaxis o instrucción aún no disponible) la entrega va al docente como "El programa no se pudo ejecutar", sin nota. Un error durante un caso (p. ej. división por cero) solo hace fallar ese caso | Un vacío del intérprete nunca debe convertirse en un 0 para el estudiante |
| T-53 | Los quizzes son actividades de tipo `QUIZ` o `PRACTICE_QUIZ`, sin formas de entrega. Un intento terminado del calificable crea una entrega con su resumen y la nota automática | Reutiliza fechas, publicación, excepciones de plazo, intentos, notas, historial y planilla sin duplicarlos |
| T-54 | Al armar el quiz se guarda una versión de rúbrica: un criterio por pregunta con sus puntos y la clave como respuesta esperada. Un quiz no acepta una rúbrica escrita a mano | RN-20: la nota queda atada a la versión vigente cuando se respondió |
| T-55 | Una pregunta del banco que ya se respondió en un intento no se edita (409) y un quiz con intentos no cambia preguntas ni tiempo; archivar una pregunta que un quiz usa también responde 409 | Cambiar la clave después cambiaría notas ya publicadas |
| T-56 | El tiempo del intento corre desde que empieza y termina en lo que llegue primero: el tiempo total o el cierre (con su excepción). El servidor acepta respuestas hasta 30 s después, por la red, y cada 30 s envía los intentos vencidos con lo guardado (`logicode.quizzes.sweeper-enabled`) | RN-13: el envío automático no depende de que la página siga abierta |
| T-57 | Nota del quiz = 5 × puntos obtenidos ÷ puntos totales, con un decimal (cada pregunta vale de 1 a 10 puntos; la respuesta corta y la múltiple valen solo si son exactas). Ranking por nota y luego por menor tiempo; en el calificable cuenta el último intento (como su nota, T-33) y en el de repaso el mejor | RN-14 |
| T-58 | Revisión de respuestas: en el quiz de repaso, al terminar cada intento; en el calificable, cuando cierra el quiz. El estudiante nunca recibe la clave antes | RF-21 sin regalar las respuestas a quien aún no responde el calificable |
| T-59 | Los juegos son públicos y corren solo en el navegador; los niveles están en el código y el avance se guarda en `localStorage` (si el navegador no deja, se juega igual sin recordar). El juego de bloques tiene un tope de 400 pasos | No dan nota, así que no necesitan servidor ni cuenta. Un editor de niveles para el docente queda pendiente |
| T-60 | La planilla tiene una columna por actividad publicada que da nota: tareas, talleres y quizzes calificables. Los quizzes de repaso y los borradores no entran | El de repaso no da nota (RF-21) y un borrador aún no existe para el estudiante |
| T-61 | RN-07 en la planilla: el porcentaje explícito cuenta tal cual y lo que falta para 100 % se reparte por igual entre las actividades sin porcentaje. La **nota definitiva** existe solo si los porcentajes suman 100 % y todas las actividades tienen resultado (calificada, o vencida sin entrega = 0,0, T-32); el **avance parcial** es el promedio ponderado de las que ya tienen resultado. Un decimal, la mitad se redondea hacia arriba (3,25 → 3,3). Aprueba con 3,0. Si los explícitos ya suman 100 %, las actividades sin porcentaje valen 0 % y la planilla lo avisa | La especificación pide mostrarlas por separado y no dice cómo se calcula el avance; 3,0 es la nota mínima aprobatoria habitual en la escala 0 a 5. Confirmar en la revisión |
| T-62 | Las cifras del panel de administración salen de una sola consulta de solo lectura que cuenta en las tablas de todos los módulos | Contar por cada módulo exigiría puertos nuevos en todos solo para números; la consulta no escribe nada |
| T-63 | Respaldos con un contenedor aparte (`postgres:17-alpine`, el de la base) que corre `respaldos/respaldo.sh`: `pg_dump` + `tar` de los archivos, prueba de restauración en una base temporal después de cada copia y borrado de las de más de `BACKUP_RETENTION_DAYS` días. La hora es la de Colombia (UTC−5 fijo, sin horario de verano). El panel pide un respaldo dejando un archivo en el volumen compartido; el backend solo lee | No agrega tecnologías y no le da al backend acceso de superusuario a la base. Restaurar es por consola, porque reemplaza todo |
| T-64 | Exportación a Excel como CSV (P-07): UTF-8 con BOM, separador `;`, coma decimal y un `'` delante de lo que empiece por `=`, `+`, `-`, `@` o tabulador. Sale del mismo cálculo que la pantalla | Excel en español lo abre directo sin Apache POI; el `'` evita que un nombre o título se ejecute como fórmula |
| T-65 | En el foro, eliminar oculta el hilo o la respuesta y guarda quién y cuándo; no se borra de la base | Deja rastro de la moderación (RN-15) y permite revisar un caso de abuso |
| T-66 | El foro muestra solo el nombre del autor, nunca su cédula ni su correo | Es de lectura pública (RF-26) |

## Decisiones técnicas tomadas en la Etapa 10

| # | Decisión | Motivo |
| --- | --- | --- |
| T-67 | HTTPS del piloto en el mismo Nginx del frontend, activado por `docker-compose.piloto.yml`. Si no hay certificado, Nginx crea al arrancar una autoridad local y un certificado firmado por ella para `localhost` y los nombres o IP de `LOGICODE_TLS_HOSTS` (397 días). Un certificado de la Universidad se usa poniéndolo en `certificados/` | El piloto es en la red del aula, sin dominio público, así que no se puede usar Let's Encrypt. Con la autoridad local, los equipos que la instalan no ven advertencias, y al cambiar la IP solo se rehace el certificado del servidor. No agrega servicios ni tecnologías: Nginx ya estaba aprobado (especificación, capa de borde) |
| T-68 | El piloto usa su propio proyecto de Compose (`COMPOSE_PROJECT_NAME=logicode-piloto`), con base de datos y archivos aparte | Los datos de demostración tienen contraseña pública y no deben llegar a la instalación real; así tampoco se borra la instancia de presentación |
| T-69 | El backend reconoce `X-Forwarded-Proto` de Nginx (`server.forward-headers-strategy: native`, solo desde redes internas) | Detrás de HTTPS la cookie CSRF también sale con `Secure` |
| T-70 | Spring Boot 4.1.1 → 4.1.2 | La revisión de dependencias (R9-05) encontró vulnerabilidades publicadas en septiembre y octubre de 2026 en Tomcat 11.0.24 y Jackson 3.1.5 / 2.21.5; 4.1.2 trae las versiones corregidas. Las 116 pruebas siguen en verde |
| T-71 | Versión 1.0.0 para el piloto | Marca la versión que se entrega; la página de inicio y `/api/v1/system/info` la muestran |

## Decisiones técnicas de los retos de bloques (RF-12, después de la Etapa 10)

| # | Decisión | Motivo |
| --- | --- | --- |
| T-72 | El reto se guarda como JSON en `rubric_version.block_challenge` (migración `V8`), con el mismo formato de programa del juego | Queda versionado con la rúbrica (RN-20) como los casos de prueba: cambiar el reto crea una versión nueva y cada entrega conserva el reto con que se calificó. Una sola columna evita tablas para un dato que siempre se lee entero |
| T-73 | Una actividad se califica con casos de prueba o con un reto de bloques, no con los dos; el reto exige entregas en texto | El programa de bloques viaja en la respuesta escrita (JSON), sin un tipo de entrega nuevo. Dos calificaciones automáticas sobre la misma entrega no tendrían una nota clara |
| T-74 | Nota todo o nada: 5,0 si el robot llega a la meta y 0,0 si no. El servidor vuelve a ejecutar el programa (`BlockChallenge`, copia en Java del intérprete de `games/blocks.js`) y no confía en el resultado del navegador. Un programa con bloques no permitidos, con más bloques del máximo o que no es un programa va a Por revisar | Es la regla de los juegos (llegar o no llegar) y la que el docente puede explicar. Volver a ejecutar evita que una entrega armada a mano por la API se califique sola. Lo que la interfaz no deja armar no merece un 0 automático: lo mira el docente, como un programa de PSeInt que no corre (T-52) |

## Decisiones técnicas del juego de bloques ampliado (27 de octubre de 2026)

| # | Decisión | Motivo |
| --- | --- | --- |
| T-78 | Los niveles siguen fijos en `levels.js` (24 de bloques), con los ids de los anteriores. Cada nivel trae una solución y las pruebas comprueban que cabe en el límite y que los niveles de ciclos o condiciones no se pueden resolver con una secuencia simple | Carlos pidió más niveles, no un editor (los retos con nota ya los configura el docente, RF-12). Conservar los ids mantiene el avance guardado. La prueba evita que un nivel de ciclos se pueda pasar sin usar el ciclo |
| T-79 | El personaje del robot es solo apariencia: SVG propios dentro de la casilla, ocultos para lectores de pantalla, y la elección se guarda en el navegador (`logicode.juegos.personaje`) | Las imágenes enviadas son de terceros, así que se redibujaron en un estilo parecido. La dirección se sigue viendo (giro o mirada más triángulo) y se sigue leyendo en el texto de estado, que no cambia. No es un dato del estudiante, así que no va al servidor |
| T-80 | Los niveles nuevos de "¿Qué imprime?", "Ordena el algoritmo" y "Completa el DFD" usan el mismo tipo de ejercicio y solo instrucciones que el intérprete de LogiCode ejecuta; las respuestas se comprueban corriendo el programa. En "Ordena" no se usan pasos que se puedan intercambiar (por ejemplo, dos inicializaciones seguidas o los casos de un Según), porque el juego compara contra un solo orden | Carlos pidió más niveles y más difíciles manteniendo la lógica y los temas de cada juego. Un nivel con dos órdenes válidos marcaría mal una respuesta correcta |
| T-81 | "Caza el error" es un juego más de `/juegos`, solo en el navegador como los demás: un error por programa, primero la línea y después la corrección entre tres opciones. Los programas no piden datos (salvo uno, cuyas entradas dice el enunciado) y cada opción se comprobó con el intérprete | Separar los dos pasos enseña a ubicar el error antes de arreglarlo, que es lo que cuesta. Elegir entre opciones evita depender del servidor y de una sesión, y permite explicar por qué fallan las otras |
| T-82 | Las fuentes del rediseño (Fraunces, DM Sans y JetBrains Mono, licencia OFL) se guardan como archivos `.woff2` en `frontend/public/fonts/` en vez de cargarlas de Google Fonts o agregar un paquete de npm | La CSP de Nginx solo permite archivos del mismo origen y el piloto puede correr sin internet; son tres archivos (unos 150 KB) y no agregan dependencias |
| T-83 | Los juegos se muestran como catálogo de tarjetas con filtros por herramienta y tema, armado solo en el navegador a partir de la lista `GAMES`; Bloques se clasifica como Scratch | Carlos pidió que los juegos se vean y filtren como la biblioteca y que sea fácil agregar más. Los juegos no tienen datos en el servidor, así que no hace falta API ni cambio de base; con una entrada por juego, agregar uno no toca el catálogo |
| T-84 | Arduino se practica con "¿Qué hace este código?": se lee un programa y se elige entre cuatro descripciones de lo que pasa en la placa, con el mismo componente de "¿Qué imprime?". No se agrega un simulador de Arduino. Las respuestas se comprueban compilando cada programa con funciones de Arduino simuladas. Los ciclos del DFD son una figura nueva del mismo dibujo (rombo con flecha de regreso) y los niveles de textos usan posiciones desde 1 | Carlos pidió algo práctico para Arduino, el tema más flojo, sin nota y sin servidor como los demás juegos. Un simulador sería una dependencia grande y no aprobada. Las posiciones desde 1 dan el mismo resultado en LogiCode y en PSeInt, sin depender de la base que use cada curso |

## Decisiones aprobadas el 27 de octubre de 2026 (P-07 y P-08)

Carlos aprobó agregar Apache POI y Apache PDFBox al terminar la Etapa 10 ("Hacer las dos cosas").

| # | Decisión | Motivo |
| --- | --- | --- |
| P-07 | Apache POI 5.5.1 (`poi-ooxml`) para importar la lista de estudiantes desde `.xlsx` y exportar la planilla a `.xlsx`. El CSV sigue en los dos sentidos | Excel es lo que usan los docentes; antes había que guardar como CSV. Se eligió 5.5.1 y no 6.0.0 porque 6.0.0 salió el 19 de octubre de 2026 y aún no tiene uso en producción |
| P-08 | Apache PDFBox 3.0.8 para que la IA lea entregas en PDF | Muchos estudiantes exportan su trabajo a PDF. Word y PowerPoint siguen con el docente: POI los abriría, pero la IA necesitaría además dibujar sus páginas, y no se aprobó |

## Decisiones técnicas de Excel y PDF

| # | Decisión | Motivo |
| --- | --- | --- |
| T-75 | Importación: se lee la primera hoja, cédula en la columna A y nombre en la B, con las mismas reglas del CSV (encabezado opcional, 2000 filas, 1 MB, repetidas una vez). Una cédula escrita como número se lee sin decimales ni notación científica. El formato se elige por la extensión (`.csv` o `.xlsx`; `.xls` antiguo no) | Que la misma lista sirva en los dos formatos. Una parte del `.xlsx` descomprimida de más de 20 MB se rechaza para que un archivo armado para inflarse no agote la memoria |
| T-76 | Exportación `.xlsx` en `/gradebook/export/xlsx`, con las mismas filas del CSV; notas y porcentajes como números (notas con formato `0.0`; porcentajes en la fila "Porcentaje (%)"), textos como texto, y encabezado fijo al desplazarse | En Excel se puede ordenar y promediar sin convertir. Una celda de texto no se ejecuta como fórmula, así que no hace falta el apóstrofo del CSV (T-64) |
| T-77 | La IA recibe de cada PDF su texto (si lo tiene) y cada página como imagen (110 ppp, lado mayor hasta 2000 px; luego se reduce como una foto). Un PDF de más de 5 páginas, de más de 25 MB, con contraseña o dañado manda la entrega completa al docente ("Formato que la IA no lee"), y también una entrega con más de 10 imágenes en total. Reemplaza la parte de PDF de T-41 | El texto evita errores de lectura en un PDF exportado; la imagen cubre un escaneo o una foto convertida a PDF. Los límites mantienen el tiempo de CPU parecido al de unas pocas fotos |

## Decisiones aprobadas el 1 de noviembre de 2026 (P-05 y P-06)

| # | Decisión | Motivo |
| --- | --- | --- |
| P-05 | Los archivos cargados se guardan en MinIO, en Docker Compose, en lugar del disco local | Lo pide la especificación (capa 7: almacenamiento de objetos compatible con S3, MinIO en local) y prepara la publicación en línea (P-09) |
| P-06 | El remitente de los correos de Brevo es `logicode.notificaciones@gmail.com` | Lo eligió Carlos; es una cuenta propia del proyecto |

## Decisiones técnicas de MinIO y Brevo

| # | Decisión | Motivo |
| --- | --- | --- |
| T-85 | P-05: los archivos se guardan en MinIO (servicio `minio`, volumen `minio-data`, sin puerto publicado) con la misma clave aleatoria de T-22. La carga se recibe primero en `file-storage` para medirla, calcular su SHA-256 y revisar el formato; luego se sube y MinIO rechaza la carga si el SHA-256 no coincide. La vista previa y la descarga siguen pasando por el backend, que revisa los permisos; los `Range` de los videos piden a MinIO solo desde esa posición. Al arrancar, el backend sube a MinIO los archivos que había en `file-storage` y borra cada copia local después de comprobar su tamaño en MinIO; mientras tanto los entrega desde la carpeta. El cliente S3 es propio (`S3ObjectClient`, firma AWS V4 comparada con la de botocore en `S3ObjectClientTest`). La imagen es `pgsty/minio`, el fork comunitario de MinIO | Carlos decidió MinIO el 1 de noviembre. LogiCode usa cinco operaciones de S3; el SDK de MinIO traería unas diez bibliotecas (OkHttp, Kotlin, Guava, Bouncy Castle, Jackson 2) para eso. La imagen oficial `minio/minio` ya no está en Docker Hub (septiembre de 2026) y el repositorio de MinIO está archivado; el fork es el mismo programa con parches. Por ser S3 estándar, cambiar a otro servidor compatible es cambiar la imagen en `docker-compose.yml` |
| T-86 | P-06: el remitente de Brevo es `logicode.notificaciones@gmail.com`, en `BREVO_SENDER_EMAIL` de `.env` (antes, el secreto `brevo_sender_email`, que si existe sigue teniendo prioridad). La API key sigue siendo secreto de Docker | Carlos lo decidió el 1 de noviembre. El remitente no es secreto. Brevo no puede autenticar un dominio gratuito como gmail.com, así que puede mostrar el correo con un dominio de Brevo o mandarlo a spam; se comprueba con un envío real antes del piloto, y un dominio propio lo resolvería |

## Decisiones técnicas del juego Prueba de escritorio (2 de noviembre de 2026)

| # | Decisión | Motivo |
| --- | --- | --- |
| T-87 | "Prueba de escritorio" es un juego más de `/juegos`, solo en el navegador. Cada fila es una línea o una vuelta del ciclo con el valor de cada variable después de ella (Verdadero o Falso en la columna de una condición y lo que sale en Pantalla); las casillas que no aplican muestran "—" y la primera fila viene llena. La respuesta se compara sin mayúsculas, tildes, comillas ni espacios de más, 3,5 vale como 3.5 y V/F como Verdadero/Falso | Carlos pidió uno de los dos juegos opcionales (2 de noviembre) y se eligió este; el traductor DFD↔PSeInt sigue pendiente. Poner el valor completo en cada fila evita la duda de si una casilla vacía significa "no cambió" o "no sé" |

## Pendiente de decidir

| # | Pregunta | Necesaria antes de | Propuesta |
| --- | --- | --- | --- |
| P-09 | Proveedor de alojamiento y de almacenamiento de objetos para publicar en línea | Publicación en línea (fuera del piloto) | En observación según la especificación. El despliegue seguirá siendo con Docker y los mismos archivos de Compose; los archivos irían al almacenamiento S3 del proveedor con el mismo adaptador de MinIO (T-85) |

## Observaciones de la especificación (no bloquean la base)

1. Sección 6, párrafo inicial: dice "solo las fotos ilegibles solicitarán confirmación docente", mientras RN-09, RN-11 y el flujo indican que también la confianza menor a 80 %. Se aplica la regla de RN-09/RN-11.
2. Tabla de prioridades: P1 y P2 repiten quizzes, PSeInt, juegos de bloques y foro. No afecta las Etapas 1 y 2.
3. Tabla de etapas: la fila de encabezado repite el texto de la Etapa 0 (error de formato).
4. El Word `LogiCode - Requerimientos y reglas de negocio.docx` no está en la carpeta del proyecto; se trabajó con el `.md`.

## Decisiones técnicas del borrado de archivos vencidos

| # | Decisión | Motivo |
| --- | --- | --- |
| T-88 | Un proceso diario (3:30 a. m., hora de Colombia) borra los archivos con `retain_until` vencida que ningún módulo usa. Cada módulo que guarda ids de archivos implementa `FileUsage` (`content`: material y tema sin archivar; `activities`: toda entrega). El registro queda con `deleted_at` y el contenido se borra después de confirmar en la base | Carlos eligió el 2 de noviembre borrar solo lo archivado o sin uso: las entregas son la evidencia de la nota y los materiales activos los sigue usando el docente. RN-21 solo exige no borrar antes de seis meses. El puerto evita que `files` consulte tablas de otros módulos. Corre después del respaldo diario, así que lo borrado se puede recuperar de las copias durante `BACKUP_RETENTION_DAYS` días |

## Calibración de la IA (4 de noviembre de 2026)

| # | Decisión | Motivo |
| --- | --- | --- |
| T-90 | Cada curso empieza "en calibración" (tabla `ai_calibration`; sin fila = en calibración). Mientras lo esté, una propuesta de la IA que cumple la regla del 80 % pasa a Por revisar con motivo `CALIBRATION` en lugar de publicarse. La regla `AiGradingPolicy` no cambia | Carlos eligió el 4 de noviembre "Calibrar la IA antes de confiar": el ensayo del 1 de noviembre mostró que la confianza no está calibrada (D-g). Así se conserva la regla aprobada y se impide que publique notas injustas mientras no haya evidencia |
| T-91 | La concordancia se calcula al consultar: última evaluación no fallida de cada nota publicada por el docente; coincide si la diferencia es ≤ 0,5. No se guarda | Evita un contador que se desincronice con las notas corregidas; con pocos cursos y entregas la consulta es barata. Si crece, se guarda por curso |
| T-92 | Cerrar la calibración lo hace solo el docente del curso y exige un mínimo de notas comparadas (`LOGICODE_AI_CALIBRATION_MIN`, 10 por defecto). Se puede volver a calibrar en cualquier momento | El mínimo de 10 sale del plan de Carlos del 2 de noviembre de medir con 10 a 20 fotos reales; pendiente que lo confirme |
| T-93 | La verificación de salud `readiness` (la que usa Docker) incluye la base de datos: `management.endpoint.health.group.readiness.include: readinessState,db` | En el simulacro del 4 de noviembre se borró la base y el backend siguió "healthy" 7 min 28 s hasta que alguien lo miró. Con el cambio, readiness falla a los 5 s y Docker lo marca `unhealthy` a los 92 s. Avisar a una persona sigue pendiente |
| T-94 | Cerrar o reabrir la calibración bloquea la fila del curso (`INSERT … ON CONFLICT DO NOTHING` y luego `SELECT … FOR UPDATE`), así que dos cambios a la vez se atienden uno tras otro: uno responde 200 y el otro 409 | En la prueba de concurrencia de la II-04 (4 de noviembre), dos cierres simultáneos dieron 500 en 10 de 20 intentos y doble 200 en 4. Después del cambio: 20 de 20 con 200 y 409 |
| T-95 | Nginx resuelve el nombre `backend` con el DNS de Docker cada 10 s (`resolver 127.0.0.11`) en lugar de una sola vez al arrancar | Al recrear solo el contenedor del backend, la interfaz respondía 502 hasta reiniciar Nginx (II-04, 4 de noviembre) |
| T-96 | `respaldo.sh restaurar` carga la copia en una base aparte (`<base>_restaurada`) y después cambia los nombres en una sola transacción; la base reemplazada queda como `<base>_anterior` | En el ensayo de la II-05 (4 de noviembre), restaurar la copia de antes de la V10 sobre una base ya migrada fallaba con `--clean`, porque la copia no conoce la tabla nueva. La vuelta atrás a la 1.0.0 era imposible con la herramienta. Corregido y probado en los dos sentidos: copia de esquema 9 sobre 10 y de 10 sobre 10 |

## Versión 1.2.0: guías del segundo corte de Ingeniería de Software II (6 de noviembre de 2026)

Carlos eligió el 6 de noviembre "Todo local, sin subir a GitHub": los workflows quedan en el repositorio, pero se ejecutan con scripts locales con las mismas etapas y puertas; Prometheus y Grafana van en un perfil de Compose apagado por defecto; todo se entrega por bundle.

| # | Decisión | Motivo |
| --- | --- | --- |
| T-97 | La línea de cada solicitud en el log cambia todo segmento de 5 a 12 dígitos por `{cedula}` (`LogPaths.sanitize`). Hecho con TDD (rojo `0e51160`, verde `6823ec7`, refactor `a20cf77`) | El log de la 1.1.2 guardaba la cédula en `DELETE …/enrollments/{cedula}` (Ley 1581). Un id de LogiCode es UUID, así que un número largo en la ruta solo puede ser una cédula |
| T-98 | La imagen de la interfaz usa `nginx:1.30-alpine` y corre como el usuario `nginx` en los puertos 8080 y 8443; la red `datos` une solo base, MinIO, backend y respaldos | Trivy: 51 CVE altos o críticos en la rama 1.29 y 0 en la 1.30; correr como root era el único hallazgo alto de configuración. Con la red aparte, la interfaz no puede llegar a la base aunque alguien la comprometa |
| T-99 | Telemetría con Micrometer: métricas Prometheus siempre expuestas dentro de Docker, trazas OpenTelemetry a Jaeger solo con `LOGICODE_TRACING_EXPORT=true` y muestreo de 1 de cada 10; logs ECS con `traceId`. Perfil `observabilidad` (Prometheus 3.7.3, Grafana 12.3.1 solo lectura, Jaeger 2.12.0) en `127.0.0.1`. Rotación de logs de 5 × 10 MB por servicio (ADR 13) | Herramientas libres que corren en el mismo equipo (decisión de Carlos). El muestreo no cambió el p95 a 39 solicitudes por segundo; se elige por espacio. Los logs sin límite podían llenar el disco |
| T-100 | SLI: solicitudes sin 5xx y en menos de 1 s, sobre todo menos `/actuator`. SLO 99 % en 30 días (7,2 h de presupuesto). Alertas de quema rápida, base sin conexión y backend caído, probadas con `promtool`. Runbook y postmortem en [observabilidad.md](observabilidad.md) | Un solo equipo sin guardia nocturna no sostiene 99,9 % (43 min al mes). El simulacro del 6 de noviembre mostró que el SLI filtrado por `/api/v1` no veía la base caída |
| T-101 | Prueba de carga con k6 del endpoint crítico (`GET /courses/{id}/activities`): hasta 50 usuarios virtuales, umbrales p95 < 500 ms y errores < 1 %, solo contra `localhost` | 50 usuarios son más que un curso entero a la vez; apuntar a otro servidor exige escribirlo a propósito |
| T-102 | CI local: `ci/ci-local.mjs` corre lint, backend, frontend y seguridad en imágenes de Docker fijas; `ci.yml` y `ci-security.yml` solo lo llaman. Puerta de cobertura con JaCoCo: 85 % de líneas y 68 % de ramas | La misma CI en el PC y en GitHub. El PC no tiene Java: con Docker no hace falta instalar nada. El umbral queda 3 puntos bajo lo medido (88,6 % y 71,2 %) |
| T-103 | Semgrep falla con hallazgos ERROR; Trivy falla con HIGH o CRITICAL. Las excepciones van en `.trivyignore.yaml`, cada una con motivo y fecha de vencimiento. Las dependencias Java se revisan en el jar de la imagen, no en `pom.xml` | Que la puerta bloquee de verdad, y que una excepción no se quede para siempre. Trivy lee el `pom.xml` sin el BOM de Spring Boot: dio 4 falsos positivos en un día; en el jar ve las versiones reales |
| T-104 | Despliegue: `ci/desplegar.mjs` (respaldo, despliegue, verificación de `/api/v1/system/info`, vuelta atrás que restaura la base si ya se migró). Entrega continua: una persona lo inicia; `deploy.yml` lo hace con aprobación manual y un runner propio | Las notas son datos sensibles: un error que llegue solo al piloto cuesta más que esperar a que alguien apruebe. Ensayado con una migración rota y con una interfaz rota después de migrar |
| T-105 | El contrato OpenAPI declara la seguridad real (cookie `LOGICODE_SESSION` y cabecera `X-XSRF-TOKEN`), no JWT, y los errores 400/401/403/404 con Problem Details. Se guarda en `docs/api/openapi.json` | La guía 6 propone JWT; LogiCode usa sesión en el servidor desde la Etapa 3 para poder cerrar todas las sesiones de una persona (decisión de Carlos del 19 de octubre). Documentar JWT sería documentar algo que no existe |
| T-107 | Con imágenes, la IA califica en dos pasos: primero lee las imágenes sin ver enunciado, respuesta esperada ni rúbrica; después califica esa transcripción, sin imágenes | En las pruebas sintéticas del 6 y 7 de noviembre, en 3 fotos ilegibles (S09, S10 y L26) el modelo devolvió como "lectura" la respuesta esperada, y publicó 5,0 a código malo borroso. Carlos eligió el 7 de noviembre "leer primero, calificar después" frente a mandar al docente las coincidencias exactas o dejarlo solo con la calibración. Cuesta una llamada más al modelo, pero el segundo paso es solo texto. Desde la 1.3.1, si la lectura se desborda (el modelo se repite hasta cortarse), la entrega cuenta como ilegible y va al docente sin reintentos |
| T-108 | Pestaña **Emulador** con un emulador libre por herramienta. PSeInt usa el intérprete del servidor; DFD y Scratch se arman como un árbol de pasos que se traduce a PSeInt y corre en ese mismo intérprete; Arduino es un simulador propio en el navegador de un subconjunto de C++ con placa virtual. Nada se guarda en el servidor: borrador en el navegador y archivo descargado | Carlos lo pidió el 10 de noviembre ("como Visual Studio Code", sin rúbrica, que diga si compila). Traducir a PSeInt evita un intérprete más y da los mismos errores. Compilar Arduino de verdad (avr-gcc y un simulador en Docker) es tecnología no aprobada y mucho más pesada; el Scratch real exigiría traer Scratch entero como dependencia |
| T-109 | La calibración de la IA pasa a ser opcional: un curso sin fila en `ai_calibration` **no** está en calibración y la IA publica sola con la regla del 80 % (RN-09, RN-11). El docente la enciende con **Calibrar la IA** (`POST …/ai-calibration/reopen`) y la cierra como antes. Las filas ya abiertas siguen en calibración. Reemplaza el "cada curso empieza en calibración" de T-90 | Carlos lo pidió el 10 de noviembre: "que la IA califique todo y publique la nota a menos que esté muy indecisa". Riesgo aceptado: en el ensayo del 1 de noviembre la confianza no separó aciertos de errores (80 en dos notas mal puestas), así que una nota injusta con 80 % o más puede publicarse; el docente la corrige y queda en el historial |
| T-110 | El umbral de publicación automática de la IA sube de 80 % a 90 % (`AiGradingPolicy.AUTO_PUBLISH_CONFIDENCE`). Se agregan el botón **Calificar todo con IA** (`POST /courses/{id}/ai-grading`: devuelve a la cola de IA el último intento de cada estudiante en Por revisar que la IA puede leer) y la sección **Control** del curso (cifras por actividad de la planilla) | Carlos lo pidió el 10 de noviembre, "por si acaso", junto con T-109. Cambia la cifra aprobada de RN-09/RN-11 (80 %) por decisión de Carlos; el resto de la regla (contenido legible, fallo técnico sin nota) no cambia |
| T-106 | Pruebas de contrato sin Pact: el backend compara lo que publica con `docs/api/openapi.json` y la interfaz comprueba que cada llamada suya está en ese archivo | Un solo consumidor y un solo proveedor en el mismo repositorio; Pact pediría un servidor de contratos (broker) para lo mismo |
