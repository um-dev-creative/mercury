-- Monitoring (phase 4): alert rules per application and the alerts they raised.
-- FAILURE_RATE: threshold is a percentage of failed outcomes over window_minutes (and at least min_volume outcomes).
-- QUEUE_STUCK: threshold is the number of messages that have waited longer than window_minutes.
-- SERVICE_DOWN: a service (or any, when service is null) degraded or down for at least window_minutes.
CREATE TABLE IF NOT EXISTS mercury.alert_rules (
    id             uuid PRIMARY KEY,
    application_id uuid          NOT NULL,
    type           varchar(20)   NOT NULL CHECK (type IN ('FAILURE_RATE', 'QUEUE_STUCK', 'SERVICE_DOWN')),
    threshold      numeric(10,2) NOT NULL,
    window_minutes integer       NOT NULL CHECK (window_minutes BETWEEN 1 AND 1440),
    min_volume     integer       NOT NULL DEFAULT 0 CHECK (min_volume >= 0),
    service        varchar(30),
    notify_emails  varchar(1500),
    enabled        boolean       NOT NULL DEFAULT true,
    created_at     timestamp     NOT NULL,
    updated_at     timestamp     NOT NULL
);

CREATE INDEX IF NOT EXISTS alert_rules_application_idx ON mercury.alert_rules (application_id);

CREATE TABLE IF NOT EXISTS mercury.alert_events (
    id             uuid PRIMARY KEY,
    rule_id        uuid          NOT NULL REFERENCES mercury.alert_rules (id) ON DELETE CASCADE,
    application_id uuid          NOT NULL,
    type           varchar(20)   NOT NULL,
    status         varchar(10)   NOT NULL CHECK (status IN ('FIRING', 'RESOLVED')),
    value          numeric(12,2) NOT NULL,
    detail         varchar(300)  NOT NULL,
    started_at     timestamp     NOT NULL,
    resolved_at    timestamp
);

CREATE INDEX IF NOT EXISTS alert_events_application_idx ON mercury.alert_events (application_id, started_at DESC);
-- a rule has at most one alert firing: a second instance racing to raise it fails here instead of raising it twice
CREATE UNIQUE INDEX IF NOT EXISTS alert_events_one_firing_idx ON mercury.alert_events (rule_id) WHERE status = 'FIRING';
