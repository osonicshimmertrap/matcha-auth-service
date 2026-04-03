-- Matcha Auth Service — initial schema
-- V1__init.sql

CREATE TABLE users (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                        VARCHAR(255) NOT NULL UNIQUE,
    password_hash                VARCHAR(255) NOT NULL,
    role                         VARCHAR(50)  NOT NULL,
    telegram_chat_id             VARCHAR(100),
    telegram_link_code           VARCHAR(6),
    telegram_link_code_expires_at TIMESTAMP WITH TIME ZONE,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info  VARCHAR(500)
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
