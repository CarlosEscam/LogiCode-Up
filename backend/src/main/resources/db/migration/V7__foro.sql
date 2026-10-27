-- Etapa 8: foro por herramienta con moderación (RF-26, RF-27, RN-15).
-- La planilla, las estadísticas y el panel de administración se calculan con las tablas existentes; los respaldos
-- viven en el volumen de Docker `backups`, no en la base de datos.

CREATE TABLE forum_thread (
    id                UUID PRIMARY KEY,
    category          VARCHAR(20)   NOT NULL,
    title             VARCHAR(150)  NOT NULL,
    body              TEXT          NOT NULL,
    author_id         UUID          NOT NULL REFERENCES user_account (id),
    pinned            BOOLEAN       NOT NULL DEFAULT FALSE,
    closed            BOOLEAN       NOT NULL DEFAULT FALSE,
    reply_count       INTEGER       NOT NULL DEFAULT 0,
    last_activity_at  TIMESTAMPTZ   NOT NULL,
    -- Moderación: quién fijó, cerró o eliminó por última vez. Un hilo eliminado se oculta, no se borra.
    moderated_by      UUID REFERENCES user_account (id),
    moderated_at      TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT forum_thread_category_ck CHECK (category IN ('PSEINT', 'DFD', 'SCRATCH', 'ARDUINO', 'GENERAL')),
    CONSTRAINT forum_thread_replies_ck CHECK (reply_count >= 0)
);

CREATE INDEX forum_thread_list_ix ON forum_thread (pinned DESC, last_activity_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE forum_post (
    id          UUID PRIMARY KEY,
    thread_id   UUID         NOT NULL REFERENCES forum_thread (id),
    author_id   UUID         NOT NULL REFERENCES user_account (id),
    body        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    deleted_by  UUID REFERENCES user_account (id),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX forum_post_thread_ix ON forum_post (thread_id, created_at) WHERE deleted_at IS NULL;
