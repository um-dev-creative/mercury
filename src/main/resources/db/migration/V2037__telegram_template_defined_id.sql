-- A Telegram message can name its template by the template_defined id (like an e-mail) instead of by kind. NULL = the message was queued by kind.
ALTER TABLE mercury.telegram_messages ADD COLUMN IF NOT EXISTS template_defined_id uuid;
