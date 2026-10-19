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
  "schemaVersion": "1"
}
```

### Planeados para la Etapa 3 · Acceso y permisos (pendientes)

Las rutas marcadas "especificación" vienen de la sección 7; las demás son **propuesta** para revisión.

| Método y ruta | Origen | Requisito |
| --- | --- | --- |
| `POST /api/v1/auth/login` | especificación | RF-03 |
| `POST /api/v1/auth/logout` | propuesta | RF-03 |
| `POST /api/v1/auth/password-recovery` | especificación | RF-04, RN-23 (respuesta genérica, `202`) |
| `POST /api/v1/auth/password-reset` | especificación | RF-04, RN-23 |
| `POST /api/v1/auth/student-registrations` | propuesta | RF-01, RN-01 |
| `POST /api/v1/auth/teacher-requests` | propuesta | RF-02, RN-02 |
| `POST /api/v1/auth/email-verifications/confirm` | propuesta | RF-29, RN-24 |
| `POST /api/v1/auth/email-change-requests` | propuesta | RF-30, RN-25 |
| `GET /api/v1/me` | propuesta | Datos y permisos de la sesión actual |
| `GET/POST /api/v1/admin/users` | especificación | RF-28 |
| `POST /api/v1/admin/teachers/{id}/approve` | especificación | RF-02, RN-02 |
| `PATCH /api/v1/admin/users/{id}/role` | propuesta | RF-28, RN-04, RN-16 (solo privilegio completo) |

### Etapas siguientes (de la especificación, pendientes)

| Recurso | Operaciones |
| --- | --- |
| Cursos y temas | `GET/POST /api/v1/courses` · `GET/POST /api/v1/topics` |
| Biblioteca | `GET /api/v1/library` · `GET /api/v1/materials/{id}/preview` · `GET /api/v1/materials/{id}/download` |
| Actividades y entregas | `GET/POST /api/v1/activities` · `POST /api/v1/activities/{id}/submissions` (`202 Accepted`) |
| Evaluación | `GET /api/v1/submissions/{id}/grading` · `POST /api/v1/submissions/{id}/grade/confirm` |
| Foro | `GET/POST /api/v1/forum/threads` · `PATCH /api/v1/forum/threads/{id}` |
