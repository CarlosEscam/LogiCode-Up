# Runbook del backend de LogiCode

Qué hacer cuando suena una alerta del backend o alguien avisa que LogiCode no carga. Está escrito para el equipo real: una sola persona de turno (Carlos o quien maneje el equipo servidor), sin equipo de soporte, con el PC donde corre Docker Compose.

Los comandos se ejecutan en la carpeta del proyecto (`LogiCode Up` en el PC). Son los mismos en PowerShell y en Linux. Cada paso se probó en el simulacro del 6 de noviembre de 2026 (copia de ensayo con datos ficticios, ver [observabilidad.md](../observabilidad.md#simulacro-la-base-no-responde)). Lo que no se ha ensayado está marcado como **no ensayado**.

## Alertas que llevan a este documento

| Alerta | Qué significa | Ir a |
| --- | --- | --- |
| `LogiCodeBaseNoResponde` | El backend no consigue conexión con PostgreSQL en 2 minutos seguidos | [1. La base no responde](#1-la-base-no-responde) |
| `LogiCodeBackendCaido` | Prometheus no puede leer las métricas del backend durante 1 minuto | [2. El backend no responde](#2-el-backend-no-responde) |
| `LogiCodePresupuestoQuemaRapida` | Más del 14,4 % de las solicitudes fallan o tardan más de 1 s (en 5 min y en 1 h) | [3. Muchos errores o lentitud](#3-muchos-errores-o-lentitud) |

Las alertas se ven en Prometheus (`http://localhost:9090/alerts`) y en el panel de Grafana (`http://localhost:3001`), con el perfil `observabilidad` encendido. **Pendiente**: que una alerta avise sola a una persona (correo o Telegram); hoy alguien tiene que mirar.

## 0. Primeros dos minutos (siempre)

1. Anotar la hora en que empezó (la de la alerta o la del aviso).
2. Ver el estado de todos los servicios:

   ```
   docker compose ps -a
   ```

   Lo normal: todos `Up` y `(healthy)`. Fíjate cuál está `Exited`, `Restarting` o `(unhealthy)`. Usa `-a`: sin él, un servicio detenido **no aparece** en la lista (en el simulacro, `db` simplemente no salía).
3. Avisar al docente del curso que LogiCode tiene una falla y que las entregas que no alcancen a subir se manejan con una excepción de plazo (manual del docente).

## 1. La base no responde

Síntomas: los estudiantes ven "Error del servidor" después de unos 30 segundos de espera; `docker compose ps -a` muestra `db` en `Exited` o sin `(healthy)`; el backend pasa a `(unhealthy)` en 1 a 2 minutos.

1. Ver por qué se detuvo:

   ```
   docker compose logs --tail 50 db
   ```

   - `No space left on device`: el disco está lleno. Revisa `docker system df` y libera espacio (por ejemplo, copias viejas fuera del equipo) **antes** de arrancar la base.
   - `database system was interrupted` y luego `ready to accept connections`: se cortó la luz o se apagó el equipo; arrancar basta.
   - Sin líneas nuevas y el contenedor `Exited (0)`: alguien la detuvo (`docker compose stop`).
2. Arrancar la base:

   ```
   docker compose start db
   ```

3. Esperar a que quede sana (unos 10 segundos):

   ```
   docker compose ps db
   ```

4. Confirmar que el backend se recuperó solo (no hace falta reiniciarlo; Hikari vuelve a conectarse):

   ```
   docker compose exec backend curl -fsS http://localhost:8080/actuator/health/readiness
   ```

   Debe decir `{"status":"UP"}`. En el simulacro tardó 11,5 segundos desde `start db`, sin tocar el backend.
5. Probar como usuario: abrir LogiCode, entrar con una cuenta y abrir las actividades de un curso.
6. Si la base no arranca o dice que los datos están dañados: restaurar la última copia (**no ensayado en este simulacro**; la restauración sí se ensayó en la II-03 y la II-05, ver [respaldos/README.md](../../respaldos/README.md)):

   ```
   docker compose run --rm --no-deps backup listar
   docker compose stop backend minio
   docker compose run --rm --no-deps backup restaurar <nombre-de-la-copia>
   docker compose start minio backend
   ```

   Se pierde lo que se hizo después de la copia (como máximo un día; RPO de los respaldos diarios).

## 2. El backend no responde

1. Ver el estado y las últimas líneas:

   ```
   docker compose ps backend
   docker compose logs --tail 100 backend
   ```

2. Buscar la causa en el log (cada línea es JSON y trae `traceId`):
   - `OutOfMemoryError`: apagar la IA (`LOGICODE_AI_ENABLED=false`) o cerrar otros programas del equipo; reiniciar.
   - `Connection refused` o `Unable to acquire JDBC Connection`: es la base, ir a la sección 1.
   - `Flyway` o `Migration`: una actualización a medias. **No** reiniciar en bucle: volver a la versión anterior con la copia previa a la actualización (manual técnico, "volver a la versión anterior").
3. Reiniciar el backend:

   ```
   docker compose restart backend
   ```

   Tarda entre 30 y 60 segundos en quedar `(healthy)`. La interfaz vuelve a encontrarlo sola (T-95).
4. Confirmar con el paso 4 y 5 de la sección 1.

## 3. Muchos errores o lentitud

1. Abrir el panel de Grafana "LogiCode · API" y mirar qué subió: porcentaje de 5xx, latencia p95 o conexiones a PostgreSQL.
2. Buscar una solicitud lenta o fallida en Jaeger (`http://localhost:16686`, servicio `logicode`; solo si `LOGICODE_TRACING_EXPORT=true`). El `traceId` de la traza es el mismo del log, así que se puede buscar la línea exacta:

   ```
   docker compose logs backend | Select-String <traceId>      # PowerShell
   docker compose logs backend | grep <traceId>               # Linux
   ```

3. Si las conexiones activas están en el máximo (10) y hay solicitudes en espera, la base está lenta: revisar si corre un respaldo (2:00 a. m., `BACKUP_HOUR`) o la IA en el mismo equipo.
4. Si la IA está encendida y el equipo se queda sin memoria, apagarla un rato: `docker compose stop ollama`. Las entregas quedan en la cola y se califican cuando vuelva.

## Después del incidente

- Escribir un postmortem sin culpables con la plantilla de [observabilidad.md](../observabilidad.md#postmortem-sin-culpables): línea de tiempo, causa raíz de proceso (no de persona), qué funcionó, qué no y acciones con responsable y fecha.
- Si se gastó más de la mitad del presupuesto de error del mes (3,6 h), se congelan los cambios nuevos hasta corregir la causa (política del SLO).
