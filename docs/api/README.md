# API REST de LogiCode

## Convenciones

| Tema | Convención |
| --- | --- |
| Prefijo | Todas las rutas de negocio empiezan por `/api/v1`. Un cambio incompatible crea `/api/v2` |
| Formato | JSON UTF-8; propiedades en `camelCase`; recursos en plural y en inglés (`/activities`, `/submissions`) |
| Fechas | ISO-8601 con desfase, p. ej. `2026-12-10T23:59:00-05:00` |
| Notas | Número con un decimal entre `0.0` y `5.0` (la interfaz lo muestra como `4,5`) |
| Errores | [Problem Details, RFC 9457](https://www.rfc-editor.org/rfc/rfc9457): `type`, `title`, `status`, `detail`, `instance`; errores de validación agregan la lista de campos. Mensajes en español y sin detalles internos |
| Códigos | `200` consulta, `201` creación (con cabecera `Location`), `202` trabajo aceptado en segundo plano (entregas con IA), `204` sin contenido, `400` formato inválido, `401` sin sesión, `403` sin permiso, `404` no existe, `409` conflicto de estado, `422` regla de negocio incumplida, `429` límite de solicitudes |
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

### Etapas siguientes (de la especificación, pendientes)

| Recurso | Operaciones |
| --- | --- |
| Actividades y entregas | `GET/POST /api/v1/activities` · `POST /api/v1/activities/{id}/submissions` (`202 Accepted`) |
| Evaluación | `GET /api/v1/submissions/{id}/grading` · `POST /api/v1/submissions/{id}/grade/confirm` |
| Foro | `GET/POST /api/v1/forum/threads` · `PATCH /api/v1/forum/threads/{id}` |
