# Modelo de datos

## Convenciones (aplican desde ya)

| Tema | Convención |
| --- | --- |
| Migraciones | Flyway en `backend/src/main/resources/db/migration`, nombre `V<n>__<descripcion_en_español>.sql`. Una migración aplicada **nunca** se edita; los cambios van en una nueva |
| Nombres | Tablas y columnas en inglés, `snake_case`, tablas en singular (`user_account`, `activity`) |
| Claves | `id UUID` como clave primaria |
| Fechas | `TIMESTAMPTZ` (se guardan en UTC). Una fecha de negocio "en Colombia" se guarda como `DATE` calculada con `America/Bogota` cuando una regla la necesita |
| Notas | `NUMERIC(2,1)` con `CHECK (valor BETWEEN 0.0 AND 5.0)` |
| Porcentajes | `NUMERIC(5,2)` entre 0 y 100 |
| Enumeraciones | `VARCHAR` con `CHECK` (más simple de migrar que tipos `ENUM` de PostgreSQL) |
| Auditoría | Columnas `created_at` y `updated_at` en tablas de negocio |
| Archivos | Nunca binarios en la base de datos: solo metadatos y la clave del objeto en el almacenamiento |
| Secretos | Contraseñas y tokens solo como hash; nunca en texto plano ni en logs |

## Esquema actual (implementado)

| Migración | Contenido |
| --- | --- |
| `V1__linea_base.sql` | Línea base: no crea tablas de negocio. Flyway crea su tabla `flyway_schema_history` |
| `V2__identidad_y_acceso.sql` | Etapa 3: cuentas, tokens de un solo uso, límites de recuperación, cédulas habilitadas, auditoría y sesiones (`spring_session`, `spring_session_attributes`) |
| `V3__cursos_temas_y_biblioteca.sql` | Etapa 4: cursos, inscripciones, archivos cargados, temas y materiales |
| `V4__actividades_y_entregas.sql` | Etapa 5: actividades, entregas y sus archivos, excepciones de plazo, versiones de rúbrica con criterios, notas e historial de notas |
| `V5__calificacion_con_ia.sql` | Etapa 6: estado `PROCESSING` y motivo de revisión en las notas, cola de evaluación con IA, evaluaciones de la IA con sus puntos por criterio |

## Identidad y acceso (Etapa 3, implementado)

```mermaid
erDiagram
    user_account ||--o{ one_time_token : "recibe"
    user_account ||--o{ recovery_request : "solicita"
    user_account ||--o{ student_enablement : "habilita"
    student_enablement |o--o| user_account : "se registra como"

    user_account {
        uuid id PK
        varchar cedula UK "solo dígitos, 3 a 15"
        varchar full_name
        varchar role "STUDENT | TEACHER | ADMIN"
        varchar status "PENDING_APPROVAL | ACTIVE | DISABLED"
        boolean full_privileges "solo ADMIN; solo las 2 cuentas iniciales"
        varchar email UK "único sin distinguir mayúsculas"
        timestamptz email_verified_at
        varchar password_hash "BCrypt, actual"
        varchar previous_password_hash "BCrypt, inmediatamente anterior"
        timestamptz password_changed_at
        bigint version "bloqueo optimista"
    }
    one_time_token {
        uuid id PK
        uuid user_id FK
        varchar purpose "EMAIL_VERIFICATION | PASSWORD_RESET"
        varchar token_hash UK "SHA-256; el token solo viaja en el enlace"
        varchar target_email "correo al que se envió; en cambio de correo, el nuevo"
        timestamptz expires_at "60 min recuperación, 24 h verificación"
        timestamptz used_at "un solo uso"
    }
    recovery_request {
        uuid id PK
        uuid user_id FK
        varchar kind "PASSWORD_RESET | EMAIL_CHANGE"
        date business_date "día calendario en Colombia"
        varchar outcome "ACCEPTED | REJECTED | LIMIT_REACHED"
    }
    student_enablement {
        uuid id PK
        varchar cedula UK
        varchar full_name "opcional"
        uuid enabled_by FK
        uuid registered_user_id FK "cuando el estudiante se registra"
    }
    audit_event {
        uuid id PK
        varchar action
        uuid actor_id
        uuid target_user_id
        varchar subject "p. ej. la cédula usada al iniciar sesión"
        varchar outcome "SUCCESS | FAILURE | DENIED | LIMIT_REACHED"
        varchar request_id
        text details "sin contraseñas ni tokens"
        timestamptz occurred_at
    }
```

