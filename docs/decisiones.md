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
| T-22 | Archivos en un volumen local de Docker (`file-storage`) detrás del puerto `BinaryStorage`, en vez de MinIO | Opción por defecto mientras se responde P-05: no agrega un servicio más al equipo de presentación. Cambiar a MinIO o S3 es escribir otro adaptador; el resto del código no cambia |
| T-23 | Módulo `files` aparte: valida extensión, tamaño y firma real, guarda solo metadatos en `stored_file` y usa claves aleatorias | Las Etapas 5 a 8 (entregas, fotos) reutilizan la misma validación. El nombre original nunca se usa como ruta |
| T-24 | Inscripción por cédula en `course_enrollment` | El docente puede inscribir antes de que el estudiante se registre; al registrarse ya ve el contenido privado |
| T-25 | El primer curso que se crea inscribe todas las cédulas ya habilitadas | Los estudiantes habilitados en la Etapa 3 no pierden acceso al crear el curso |
| T-26 | Temas y materiales se archivan, no se borran | Cumple la retención de seis meses (RNF-12) y permite recuperar contenido. La limpieza de archivos vencidos queda pendiente |
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
| T-41 | La IA lee respuestas escritas, fotos PNG/JPG y archivos `.psc`/`.ino`. Si la entrega trae otro formato, va completa al docente; no se califica a medias | PDF, Word, PowerPoint, video y Scratch necesitan conversión (P-08) |
| T-42 | Fallo técnico: hasta 3 intentos con espera creciente (2 y 4 minutos); después la entrega pasa a Por revisar como "Falló la IA", sin nota. Cada intento queda registrado | RF-15 y RNF-06: reintento controlado y estado visible |
| T-43 | El docente puede calificar en cualquier momento, también mientras la IA trabaja. Si la IA termina después, su resultado se guarda pero no cambia la nota | La revisión humana manda (RN-09) y nunca queda bloqueada por el modelo |
| T-44 | Al modelo se envía el nombre del estudiante, el título, el enunciado y la respuesta esperada de la versión de rúbrica, los criterios y el contenido con etiquetas neutras ("Imagen 1", "Archivo 2"). Nunca cédula, correo ni el nombre original de los archivos. Fotos enderezadas según EXIF y reducidas a 1280 px; temperatura 0; respuesta JSON con esquema; tope de 3072 tokens | RN-08 y RN-18; el nombre de un archivo puede contener la cédula. Resultados reproducibles y sin respuestas sin fin |
| T-45 | La IA viene apagada (`LOGICODE_AI_ENABLED=false`) y Ollama corre como servicio opcional de Docker Compose con el perfil `ia` (imagen `ollama/ollama:0.35.1`, modelo en el volumen `ollama-models`). Apagada, las entregas pasan a Por revisar como "Calificación manual", y los trabajos que hubieran quedado en cola pasan al docente al arrancar | Quien no tenga el modelo descargado puede usar todo lo demás; el modelo pesa unos 6 GB |
| T-46 | La entrega sigue respondiendo `201` con el intento; el estado de la calificación se consulta en `GET /submissions/{id}/grade` (`PROCESSING` mientras la IA trabaja) y la interfaz lo actualiza cada 10 s | La entrega sí queda creada al responder; reemplaza la nota de la Etapa 5 que anunciaba `202` |
| T-47 | El estudiante ve "Calificando con IA" o "En revisión" y su nota cuando se publica; no ve el motivo, la confianza ni el resultado de la IA | La confirmación docente es un trámite interno; evita que el estudiante dispute la propuesta antes de la revisión |
| T-48 | La calificación determinista (RF-16: casos de prueba de PSeInt y claves de quizzes) se implementa en la Etapa 7 | Los casos de prueba, el intérprete aislado y los quizzes se crean en esa etapa; hoy no hay contra qué comparar |

## Pendiente de decidir

| # | Pregunta | Necesaria antes de | Propuesta |
| --- | --- | --- | --- |
| P-05 | Almacenamiento de archivos en local: MinIO (mencionado en la especificación) o volumen de Docker | Publicación en línea | Implementado con volumen local (T-22). Si se prefiere MinIO, se agrega como adaptador sin cambiar el resto |
| P-06 | Dominio y remitente de Brevo | Envío real de correos | Lo define el equipo. Todo está listo: se ponen la API key y el remitente en `secrets/` y se arranca con `docker-compose.brevo.yml` |
| P-08 | Que la IA lea PDF (y, si se quiere, Word y PowerPoint) | Calificar con IA entregas en PDF | Agregar Apache PDFBox (librería Java estándar de Apache) para convertir cada página a imagen y extraer el texto antes de enviarlo al modelo. Word y PowerPoint seguirían con el docente salvo que se apruebe también Apache POI. Mientras tanto van a Por revisar como "Formato que la IA no lee" |
| P-07 | Carga de estudiantes desde Excel (RF-05) | Cierre de la Etapa 3 | Agregar Apache POI (librería Java estándar para leer `.xlsx`) solo para esta importación. Mientras tanto se acepta CSV, que Excel exporta con "Guardar como" |

## Observaciones de la especificación (no bloquean la base)

1. Sección 6, párrafo inicial: dice "solo las fotos ilegibles solicitarán confirmación docente", mientras RN-09, RN-11 y el flujo indican que también la confianza menor a 80 %. Se aplica la regla de RN-09/RN-11.
2. Tabla de prioridades: P1 y P2 repiten quizzes, PSeInt, juegos de bloques y foro. No afecta las Etapas 1 y 2.
3. Tabla de etapas: la fila de encabezado repite el texto de la Etapa 0 (error de formato).
4. El Word `LogiCode - Requerimientos y reglas de negocio.docx` no está en la carpeta del proyecto; se trabajó con el `.md`.
