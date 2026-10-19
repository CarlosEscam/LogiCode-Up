-- LogiCode · V3 · Cursos, temas y biblioteca (Etapa 4)
-- Curso con su docente responsable, estudiantes inscritos por cédula, temas ordenados por herramienta,
-- materiales públicos o privados y metadatos de los archivos cargados (el contenido vive fuera de la base).

CREATE TABLE course (
    id           UUID PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    period       VARCHAR(20)  NOT NULL,
    description  VARCHAR(2000),
    owner_id     UUID         NOT NULL REFERENCES user_account (id),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0
);

-- Inscripción por cédula: el estudiante ve el contenido privado del curso desde que se registra (RN-03).
CREATE TABLE course_enrollment (
    id           UUID PRIMARY KEY,
    course_id    UUID         NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    cedula       VARCHAR(15)  NOT NULL,
    enrolled_by  UUID REFERENCES user_account (id),
    enrolled_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT course_enrollment_uk UNIQUE (course_id, cedula),
    CONSTRAINT course_enrollment_cedula_ck CHECK (cedula ~ '^[0-9]{3,15}$')
);

CREATE INDEX course_enrollment_cedula_ix ON course_enrollment (cedula);

-- Archivos cargados. Solo metadatos; el contenido está en el almacenamiento de archivos con una clave aleatoria.
CREATE TABLE stored_file (
    id             UUID PRIMARY KEY,
    storage_key    VARCHAR(100) NOT NULL,
    original_name  VARCHAR(255) NOT NULL,
    format         VARCHAR(10)  NOT NULL,
    content_type   VARCHAR(100) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    sha256         VARCHAR(64)  NOT NULL,
    uploaded_by    UUID REFERENCES user_account (id),
    uploaded_at    TIMESTAMPTZ  NOT NULL,
    -- RN-21 / RNF-12: el archivo no se borra antes de esta fecha (carga + 6 meses como mínimo).
    retain_until   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT stored_file_key_uk UNIQUE (storage_key),
    CONSTRAINT stored_file_size_ck CHECK (size_bytes > 0)
);

CREATE TABLE topic (
    id           UUID PRIMARY KEY,
    course_id    UUID         NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title        VARCHAR(150) NOT NULL,
    description  VARCHAR(2000),
    tool         VARCHAR(20)  NOT NULL,
    position     INTEGER      NOT NULL,
    next_class   BOOLEAN      NOT NULL DEFAULT FALSE,
    archived_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT topic_tool_ck CHECK (tool IN ('PSEINT', 'DFD', 'SCRATCH', 'ARDUINO', 'GENERAL'))
);

CREATE INDEX topic_course_ix ON topic (course_id, position);
-- RF-09: como máximo un tema marcado como próxima clase por curso.
CREATE UNIQUE INDEX topic_next_class_uk ON topic (course_id) WHERE next_class AND archived_at IS NULL;

CREATE TABLE material (
    id            UUID PRIMARY KEY,
    topic_id      UUID         NOT NULL REFERENCES topic (id) ON DELETE CASCADE,
    title         VARCHAR(150) NOT NULL,
    description   VARCHAR(2000),
    kind          VARCHAR(20)  NOT NULL,
    visibility    VARCHAR(10)  NOT NULL,
    position      INTEGER      NOT NULL,
    file_id       UUID REFERENCES stored_file (id),
    external_url  VARCHAR(2000),
    created_by    UUID REFERENCES user_account (id),
    archived_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT material_kind_ck CHECK (kind IN ('GUIDE', 'DOCUMENT', 'PRESENTATION', 'IMAGE', 'VIDEO', 'TOOL_FILE', 'LINK')),
    CONSTRAINT material_visibility_ck CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    -- Un material es un archivo cargado o un enlace externo, nunca ambos.
    CONSTRAINT material_source_ck CHECK ((file_id IS NULL) <> (external_url IS NULL)),
    CONSTRAINT material_link_ck CHECK ((kind = 'LINK') = (external_url IS NOT NULL))
);

CREATE INDEX material_topic_ix ON material (topic_id, position);
CREATE INDEX material_visibility_ix ON material (visibility) WHERE archived_at IS NULL;
