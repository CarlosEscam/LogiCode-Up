# Observabilidad, SLO e incidentes

Cómo LogiCode cuenta lo que le pasa mientras corre: logs, métricas, trazas, el panel, el objetivo de confiabilidad (SLO), las alertas y qué hacer cuando suenan. Corresponde a las guías 13 y 14 de Ingeniería de Software II (T-99, T-100) y a la versión 1.2.0.

## Qué hay

| Señal | Qué es | Dónde se ve | Estado |
| --- | --- | --- | --- |
| Logs | Una línea JSON (formato ECS) por evento, con `traceId`, `spanId` y `requestId`. La línea de cada solicitud cambia la cédula de la ruta por `{cedula}` (T-97) | `docker compose logs backend` | Implementado desde la Etapa 9; correlación con trazas y limpieza de cédulas en 1.2.0 |
| Métricas | Micrometer en formato Prometheus: solicitudes por ruta y estado con histograma de latencia, el tramo `logicode.activities.list`, conexiones a PostgreSQL, memoria de Java | `http://localhost:9090` (Prometheus) y el panel de Grafana | Implementado (1.2.0) |
| Trazas | OpenTelemetry (Micrometer Tracing): árbol de cada solicitud con los filtros de seguridad y el tramo del caso de uso, etiquetado con el **rol**, nunca con la cédula ni el nombre | `http://localhost:16686` (Jaeger) | Implementado (1.2.0); apagado por defecto |
| Panel | "LogiCode · API" en Grafana: solicitudes por segundo, p95 del endpoint crítico, % de 5xx, SLI, p95 del tramo por rol y conexiones a la base, con umbrales en rojo | `http://localhost:3001` | Implementado (1.2.0) |
| Alertas | `LogiCodePresupuestoQuemaRapida`, `LogiCodeBaseNoResponde`, `LogiCodeBackendCaido` en Prometheus | `http://localhost:9090/alerts` | Implementado; **pendiente** que avisen solas a una persona |

El endpoint instrumentado a mano es el crítico de la prueba de carga: `GET /api/v1/courses/{courseId}/activities` en [ActivityController.java](../backend/src/main/java/co/edu/unipamplona/logicode/activities/api/ActivityController.java). El resto de rutas queda medido por la instrumentación automática de Spring.

## Cómo encenderlo

Prometheus, Grafana y Jaeger van en el perfil `observabilidad` de Compose, apagado por defecto (decisión de Carlos del 6 de noviembre: todo local, sin servicios en internet). Todo escucha solo en `127.0.0.1` del equipo servidor.

```
docker compose --profile observabilidad up -d
```

O, para que quede siempre, en `.env`: `COMPOSE_PROFILES=observabilidad` (con IA: `COMPOSE_PROFILES=ia,observabilidad`). Para mandar trazas a Jaeger, además `LOGICODE_TRACING_EXPORT=true`. Las métricas no necesitan nada: el backend las expone siempre en `/actuator/prometheus`, que solo es visible dentro de Docker (Nginx publica únicamente `/api`).

Grafana no tiene formulario de entrada ni cuenta admin utilizable (`GF_AUTH_DISABLE_LOGIN_FORM`, `GF_AUTH_BASIC_ENABLED=false`): solo se mira. El panel se cambia editando [logicode-api.json](../observabilidad/grafana/dashboards/logicode-api.json).

## Muestreo

Se guarda 1 de cada 10 trazas (`LOGICODE_TRACING_SAMPLING=0.1`). En la copia de ensayo, con 50 usuarios virtuales y 2003 solicitudes, el p95 fue 26,4 ms con todas las trazas y 26,8 ms con 1 de cada 10: a esta carga la diferencia está dentro del ruido. El 10 % se elige por espacio y memoria de Jaeger, no por CPU: con un curso de 30 estudiantes alcanza para ver cualquier patrón que se repita. El `traceId` queda en cada línea del log aunque la traza no se guarde.

Consumo medido del perfil en la copia de ensayo: Prometheus 25 MB, Grafana 77 MB y Jaeger 82 MB de RAM, menos de 0,5 % de CPU en reposo. Prometheus guardó 0,5 MB en la primera media hora; con 15 días de retención se estiman unos 400 MB (estimación, no medido en 15 días).

