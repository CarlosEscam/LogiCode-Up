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

## Revisión de la versión 1.2.0 (6 de noviembre de 2026)

Hecha para las guías del segundo corte de Ingeniería de Software II, con herramientas que corren en el propio equipo ([ci/README.md](../ci/README.md)). Línea base: la 1.1.2.

### Defectos encontrados y corregidos

| Id | Severidad | Defecto | Estado |
| --- | --- | --- | --- |
| D12-01 | Seria (Ley 1581) | El log de cada solicitud guardaba la cédula cuando va en la ruta (`DELETE …/enrollments/600001 204`, visto en el log de pruebas de la 1.1.2) | Corregido con TDD (T-97): la ruta pasa por `LogPaths.sanitize`; también la URL de las trazas |
| D12-02 | Media (seguridad) | La interfaz corría como root dentro del contenedor (Trivy DS-0002) y sobre Nginx 1.29, con 51 CVE altos o críticos en su Alpine | Corregido (T-98): usuario `nginx`, puertos 8080/8443, rama estable 1.30 (0 CVE) |
| D12-03 | Media (operación) | Con la base caída, ninguna alerta lo notaba: los 500 salen como `uri="UNKNOWN"` y el SLI solo miraba `/api/v1` | Corregido en el simulacro de la guía 14 (T-100): SLI sin filtrar por ruta y alerta `LogiCodeBaseNoResponde` ([observabilidad.md](observabilidad.md)) |
| D12-04 | Baja (operación) | El SLI usaba el balde `le="1.0"`, que Micrometer no crea por sí solo: el panel decía "No data" | Corregido: `management.metrics.distribution.slo.http.server.requests: 1s` |
| D12-05 | Baja (operación) | Los logs de Docker no tenían límite y podían llenar el disco | Corregido: `json-file` con 5 archivos de 10 MB por servicio (ADR 13) |
| D12-06 | Media (operación) | Sin solicitudes de usuarios (de noche) el SLI daba 0 % en vez de "sin dato", y tras una hora la alerta de quema rápida se dispararía sola. Visto en el panel de Grafana durante la redacción de la guía 13 | Corregido con prueba primero: caso "sin tráfico" en `alertas-test.yml` en rojo, luego el SLI sin `clamp_min` (0/0 no compara) y en verde |

### Herramientas y resultados

| Revisión | Herramienta | Resultado |
| --- | --- | --- |
| Complejidad ciclomática | lizard 1.17 | Backend: 888 funciones, promedio 2,30, 21 con más de 10 (antes 22) y 6 con más de 15 (antes 7). La más alta sigue en el intérprete de PSeInt (`Lexer.tokens`, 38). Interfaz: 584 funciones, promedio 2,41, 11 con más de 10, máximo 20 (`QuizAttemptPage`) |
| SAST | Semgrep 1.184.0, reglas de Java, JavaScript, secretos, Dockerfile y Compose (553 reglas) | 12 hallazgos, 0 ERROR. 9 INFO: imágenes base sin fijar por digest (se fijan por versión), `HEALTHCHECK` en forma de texto, y `App.jsx:167` (falso positivo: el texto no es una plantilla). 3 WARNING `no-replaceall` en `ActivityPage.jsx:325`, `TopicsPanel.jsx:333` y `PseintPage.jsx:86`: falsos positivos, `replaceAll` con un texto reemplaza todas las apariciones desde ES2021 |
| SCA, secretos y configuración del repositorio | Trivy 0.69.3 `fs` | 0 HIGH o CRITICAL en `package-lock.json`, Dockerfiles, compose y secretos. `backend/pom.xml` se excluye: Trivy lo lee sin el BOM de Spring Boot y adivina versiones. Dio 4 falsos positivos el 6 de noviembre: CVE-2026-49844 en `log4j-api` (leída 2.24.3, se usa 2.25.5) y, con la base de Trivy de la tarde, CVE-2026-68497, CVE-2026-91776 y CVE-2026-91777 en `jackson-databind` (leída 2.22.1, se usa 2.21.7, ya corregida). Las dependencias Java se revisan en el jar de la imagen, con las versiones reales: 0 |
| Imágenes | Trivy 0.69.3 `image` | Interfaz: 0. Backend: 2 HIGH, CVE-2026-109518 en `apt` y `libapt-pkg7.0` de la base `eclipse-temurin:21-jre` (la más reciente, del 29 de octubre). Riesgo aceptado hasta el 31 de enero de 2027: el contenedor corre como `logicode`, sin permiso para usar apt, y nada lo ejecuta ([.trivyignore.yaml](../.trivyignore.yaml)). `postgres:17-alpine` (imagen de terceros): 26 en el Go de `gosu`, que solo cambia de usuario al arrancar |
| Cobertura del backend | JaCoCo 0.8.15 (unitarias + integración) | 88,6 % de líneas y 71,2 % de ramas al poner la puerta (85 % y 68 %); 88,8 % y 72,2 % con las pruebas nuevas de la 1.2.0 |
| Contrato de la API | Redocly CLI 1.34.5 | 0 errores, 2 avisos (licencia sin decidir; `GET /system/info` sin respuestas 4xx) |
| Carga | k6 1.4.2, 50 usuarios virtuales | p95 26 a 31 ms, 0 % de errores ([pruebas-carga](../pruebas-carga/README.md)) |

