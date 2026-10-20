-- Etapa 6: calificación con IA en segundo plano (RF-14, RF-15, RF-17, RNF-06 a RNF-08, RN-08 a RN-11, RN-18).

-- PROCESSING: la IA evalúa la entrega. review_reason explica por qué una nota espera al docente.
ALTER TABLE grade DROP CONSTRAINT grade_status_ck;
ALTER TABLE grade ADD CONSTRAINT grade_status_ck CHECK (status IN ('PROCESSING', 'PENDING_REVIEW', 'PUBLISHED'));
ALTER TABLE grade ADD COLUMN review_reason VARCHAR(20);
ALTER TABLE grade ADD CONSTRAINT grade_review_reason_ck CHECK (review_reason IS NULL OR review_reason IN
    ('AI_DISABLED', 'UNSUPPORTED', 'ILLEGIBLE', 'LOW_CONFIDENCE', 'AI_FAILED'));

DROP INDEX grade_status_ix;
CREATE INDEX grade_status_ix ON grade (status) WHERE status <> 'PUBLISHED';

-- Cola de evaluación: el worker toma un trabajo con FOR UPDATE SKIP LOCKED. claim_token identifica la toma vigente
-- para que un trabajo recuperado tras un bloqueo vencido no aplique un resultado dos veces.
CREATE TABLE ai_grading_job (
    id             UUID PRIMARY KEY,
    grade_id       UUID          NOT NULL REFERENCES grade (id) ON DELETE CASCADE,
    status         VARCHAR(10)   NOT NULL,
    attempts       INTEGER       NOT NULL DEFAULT 0,
    next_run_at    TIMESTAMPTZ   NOT NULL,
    locked_at      TIMESTAMPTZ,
    claim_token    UUID,
    last_error     TEXT,
    requested_by   UUID REFERENCES user_account (id),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ai_grading_job_status_ck CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED'))
);

-- Un solo trabajo activo por nota.
CREATE UNIQUE INDEX ai_grading_job_active_uk ON ai_grading_job (grade_id) WHERE status IN ('PENDING', 'RUNNING');
CREATE INDEX ai_grading_job_queue_ix ON ai_grading_job (next_run_at) WHERE status IN ('PENDING', 'RUNNING');

-- Resultado de cada evaluación con IA (RF-17): lo que el modelo leyó, legibilidad, confianza y puntos por criterio.
-- Los fallos técnicos también quedan registrados, sin nota.
CREATE TABLE ai_evaluation (
    id                 UUID PRIMARY KEY,
    grade_id           UUID          NOT NULL REFERENCES grade (id) ON DELETE CASCADE,
    rubric_version_id  UUID          NOT NULL REFERENCES rubric_version (id),
    model              VARCHAR(100)  NOT NULL,
    outcome            VARCHAR(20)   NOT NULL,
    legible            BOOLEAN,
    confidence         INTEGER,
    proposed_score     NUMERIC(2, 1),
    feedback           TEXT,
    transcription      TEXT,
    error              TEXT,
    duration_ms        BIGINT        NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ai_evaluation_outcome_ck CHECK (outcome IN ('PUBLISHED', 'ILLEGIBLE', 'LOW_CONFIDENCE', 'FAILED')),
    CONSTRAINT ai_evaluation_confidence_ck CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 100)),
    CONSTRAINT ai_evaluation_score_ck CHECK (proposed_score IS NULL OR (proposed_score >= 0 AND proposed_score <= 5)),
    CONSTRAINT ai_evaluation_failed_ck CHECK (outcome <> 'FAILED' OR (proposed_score IS NULL AND error IS NOT NULL))
);

CREATE INDEX ai_evaluation_grade_ix ON ai_evaluation (grade_id, created_at);

CREATE TABLE ai_evaluation_criterion (
    ai_evaluation_id  UUID          NOT NULL REFERENCES ai_evaluation (id) ON DELETE CASCADE,
    position          INTEGER       NOT NULL,
    name              VARCHAR(150)  NOT NULL,
    points            NUMERIC(6, 2) NOT NULL,
    max_points        NUMERIC(6, 2) NOT NULL,
    comment           TEXT,
    PRIMARY KEY (ai_evaluation_id, position)
);