## Datos personales (Ley 1581)

Ninguna cédula, nombre ni correo va a un log, métrica o traza:

- Las métricas usan la plantilla de la ruta (`/courses/{courseId}/enrollments/{cedula}`), nunca la ruta real.
- La URL que guarda cada traza y la línea de cada solicitud pasan por `LogPaths.sanitize`, que cambia cualquier segmento de 5 a 12 dígitos por `{cedula}` ([TelemetryConfig.java](../backend/src/main/java/co/edu/unipamplona/logicode/shared/infrastructure/web/TelemetryConfig.java), prueba `RequestIdFilterTest`).
- El tramo `logicode.activities.list` lleva solo el rol (`STUDENT`, `TEACHER`…).

Verificado el 6 de noviembre: se inscribió y se borró la cédula ficticia `1099999901`; aparece 0 veces en el log del backend y 0 veces en Jaeger. Antes de 1.2.0 el log de solicitudes sí tenía la cédula (`DELETE /api/v1/courses/…/enrollments/600001 204`, visto en el log de pruebas de 1.1.2).

## Retención y rotación de logs (ADR 13)

- Todos los contenedores usan `json-file` con `max-size: 10m` y `max-file: 5`: como máximo 50 MB por servicio, unos 500 MB en total con todos los perfiles. Antes no había límite y los logs podían llenar el disco (la primera causa de la sección 1 del runbook).
- Los logs viven en el equipo; se pierden al borrar el contenedor (`docker compose down`, una actualización con imagen nueva). Si hace falta guardar uno para un incidente: `docker compose logs backend > incidente-AAAAMMDD.log` antes de tocar nada.
- Métricas: 15 días en Prometheus (`--storage.tsdb.retention.time=15d`). Trazas: en memoria de Jaeger, se pierden al reiniciarlo.
- Los logs no tienen datos personales, así que conservarlos no choca con la Ley 1581.

## SLI, SLO y presupuesto de error

| SLI | SLO | Presupuesto de error |
| --- | --- | --- |
| Proporción de solicitudes de usuarios (todo menos `/actuator`) que responden sin error 5xx y en menos de 1 s | 99 % en 30 días | 1 % = 7,2 horas al mes |

Por qué 99 % y no más: LogiCode corre en un solo equipo (el PC del piloto), sin réplica ni persona de guardia de noche. 99,9 % serían 43 minutos al mes: un solo corte de luz o una actualización mal hecha lo gastan. Lo que de verdad importa son las horas de cierre de una actividad; para eso el docente tiene las excepciones de plazo (RN-05).

Sin solicitudes de usuarios (de noche) el SLI no tiene dato en vez de valer 0 %: si valiera 0, la alerta de quema rápida se dispararía sola tras una hora sin tráfico (D12-06, probado en `alertas-test.yml`). En el panel esos tramos se ven vacíos.

Política del presupuesto: si en un mes se gasta más de la mitad (3,6 h), no se agregan funciones nuevas hasta corregir la causa del incidente.

Las reglas están en [alertas.yml](../observabilidad/alertas.yml) y se prueban con datos simulados:

```
docker run --rm -v "${PWD}/observabilidad:/etc/prometheus" --entrypoint promtool prom/prometheus:v3.7.3 test rules /etc/prometheus/alertas-test.yml
```

La alerta de quema rápida sigue el método de varias ventanas del libro de SRE de Google: se dispara si en 5 minutos **y** en 1 hora fallan más de 14,4 veces lo permitido (con ese ritmo se gasta el 2 % del mes en una hora).

## Simulacro: la base no responde

6 de noviembre de 2026, copia de ensayo en la nube con datos ficticios (proyecto Compose `g2`, versión 1.2.0 en preparación, perfil `observabilidad`). Un estudiante ficticio consultaba sus actividades 5 veces por segundo. Se detuvo la base a propósito y se siguió el [runbook](runbooks/Runbook_backend.md) tal como estaba escrito.

