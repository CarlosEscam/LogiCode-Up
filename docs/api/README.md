# API REST de LogiCode

## Convenciones

| Tema | Convención |
| --- | --- |
| Prefijo | Todas las rutas de negocio empiezan por `/api/v1`. Un cambio incompatible crea `/api/v2` |
| Formato | JSON UTF-8; propiedades en `camelCase`; recursos en plural y en inglés (`/activities`, `/submissions`) |
| Fechas | ISO-8601 con desfase, p. ej. `2026-12-10T23:59:00-05:00` |
| Notas | Número con un decimal entre `0.0` y `5.0` (la interfaz lo muestra como `4,5`) |
| Errores | [Problem Details, RFC 9457](https://www.rfc-editor.org/rfc/rfc9457): `type`, `title`, `status`, `detail`, `instance`; errores de validación agregan la lista de campos. Mensajes en español y sin detalles internos |
| Códigos | `200` consulta, `201` creación (con cabecera `Location`), `202` solicitud aceptada (correos), `204` sin contenido, `400` formato inválido, `401` sin sesión, `403` sin permiso, `404` no existe, `409` conflicto de estado, `422` regla de negocio incumplida, `429` límite de solicitudes |
| Paginación | `?page=0&size=20&sort=campo,asc`; respuesta `{ "items": [], "page": 0, "size": 20, "totalItems": 0, "totalPages": 0 }` |
| Trazabilidad | Cada respuesta incluye `X-Request-Id` |
| Documentación | El contrato vivo lo genera el backend: `/api/v1/openapi` y `/swagger-ui.html` |

## Catálogo de endpoints

### Implementados (Etapa 2)

| Método y ruta | Descripción |
| --- | --- |
| `GET /api/v1/system/info` | Nombre, versión, entorno, hora del negocio, zona horaria y versión del esquema |
| `GET /api/v1/openapi` | Contrato OpenAPI en JSON |
| `GET /swagger-ui.html` | Interfaz Swagger para explorar la API |
| `GET /actuator/health` | Estado general (incluye base de datos); también `/liveness` y `/readiness` |

Ejemplo:

```http
GET /api/v1/system/info
```

```json
{
  "application": "LogiCode",
  "version": "0.1.0-SNAPSHOT",
  "environment": "docker",
  "serverTime": "2026-10-18T20:33:30.763-05:00",
  "timeZone": "America/Bogota",
  "schemaVersion": "2"
}
```

### Implementados (Etapa 3 · Acceso y permisos)

Sesión: cookie `LOGICODE_SESSION` (HttpOnly, SameSite=Lax). Toda petición `POST`/`PATCH` lleva la cabecera `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`; si aún no existe, se obtiene con `GET /api/v1/auth/csrf`. Sin token válido la respuesta es `403`.

| Método y ruta | Quién | Respuesta | Requisito |
| --- | --- | --- | --- |
| `GET /api/v1/auth/csrf` | Cualquiera | `200` y cookie `XSRF-TOKEN` | Protección CSRF (D-01) |
| `POST /api/v1/auth/student-registrations` | Visitante con cédula habilitada | `201` | RF-01, RN-01 |
| `POST /api/v1/auth/teacher-requests` | Visitante | `201`, cuenta `PENDING_APPROVAL` | RF-02, RN-02 |
| `POST /api/v1/auth/login` | Visitante | `200` con la cuenta y sus permisos; `401` datos incorrectos; `403` cuenta pendiente o desactivada; `429` tras 5 fallos en 15 min | RF-03 |
| `POST /api/v1/auth/logout` | Cualquiera | `204` | RF-03 |
| `POST /api/v1/auth/email-verifications/confirm` | Dueño del enlace | `200` (`passwordSetupRequired` indica si falta crear contraseña) | RF-29, RN-24 |
| `POST /api/v1/auth/password-recovery` | Cualquiera | `202` con mensaje genérico, exista o no la cuenta | RF-04, RN-23 |
| `POST /api/v1/auth/password-reset` | Dueño del enlace | `200`; cierra todas las sesiones | RF-04, RN-23 |
| `POST /api/v1/auth/email-change-requests` | Cualquiera | `202` con mensaje genérico | RF-30, RN-25 |
| `GET /api/v1/me` | Con sesión | `200` cuenta y permisos; `401` sin sesión | |
| `POST /api/v1/me/password` | Con sesión | `200`; cierra todas las sesiones, incluida la actual | RNF-02 |
| `POST /api/v1/me/email-verification` | Con sesión y correo sin verificar | `202` | RF-29 |
| `GET /api/v1/admin/users?q=&role=&status=&page=&size=` | `MANAGE_USERS` o `APPROVE_TEACHERS` | Página de usuarios; `q` busca por cédula, nombre o correo | RF-28 |
| `POST /api/v1/admin/teachers/{id}/approve` | `APPROVE_TEACHERS` | `200` | RF-02 |
| `PATCH /api/v1/admin/users/{id}/role` | `CHANGE_ROLES` (solo cuentas iniciales) | `200`; cierra las sesiones de esa cuenta | RF-28, RN-04, RN-16 |
| `PATCH /api/v1/admin/users/{id}/status` | `MANAGE_USERS` | `200`; `422` si dejaría la plataforma sin cuenta inicial activa | RN-16 |
| `GET /api/v1/admin/audit-events?page=&size=` | `VIEW_AUDIT` | Página de eventos, del más reciente al más antiguo | Auditoría |
| `GET /api/v1/student-enablements?page=&size=` | `MANAGE_STUDENT_ENABLEMENTS` | Cédulas habilitadas | RF-05 |
| `POST /api/v1/student-enablements` | `MANAGE_STUDENT_ENABLEMENTS` | `201` | RF-05, RN-01 |
| `POST /api/v1/student-enablements/import` | `MANAGE_STUDENT_ENABLEMENTS` | `200` con habilitadas, repetidas y filas con error. Multipart, campo `file`, `.csv` de hasta 1 MB y 2000 filas | RF-05 |

Ejemplo de inicio de sesión:

```http
POST /api/v1/auth/login
X-XSRF-TOKEN: <valor de la cookie XSRF-TOKEN>
Content-Type: application/json

{ "cedula": "1234567", "password": "********" }
```

### Implementados (Etapa 4 · Curso, temas y biblioteca)

Todas requieren sesión salvo las marcadas como públicas. Lo que la cuenta no puede ver responde `404`.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/courses` | Cursos de la cuenta (todos para un Administrador) |
| `POST /api/v1/courses` | Crear curso `{name, period, description}`; quien lo crea es el docente responsable |
| `GET/PUT /api/v1/courses/{id}` | Consultar o editar el curso |
| `GET/POST /api/v1/courses/{id}/enrollments` | Inscritos (paginado) e inscripción `{cedula, fullName}` |
| `POST /api/v1/courses/{id}/enrollments/import` | Inscripción por CSV (`file`, máx. 1 MB y 2000 filas) |
| `DELETE /api/v1/courses/{id}/enrollments/{cedula}` | Retirar del curso |
| `GET/POST /api/v1/courses/{id}/topics` | Temas con sus materiales · crear tema `{title, description, tool}` |
| `PUT /api/v1/courses/{id}/topics/order` | Nuevo orden `{ids: [...]}` (todos los temas) |
| `PUT /api/v1/courses/{id}/next-class` | Tema de la próxima clase `{topicId}` (`null` quita la marca) |
| `PUT/DELETE /api/v1/topics/{id}` | Editar o archivar un tema |
| `POST /api/v1/topics/{id}/materials` | Material en `multipart/form-data`: `title`, `description`, `kind`, `visibility` y `file`, o `url` con `kind=LINK` |
| `PUT /api/v1/topics/{id}/materials/order` | Nuevo orden de los materiales |
| `GET /api/v1/materials/{id}` | Detalle (**público** si el material es público) |
| `GET /api/v1/materials/{id}/preview` | Archivo en línea: PDF, PNG, JPG, MP4, MOV; admite `Range` (**público** si el material es público) |
| `GET /api/v1/materials/{id}/download` | Descarga con el nombre original (**público** si el material es público) |
| `PUT /api/v1/materials/{id}` | Editar título, descripción y visibilidad (docente del curso) |
| `PUT /api/v1/materials/{id}/visibility` | Público o privado (docente del curso o Administrador) |
| `DELETE /api/v1/materials/{id}` | Archivar (docente del curso o Administrador) |
| `GET /api/v1/library` | **Público.** Búsqueda paginada: `q`, `tool`, `topicId`, `kind`, `page`, `size` |
| `GET /api/v1/library/topics` | **Público.** Temas con materiales visibles, para los filtros |

### Implementados (Etapa 5 · Actividades y entregas)

Todas requieren sesión. Lo que la cuenta no puede ver responde `404`; lo que ve pero no puede hacer, `403`. Fechas en ISO 8601 (UTC).

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/courses/{id}/activities` | Actividades por fecha de cierre; borradores solo para el docente. Para un estudiante inscrito incluye `progress` (estado `NOT_OPEN`, `OPEN`, `SUBMITTED` u `OVERDUE`, intentos, plazo efectivo) |
| `POST /api/v1/courses/{id}/activities` | Crear borrador `{title, kind, statement, acceptsText, acceptsFiles, acceptsPhotos, fileGroups, weightPercent, opensAt, closesAt, maxAttempts}`; `422` si los porcentajes del curso pasan de 100 % |
| `GET/PUT/DELETE /api/v1/activities/{id}` | Consultar, editar (cambiar el enunciado crea versión de rúbrica) o archivar |
| `POST /api/v1/activities/{id}/publish` | Publicar; `422` sin rúbrica |
| `GET/PUT /api/v1/activities/{id}/rubric` | Rúbrica vigente (`204` si no hay; el estudiante no recibe `expectedAnswer`) · definir `{expectedAnswer, criteria: [{name, description, maxPoints}]}` crea una versión |
| `GET /api/v1/activities/{id}/rubric/versions` | Todas las versiones (docente y Administradores) |
| `POST /api/v1/activities/{id}/submissions` | Entregar en `multipart/form-data`: `text` y hasta 5 `files`. `201` con el intento; `422` si está cerrada, sin intentos o vacía. Desde la Etapa 6 sigue respondiendo `201`; la calificación con IA se consulta en `GET /submissions/{id}/grade` (T-46) |
| `GET /api/v1/activities/{id}/submissions/mine` | Mis intentos |
| `GET /api/v1/activities/{id}/submissions` | Inscritos con su situación, último intento y excepción (docente y Administradores) |
| `PUT/DELETE /api/v1/activities/{id}/exceptions/{studentId}` | Excepción `{extendedUntil, extraAttempts, reason}` o quitarla (docente del curso) |
| `GET /api/v1/activities/{id}/grades` | Todos los intentos con su nota (docente y Administradores) |
| `GET /api/v1/submissions/{id}` | Detalle de la entrega (autor, docente, Administradores) |
| `GET /api/v1/submissions/{id}/files/{fileId}/preview` · `/download` | Archivo de la entrega en línea o descarga |
| `GET /api/v1/submissions/{id}/grade` | Nota con la versión de rúbrica usada; historial solo para docente y Administradores; el estudiante ve nota y retroalimentación solo publicadas |
| `POST /api/v1/submissions/{id}/grade` | Calificar o corregir `{score, feedback}` (0,0 a 5,0, un decimal; docente del curso) |
| `GET /api/v1/courses/{id}/review-queue` | Bandeja Por revisar: último intento de cada estudiante sin nota publicada, con `status` y `reviewReason` (desde la Etapa 6) |
| `GET /api/v1/courses/{id}/grades/mine` | Mis notas en el curso (último intento de cada actividad) |

### Implementados (Etapa 6 · Calificación con IA)

No hay rutas nuevas para consultar: la IA trabaja sobre la nota de la entrega.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/submissions/{id}/grade` | Ahora el `status` puede ser `PROCESSING` (la IA evalúa), `PENDING_REVIEW` o `PUBLISHED`. Para docente y Administradores incluye `reviewReason` (`AI_DISABLED`, `UNSUPPORTED`, `ILLEGIBLE`, `LOW_CONFIDENCE`, `AI_FAILED`), `aiEvaluations` (más reciente primero: `model`, `outcome`, `legible`, `confidence`, `proposedScore`, `criteria[{name, points, maxPoints, comment}]`, `feedback`, `transcription`, `error`, `durationMs`, `createdAt`) y `canRetryWithAi`. El estudiante no recibe motivo ni evaluaciones (T-47) |
| `POST /api/v1/submissions/{id}/grade/ai-retry` | Volver a enviar a la IA una entrega sin nota publicada (docente del curso). `200` con el detalle y la nota en `PROCESSING`; `409` si ya está publicada o en proceso; `422` si la IA está apagada o el formato no lo lee |
| `POST /api/v1/submissions/{id}/grade` | Sin cambios: confirmar la propuesta de la IA es calificar con `{score, feedback}`; también funciona mientras la IA trabaja (T-43) |

### Implementados (Etapa 7 · Quizzes, PSeInt y juegos)

Todas requieren sesión. Los juegos no usan la API (T-59).

| Método y ruta | Uso |
| --- | --- |
| `POST /api/v1/pseint/runs` | Ejecutar un programa `{code, input}` (hasta 20 000 y 10 000 caracteres) con los límites de T-50. Responde `{output, completed, errorKind, error, steps, durationMs}`; `errorKind` es `SYNTAX`, `UNSUPPORTED`, `RUNTIME` o `LIMIT`. `429` si el intérprete está ocupado |
| `PUT /api/v1/activities/{id}/rubric` | Acepta además `testCases: [{input, expectedOutput}]` (hasta 20). La respuesta trae `testCases` (solo para docente y Administradores) y `testCaseCount`. En un quiz responde `422`: su rúbrica sale de las preguntas |
| `PUT /api/v1/activities/{id}/rubric` con `blockChallenge` | Reto de bloques (RF-12): `{grid: ["S..#", …], facing: "N"\|"E"\|"S"\|"O", maxBlocks: 1-40, allowed: ["avanzar", "izquierda", "derecha", "repetir", "si", "mientras"], start: [bloques]}`. Cada bloque es `{type, times?, body?, else?}`. `422` si el tablero no es válido, si la actividad no recibe texto o si también trae `testCases`. La rúbrica lo devuelve a todos (es el enunciado). La entrega es el programa en `text`, como JSON, y se califica sola: `5.0` si llega a la meta y `0.0` si no (T-74) |
| `POST /api/v1/submissions/{id}/test-run` | Volver a correr los casos con el programa entregado (docente y Administradores): `{cases: [{number, input, expectedOutput, output, passed, error}], programError, passed, total, score}` |
| `GET /api/v1/submissions/{id}/grade` | `source` puede ser `AUTOMATIC` (casos de prueba, reto de bloques o quiz) y `reviewReason` puede ser `CODE_ERROR`; trae `canRunTests` |
| `POST /api/v1/courses/{id}/activities` | `kind` acepta `QUIZ` y `PRACTICE_QUIZ`: sin formas de entrega; el de repaso sin `weightPercent`. `progress.state` puede ser `CLOSED` (repaso cerrado). Un quiz no recibe entregas por `POST /activities/{id}/submissions` (`422`) |
| `GET/POST /api/v1/courses/{id}/questions` | Banco de preguntas con su clave (ver: docente y Administradores; crear: docente del curso). Pregunta `{kind: SINGLE\|MULTIPLE\|TRUE_FALSE\|SHORT, prompt, points (1 a 10), explanation, options: [{text, correct}], acceptedAnswers}`; cada una trae `locked` |
| `PUT/DELETE /api/v1/questions/{id}` | Editar o archivar (docente del curso); `409` si ya se respondió o si un quiz la usa (T-55) |
| `GET/PUT /api/v1/activities/{id}/quiz` | Tiempo y preguntas del quiz (`204` si no tiene; el estudiante recibe cuántas son, no cuáles) · armarlo `{timeLimitMinutes (1 a 300), questionIds}` en orden; `409` si ya tiene intentos |
| `POST /api/v1/activities/{id}/quiz/attempts` | Empezar un intento o continuar el que está en curso (estudiante inscrito); `422` fuera de fechas o sin intentos |
| `GET /api/v1/activities/{id}/quiz/attempts/mine` | Mis intentos, sin preguntas |
| `GET /api/v1/activities/{id}/quiz/ranking` | `[{position, studentName, score, correctCount, questionCount, durationSeconds, mine, attemptId}]`; `attemptId` solo para docente y Administradores |
| `GET /api/v1/quiz-attempts/{id}` | El intento con sus preguntas (autor, docente, Administradores). `correct`, `correctChoices`, `acceptedAnswers` y `explanation` solo con `reviewAvailable` (T-58) |
| `PUT /api/v1/quiz-attempts/{id}/answers` | Guardar `{answers: [{questionId, choices, text}]}` (`choices`: posiciones desde 0); `204`; `409` si ya se envió o se acabó el tiempo |
| `POST /api/v1/quiz-attempts/{id}/finish` | Enviar (con las últimas respuestas opcionales) y recibir el resultado |

### Implementados (Etapa 8 · Foro y operación docente)

Leer el foro no pide sesión; lo demás sí.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/v1/forum/threads?category&q&page&size` | Hilos visibles, fijados primero y luego por actividad reciente (`size` hasta 50). `category`: `PSEINT`, `DFD`, `SCRATCH`, `ARDUINO` o `GENERAL`; `q` busca en título y texto. Cada hilo: `{id, category, title, authorName, pinned, closed, replyCount, createdAt, lastActivityAt}` |
| `GET /api/v1/forum/threads/{id}` | El hilo con sus respuestas visibles, `canReply` y `canModerate`. `404` si no existe o fue eliminado |
| `POST /api/v1/forum/threads` | Abrir un hilo `{category, title (3 a 150), body}` (`PARTICIPATE_FORUM`); `201` |
| `POST /api/v1/forum/threads/{id}/posts` | Responder `{body}` (hasta 10 000 caracteres); `201`; `409` si el hilo está cerrado |
| `PATCH /api/v1/forum/threads/{id}` | Fijar o cerrar `{pinned, closed}` (`MODERATE_FORUM`; lo que no se envía no cambia) |
| `DELETE /api/v1/forum/threads/{id}` · `DELETE /api/v1/forum/posts/{id}` | Ocultar un hilo o una respuesta (`MODERATE_FORUM`); `204` (T-65) |
| `GET /api/v1/courses/{id}/gradebook` | Planilla (T-61): `columns` con porcentaje explícito, efectivo y estadísticas; `rows` con cada celda (`GRADED`, `OVERDUE`, `IN_REVIEW`, `PENDING`), `partial` y `definitive`; `weightsComplete`, `weightsWarning`, `passingScore` y `stats` del curso. Docente del curso y Administradores la ven completa (`fullView: true`); el estudiante inscrito solo su fila y sin estadísticas |
| `GET /api/v1/courses/{id}/gradebook/export` | La misma planilla en CSV para Excel (T-64), como descarga `planilla-<curso>-<periodo>.csv`. Docente del curso y Administradores |
| `GET /api/v1/admin/overview` | Cifras de la plataforma (`MANAGE_USERS`, T-62) |
| `GET /api/v1/admin/backups` | `{service: {running, dailyHour, retentionDays}, pendingRequest, items: [{name, createdAt, databaseBytes, filesBytes, restoreTest, restoreTestDetail}]}` (`MANAGE_BACKUPS`). `restoreTest`: `OK`, `ERROR` o vacío si no se probó |
| `POST /api/v1/admin/backups` | Pedir un respaldo inmediato; `202`. `409` si el servicio de respaldos no corre o ya hay uno pedido |

### Etapas siguientes (de la especificación, pendientes)

Ninguno por ahora. Falta la página de inicio (RF-23).
