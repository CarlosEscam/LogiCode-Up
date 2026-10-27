-- Etapa 7: quizzes con banco de preguntas, casos de prueba de PSeInt y calificación determinista
-- (RF-10, RF-12, RF-16, RF-21, RN-13, RN-14, RN-20).

-- Quizzes calificables y de repaso como tipos de actividad. Un quiz no recibe texto ni archivos: se responde en su
-- página, y el de repaso no tiene porcentaje.
ALTER TABLE activity DROP CONSTRAINT activity_kind_ck;
ALTER TABLE activity ADD CONSTRAINT activity_kind_ck
    CHECK (kind IN ('TASK', 'WORKSHOP', 'EXAM', 'QUIZ', 'PRACTICE_QUIZ'));
ALTER TABLE activity DROP CONSTRAINT activity_modes_ck;
ALTER TABLE activity ADD CONSTRAINT activity_modes_ck
    CHECK (accepts_text OR accepts_files OR accepts_photos OR kind IN ('QUIZ', 'PRACTICE_QUIZ'));
ALTER TABLE activity ADD CONSTRAINT activity_practice_weight_ck
    CHECK (kind <> 'PRACTICE_QUIZ' OR weight_percent IS NULL);

-- RF-12: casos de prueba de PSeInt como parte de la rúbrica versionada (RN-20).
CREATE TABLE rubric_test_case (
    rubric_version_id  UUID     NOT NULL REFERENCES rubric_version (id) ON DELETE CASCADE,
    position           INTEGER  NOT NULL,
    input_data         TEXT     NOT NULL,
    expected_output    TEXT     NOT NULL,
    PRIMARY KEY (rubric_version_id, position)
);

-- AUTOMATIC: nota determinista (quiz o casos de prueba). CODE_ERROR: el programa no se pudo leer para ejecutarlo.
ALTER TABLE grade DROP CONSTRAINT grade_source_ck;
ALTER TABLE grade ADD CONSTRAINT grade_source_ck CHECK (source IS NULL OR source IN ('TEACHER', 'AI', 'AUTOMATIC'));
ALTER TABLE grade DROP CONSTRAINT grade_review_reason_ck;
ALTER TABLE grade ADD CONSTRAINT grade_review_reason_ck CHECK (review_reason IS NULL OR review_reason IN
    ('AI_DISABLED', 'UNSUPPORTED', 'ILLEGIBLE', 'LOW_CONFIDENCE', 'AI_FAILED', 'CODE_ERROR'));

-- Banco de preguntas del curso. Una pregunta usada en un intento ya no se edita: se archiva o se duplica.
CREATE TABLE quiz_question (
    id           UUID PRIMARY KEY,
    course_id    UUID           NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    kind         VARCHAR(20)    NOT NULL,
    prompt       TEXT           NOT NULL,
    points       INTEGER        NOT NULL,
    explanation  TEXT,
    archived_at  TIMESTAMPTZ,
    created_by   UUID REFERENCES user_account (id),
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT quiz_question_kind_ck CHECK (kind IN ('SINGLE', 'MULTIPLE', 'TRUE_FALSE', 'SHORT')),
    CONSTRAINT quiz_question_points_ck CHECK (points BETWEEN 1 AND 10)
);

CREATE INDEX quiz_question_course_ix ON quiz_question (course_id) WHERE archived_at IS NULL;

CREATE TABLE quiz_question_option (
    question_id  UUID          NOT NULL REFERENCES quiz_question (id) ON DELETE CASCADE,
    position     INTEGER       NOT NULL,
    text         VARCHAR(500)  NOT NULL,
    correct      BOOLEAN       NOT NULL,
    PRIMARY KEY (question_id, position)
);

-- Respuestas aceptadas de una pregunta de respuesta corta (se comparan sin mayúsculas, tildes ni espacios extra).
CREATE TABLE quiz_question_answer (
    question_id  UUID          NOT NULL REFERENCES quiz_question (id) ON DELETE CASCADE,
    position     INTEGER       NOT NULL,
    answer       VARCHAR(200)  NOT NULL,
    PRIMARY KEY (question_id, position)
);

-- RN-13: preguntas y tiempo total de un quiz (una actividad de tipo QUIZ o PRACTICE_QUIZ).
CREATE TABLE quiz (
    activity_id         UUID PRIMARY KEY REFERENCES activity (id) ON DELETE CASCADE,
    time_limit_minutes  INTEGER      NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT quiz_time_limit_ck CHECK (time_limit_minutes BETWEEN 1 AND 300)
);

CREATE TABLE quiz_item (
    quiz_id      UUID     NOT NULL REFERENCES quiz (activity_id) ON DELETE CASCADE,
    position     INTEGER  NOT NULL,
    question_id  UUID     NOT NULL REFERENCES quiz_question (id),
    PRIMARY KEY (quiz_id, position)
);

-- Intento de un estudiante. El calificable, al terminar, se registra como entrega (submission) con su nota.
CREATE TABLE quiz_attempt (
    id             UUID PRIMARY KEY,
    activity_id    UUID           NOT NULL REFERENCES activity (id) ON DELETE CASCADE,
    student_id     UUID           NOT NULL REFERENCES user_account (id),
    number         INTEGER        NOT NULL,
    practice       BOOLEAN        NOT NULL,
    status         VARCHAR(12)    NOT NULL,
    started_at     TIMESTAMPTZ    NOT NULL,
    deadline       TIMESTAMPTZ    NOT NULL,
    finished_at    TIMESTAMPTZ,
    correct_count  INTEGER,
    question_count INTEGER        NOT NULL,
    points         INTEGER,
    max_points     INTEGER        NOT NULL,
    score          NUMERIC(2, 1),
    submission_id  UUID REFERENCES submission (id),
    version        BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT quiz_attempt_uk UNIQUE (activity_id, student_id, number),
    CONSTRAINT quiz_attempt_status_ck CHECK (status IN ('IN_PROGRESS', 'FINISHED')),
    CONSTRAINT quiz_attempt_finished_ck CHECK (status <> 'FINISHED' OR (finished_at IS NOT NULL AND score IS NOT NULL)),
    CONSTRAINT quiz_attempt_score_ck CHECK (score IS NULL OR (score >= 0 AND score <= 5))
);

-- Un solo intento en curso por estudiante y quiz.
CREATE UNIQUE INDEX quiz_attempt_open_uk ON quiz_attempt (activity_id, student_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX quiz_attempt_deadline_ix ON quiz_attempt (deadline) WHERE status = 'IN_PROGRESS';

-- Respuestas del intento, en el orden de las preguntas. choices: posiciones elegidas separadas por comas.
CREATE TABLE quiz_attempt_answer (
    attempt_id   UUID          NOT NULL REFERENCES quiz_attempt (id) ON DELETE CASCADE,
    position     INTEGER       NOT NULL,
    question_id  UUID          NOT NULL REFERENCES quiz_question (id),
    choices      VARCHAR(50),
    text_answer  VARCHAR(500),
    correct      BOOLEAN,
    PRIMARY KEY (attempt_id, position)
);
