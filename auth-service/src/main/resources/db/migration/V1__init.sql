CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    role                  VARCHAR(20)  NOT NULL,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_tokens (
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users (id),
    token_hash          VARCHAR(64) NOT NULL UNIQUE,
    session_started_at  TIMESTAMPTZ NOT NULL,
    idle_expires_at     TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    revoked             BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
