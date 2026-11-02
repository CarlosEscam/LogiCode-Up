# Calidad y seguridad (Etapa 9)

Revisión del 27 de octubre de 2026 sobre el código de las Etapas 3 a 8. Punto de control de la especificación: lista de defectos críticos resuelta; restauración y flujo de calificación demostrados.

## Lista de defectos

No se encontraron defectos críticos (pérdida de datos, acceso sin permiso o notas mal calculadas). Lo encontrado:

| Id | Severidad | Defecto | Estado |
| --- | --- | --- | --- |
| D9-01 | Seria (accesibilidad, RNF-11) | Las tablas que se desplazan a lo ancho en el teléfono (planilla, panel de administración, habilitaciones, auditoría) no se podían desplazar con el teclado | Corregido: toda `.table-wrap` recibe foco con Tab y se desplaza con las flechas |
| D9-02 | Media (seguridad) | Nginx servía la interfaz sin `Content-Security-Policy`, `X-Content-Type-Options` ni `Referrer-Policy`, y mostraba su versión | Corregido en la configuración de Nginx (desde la Etapa 10, `frontend/nginx/logicode.conf`): solo se cargan recursos del propio sitio (scripts, estilos, imágenes, videos y la vista previa de PDF); la página no se puede incrustar en otro sitio |
| D9-03 | Media (observabilidad) | No quedaba registro de cada solicitud a la API, así que un error reportado por un usuario no se podía ubicar en los logs | Corregido: una línea por solicitud con método, ruta, estado, duración y el `X-Request-Id`; métricas en `/actuator/metrics` |

Riesgos aceptados o que pasaron a la Etapa 10 (no críticos):

| Id | Riesgo | Decisión |
| --- | --- | --- |
| R9-01 | Sin HTTPS: en local la sesión viaja sin cifrar por la red del aula (RNF-02 pide HTTPS en despliegue) | **Resuelto en la Etapa 10**: el modo piloto (`docker-compose.piloto.yml`) sirve solo por HTTPS con cookies `Secure` ([piloto.md](piloto.md)). El modo de desarrollo sigue en HTTP local |
| R9-02 | `/swagger-ui.html` y `/api/v1/openapi` son públicos | Solo describen la API, sin datos. **Etapa 10:** apagados en el modo piloto (`SPRINGDOC_APIDOCS_ENABLED=false`, `SPRINGDOC_SWAGGERUI_ENABLED=false`); siguen en desarrollo |
| R9-03 | Cinco intentos fallidos bloquean 15 minutos la cédula, aunque los haga otra persona | Se mantiene: protege contra adivinar contraseñas; el bloqueo es corto y queda en la auditoría |
| R9-04 | Una cédula sin cuenta responde un poco más rápido al iniciar sesión | Se mantiene: el registro ya informa si una cédula está registrada, así que medir tiempos no revela nada nuevo |
| R9-05 | Las dependencias Java no se revisaron contra la base de vulnerabilidades: el entorno de Claude no llega a esa base | **Resuelto en la Etapa 10** (27 de octubre): las 168 dependencias de `./mvnw dependency:list` (todas las de ejecución y de prueba) contra la base OSV de Maven. Con Spring Boot 4.1.1 había 10 avisos: tres críticos de Tomcat 11.0.24 en la autenticación DIGEST y FORM y en las restricciones de seguridad declaradas en el contenedor (LogiCode no usa ninguna de las tres: la seguridad la hace Spring Security) y siete altos o medios de Jackson 3.1.5 y 2.21.5 (consumo de recursos al leer JSON). Se subió a Spring Boot 4.1.2 (Tomcat 11.0.27, Jackson 3.1.7 y 2.21.7): 0 avisos. `npm audit`: 0. Repetir antes de cada entrega (manual técnico, sección 8). Con Apache POI 5.5.1 y PDFBox 3.0.8 (P-07, P-08) y sus dependencias: 0 avisos. **Repetida el 2 de noviembre** (con MinIO y Brevo ya integrados): 179 dependencias contra la base OSV de Maven actualizada al 31 de octubre de 2026: 0 avisos; el escáner se comprobó con versiones viejas conocidas (Tomcat 11.0.24, Jackson 3.1.5, Log4j 2.14.1) y sí las marca |

## Qué se revisó y cómo

