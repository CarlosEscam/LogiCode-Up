# Estado del proyecto

Última actualización: 20 de octubre de 2026 · Etapa en curso: **cierre de la Etapa 6 (calificación con IA)**

Leyenda: **Implementado** = existe en el código y tiene prueba o verificación. **Pendiente** = no existe todavía. **Decisión aprobada** = definido en la especificación, sin implementar.

## Etapas

| Etapa | Estado | Notas |
| --- | --- | --- |
| 0 · Aprobación de alcance | Aprobada (especificación) | |
| 1 · Análisis y diseño | Diseño técnico mínimo hecho | Pendientes de la etapa completa: casos de uso, historias con criterios de aceptación, maquetas, rúbricas y prueba de lectura manuscrita |
| 2 · Fundaciones técnicas | Implementada | Detalle en la sección de la Etapa 2 |
| 3 · Acceso y permisos | Implementada y aprobada | Falta: envío real con Brevo (P-06) y carga desde Excel (P-07) |
| 4 · Curso, temas y biblioteca | Implementada y aprobada | Almacenamiento local como opción por defecto mientras se decide P-05 |
| 5 · Actividades y entregas | Implementada y aprobada | Calificación manual del docente |
| 6 · Calificación determinista e IA | Implementada, pendiente de revisión y de las pruebas de aceptación en el equipo de presentación | Detalle abajo. La parte determinista (PSeInt y quizzes, RF-16) pasa a la Etapa 7 (T-48) |
| 7 a 10 | Pendientes | Quizzes, PSeInt, juegos, foro, planilla y demás aún no existen |

## Datos de demostración

Implementado: `datos-demo/cargar-datos-demo.mjs` carga por la API datos ficticios de las Etapas 3 a 6 (cuentas de cada rol, curso con temas y materiales, actividades en distintos estados, entregas, notas, historial, excepciones y un taller con fotos, texto, `.psc` y PDF para la IA). Se amplía en cada etapa. Si ya hay datos de etapas anteriores, solo agrega los que faltan. Verificado en Docker sobre una base con los datos de la Etapa 5; volver a ejecutarlo no duplica datos. Detalle en [datos-demo/README.md](../datos-demo/README.md).

