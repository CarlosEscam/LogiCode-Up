# Arquitectura técnica de LogiCode

> Diseño técnico mínimo (Etapa 1). Fuente de requisitos: `LogiCode - Especificacion para Claude Code.md`, sección 7.
> Estado de cada pieza: ver [estado.md](estado.md).

## Vista general

LogiCode es un **monolito modular**: un único backend Spring Boot con módulos internos, una interfaz React y una base de datos PostgreSQL, todo ejecutado con Docker Compose en local.

```mermaid
flowchart LR
    U[Navegador] -->|HTTP :3000| N[Nginx + React compilado]
    N -->|/api/v1/*| B[Backend Spring Boot :8080]
    B -->|JDBC| P[(PostgreSQL 17)]
    B -->|disco local| F[(Almacenamiento de archivos)]
    B -->|HTTPS| M[Brevo · correo transaccional]
    B -.->|HTTP :11434, perfil ia| O[Ollama + Qwen2.5-VL 7B]
```

La línea punteada es opcional: Ollama solo corre con `docker compose --profile ia` y `LOGICODE_AI_ENABLED=true` (T-45). El correo usa `outbox` (archivos) mientras no se configure el remitente de Brevo (P-06).

| Capa (especificación) | Implementación en el repositorio | Estado |
| --- | --- | --- |
| 1. Presentación | `frontend/` React 19 + Vite, JavaScript, CSS propio responsive | Implementada la base |
| 2. Borde y entrega | Nginx en la imagen del frontend: sirve estáticos y reenvía `/api` al backend | Implementada la base (sin TLS: solo local) |
| 3. API e integración | Controladores REST con prefijo `/api/v1`, JSON, errores RFC 9457, OpenAPI (springdoc) | Implementada la base |
| 4. Lógica de negocio | Paquetes `application` y `domain` de cada módulo | Implementada hasta la Etapa 6 (identidad, cursos, contenido, actividades, calificación) |
| 5. Tareas lentas | Worker en el mismo backend con tabla de trabajos en PostgreSQL (`ai_grading_job`) | Implementada para la calificación con IA (Etapa 6) |
| 6. Acceso a datos | Spring Data JPA/Hibernate + Flyway | Implementada (migraciones V1 a V5) |
| 7. Almacenamiento | PostgreSQL; archivos en almacenamiento de objetos o volumen local | PostgreSQL y disco local (volumen de Docker, P-05) |
| 8. Infraestructura | `docker-compose.yml`: `db`, `backend`, `frontend` con health checks | Implementada |

## Backend: módulos y capas

Paquete raíz: `co.edu.unipamplona.logicode`. Cada módulo funcional es un paquete hijo directo y se divide en las cuatro capas de la especificación:

```
co.edu.unipamplona.logicode
├── shared/            Transversal: rutas de la API, errores, reloj del negocio, ID de solicitud
├── system/            Módulo de referencia: GET /api/v1/system/info (usa las 4 capas)
│   ├── api/             SystemController, SystemInfoResponse
│   ├── application/     GetSystemInfo (caso de uso), SchemaVersionPort (puerto)
│   ├── domain/          SystemInfo
│   └── infrastructure/  FlywaySchemaVersionAdapter (implementa el puerto)
├── identity/          Etapa 3 · identidad, sesión, correo, recuperación, roles, habilitación de cédulas, auditoría
│   ├── api/             AuthController, MeController, AdminUserController, StudentEnablementController
│   ├── application/     Un caso de uso por clase (RegisterStudent, AuthenticateUser, ResetPassword…) y puertos
│   ├── domain/          UserAccount, OneTimeToken, RecoveryRequest, AccessPolicy, PasswordPolicy…
│   └── infrastructure/  persistence/ (Spring Data), security/ (Spring Security, BCrypt, sesiones), mail/ (outbox, Brevo)
├── files/             Etapa 4 · carga, validación del tipo real, almacenamiento y entrega de archivos
│   ├── application/     FileStore (caso de uso), AcceptedFiles, BinaryStorage y StoredFileRepository (puertos)
│   ├── domain/          StoredFile, FileFormat (formatos, límites y firmas)
│   └── infrastructure/  LocalBinaryStorage (volumen file-storage), persistencia
├── courses/           Etapa 4 · cursos, docente responsable, inscripción por cédula, reglas de acceso (CourseAccess)
├── content/           Etapa 4 · temas, materiales, próxima clase, biblioteca, vista previa y descarga
├── activities/        Etapa 5 · actividades, plazos, entregas, excepciones; define el puerto GradingGateway
├── grading/           Etapas 5-6 · rúbricas versionadas, notas, Por revisar, historial, calificación con IA (planilla en la 8)
├── pseint/           Etapa 7 · intérprete de PSeInt con límites (domain puro) y RunPrograms (ejecución y casos de prueba)
├── quizzes/           Etapa 7 · banco de preguntas, quizzes, intentos, envío automático y ranking
├── forum/             Etapa 8 · foro y moderación
└── administration/    Etapas 3, 8 · usuarios, aprobación docente, respaldos
```