Cómo cubre las reglas aprobadas:

- **RNF-02 / RN-25 (dos hashes)**: al cambiar la contraseña, `previous_password_hash ← password_hash` y `password_hash ← nuevo`; el más antiguo se sobrescribe. Nunca hay texto plano.
- **RN-23 (token)**: en la base solo queda el hash del token, su vencimiento y `used_at`. Un token usado o vencido se rechaza; emitir uno nuevo invalida los anteriores sin usar del mismo propósito.
- **RN-23 / D-04 (2 por día)**: se cuentan las filas de `recovery_request` por cuenta, `kind` y `business_date` (fecha en `America/Bogota`). `LIMIT_REACHED` no suma; en cambio de correo, un intento con datos incorrectos (`REJECTED`) sí suma.
- **RN-24 / RN-25 (correo)**: hay un solo correo vigente, en `user_account.email`. Un correo nuevo espera en `one_time_token.target_email` y solo reemplaza al actual cuando se abre el enlace (T-18).
- **RN-16**: los casos de uso rechazan cualquier cambio que deje la plataforma sin una cuenta inicial activa con privilegio completo; el arranque además lo restablece (T-17).
- **D-01**: al cambiar o restablecer la contraseña se borran las filas de `spring_session` de la cuenta (el nombre del principal es el `id` de la cuenta).
- **RN-01**: el registro de estudiante exige una fila en `student_enablement` con su cédula y sin `registered_user_id`.

## Cursos, temas y biblioteca (Etapa 4, implementado)

```mermaid
erDiagram
    user_account ||--o{ course : "es docente de"
    course ||--o{ course_enrollment : "inscribe"
    course ||--o{ topic : "tiene"
    topic ||--o{ material : "contiene"
    stored_file |o--o| material : "archivo de"

    course {
        uuid id PK
        varchar name
        varchar period "p. ej. 2026-2"
        uuid owner_id FK "docente responsable"
    }
    course_enrollment {
        uuid id PK
        uuid course_id FK
        varchar cedula "UK con course_id; la cuenta puede no existir aún"
        uuid enrolled_by FK
    }
    topic {
        uuid id PK
        uuid course_id FK
        varchar title
        varchar tool "PSEINT | DFD | SCRATCH | ARDUINO | GENERAL"
        int position
        boolean next_class "uno por curso (índice único parcial)"
        timestamptz archived_at
    }
    material {
        uuid id PK
        uuid topic_id FK
        varchar kind "GUIDE | DOCUMENT | PRESENTATION | IMAGE | VIDEO | TOOL_FILE | LINK"
        varchar visibility "PUBLIC | PRIVATE"
        int position
        uuid file_id FK "o external_url, nunca ambos"
        varchar external_url "solo LINK, http o https"
        timestamptz archived_at
    }
    stored_file {
        uuid id PK
        varchar storage_key UK "aleatoria; nunca el nombre original"
        varchar original_name
        varchar format "PDF, DOCX, PNG, MP4, SB3..."
        bigint size_bytes
        varchar sha256
        timestamptz retain_until "carga + 6 meses como mínimo"
    }
```

- **RN-03 / RN-17**: un material privado solo lo ven los inscritos (por cédula), el docente responsable y las cuentas Administrador. Para los demás responde `404`.
- **RNF-04 / RN-21**: `files` comprueba la extensión permitida para el tipo de material, el tamaño (500 MB; 1 GB videos) y la firma del contenido antes de guardar.
- **RNF-12**: temas y materiales se archivan (`archived_at`); el archivo se conserva al menos hasta `retain_until`.

## Actividades, entregas y notas (Etapa 5, implementado)