| Tema | Revisión | Resultado |
| --- | --- | --- |
| Permisos de toda la API | `ApiSecurityIT` recorre **todos** los endpoints `/api/v1` que existen: sin sesión solo responden los públicos aprobados (biblioteca, materiales, foro, información del sistema, documentación y acceso) y ninguna escritura pasa sin token CSRF. Un endpoint nuevo queda cubierto solo | Sin hallazgos |
| Permisos por rol y por curso | Pruebas de las Etapas 3 a 8 (administrador ordinario, estudiante de otro curso, compañero de curso) y, nuevo, un docente de otro curso: no ve las entregas, la bandeja ni califica (`404`, no revela que el curso existe) | Sin hallazgos |
| Carga de archivos | Formato por extensión y por contenido real, tamaño, archivo vacío, nombre con rutas o caracteres de control, borrado si la operación falla (Etapas 4 y 5); `nosniff` en las descargas | Sin hallazgos |
| Sesión y contraseñas | Cookie HttpOnly, nueva sesión al ingresar, CSRF, bloqueo por intentos, BCrypt, cierre de todas las sesiones al cambiar la contraseña (Etapa 3) | R9-01, R9-03, R9-04 |
| Exportación | El CSV antepone `'` a celdas que empiezan con `=`, `+`, `-` o `@` para que Excel no las ejecute | Sin hallazgos |
| Interfaz | Sin `dangerouslySetInnerHTML` ni `eval`; React escapa todo el texto del foro y de las entregas | Sin hallazgos |
| Accesibilidad (RNF-11) y móvil | axe-core 4.10 (WCAG 2.1 A y AA: contraste, etiquetas, teclado, nombres de botones) en 37 vistas de visitante, estudiante, docente y administradora, incluidas todas las pestañas del curso, a 1280 px y a 390 px; también se mide desplazamiento horizontal | D9-01; después, 0 hallazgos y nada se sale de la pantalla |
| Dependencias | `npm audit`: 0 vulnerabilidades; Java: 0 avisos en OSV (Etapa 10 y de nuevo el 2 de noviembre) | R9-05 (resuelto) |

## Observabilidad

- Cada solicitud a la API deja una línea: `GET /api/v1/courses/… 200 12 ms`, con el identificador de la solicitud. En Docker los logs son JSON (ECS): `docker compose logs backend`.
- Cuando un usuario reporta un error, la respuesta trae `X-Request-Id`; con ese valor se encuentran todas sus líneas: `docker compose logs backend | grep <id>`.
- Métricas desde el mismo PC (el puerto del backend solo escucha en `127.0.0.1`): `curl http://localhost:8080/actuator/metrics/jvm.memory.used`, `…/http.server.requests`, `…/hikaricp.connections.active`.
- Salud: `/actuator/health`. Estado de la IA y de los respaldos: panel `/admin`.

## Demostraciones

Verificadas el 27 de octubre en Docker sobre una base limpia con los datos de demostración de las Etapas 3 a 8. Se pueden repetir así:

### Restauración

1. En `/admin` → Respaldos, **Crear respaldo ahora**. Al minuto aparece la copia con "Restauración probada".
2. Crear algo después del respaldo, por ejemplo un hilo en el foro.
3. Restaurar:
   ```bash
   docker compose stop backend
   docker compose run --rm --no-deps backup restaurar <nombre-del-respaldo>
   docker compose start backend
   ```
4. El hilo creado en el paso 2 ya no existe y todo lo anterior sigue igual.

Resultado de la verificación: el hilo "Creado después del respaldo" desapareció; las cuentas y sesiones del respaldo siguieron funcionando.

### Flujo de calificación

1. **Automática (PSeInt):** Elena (`1000000205`) abre "Taller 4: suma de 1 a n", escribe el programa y envía. La nota (5,0, "Casos de prueba aprobados: 3 de 3") aparece sin intervención del docente.
2. **Manual:** Camila (`1000000203`) entrega en texto "Taller 2: tabla de multiplicar". La entrega llega a la pestaña "Por revisar" de Laura (`1000000101`), que la abre, pone 4,2 con retroalimentación y publica.
3. Camila ve la nota y la retroalimentación desde el teléfono; la planilla del curso la incluye.
4. **Con IA:** necesita Qwen2.5-VL en el equipo de presentación (`docker compose --profile ia`); las fotos del Taller 3 sirven con "Reintentar con IA". Las reglas de la IA están probadas en `AiGradingFlowsIT` con un proveedor simulado.

Contraseña de todas las cuentas de demostración: `Demo#2026`.