La habilitación de cédulas quedó en `identity` porque es requisito del registro; la inscripción por curso está en `courses`, que usa `identity` para habilitar la cédula al inscribir. `content` usa `courses` (quién ve o administra un curso) y `files` (archivos); las dependencias van en un solo sentido: content → courses → identity, y content → files. `activities` usa `courses` y `files`; `grading` usa `activities` (a través de `ActivityCatalog`) y `courses`, e implementa el puerto `GradingGateway` que `activities` necesita para pedir la rúbrica y registrar cada entrega (T-30). `Role` y `Permission` viven en `shared.domain` (T-29). En la Etapa 7, `quizzes` usa `activities` (el quiz es una actividad) y `grading` (guarda su rúbrica); `grading` usa `pseint` para los casos de prueba. Los juegos no tienen módulo en el backend: corren en el navegador (`frontend/src/features/games`, T-59). Los módulos pendientes solo contienen `package-info.java` describiendo su responsabilidad; no tienen código.

### Responsabilidad de cada capa

| Capa | Contiene | No contiene |
| --- | --- | --- |
| `api` | Controladores, DTO de entrada/salida, validación de formato (`@Valid`), traducción a códigos HTTP | Reglas de negocio, SQL, repositorios |
| `application` | Casos de uso (un verbo por clase: `CreateActivity`, `SubmitWork`), transacciones, autorización por caso de uso, puertos (interfaces) hacia infraestructura | Detalles HTTP, implementaciones técnicas |
| `domain` | Entidades, objetos de valor, invariantes y políticas puras (p. ej. escala 0,0-5,0, regla del 80 %) | Spring, acceso a datos, HTTP |
| `infrastructure` | Repositorios JPA, adaptadores (correo Brevo, IA Ollama, almacenamiento de archivos), configuración técnica | Decisiones de negocio |

### Reglas de dependencia (verificadas automáticamente)

`backend/src/test/java/.../architecture/LayeredArchitectureTest.java` (ArchUnit) falla la compilación si:

1. `domain` depende de Spring o de otra capa. Las anotaciones `jakarta.persistence` sí se permiten (T-19).
2. `application` depende de `api` o de `infrastructure` (usa puertos).
3. `api` depende de `infrastructure`.
4. Un `@RestController` está fuera de `api`.
5. `api` usa JDBC, Spring Data o JPA directamente.

6. Un módulo usa `api`, `domain` o `infrastructure` de otro módulo. Entre módulos solo se usa la capa `application` (casos de uso o interfaces públicas); `shared` está disponible para todos.

## Decisiones transversales ya implementadas

