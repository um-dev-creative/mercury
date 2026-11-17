-- Monitoring (phase 2): the history of service outages. One row per outage of a service (SMTP, Kafka, MongoDB, PostgreSQL, Backbone, Telegram);
-- ended_at stays null while it lasts. cause_code is one of a fixed list and cause_text a fixed sentence: no host, address or credential.
CREATE TABLE IF NOT EXISTS mercury.service_outages (
    id          uuid PRIMARY KEY,
    service     varchar(30)  NOT NULL,
    status      varchar(10)  NOT NULL CHECK (status IN ('DEGRADED', 'DOWN')),
    cause_code  varchar(40)  NOT NULL,
    cause_text  varchar(200) NOT NULL,
    started_at  timestamp    NOT NULL,
    ended_at    timestamp
);

CREATE INDEX IF NOT EXISTS service_outages_started_idx ON mercury.service_outages (started_at DESC);
CREATE INDEX IF NOT EXISTS service_outages_open_idx ON mercury.service_outages (service) WHERE ended_at IS NULL;
