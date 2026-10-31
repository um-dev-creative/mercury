-- Telegram channel (T-115): the queue of messages to deliver. One row per accepted POST /api/v1/telegram/messages.
-- `params` holds the template parameters (amounts) only until the message reaches a final state, then it is emptied.
CREATE TABLE IF NOT EXISTS mercury.telegram_messages (
    id                   uuid PRIMARY KEY,
    application          varchar(100) NOT NULL,
    user_id              uuid         NOT NULL,
    idempotency_key      varchar(200),
    request_hash         char(64)     NOT NULL,
    kind                 varchar(50)  NOT NULL,
    language             varchar(5)   NOT NULL,
    params               jsonb        NOT NULL DEFAULT '{}',
    disable_notification boolean      NOT NULL DEFAULT false,
    status               varchar(10)  NOT NULL CHECK (status IN ('QUEUED', 'SENT', 'FAILED', 'BLOCKED')),
    attempts             integer      NOT NULL DEFAULT 0,
    parts_sent           integer      NOT NULL DEFAULT 0,
    next_attempt_at      timestamp    NOT NULL,
    created_at           timestamp    NOT NULL,
    updated_at           timestamp    NOT NULL,
    sent_at              timestamp,
    last_error           varchar(40)
);

CREATE UNIQUE INDEX IF NOT EXISTS telegram_messages_idem_uk ON mercury.telegram_messages (application, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS telegram_messages_due_idx ON mercury.telegram_messages (next_attempt_at) WHERE status = 'QUEUED';
