-- LogiCode · V10 · Calibración de la IA por curso (D-g de la guía II-01, T-90)
-- Mientras un curso está en calibración, la IA propone pero no publica sola: la nota pasa a Por revisar con el
-- motivo CALIBRATION. Sin fila para un curso, el curso está en calibración; cerrarla guarda quién y cuándo.

ALTER TABLE grade DROP CONSTRAINT grade_review_reason_ck;
ALTER TABLE grade ADD CONSTRAINT grade_review_reason_ck CHECK (review_reason IS NULL OR review_reason IN
    ('AI_DISABLED', 'UNSUPPORTED', 'ILLEGIBLE', 'LOW_CONFIDENCE', 'AI_FAILED', 'CODE_ERROR', 'CALIBRATION'));

CREATE TABLE ai_calibration (
    course_id   UUID PRIMARY KEY REFERENCES course (id) ON DELETE CASCADE,
    closed_at   TIMESTAMPTZ,
    closed_by   UUID REFERENCES user_account (id),
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ai_calibration_closed_ck CHECK ((closed_at IS NULL) = (closed_by IS NULL))
);
