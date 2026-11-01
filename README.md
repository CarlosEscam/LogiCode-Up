# LogiCode

Plataforma web de Pensamiento Computacional para estudiantes de primer semestre · Universidad de Pamplona.
Fecha objetivo de entrega: 10 de diciembre de 2026.

> **Estado actual: Etapa 10 (piloto y entrega) preparada, pendiente de revisión y del piloto con el docente.** LogiCode tiene acceso y permisos, cursos, temas y biblioteca, actividades y entregas, calificación manual, automática (PSeInt y quizzes) y con IA (pruebas de aceptación de Qwen pendientes en el equipo de referencia), juegos, foro, planilla, panel de administración y respaldos con prueba de restauración (Etapas 3 a 9). La Etapa 10 agregó el modo piloto con HTTPS para usarlo desde los equipos y celulares del aula, la página de inicio con misión y visión, la actualización de Spring Boot por vulnerabilidades y los manuales técnico, docente y estudiante. Versión 1.0.0. Detalle en [docs/estado.md](docs/estado.md).

## Documentación

Empieza por **[docs/README.md](docs/README.md)**: el índice de toda la documentación según quién lee (directivo, docente, estudiante, administrador o desarrollador).

| Documento | Contenido |
| --- | --- |
| [docs/README.md](docs/README.md) | Índice de la documentación por tipo de lector |
| [docs/vision-general.md](docs/vision-general.md) | Qué es LogiCode, roles, funciones, recorrido de un curso, estado y glosario |
| [docs/instalacion.md](docs/instalacion.md) | Instalación paso a paso en Windows o Linux y cómo pasar LogiCode a otro PC |
| [docs/casos-de-uso.md](docs/casos-de-uso.md) y [docs/historias-de-usuario.md](docs/historias-de-usuario.md) | Casos de uso e historias con criterios de aceptación |
| [LogiCode - Especificacion para Claude Code.md](LogiCode%20-%20Especificacion%20para%20Claude%20Code.md) | Requisitos, reglas de negocio, arquitectura y etapas (fuente de verdad) |
| [docs/estado.md](docs/estado.md) | Qué está implementado, qué está pendiente |
| [docs/decisiones.md](docs/decisiones.md) | Decisiones aprobadas, decisiones técnicas tomadas y preguntas abiertas |
| [docs/arquitectura.md](docs/arquitectura.md) | Monolito modular, capas, módulos y reglas de dependencia |
| [docs/api/README.md](docs/api/README.md) | Convenciones REST y catálogo de endpoints |
| [docs/modelo-de-datos.md](docs/modelo-de-datos.md) | Convenciones de base de datos y esquema actual |
| [docs/permisos.md](docs/permisos.md) | Matriz de roles y privilegio completo |
| [docs/calidad.md](docs/calidad.md) | Defectos, riesgos aceptados, revisión de seguridad y observabilidad |
| [docs/piloto.md](docs/piloto.md) | Despliegue del piloto con HTTPS, plan, registro de observaciones y acta de aceptación |
| [docs/manuales/](docs/manuales/) | Manuales [técnico](docs/manuales/manual-tecnico.md), [del administrador](docs/manuales/manual-administrador.md), [docente](docs/manuales/manual-docente.md) y [estudiante](docs/manuales/manual-estudiante.md) |
| [docs/diagramas/](docs/diagramas/) | DFD, modelo de datos, secuencia y estados de una entrega (Mermaid e imagen) |

## Requisitos previos

Para ejecutar todo con Docker (recomendado):

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) con Docker Compose v2 (en Windows, con WSL 2).
- Git.

Para desarrollar fuera de Docker (opcional):

- JDK 21 (por ejemplo, Eclipse Temurin 21). Maven no es necesario: se usa el wrapper `mvnw`.
- Node.js 22 LTS y npm.
- Docker sigue siendo necesario para PostgreSQL y para la prueba de integración del backend.

## Configuración local

1. Copia el archivo de variables de ejemplo y cambia la contraseña de la base de datos:

   ```bash
   cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
   ```

2. Edita `.env`: define `DB_PASSWORD` con una contraseña propia y completa `INITIAL_ADMIN_1_*` e `INITIAL_ADMIN_2_*` con la cédula, el nombre y el correo de las dos cuentas iniciales. El archivo `.env` está en `.gitignore` y nunca debe subirse al repositorio.

