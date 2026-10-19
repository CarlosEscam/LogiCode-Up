# Estado del proyecto

Última actualización: 19 de octubre de 2026 · Etapa en curso: **cierre de la Etapa 2 (fundaciones técnicas)**

Leyenda: **Implementado** = existe en el código y tiene prueba o verificación. **Pendiente** = no existe todavía. **Decisión aprobada** = definido en la especificación, sin implementar.

## Etapas

| Etapa | Estado | Notas |
| --- | --- | --- |
| 0 · Aprobación de alcance | Aprobada (especificación) | |
| 1 · Análisis y diseño | Diseño técnico mínimo hecho | Arquitectura, convenciones de API y datos, matriz de permisos y propuesta de modelo de identidad. Pendientes de la etapa completa: casos de uso, historias con criterios de aceptación, maquetas, rúbricas y prueba de lectura manuscrita |
| 2 · Fundaciones técnicas | Implementada, pendiente de revisión | Ver detalle abajo |
| 3 a 10 | Pendientes | No se ha construido ninguna funcionalidad de negocio |

## Etapa 2 · detalle

| Entregable de la especificación | Estado | Dónde |
| --- | --- | --- |
| Repositorio Git y ramas | Implementado | Rama `main`; flujo en README |
| Estructura React | Implementado | `frontend/` |
| Estructura Spring Boot modular por capas | Implementado | `backend/src/main/java/co/edu/unipamplona/logicode/` |
| API REST JSON con prefijo `/api/v1` | Implementado | `GET /api/v1/system/info` |
| PostgreSQL | Implementado | Servicio `db` en Docker Compose |
| Migraciones versionadas | Implementado | Flyway, `V1__linea_base.sql` |
| Docker Compose | Implementado | `docker-compose.yml` (db, backend, frontend) |
| Configuración por entorno | Implementado | `.env.example`, `application.yml` |
| Almacenamiento local | Parcial | Volumen `file-storage` reservado; la interfaz de almacenamiento se define con la carga de archivos (P-05) |
| CI básico | Implementado (sin ejecutar en GitHub) | `.github/workflows/ci.yml`; el repositorio aún no está publicado en GitHub |
| Health checks | Implementado | `/actuator/health/*` y health checks de los tres contenedores |
| OpenAPI | Implementado | `/api/v1/openapi`, `/swagger-ui.html` |

### Verificación realizada

- Backend: `./mvnw verify` → 11 pruebas en verde (unitarias, controlador, arquitectura e integración con PostgreSQL 17 real).
- Frontend: `npm run lint`, `npm test` (2 pruebas) y `npm run build` en verde.
- Docker Compose: los tres servicios quedan `healthy`; a través de Nginx, `GET /api/v1/system/info` devuelve `schemaVersion: "1"` y la página de inicio muestra el estado.

## Requerimientos funcionales

Todos los RF-01 a RF-30 están **pendientes de construir**. Ninguno se implementó en esta etapa.
