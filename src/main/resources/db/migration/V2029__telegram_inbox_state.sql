-- Telegram channel (T-114): the last update processed per application (an update is never handled twice, and getUpdates resumes after it)
-- and the lease that makes one mercury instance the only poller (Telegram answers 409 when two poll the same bot).
CREATE TABLE IF NOT EXISTS mercury.telegram_inbox_state (
    application    varchar(100) PRIMARY KEY,
    last_update_id bigint       NOT NULL DEFAULT 0,
    lease_owner    varchar(64),
    lease_until    timestamp
);