## Etapa 6 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Calificación con IA de fotos y archivos compatibles (RF-14, RN-08) | Implementado | Al llegar una entrega se crea la nota en evaluación y un trabajo en la cola (`ai_grading_job`). El worker envía a Ollama el nombre del estudiante, el enunciado, la respuesta esperada, la rúbrica y el contenido (texto, fotos PNG/JPG, archivos `.psc`/`.ino`). Nunca cédula, correo ni nombres de archivo (T-44) |
| Publicación automática con contenido legible y confianza ≥ 80 % (RN-09, RN-11) | Implementado | `AiGradingPolicy`: la nota sale de los puntos por criterio (T-39). Con 80 % o más se publica con su retroalimentación y queda en el historial como "IA" |
| Foto ilegible o confianza < 80 % a confirmación docente (RF-15) | Implementado | La entrega pasa a Por revisar con el motivo y la propuesta de la IA; el docente la confirma o la ajusta |
| Fallo técnico sin nota y con reintento (RF-15, RNF-06, RNF-07) | Implementado | 3 intentos automáticos con espera creciente; después, "Falló la IA" en Por revisar. Botón "Reintentar con IA" y calificación manual siempre disponibles. La entrega nunca se bloquea ni se pierde |
| Trabajo en segundo plano con estado visible (RNF-08) | Implementado | Estado "Calificando con IA" en la bandeja, la entrega y la vista del estudiante; se actualiza solo |
| Resultado de la IA e historial para el docente (RF-17) | Implementado | Lectura del modelo, legibilidad, confianza, puntos y comentario por criterio, retroalimentación propuesta, duración y evaluaciones anteriores. El estudiante no ve nada de esto |
| Scratch y Arduino (RN-10) | Parcial | `.ino` se evalúa como texto. `.sb3`, PDF, Word, PowerPoint y video pasan al docente ("Formato que la IA no lee"); ver P-08 |
| Calificación determinista de PSeInt y quizzes (RF-16) | Pendiente (Etapa 7) | Requiere los casos de prueba y las claves que crea la Etapa 7 (T-48) |
| Pruebas de aceptación con Qwen2.5-VL 7B en el equipo Ryzen 7 5700U | **Pendiente: las ejecuta Carlos** | Arnés listo en [ia-pruebas/](../ia-pruebas/README.md). El modelo no se puede descargar en el entorno de desarrollo de Claude, así que no hay resultados de Qwen todavía; no se inventan |

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
| Lectura manuscrita, coincidencia de notas, confianza, legibilidad, latencia y RAM con Qwen2.5-VL 7B Q4_K_M en el Ryzen 7 5700U | **Pendiente.** `node ia-pruebas/ejecutar-pruebas.mjs ia-pruebas/muestras/muestras.json` en el equipo de presentación; el resumen queda en `ia-pruebas/resultados/` |

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
| Inscripción de estudiantes al curso (RN-03) | Implementado | Por cédula, individual o CSV: `/courses/{id}/enrollments` y `/import`; pestaña Estudiantes. Inscribir también habilita la cédula. El primer curso inscribe las cédulas ya habilitadas (T-25) |
| Temas con herramienta y orden (RF-06) | Implementado | PSeInt, DFD, Scratch, Arduino o General; crear, editar, reordenar, archivar |
| Próxima clase (RF-09) | Implementado | `PUT /courses/{id}/next-class`; un solo tema por curso (índice único) |
| Materiales: guías, documentos, presentaciones, imágenes, videos, archivos de herramienta y enlaces (RF-07) | Implementado | `POST /topics/{id}/materials` (multipart, con avance de carga en la interfaz) |
| Público o privado (RF-08, RN-03, RN-17) | Implementado | Lo privado solo lo ven inscritos, el docente responsable y las cuentas Administrador; lo demás responde `404` |
| Biblioteca con búsqueda y filtros por tema, herramienta y tipo (RF-24) | Implementado | `GET /library`, `GET /library/topics`; pantalla pública `/biblioteca` |
| Previsualización y descarga con permisos (RF-25) | Implementado | `/materials/{id}/preview` (PDF, imágenes y videos; admite `Range` para adelantar videos) y `/download`; pantalla `/materiales/{id}` |
| Formatos, límites y validación del tipo real (RNF-04, RN-21) | Implementado | Lista de formatos aprobada; 500 MB y 1 GB para videos; se revisa la firma del archivo, no solo la extensión |
| Retención mínima de seis meses (RNF-12) | Implementado en datos | Cada archivo guarda `retain_until`; nada se borra, los temas y materiales se archivan. La limpieza automática después de la retención está pendiente |
| Administrador ordinario: publicar biblioteca, sin crear temas ni materiales (RN-04) | Implementado | Puede cambiar visibilidad y archivar; crear o editar responde `403` |
| Almacenamiento de archivos | Implementado (local) | Volumen `file-storage` detrás de un puerto; MinIO queda como alternativa (P-05) |

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
| Habilitación de estudiantes individual y por lista (RF-05) | Implementado para CSV | `POST /student-enablements` y `/import` (CSV hasta 2000 filas); pantalla `/admin/habilitaciones`. **Excel pendiente** (P-07) |
| Roles y autorización (RF-28, RN-04, RN-15, RN-22) | Implementado | `AccessPolicy` + verificación en cada caso de uso; matriz en [permisos.md](permisos.md) |
| Privilegio completo solo para las 2 cuentas iniciales (D-03, RN-16) | Implementado | Se crean desde `INITIAL_ADMIN_*` en `.env`; nadie más puede tenerlo; no se puede quedar la plataforma sin una activa |
| Auditoría de acceso | Implementado | Tabla `audit_event`; `GET /admin/audit-events`; pantalla `/admin/auditoria` |
| Envío de correo con Brevo | Implementado, sin probar con la cuenta real | `docker-compose.brevo.yml` + secretos; falta el remitente verificado (P-06). En desarrollo se usa la bandeja local `.local/mail-outbox` |

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
| Almacenamiento local | Implementado en la Etapa 4 | Volumen `file-storage` (T-22) |
| CI básico | Implementado (sin ejecutar en GitHub) | `.github/workflows/ci.yml`; el repositorio aún no está publicado en GitHub |
| Health checks y OpenAPI | Implementado | `/actuator/health/*`, `/api/v1/openapi`, `/swagger-ui.html` |

## Requerimientos funcionales

| Requerimientos | Estado |
| --- | --- |
| RF-01, RF-02, RF-03, RF-04, RF-29, RF-30 | Implementados |
| RF-05 | Implementado con CSV; Excel pendiente (P-07) |
| RF-06, RF-07, RF-08, RF-09, RF-24, RF-25 | Implementados (Etapa 4) |
| RF-28 | Parcial: usuarios, roles, aprobación docente, auditoría y publicación de la biblioteca implementados; foro y respaldos llegan con sus etapas |
| RF-10, RF-11, RF-13, RF-19 | Implementados (Etapa 5) |
| RF-14, RF-15, RF-17 | Implementados (Etapa 6): calificación con IA en segundo plano, confirmación docente y registro de cada evaluación. Pruebas de aceptación del modelo pendientes en el equipo de referencia |
| RF-16 | Pendiente (Etapa 7, T-48) |
| RF-12, RF-18, RF-20 a RF-23, RF-26, RF-27 | Pendientes |
