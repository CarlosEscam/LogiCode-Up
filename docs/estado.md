# Estado del proyecto

Última actualización: 19 de octubre de 2026 · Etapa en curso: **cierre de la Etapa 4 (curso, temas y biblioteca)**

Leyenda: **Implementado** = existe en el código y tiene prueba o verificación. **Pendiente** = no existe todavía. **Decisión aprobada** = definido en la especificación, sin implementar.

## Etapas

| Etapa | Estado | Notas |
| --- | --- | --- |
| 0 · Aprobación de alcance | Aprobada (especificación) | |
| 1 · Análisis y diseño | Diseño técnico mínimo hecho | Pendientes de la etapa completa: casos de uso, historias con criterios de aceptación, maquetas, rúbricas y prueba de lectura manuscrita |
| 2 · Fundaciones técnicas | Implementada | Detalle en la sección de la Etapa 2 |
| 3 · Acceso y permisos | Implementada y aprobada | Falta: envío real con Brevo (P-06) y carga desde Excel (P-07) |
| 4 · Curso, temas y biblioteca | Implementada, pendiente de revisión | Detalle abajo. Almacenamiento local como opción por defecto mientras se decide P-05 |
| 5 a 10 | Pendientes | Actividades, calificación, quizzes, juegos, foro y demás aún no existen |

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
| RF-10 a RF-23, RF-26, RF-27 | Pendientes |