### Variables de entorno

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `DB_NAME` | `logicode` | Nombre de la base de datos |
| `DB_USERNAME` | `logicode` | Usuario de PostgreSQL |
| `DB_PASSWORD` | *(obligatoria)* | Contraseña de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL en `127.0.0.1` |
| `BACKEND_PORT` | `8080` | Puerto del backend en `127.0.0.1` |
| `FRONTEND_PORT` | `3000` | Puerto de la aplicación web |
| `LOGICODE_TIME_ZONE` | `America/Bogota` | Zona horaria de las reglas de negocio |
| `DB_URL` | `jdbc:postgresql://localhost:5432/logicode` | Solo al ejecutar el backend fuera de Docker |
| `LOGICODE_ENVIRONMENT` | `local` | Nombre del entorno (informativo) |
| `INITIAL_ADMIN_1_*`, `INITIAL_ADMIN_2_*` | *(vacías)* | Cédula (`_CEDULA`), nombre (`_NAME`) y correo (`_EMAIL`) de las dos cuentas iniciales con privilegio completo. Solo en `.env` |
| `LOGICODE_PUBLIC_URL` | `http://localhost:3000` | Dirección de la interfaz que aparece en los enlaces de los correos |
| `SESSION_TIMEOUT` | `2h` | Tiempo sin actividad tras el cual se cierra la sesión |
| `SESSION_COOKIE_SECURE` | `false` | `true` solo cuando la plataforma se sirva por HTTPS |
| `LOGICODE_MAIL_PROVIDER` | `outbox` | `outbox` guarda los correos en `.local/mail-outbox`; `brevo` los envía (ver abajo) |
| `BREVO_SENDER_EMAIL` | `logicode.notificaciones@gmail.com` | Remitente de los correos con Brevo (P-06); debe estar verificado en la cuenta de Brevo |
| `BREVO_SENDER_NAME` | `LogiCode` | Nombre del remitente en Brevo |
| `MINIO_ROOT_USER` | `logicode` | Usuario de MinIO (solo dentro de Docker) |
| `MINIO_ROOT_PASSWORD` | *(obligatoria)* | Contraseña de MinIO, mínimo 8 caracteres. Solo en `.env` |
| `LOGICODE_FILE_RETENTION_MONTHS` | `6` | Meses que se conserva cada archivo cargado (mínimo 6) |
| `LOGICODE_FILES_STORAGE` | `local` | Solo al ejecutar el backend fuera de Docker: `local` guarda en `LOGICODE_FILES_DIR`; `minio` usa MinIO en `LOGICODE_MINIO_URL`. En Docker siempre es `minio` |
| `LOGICODE_FILES_DIR` | `file-storage` | Solo al ejecutar el backend fuera de Docker: carpeta de los archivos cargados (o de las cargas en curso con MinIO) |
| `LOGICODE_AI_ENABLED` | `false` | `true` califica con IA (ver "Calificación con IA") |
| `LOGICODE_AI_MODEL` | `qwen2.5vl:7b` | Modelo de Ollama |
| `LOGICODE_AI_TIMEOUT` | `10m` | Tiempo máximo por evaluación |
| `LOGICODE_AI_MAX_ATTEMPTS` | `3` | Intentos ante fallos técnicos antes de pasar la entrega al docente |
| `LOGICODE_AI_OLLAMA_URL` | `http://localhost:11434` | Solo al ejecutar el backend fuera de Docker (en Docker es `http://ollama:11434`) |
| `BACKUP_HOUR` | `2` | Hora de Colombia (0 a 23) del respaldo diario |
| `BACKUP_RETENTION_DAYS` | `30` | Días que se guarda cada respaldo |
| `LOGICODE_BACKUPS_DIR` | `backups` | Solo al ejecutar el backend fuera de Docker: carpeta donde lee los respaldos |

La API key de Brevo es un **secreto de Docker**, nunca una variable de `.env` (ver "Correo real con Brevo"). El remitente no es secreto y va en `.env`.

## Iniciar

### Todo con Docker

```bash
docker compose up --build
```

| Dirección | Qué es |
| --- | --- |
| http://localhost:3000 | Aplicación web (muestra el estado del servidor y la base de datos) |
| http://localhost:3000/api/v1/system/info | API a través de Nginx |
| http://localhost:3000/swagger-ui.html | Documentación interactiva de la API |
| http://localhost:8080/actuator/health | Estado del backend |

Detener: `docker compose down` (conserva los datos) · Borrar también la base de datos y los archivos cargados: `docker compose down -v`.

Los archivos de los materiales y las entregas se guardan en MinIO (servicio `minio`, volumen `minio-data`), que solo es accesible desde el backend; quien los ve o descarga lo sigue decidiendo LogiCode. Al pasar de una versión anterior, el backend sube solo a MinIO los archivos que había en el volumen `file-storage` y los borra de allí cuando comprueba cada uno. El servicio `backup` respalda cada día la base de datos y los archivos en el volumen `backups` y prueba que cada copia se pueda restaurar; desde la web (Administración) se ve el estado y se pide un respaldo inmediato. Cómo copiarlos fuera del equipo y cómo restaurar: [respaldos/README.md](respaldos/README.md).