| Hora (UTC) | Qué pasó |
| --- | --- |
| 01:47:34 | `docker stop` de la base (inicio del incidente) |
| 01:48:04 | Primera respuesta 500 al estudiante, después de 30 s de espera (tiempo de espera de Hikari) |
| 01:48:50 | Se ve que ninguna alerta se movió: el SLI seguía en 100 %. Se agrega `LogiCodeBaseNoResponde` y se recarga Prometheus |
| 01:49:03 | La alerta nueva pasa a `pending` |
| 01:49:13 | Docker marca el backend `unhealthy` (T-93) |
| 01:50:03 | La alerta pasa a `firing` |
| 01:50:06 | Se sigue el runbook: `docker compose ps` (la base **no aparece**), `logs db` (apagado ordenado), `start db` |
| 01:50:15 | El estudiante vuelve a recibir 200 |
| 01:50:17 | Base `healthy` y backend `readiness` en `UP`, sin reiniciarlo |

Duración vista por el usuario: 2 min 11 s (de la primera espera a la primera respuesta buena). Recuperación una vez iniciada: 11,5 s. Evidencias: `g14-vigia.log`, `g14-trafico.log`, `g14-runbook.txt` y la captura del panel en la carpeta de evidencias de la guía 14.

## Postmortem sin culpables

**Resumen.** Durante 2 minutos los estudiantes no pudieron ver sus actividades porque PostgreSQL estaba detenido. Ninguna alerta existente lo detectó.

**Causa raíz (de proceso, no de persona).** Las alertas se diseñaron solo sobre el SLI de solicitudes que llegan a una ruta conocida. Con la base caída, el filtro de sesiones falla antes de saber la ruta, Micrometer etiqueta esos 500 como `uri="UNKNOWN"` y el SLI filtraba por `/api/v1`, así que no los contaba. Además, cada solicitud espera 30 s antes de fallar, así que llegan muy pocas y una alerta por porcentaje tarda en moverse. Las pruebas de las reglas usaban datos simulados con la ruta ya conocida, por eso pasaban.

**Qué funcionó.** La readiness con la base (T-93) marcó el backend `unhealthy` en 99 s. El backend se reconectó solo al volver la base. Los pasos 1 a 4 del runbook funcionaron tal como estaban escritos.

**Qué no funcionó.** `docker compose ps` no muestra un servicio detenido; el runbook decía usarlo. El SLI no veía los 500.

| Acción | Tipo | Estado |
| --- | --- | --- |
| Alerta `LogiCodeBaseNoResponde` sobre las esperas vencidas de Hikari, con prueba en `alertas-test.yml` | Detección | Hecho (1.2.0) |
| El SLI cuenta todo menos `/actuator` (incluye `UNKNOWN`); la prueba de quema rápida usa ahora 500 con `uri="UNKNOWN"` | Detección | Hecho (1.2.0) |
| Runbook: `docker compose ps -a` | Respuesta | Hecho (1.2.0) |
| Que las alertas avisen a una persona (correo por Brevo o Telegram con Alertmanager) | Detección | **Pendiente**: tecnología nueva, se presenta antes a Carlos |
| Bajar la espera de Hikari de 30 s a unos 5 s para que el estudiante vea el error antes | Experiencia | **Propuesto**: medir primero con la prueba de carga que no aparezcan errores falsos con 50 usuarios |
| Repetir el simulacro en el equipo del piloto | Verificación | **Pendiente** (Carlos) |

## Archivos

| Archivo | Qué tiene |
| --- | --- |
| [observabilidad/prometheus.yml](../observabilidad/prometheus.yml) | Lectura del backend cada 15 s |
| [observabilidad/alertas.yml](../observabilidad/alertas.yml) | SLI y alertas |
| [observabilidad/alertas-test.yml](../observabilidad/alertas-test.yml) | Pruebas de las alertas con `promtool` |
| [observabilidad/grafana/](../observabilidad/grafana/) | Fuentes de datos y panel, cargados al arrancar |
| [runbooks/Runbook_backend.md](runbooks/Runbook_backend.md) | Qué hacer cuando suena una alerta |
| [pruebas-carga/](../pruebas-carga/README.md) | Prueba de carga del endpoint crítico (k6) |