```mermaid
erDiagram
    course ||--o{ activity : "tiene"
    activity ||--o{ submission : "recibe"
    submission ||--o{ submission_file : "adjunta"
    stored_file ||--o{ submission_file : "archivo"
    activity ||--o{ deadline_exception : "ajusta"
    activity ||--o{ rubric_version : "versiona"
    rubric_version ||--o{ rubric_criterion : "criterios"
    submission ||--|| grade : "nota"
    rubric_version ||--o{ grade : "evalúa con"
    grade ||--o{ grade_change : "historial"

    activity {
        uuid id PK
        uuid course_id FK
        varchar kind "TASK | WORKSHOP | EXAM"
        text statement
        boolean accepts_text
        boolean accepts_files
        boolean accepts_photos
        varchar accepted_files "grupos: DOCUMENTS, PRESENTATIONS, IMAGES, VIDEOS, TOOL_FILES"
        numeric weight_percent "opcional; suma del curso <= 100"
        timestamptz opens_at
        timestamptz closes_at "posterior a opens_at"
        int max_attempts "1 a 10"
        varchar status "DRAFT | PUBLISHED"
        timestamptz archived_at
    }
    submission {
        uuid id PK
        uuid activity_id FK
        uuid student_id FK
        int attempt "UK con actividad y estudiante"
        text text_answer
        timestamptz submitted_at
        boolean late "después del cierre original"
    }
    deadline_exception {
        uuid id PK
        uuid activity_id FK
        uuid student_id FK "UK con activity_id"
        timestamptz extended_until
        int extra_attempts "0 a 10"
        varchar reason
    }
    rubric_version {
        uuid id PK
        uuid activity_id FK
        int number "UK con activity_id"
        text statement "copia del enunciado vigente"
        text expected_answer
    }
    grade {
        uuid id PK
        uuid submission_id FK "UK"
        uuid rubric_version_id FK
        varchar status "PROCESSING | PENDING_REVIEW | PUBLISHED"
        numeric score "0,0 a 5,0"
        varchar source "TEACHER | AI"
    }
    grade_change {
        uuid id PK
        uuid grade_id FK
        numeric previous_score
        numeric new_score
        uuid changed_by FK
    }
```

- **RN-20**: cada entrega guarda en `grade.rubric_version_id` la versión vigente al enviarla; editar la rúbrica o el enunciado crea una fila nueva en `rubric_version`.
- **RN-05**: la actividad vencida sin entrega no tiene fila: el estado `OVERDUE` (0,0) se calcula al consultar (T-32).
- Las entregas y las notas no se borran; archivar la actividad solo la oculta.

## Calificación con IA (Etapa 6, implementado)

Migración `V5__calificacion_con_ia.sql`. `grade.status` admite además `PROCESSING` y `grade.review_reason` dice por qué una nota espera al docente: `AI_DISABLED`, `UNSUPPORTED`, `ILLEGIBLE`, `LOW_CONFIDENCE` o `AI_FAILED`.

```mermaid
erDiagram
    grade ||--o{ ai_grading_job : "cola"
    grade ||--o{ ai_evaluation : "evaluaciones"
    rubric_version ||--o{ ai_evaluation : "evalúa con"
    ai_evaluation ||--o{ ai_evaluation_criterion : "puntos"

    ai_grading_job {
        uuid id PK
        uuid grade_id FK "un solo trabajo PENDING o RUNNING por nota"
        varchar status "PENDING | RUNNING | DONE | FAILED"
        int attempts
        timestamptz next_run_at
        timestamptz locked_at
        uuid claim_token "toma vigente"
        text last_error
        uuid requested_by FK "docente que pidió reintentar"
    }
    ai_evaluation {
        uuid id PK
        uuid grade_id FK
        uuid rubric_version_id FK
        varchar model
        varchar outcome "PUBLISHED | ILLEGIBLE | LOW_CONFIDENCE | FAILED"
        boolean legible
        int confidence "0 a 100"
        numeric proposed_score "0,0 a 5,0; nulo si FAILED"
        text feedback
        text transcription
        text error "obligatorio si FAILED"
        bigint duration_ms
    }
    ai_evaluation_criterion {
        uuid ai_evaluation_id PK
        int position PK
        varchar name
        numeric points
        numeric max_points
        text comment
    }
```

- **RF-17**: cada evaluación de la IA queda guardada, incluidas las fallidas, aunque el docente califique antes o después (T-43).
- **RN-11**: un fallo técnico no deja nota: `ai_evaluation_failed_ck` exige `proposed_score` nulo y un `error`.
- La nota publicada sigue viviendo en `grade` con `source = AI` o `TEACHER`; confirmar la propuesta registra la nota del docente y su historial.