### Piloto con HTTPS

Para usar LogiCode desde otros equipos y celulares de la red (el piloto), se suma `docker-compose.piloto.yml`: HTTPS en el puerto 443 con un certificado local que se crea solo, cookies seguras y la documentación de la API apagada, en una base de datos aparte de la de desarrollo. Pasos, variables (`COMPOSE_PROJECT_NAME`, `COMPOSE_FILE`, `LOGICODE_PUBLIC_URL`, `LOGICODE_TLS_HOSTS`) y cómo confiar en el certificado: [docs/piloto.md](docs/piloto.md).

### Datos de demostración

Para probar o presentar con datos ficticios (cuentas, curso, materiales, actividades, entregas y notas), con la aplicación corriendo: `node datos-demo/cargar-datos-demo.mjs`. Las cuentas y lo que contiene cada etapa están en [datos-demo/README.md](datos-demo/README.md).

### Primer ingreso de las cuentas iniciales

Las cuentas iniciales se crean solas al arrancar el backend, sin contraseña:

1. Abre el correo de verificación. En desarrollo no se envía: queda como archivo `.html` en `.local/mail-outbox/` (ábrelo con el navegador). Si se perdió o venció, reinicia el backend (`docker compose restart backend`) y llega uno nuevo.
2. El enlace confirma el correo. Luego entra a "¿Olvidaste tu contraseña?", escribe el mismo correo y abre el nuevo archivo de `.local/mail-outbox/` para crear la contraseña.
3. Inicia sesión con la cédula y la contraseña.

En Linux, si el backend no puede escribir en `.local/mail-outbox`, crea la carpeta antes de arrancar y dale permiso de escritura (`mkdir -p .local/mail-outbox && chmod a+w .local/mail-outbox`). En Windows y macOS no hace falta.

### Correo real con Brevo

1. Crea `secrets/brevo_api_key.txt` con la API key. La carpeta `secrets/` está fuera de Git. El remitente es `BREVO_SENDER_EMAIL` en `.env` (`logicode.notificaciones@gmail.com`, P-06) y debe estar verificado como remitente en Brevo (Brevo envía un código a ese correo).
2. Arranca con el archivo adicional:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.brevo.yml up --build
   ```

### Calificación con IA

La IA viene apagada: las entregas van a Por revisar para calificación manual. Para encenderla (Windows, desde la carpeta del proyecto):

1. En `.env`, pon `LOGICODE_AI_ENABLED=true`.
2. Arranca también Ollama y descarga el modelo una sola vez (unos 6 GB, quedan en el volumen `ollama-models`):

   ```powershell
   docker compose --profile ia up -d --build --wait
   docker compose exec ollama ollama pull qwen2.5vl:7b
   docker compose exec ollama ollama show qwen2.5vl:7b
   ```

   `ollama show` debe indicar `Q4_K_M`, la variante aprobada.
3. Desde entonces, arranca siempre con `docker compose --profile ia up -d`. Para apagarla, vuelve a `false` y reinicia el backend; lo que estaba en cola pasa al docente.

Con la IA encendida, la entrega aparece como "Calificando con IA" y se procesa de a una. En el equipo de referencia (Ryzen 7 5700U, 32 GB, sin GPU) una foto puede tardar varios minutos. Las pruebas de aceptación del modelo se ejecutan con [ia-pruebas/README.md](ia-pruebas/README.md).

### Desarrollo con recarga rápida

```bash
# 1. Solo la base de datos
docker compose up -d db

