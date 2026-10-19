-- LogiCode · V2 · Identidad y acceso (Etapa 3)
-- Cuentas, roles, privilegio completo, tokens de un solo uso, límites de recuperación,
-- cédulas habilitadas, auditoría de acceso y sesiones en servidor (Spring Session JDBC).

CREATE TABLE user_account (
    id                      UUID PRIMARY KEY,
    cedula                  VARCHAR(15)  NOT NULL,
    full_name               VARCHAR(150) NOT NULL,
    role                    VARCHAR(20)  NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    full_privileges         BOOLEAN      NOT NULL DEFAULT FALSE,
    email                   VARCHAR(254) NOT NULL,
    email_verified_at       TIMESTAMPTZ,
    -- Solo hashes BCrypt: el de la contraseña actual y el de la inmediatamente anterior.
    password_hash           VARCHAR(100),
    previous_password_hash  VARCHAR(100),
    password_changed_at     TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT user_account_cedula_uk UNIQUE (cedula),
    CONSTRAINT user_account_cedula_ck CHECK (cedula ~ '^[0-9]{3,15}$'),
    CONSTRAINT user_account_role_ck CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
    CONSTRAINT user_account_status_ck CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'DISABLED')),
    CONSTRAINT user_account_full_privileges_ck CHECK (NOT full_privileges OR role = 'ADMIN')
);

CREATE UNIQUE INDEX user_account_email_uk ON user_account (lower(email));
CREATE INDEX user_account_role_status_ix ON user_account (role, status);

-- Tokens de un solo uso. Solo se guarda el hash SHA-256; el token viaja únicamente en el enlace del correo.
CREATE TABLE one_time_token (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    purpose       VARCHAR(30)  NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL,
    target_email  VARCHAR(254),
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT one_time_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT one_time_token_purpose_ck CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX one_time_token_user_purpose_ix ON one_time_token (user_id, purpose);

-- Solicitudes de recuperación por cuenta y día calendario en Colombia (máximo 2 por flujo).
CREATE TABLE recovery_request (
    id             UUID PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    kind           VARCHAR(30)  NOT NULL,
    business_date  DATE         NOT NULL,
    outcome        VARCHAR(30)  NOT NULL,
    requested_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT recovery_request_kind_ck CHECK (kind IN ('PASSWORD_RESET', 'EMAIL_CHANGE'))
);

CREATE INDEX recovery_request_user_day_ix ON recovery_request (user_id, kind, business_date);

-- Cédulas habilitadas por un docente para que el estudiante pueda registrarse (RN-01).
CREATE TABLE student_enablement (
    id                  UUID PRIMARY KEY,
    cedula              VARCHAR(15)  NOT NULL,
    full_name           VARCHAR(150),
    enabled_by          UUID         NOT NULL REFERENCES user_account (id),
    enabled_at          TIMESTAMPTZ  NOT NULL,
    registered_user_id  UUID REFERENCES user_account (id),
    CONSTRAINT student_enablement_cedula_uk UNIQUE (cedula),
    CONSTRAINT student_enablement_cedula_ck CHECK (cedula ~ '^[0-9]{3,15}$')
);

-- Auditoría de acceso y administración. Nunca contiene contraseñas ni tokens.
CREATE TABLE audit_event (
    id              UUID PRIMARY KEY,
    action          VARCHAR(50)  NOT NULL,
    actor_id        UUID,
    target_user_id  UUID,
    subject         VARCHAR(254),
    outcome         VARCHAR(30)  NOT NULL,
    request_id      VARCHAR(64),
    details         TEXT,
    occurred_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX audit_event_occurred_ix ON audit_event (occurred_at DESC);
CREATE INDEX audit_event_action_subject_ix ON audit_event (action, subject, occurred_at);

-- Sesiones en servidor (Spring Session JDBC, esquema oficial para PostgreSQL).
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
