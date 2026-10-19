# LogiCode

Plataforma web de Pensamiento Computacional para estudiantes de primer semestre · Universidad de Pamplona.
Fecha objetivo de entrega: 10 de diciembre de 2026.

> **Estado actual: fundación técnica (Etapas 1 y 2).** La aplicación arranca completa (React, Spring Boot, PostgreSQL) pero **todavía no tiene funcionalidades de negocio**: autenticación, cursos, biblioteca, actividades, calificación con IA, quizzes, juegos y foro están pendientes. Detalle en [docs/estado.md](docs/estado.md).

## Documentación

| Documento | Contenido |
| --- | --- |
| [LogiCode - Especificacion para Claude Code.md](LogiCode%20-%20Especificacion%20para%20Claude%20Code.md) | Requisitos, reglas de negocio, arquitectura y etapas (fuente de verdad) |
| [docs/estado.md](docs/estado.md) | Qué está implementado, qué está pendiente |
| [docs/decisiones.md](docs/decisiones.md) | Decisiones aprobadas, decisiones técnicas tomadas y preguntas abiertas |
| [docs/arquitectura.md](docs/arquitectura.md) | Monolito modular, capas, módulos y reglas de dependencia |
| [docs/api/README.md](docs/api/README.md) | Convenciones REST y catálogo de endpoints |
| [docs/modelo-de-datos.md](docs/modelo-de-datos.md) | Convenciones de base de datos y propuesta de modelo para la Etapa 3 |
| [docs/permisos.md](docs/permisos.md) | Matriz de roles y privilegio completo |

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

2. Edita `.env` y define `DB_PASSWORD` con una contraseña propia. El archivo `.env` está en `.gitignore` y nunca debe subirse al repositorio.

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
| `INITIAL_ADMIN_1_*`, `INITIAL_ADMIN_2_*` | *(vacías)* | Cédula, nombre y correo de las dos cuentas iniciales con privilegio completo. Solo en `.env`; se usarán desde la Etapa 3 |

Las credenciales de Brevo (Etapa 3) se configurarán como **secretos de Docker**, no en `.env`.

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

Detener: `docker compose down` (conserva los datos) · Borrar también la base de datos local: `docker compose down -v`.

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
│   │   └── identity/ courses/ content/ activities/ grading/
│   │       quizzes/ games/ forum/ administration/   Módulos pendientes (solo descripción)
│   ├── src/main/resources/
│   │   ├── application.yml          Configuración por variables de entorno
│   │   └── db/migration/            Migraciones Flyway (V1__linea_base.sql)
│   ├── src/test/                    Pruebas (JUnit, Testcontainers, ArchUnit)
│   └── Dockerfile
├── frontend/                        React 19 · Vite · JavaScript
│   ├── src/app, pages, features, shared/api, styles
│   ├── nginx.conf                   Sirve la interfaz y reenvía /api al backend
│   └── Dockerfile
├── docs/                            Diseño técnico, estado y decisiones
├── .github/workflows/ci.yml         Integración continua
├── docker-compose.yml               db + backend + frontend
└── .env.example                     Variables de ejemplo (sin secretos)
```

## Flujo de trabajo con Git

- `main` siempre compila y arranca.
- Cada cambio se hace en una rama `feature/<etapa>-<tema>` (por ejemplo `feature/e3-login`) y entra a `main` por pull request con la CI en verde.
- Mensajes de commit en español, en imperativo y breves (`Agrega migración de cuentas de usuario`).
- Nunca se suben `.env`, contraseñas, tokens ni llaves de Brevo.

## Siguiente paso

Las decisiones previas a la Etapa 3 (D-01 a D-04) están aprobadas en [docs/decisiones.md](docs/decisiones.md). Queda iniciar la **Etapa 3 · Acceso y permisos** cuando se apruebe el cierre de la Etapa 2.