### Pirámide de pruebas

| Nivel | Cuántas | Dónde | Tiempo |
| --- | --- | --- | --- |
| Unitarias y de arquitectura | 96 del backend (JUnit, ArchUnit) y 78 de la interfaz (Vitest, Testing Library) | `backend/src/test` (`*Test`), `frontend/src/**/*.test.*` | Segundos |
| Integración | 57 con PostgreSQL real en un contenedor desechable (Testcontainers), servidor completo y MockMvc; MinIO también real (`MinioStorageIT`). Solo se reemplazan en el borde la IA (`GradingProvider` con respuestas guionadas) y el correo (`EmailSender`), nunca la lógica de negocio | `backend/src/test` (`*IT`) | Unos 2 minutos |
| Contrato | 1 del proveedor (`LogiCodeApplicationIT`) y 1 del consumidor (`contrato.test.js`) sobre `docs/api/openapi.json` | Ver [api/README.md](api/README.md) | Segundos |
| De punta a punta (E2E) | 0 automatizadas. Se hacen a mano en cada etapa (capturas y axe-core con Playwright) y con la colección de Postman | — | — |

Las pruebas de integración nunca tocan la base de desarrollo: cada clase crea su propio PostgreSQL y lo borra al terminar.

### Inventario de deuda técnica

Cuadrante de Fowler: deliberada o inadvertida, prudente o imprudente.

| Id | Deuda | Estado | Cuadrante | Qué se hace |
| --- | --- | --- | --- | --- |
| DT-01 | Intérprete de PSeInt con funciones muy complejas (`Lexer.tokens` 38, `Parser.statement` 34, `Builtins.call` 28) | Pendiente | Deliberada y prudente: un analizador escrito a mano es así, y tiene muchas pruebas | Partir solo si hay que agregar sintaxis nueva |
| DT-02 | `ManageQuestionBank.validate` con complejidad 26 y sin pruebas unitarias | **Pagada** (6 de noviembre): pruebas de caracterización primero (`51abfc5`), luego el refactor (complejidad 9) | Inadvertida y prudente | — |
| DT-03 | El reto de bloques se evalúa con la misma lógica escrita dos veces (`BlockChallenge` en Java y `blocks.js` en la interfaz) | Pendiente | Deliberada y prudente (T-72: el servidor debe recalcular la nota) | Pruebas que corran los mismos programas en los dos lados |
| DT-04 | `QuizAttemptPage` (20) y `QuizPanel` (18) en la interfaz | Pendiente | Inadvertida y prudente | Partir cuando se toque el quiz |
| DT-05 | Cédula en el log de solicitudes | **Pagada** (D12-01) | Inadvertida e imprudente | — |
| DT-06 | Nginx como root y en una rama sin parches | **Pagada** (D12-02) | Inadvertida y prudente | — |
| DT-07 | Sin métricas, trazas ni alertas | **Pagada en parte** (T-99, T-100) | Deliberada y prudente (no hacían falta antes del piloto) | Falta que las alertas avisen a una persona |
| DT-08 | El `ci.yml` existía pero nunca se ejecutó | **Pagada** (T-102): `ci/ci-local.mjs` | Deliberada y prudente (no se sube a GitHub) | — |
| DT-09 | CVE de `apt` en la imagen base del backend | Aceptada hasta el 31 de enero de 2027 | Deliberada y prudente | `docker compose build --pull` cuando haya imagen corregida |
| DT-10 | Al terminar las pruebas de integración se ven 5 errores `CannotCreateTransactionException` de la tarea que limpia sesiones (la base ya se cerró) | Pendiente | Inadvertida y prudente | Apagar las tareas programadas en las pruebas |
| DT-11 | La espera de conexión de 30 s hace que un estudiante espere medio minuto para ver un error si la base cae | Pendiente (postmortem de la guía 14) | Inadvertida y prudente | Bajar a 5 s después de medirlo con la prueba de carga |
| DT-12 | Sin pruebas E2E automáticas ni cobertura medida en la interfaz | Pendiente | Deliberada y prudente | Ver la hoja de ruta (guía 15) |

## Observabilidad

Desde la 1.2.0, métricas, trazas, panel, SLO y alertas están en [observabilidad.md](observabilidad.md). Lo de esta sección sigue valiendo:

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