# 2. Backend (otra terminal). Lee DB_PASSWORD del entorno.
cd backend
export DB_PASSWORD=<la misma de .env>        # PowerShell: $env:DB_PASSWORD="<la misma de .env>"
./mvnw spring-boot:run                       # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend (otra terminal)
cd frontend
npm install
npm run dev                                  # http://localhost:5173 (reenvía /api al backend)
```

## Pruebas y calidad

```bash
cd backend && ./mvnw verify      # unitarias, controlador, arquitectura e integración con PostgreSQL (requiere Docker)
cd frontend && npm run lint && npm test && npm run build
```

La prueba de arquitectura (`LayeredArchitectureTest`) falla si una capa rompe las reglas de dependencia descritas en [docs/arquitectura.md](docs/arquitectura.md). La integración continua (`.github/workflows/ci.yml`) ejecuta lo mismo en cada push y pull request.

## Estructura del proyecto

```
.
├── backend/                         API REST · Java 21 · Spring Boot 4.1 · Maven
│   ├── src/main/java/co/edu/unipamplona/logicode/
│   │   ├── shared/                  Transversal: rutas /api/v1, errores, reloj, X-Request-Id
│   │   ├── system/                  Módulo de referencia con las 4 capas (api, application, domain, infrastructure)
│   │   ├── identity/                Etapa 3: cuentas, sesión, correo, recuperación, roles, auditoría
│   │   ├── files/                   Etapa 4: carga y validación de archivos, almacenamiento local
│   │   ├── courses/                 Etapa 4: cursos e inscripción por cédula
│   │   ├── content/                 Etapa 4: temas, materiales, biblioteca, vista previa y descarga
│   │   ├── activities/              Etapa 5: actividades, entregas y excepciones de plazo
│   │   ├── grading/                 Etapas 5-7: rúbricas versionadas, notas, Por revisar, historial, calificación con IA y con casos de prueba
│   │   ├── pseint/                  Etapa 7: intérprete de PSeInt con límites
│   │   ├── quizzes/                 Etapa 7: banco de preguntas, quizzes, intentos y ranking
│   │   ├── forum/                   Etapa 8: foro por herramienta con moderación
│   │   └── administration/          Etapa 8: panel de administración y respaldos
│   ├── src/main/resources/
│   │   ├── application.yml          Configuración por variables de entorno
│   │   └── db/migration/            Migraciones Flyway (V1 línea base, V2 identidad y acceso, V3 cursos y biblioteca, V4 actividades y entregas, V5 calificación con IA, V6 quizzes y PSeInt, V7 foro, V8 retos de bloques)
│   ├── src/test/                    Pruebas (JUnit, Testcontainers, ArchUnit)
│   └── Dockerfile
├── frontend/                        React 19 · Vite · JavaScript
│   ├── src/app                      Rutas y navegación según permisos
│   ├── src/features                 auth, account, admin, courses, content, activities, quizzes, pseint, games, forum, gradebook, system
│   ├── src/shared                   Cliente HTTP con CSRF, enrutador, formularios
│   ├── nginx/                       Nginx: sitio común, modo HTTP y modo HTTPS del piloto (crea el certificado local)
│   └── Dockerfile
├── datos-demo/                      Script de datos de demostración (se amplía en cada etapa)
├── respaldos/                       Script del servicio de respaldos y cómo restaurar
├── ia-pruebas/                      Arnés de pruebas de aceptación de la IA (muestras y resultados fuera de Git)
├── migracion/                       Scripts para pasar una instalación con sus datos a otro PC (Windows)
├── docs/                            Índice, visión, instalación, diseño técnico, manuales, diagramas, estado y decisiones
├── .github/workflows/ci.yml         Integración continua
├── docker-compose.yml               db + backend + frontend + backup (+ ollama con el perfil ia)
├── docker-compose.brevo.yml         Envío real de correos con Brevo (remitente y API key)
├── docker-compose.piloto.yml        Piloto: HTTPS, cookies seguras, API sin documentación pública
├── certificados/                    Certificado y autoridad local del piloto (se crean solos, fuera de Git)
└── .env.example                     Variables de ejemplo (sin secretos)
```

## Flujo de trabajo con Git

- `main` siempre compila y arranca.
- Cada cambio se hace en una rama `feature/<etapa>-<tema>` (por ejemplo `feature/e3-login`) y entra a `main` por pull request con la CI en verde.
- Mensajes de commit en español, en imperativo y breves (`Agrega migración de cuentas de usuario`).
- Nunca se suben `.env`, contraseñas, tokens ni llaves de Brevo.

## Siguiente paso

Revisar la Etapa 10: [docs/piloto.md](docs/piloto.md) y los [manuales](docs/manuales/). Después, preparar el equipo servidor, probar allí la restauración y hacer el piloto con el docente hasta firmar el acta de aceptación (entrega objetivo: 10 de diciembre de 2026). Antes del piloto conviene activar Brevo con el remitente decidido (P-06, `logicode.notificaciones@gmail.com`): verificarlo en Brevo, poner la API key y probar un envío real; sin eso los correos de verificación y recuperación no llegan a los estudiantes. Los archivos ya se guardan en MinIO (P-05): al actualizar hay que agregar `MINIO_ROOT_PASSWORD` a `.env`. Siguen pendientes las pruebas de aceptación de la IA en el equipo de referencia ([ia-pruebas/README.md](ia-pruebas/README.md)) y la decisión P-09 de [docs/decisiones.md](docs/decisiones.md). Los retos de bloques (RF-12), Excel y PDF (P-07, P-08) y la documentación completa ([docs/README.md](docs/README.md)) están entregados y esperan revisión.
