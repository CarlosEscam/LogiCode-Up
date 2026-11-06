# Manual técnico · LogiCode

Para quien instala, opera y mantiene LogiCode. La instalación paso a paso está en [instalacion.md](../instalacion.md) y el uso de la web por las cuentas Administrador en el [manual del administrador](manual-administrador.md). El detalle de cada tema está en los documentos enlazados; aquí está lo necesario para operar el piloto.

## 1. Qué es

| Pieza | Tecnología | Dónde |
| --- | --- | --- |
| Interfaz | React 19 (Vite), servida por Nginx, que además termina HTTPS y reenvía `/api` | `frontend/` |
| API | Java 21, Spring Boot 4.1, REST JSON con prefijo `/api/v1`, monolito modular por capas | `backend/`, [arquitectura.md](../arquitectura.md), [api/README.md](../api/README.md) |
| Base de datos | PostgreSQL 17 con migraciones Flyway (versión 10) | [modelo-de-datos.md](../modelo-de-datos.md) |
| Archivos | MinIO en Docker (servicio `minio`, volumen `minio-data`; P-05) | README, "Iniciar" |
| Respaldos | Servicio `backup` con copia diaria y prueba de restauración | [respaldos/README.md](../../respaldos/README.md) |
| IA (opcional) | Ollama + Qwen2.5-VL 7B en Docker, perfil `ia` | README, "Calificación con IA" |
| Correo | Bandeja local `.local/mail-outbox` o Brevo (`docker-compose.brevo.yml`) | README, "Correo real con Brevo" |

Todo corre con Docker Compose en un solo equipo. Servicios: `db`, `minio`, `backend`, `frontend`, `backup` y, con IA, `ollama`. Diagramas de despliegue, módulos y flujo de datos: [arquitectura.md](../arquitectura.md) y [diagramas/](../diagramas/).

## 2. Equipo servidor

- Windows 10/11 con Docker Desktop (WSL 2), o Linux con Docker Engine y Compose v2.24 o superior.
- Sin IA: 8 GB de RAM y unos 10 GB libres más el espacio de los archivos que se carguen.
- Con IA: el equipo de referencia (Ryzen 7 5700U, 32 GB); el modelo ocupa unos 6 GB en disco y en memoria.
- Para el piloto: red local con los equipos del aula, puertos 80 y 443 libres, y una IP que no cambie.

## 3. Instalar y configurar

1. Instalación y variables: [instalacion.md](../instalacion.md) (Windows y Linux, paso a paso) y el [README](../../README.md), secciones "Configuración local" e "Iniciar". Todo se configura en `.env` (fuera de Git); `.env.example` lista las variables sin secretos.
2. Modo piloto con HTTPS: [piloto.md](../piloto.md), sección 2.
3. Secretos: las contraseñas de la base y de MinIO van en `.env`; la API key de Brevo, en `secrets/` como secreto de Docker; las llaves del certificado, en `certificados/`. Ninguno de los tres se sube al repositorio ni se comparte.

### Cuentas iniciales

Las dos cuentas con privilegio completo se crean al arrancar desde `INITIAL_ADMIN_1_*` e `INITIAL_ADMIN_2_*` de `.env`, sin contraseña. Cada una confirma su correo y crea la contraseña con "¿Olvidaste tu contraseña?" ([instalacion.md](../instalacion.md) y README, "Primer ingreso de las cuentas iniciales"). Son las únicas que cambian roles y la plataforma no permite desactivar la última que quede activa.

## 4. Tareas de administración en la web

Menú **Administración** (cuentas Administrador). Paso a paso en el [manual del administrador](manual-administrador.md). Las cuentas Administrador ordinarias no responden actividades, no crean temas, materiales ni actividades, no califican, no ajustan notas y no cambian roles ([permisos.md](../permisos.md)).

| Tarea | Dónde |
| --- | --- |
| Ver cifras de la plataforma, la cola de la IA y el espacio usado | **Administración** (`/admin`) |
| Pedir un respaldo inmediato y ver la prueba de restauración de cada copia | **Administración** → Respaldos → **Crear respaldo ahora** |
| Aprobar solicitudes de docentes | **Usuarios**, filtro "Pendiente de aprobación" → **Aprobar** |
| Buscar, desactivar o reactivar cuentas | **Usuarios** |
| Cambiar el rol de una cuenta | **Usuarios** (solo las dos cuentas iniciales) |
| Habilitar cédulas de estudiantes (una a una o con una lista de Excel o CSV) | **Habilitar estudiantes**. El docente también lo hace desde su curso |
| Revisar ingresos, intentos fallidos, cambios de rol y de contraseña | **Auditoría** |
| Publicar, ocultar o archivar materiales | **Biblioteca** (con sesión de Administrador muestra todos los materiales) |
| Moderar el foro | **Foro**, dentro de cada hilo |

## 5. Operación diaria

