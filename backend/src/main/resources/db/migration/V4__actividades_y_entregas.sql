-- LogiCode · V4 · Actividades y entregas (Etapa 5)
-- Actividades del curso con plazos e intentos, entregas de los estudiantes con sus archivos, excepciones de plazo
-- por estudiante, rúbricas versionadas y notas con historial de cambios.

CREATE TABLE activity (
    id                 UUID PRIMARY KEY,
    course_id          UUID          NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title              VARCHAR(150)  NOT NULL,
    kind               VARCHAR(20)   NOT NULL,
    statement          TEXT          NOT NULL,
    accepts_text       BOOLEAN       NOT NULL,
    accepts_files      BOOLEAN       NOT NULL,
    accepts_photos     BOOLEAN       NOT NULL,
    -- Grupos de formatos aceptados cuando accepts_files: DOCUMENTS, PRESENTATIONS, IMAGES, VIDEOS, TOOL_FILES.
    accepted_files     VARCHAR(100),
    -- RN-07: porcentaje explícito; NULL = se reparte el resto por igual.
    weight_percent     NUMERIC(5, 2),
    opens_at           TIMESTAMPTZ   NOT NULL,
    closes_at          TIMESTAMPTZ   NOT NULL,
    max_attempts       INTEGER       NOT NULL DEFAULT 1,
    status             VARCHAR(10)   NOT NULL,
    archived_at        TIMESTAMPTZ,
    created_by         UUID REFERENCES user_account (id),
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT activity_kind_ck CHECK (kind IN ('TASK', 'WORKSHOP', 'EXAM')),
    CONSTRAINT activity_status_ck CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT activity_modes_ck CHECK (accepts_text OR accepts_files OR accepts_photos),
    CONSTRAINT activity_files_ck CHECK (NOT accepts_files OR accepted_files IS NOT NULL),
    CONSTRAINT activity_weight_ck CHECK (weight_percent IS NULL OR (weight_percent > 0 AND weight_percent <= 100)),
    CONSTRAINT activity_dates_ck CHECK (closes_at > opens_at),
    CONSTRAINT activity_attempts_ck CHECK (max_attempts BETWEEN 1 AND 10)
);

CREATE INDEX activity_course_ix ON activity (course_id, closes_at);

-- Cada intento de un estudiante es una fila; no se edita después de enviada.
CREATE TABLE submission (
    id            UUID PRIMARY KEY,
    activity_id   UUID         NOT NULL REFERENCES activity (id) ON DELETE CASCADE,
    student_id    UUID         NOT NULL REFERENCES user_account (id),
    attempt       INTEGER      NOT NULL,
    text_answer   TEXT,
    submitted_at  TIMESTAMPTZ  NOT NULL,
    -- Entregada después del cierre gracias a una excepción de plazo.
    late          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT submission_attempt_uk UNIQUE (activity_id, student_id, attempt),
    CONSTRAINT submission_attempt_ck CHECK (attempt >= 1)
);

CREATE INDEX submission_student_ix ON submission (student_id, activity_id);

CREATE TABLE submission_file (
    submission_id  UUID     NOT NULL REFERENCES submission (id) ON DELETE CASCADE,
    position       INTEGER  NOT NULL,
    file_id        UUID     NOT NULL REFERENCES stored_file (id),
    PRIMARY KEY (submission_id, position)
);

-- RN-05: el docente amplía el plazo o habilita más intentos para un estudiante.
CREATE TABLE deadline_exception (
    id              UUID PRIMARY KEY,
    activity_id     UUID          NOT NULL REFERENCES activity (id) ON DELETE CASCADE,
    student_id      UUID          NOT NULL REFERENCES user_account (id),
    extended_until  TIMESTAMPTZ,
    extra_attempts  INTEGER       NOT NULL DEFAULT 0,
    reason          VARCHAR(500),
    granted_by      UUID REFERENCES user_account (id),
    granted_at      TIMESTAMPTZ   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT deadline_exception_uk UNIQUE (activity_id, student_id),
    CONSTRAINT deadline_exception_attempts_ck CHECK (extra_attempts BETWEEN 0 AND 10),
    CONSTRAINT deadline_exception_effect_ck CHECK (extended_until IS NOT NULL OR extra_attempts > 0)
);

-- RN-20: enunciado, respuesta esperada y criterios vigentes; cada cambio crea una versión nueva.
CREATE TABLE rubric_version (
    id               UUID PRIMARY KEY,
    activity_id      UUID         NOT NULL REFERENCES activity (id) ON DELETE CASCADE,
    number           INTEGER      NOT NULL,
    statement        TEXT         NOT NULL,
    expected_answer  TEXT,
    created_by       UUID REFERENCES user_account (id),
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT rubric_version_uk UNIQUE (activity_id, number)
);

CREATE TABLE rubric_criterion (
    rubric_version_id  UUID          NOT NULL REFERENCES rubric_version (id) ON DELETE CASCADE,
    position           INTEGER       NOT NULL,
    name               VARCHAR(150)  NOT NULL,
    description        VARCHAR(2000),
    max_points         NUMERIC(6, 2) NOT NULL,
    PRIMARY KEY (rubric_version_id, position),
    CONSTRAINT rubric_criterion_points_ck CHECK (max_points > 0)
);

-- Una nota por entrega, evaluada con la versión de rúbrica vigente al entregar.
CREATE TABLE grade (
    id                 UUID PRIMARY KEY,
    submission_id      UUID          NOT NULL REFERENCES submission (id) ON DELETE CASCADE,
    rubric_version_id  UUID          NOT NULL REFERENCES rubric_version (id),
    status             VARCHAR(20)   NOT NULL,
    score              NUMERIC(2, 1),
    feedback           TEXT,
    source             VARCHAR(10),
    graded_by          UUID REFERENCES user_account (id),
    graded_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT grade_submission_uk UNIQUE (submission_id),
    CONSTRAINT grade_status_ck CHECK (status IN ('PENDING_REVIEW', 'PUBLISHED')),
    CONSTRAINT grade_score_ck CHECK (score IS NULL OR (score >= 0 AND score <= 5)),
    CONSTRAINT grade_published_ck CHECK (status <> 'PUBLISHED' OR score IS NOT NULL),
    CONSTRAINT grade_source_ck CHECK (source IS NULL OR source IN ('TEACHER', 'AI'))
);

CREATE INDEX grade_status_ix ON grade (status) WHERE status = 'PENDING_REVIEW';

-- RF-17, RN-09: cada nota publicada o corregida queda en el historial.
CREATE TABLE grade_change (
    id              UUID PRIMARY KEY,
    grade_id        UUID          NOT NULL REFERENCES grade (id) ON DELETE CASCADE,
    previous_score  NUMERIC(2, 1),
    new_score       NUMERIC(2, 1) NOT NULL,
    feedback        TEXT,
    source          VARCHAR(10)   NOT NULL,
    changed_by      UUID REFERENCES user_account (id),
    changed_at      TIMESTAMPTZ   NOT NULL
);

CREATE INDEX grade_change_grade_ix ON grade_change (grade_id, changed_at);
