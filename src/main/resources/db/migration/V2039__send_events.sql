-- Monitoring (phase 1): one durable row per delivery outcome, so the dashboard can draw history that the in-flight stores do not keep
-- (an e-mail leaves MongoDB once it is recorded). Only the outcome is stored: the recipient is masked and no body or code is kept.
CREATE TABLE IF NOT EXISTS mercury.send_events (
    id                  uuid PRIMARY KEY,
    occurred_at         timestamp    NOT NULL,
    application_id      uuid,
    channel             varchar(20)  NOT NULL,
    kind                varchar(20)  NOT NULL CHECK (kind IN ('TRANSACTIONAL', 'CAMPAIGN', 'VERIFICATION_CODE')),
    status              varchar(10)  NOT NULL CHECK (status IN ('SENT', 'FAILED')),
    message_id          uuid,
    campaign_id         uuid,
    template_defined_id uuid,
    recipient_masked    varchar(120),
    error_cause         varchar(60)
);

CREATE INDEX IF NOT EXISTS send_events_app_time_idx ON mercury.send_events (application_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS send_events_time_idx ON mercury.send_events (occurred_at);