| Tarea | Comando o lugar |
| --- | --- |
| Arrancar | `docker compose up -d --wait` (con IA: `docker compose --profile ia up -d --wait`) |
| Estado | `docker compose ps` (todos `healthy`); página de inicio → "Estado de la plataforma"; `http://localhost:8080/actuator/health` en el equipo servidor (`BACKEND_PORT`) |
| Detener sin borrar nada | `docker compose stop` o `docker compose down` |
| Logs | `docker compose logs backend --since 1h`. Cada solicitud deja una línea con su `X-Request-Id` ([calidad.md](../calidad.md#observabilidad)) |
| Métricas | `http://localhost:8080/actuator/metrics/...`, solo desde el equipo servidor |
| Correos en desarrollo | Archivos `.html` en `.local/mail-outbox/` |

**`docker compose down -v` borra la base de datos y todos los archivos.** No se usa en el piloto.

## 6. Respaldos y restauración

- Copia diaria a la hora `BACKUP_HOUR` (hora de Colombia) de la base y los archivos, con prueba de restauración en una base aparte después de cada copia y retención de `BACKUP_RETENTION_DAYS` días.
- El panel de Administración muestra cada copia con "Restauración probada" o el error.
- Guardar fuera del equipo: `docker compose cp backup:/respaldos ./copias-logicode` y llevar la carpeta a un disco cifrado. Las copias contienen datos personales y hashes de contraseñas.
- Restaurar: `docker compose stop backend`, `docker compose run --rm --no-deps backup restaurar <copia>`, `docker compose start backend` ([respaldos/README.md](../../respaldos/README.md)).
- Antes del piloto y después de cada actualización: hacer un respaldo y probar la restauración completa ([piloto.md](../piloto.md), sección 5).

## 7. Actualizar a una versión nueva

1. Respaldo inmediato desde el panel con **Crear respaldo ahora** (y copia fuera del equipo).
2. `git pull <archivo .bundle> main` en la carpeta del proyecto.
3. `docker compose up -d --build --wait`. Flyway aplica las migraciones nuevas al arrancar el backend; si una falla, el backend no arranca y la base queda como estaba.
4. Revisa "Estado de la plataforma" (versión y esquema).

Desde la 1.2.0 los pasos 1, 3 y 4 los hace `node ci/desplegar.mjs`, que además vuelve atrás solo si la versión nueva no queda sana ([ci/README.md](../../ci/README.md)).

Para volver atrás a mano, en este orden: `docker compose stop backend minio`, `docker compose run --rm --no-deps backup restaurar <copia del paso 1>` (con la versión nueva todavía instalada; una base migrada no sirve a una versión anterior, T-96), `git checkout` de la versión anterior y `docker compose up -d --build --wait`.

### Pasar la instalación a otro PC

- Solo el código: un bundle de git ([instalacion.md, 11.1](../instalacion.md#111-recibir-el-código-como-bundle)).
- El código con la base de datos, los archivos subidos y los respaldos: los scripts `exportar.ps1` e `importar.ps1` de [`migracion/`](../../migracion/) y su [guía](../../migracion/Guia-pasar-LogiCode-a-otro-PC.md). El `.env` y `secrets/` se copian a mano por separado.

## 8. Seguridad

- HTTPS en el piloto con cookies `Secure` y `HttpOnly`, CSRF en toda escritura, CSP y cabeceras de seguridad en Nginx, documentación de la API apagada.
- La base (`DB_PORT`) y el backend (`BACKEND_PORT`) solo escuchan en `127.0.0.1`; desde la red solo se llega a Nginx.
- Contraseñas con BCrypt; solo se guardan el hash actual y el anterior. Cinco intentos fallidos bloquean la cédula 15 minutos.
- Los datos de demostración usan una contraseña pública (`Demo#2026`): nunca se cargan en la instalación del piloto.
- Revisión de dependencias contra vulnerabilidades conocidas antes de cada entrega: `npm audit` en `frontend/` y, para Java, la base OSV de Maven contra `./mvnw dependency:list` ([calidad.md](../calidad.md), R9-05). La de la Etapa 10 subió Spring Boot a 4.1.2 y terminó sin hallazgos; la de Excel y PDF (Apache POI 5.5.1 y PDFBox 3.0.8) también. La del 2 de noviembre (179 dependencias, con MinIO y Brevo) también dio 0 avisos.
- Riesgos aceptados y su decisión: [calidad.md](../calidad.md).

## 9. Problemas frecuentes

| Síntoma | Causa probable y solución |
| --- | --- |
| `docker compose up` dice que falta `DB_PASSWORD`, `MINIO_ROOT_PASSWORD` o `LOGICODE_PUBLIC_URL` | Falta la variable en `.env` |
| El backend queda `unhealthy` con "credenciales de MinIO" o "HTTP 403" | `MINIO_ROOT_PASSWORD` cambió después de crear el volumen `minio-data`; vuelve a poner la anterior |
| El frontend no arranca: puerto 80 o 443 en uso | Otro programa usa el puerto (IIS, otro proyecto). Detenlo durante el piloto |
| Los celulares no abren la dirección | Firewall de Windows (piloto.md, paso 4), red marcada como pública, o equipos en otra red o VLAN |
| "La conexión no es privada" | Normal sin la autoridad instalada. Instala `logicode-ca.crt` (piloto.md, sección 3) |
| El aviso de certificado volvió tras cambiar de red | La IP cambió: actualiza `.env`, borra `logicode.crt` y `logicode.key` y reinicia (piloto.md, sección 4) |
| El backend queda `unhealthy` | `docker compose logs backend`: base no disponible, contraseña de la base distinta a la del volumen o una migración que falló |
| No llegan los correos | Sin Brevo quedan en `.local/mail-outbox`. Con Brevo: API key, remitente verificado y límite diario del plan |
| Las entregas se quedan "Calificando con IA" | `docker compose --profile ia ps`; ¿se descargó el modelo? En CPU cada foto tarda minutos; el panel muestra cuántas hay en cola |
| Un respaldo dice ERROR | Ver el detalle en el panel y `docker compose logs backup`. Pedir uno nuevo y no borrar los anteriores |
| Un usuario reporta un error | Pide la hora y, si la hay, el `X-Request-Id`; búscalo en los logs |