| Tema | Implementación |
| --- | --- |
| Prefijo de API | `ApiPaths.V1 = "/api/v1"` en todos los controladores |
| Errores | Problem Details (RFC 9457) en JSON con mensajes en español (`GlobalExceptionHandler`); nunca se devuelve la traza |
| Zona horaria | Datos en UTC (`TIMESTAMPTZ`, JVM en UTC). Reglas por "día calendario en Colombia" usan el `Clock` del negocio (`America/Bogota`, configurable con `LOGICODE_TIME_ZONE`) |
| Trazabilidad | `RequestIdFilter`: cabecera `X-Request-Id` en cada respuesta y en cada línea de log; en Docker los logs son JSON estructurado (ECS) |
| Health checks | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` (incluye la base de datos) |
| OpenAPI | `/api/v1/openapi` (JSON) y `/swagger-ui.html` |
| Configuración | Variables de entorno con valores por defecto seguros para desarrollo; secretos fuera del repositorio |
| Seguridad (Etapa 3) | Spring Security con sesión en PostgreSQL (Spring Session JDBC), cookie HttpOnly, CSRF para SPA, `ActiveAccountFilter` que recarga la cuenta en cada petición (una cuenta desactivada pierde el acceso de inmediato) |
| Correo (Etapa 3) | Puerto `EmailSender`; se envía tras confirmar la transacción y en segundo plano; proveedor `outbox` (archivos) o `brevo` |

## Frontend

```
frontend/src
├── app/          Raíz: tabla de rutas, protección por sesión y permiso, barra de navegación
├── pages/        Páginas generales (HomePage provisional)
├── features/     Funcionalidades por módulo: system/, auth/, account/, admin/, courses/, content/, activities/ (actividades, entregas, rúbrica, calificación)
├── shared/api/   Cliente HTTP (httpClient.js): prefijo /api/v1, CSRF, errores RFC 9457; consultas paginadas
├── shared/router/ Enrutador mínimo con la History API (T-20)
├── shared/ui/    Campos de formulario, mensajes, paginador
├── styles/       Estilos globales
└── test/         Configuración de pruebas (Vitest + Testing Library)
```

Regla: la interfaz muestra estado y recoge acciones; **no decide permisos, notas oficiales ni reglas del curso**. Toda autorización se aplica en el backend.

En desarrollo, Vite reenvía `/api` a `http://localhost:8080`; en Docker lo hace Nginx. En ambos casos el navegador ve un único origen, por lo que no se configura CORS.

## Procesos en segundo plano (Etapa 6)

Tabla de trabajos en PostgreSQL y worker dentro del mismo backend, como pide la especificación; RabbitMQ queda como opción de crecimiento (T-38).

```mermaid
sequenceDiagram
    participant E as Estudiante
    participant API as Backend (API)
    participant DB as PostgreSQL
    participant W as Worker (mismo backend)
    participant O as Ollama
    E->>API: POST /activities/{id}/submissions
    API->>DB: entrega + nota PROCESSING + ai_grading_job PENDING
    API-->>E: 201 (la interfaz consulta cada 10 s)
    loop cada 5 s
        W->>DB: toma un trabajo (FOR UPDATE SKIP LOCKED, token de toma)
    end
    W->>O: /api/chat con rúbrica, texto e imágenes (fuera de transacción)
    O-->>W: JSON con puntos por criterio, legibilidad y confianza
    W->>DB: ai_evaluation + nota PUBLISHED o PENDING_REVIEW
```

- **Toma:** `lockNext` usa `FOR UPDATE SKIP LOCKED`; cada toma recibe un `claim_token`. Un trabajo `RUNNING` cuyo bloqueo pasó de `timeout + 2 min` se retoma, y el resultado viejo se descarta porque su token ya no coincide.
- **Reintentos:** un fallo técnico se reintenta hasta `LOGICODE_AI_MAX_ATTEMPTS` veces con espera `RETRY_DELAY × intento`; al agotarse, la nota pasa a Por revisar con motivo `AI_FAILED` y sin nota (RN-11).
- **Decisión:** el dominio (`AiGradingPolicy`) calcula la nota con la rúbrica y publica solo si el contenido es legible y la confianza es de 80 % o más; si no, la propuesta espera al docente (RN-09, RN-10).
- **Sin IA:** con `LOGICODE_AI_ENABLED=false` no hay worker; las entregas van a Por revisar como `AI_DISABLED` y los trabajos pendientes se liberan al arrancar (`AiQueueRelease`).
- **Integración:** el puerto `GradingProvider` (aplicación) lo implementa `OllamaGradingAdapter` (infraestructura). Las fotos se enderezan y reducen en `ImagePreparer`.
