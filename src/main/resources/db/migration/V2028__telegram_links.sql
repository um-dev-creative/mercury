-- Telegram channel (T-113, zerobound decision D-11): who is linked to which chat, and the single-use tokens of the deep link.
-- Run against the Mercury schema (PostgreSQL), like the other migrations in this folder.
-- `application` is the login-client alias of the caller (MachineTokenInterceptor.CALLER): a caller only sees its own rows.

CREATE TABLE IF NOT EXISTS mercury.telegram_links (
    id          uuid PRIMARY KEY DEFAULT general.uuid_generate_v4(),
    application varchar(100) NOT NULL,
    user_id     uuid         NOT NULL,
    chat_id     bigint       NOT NULL,
    status      varchar(10)  NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    language    varchar(5)   NOT NULL,
    linked_at   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT telegram_links_user_uk UNIQUE (application, user_id),
    CONSTRAINT telegram_links_chat_uk UNIQUE (application, chat_id)
);

-- Only the SHA-256 of the token is stored; the raw token exists in the deep link and nowhere else.
CREATE TABLE IF NOT EXISTS mercury.telegram_link_tokens (
    token_hash  char(64)     PRIMARY KEY,
    application varchar(100) NOT NULL,
    user_id     uuid         NOT NULL,
    language    varchar(5)   NOT NULL,
    expires_at  timestamp    NOT NULL,
    created_at  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS telegram_link_tokens_user_idx ON mercury.telegram_link_tokens (application, user_id);
CREATE INDEX IF NOT EXISTS telegram_link_tokens_expires_idx ON mercury.telegram_link_tokens (expires_at);
