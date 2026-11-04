-- History of the content of the templates (TemplateContentService). Run against the Mercury schema (PostgreSQL), like the other migrations
-- in this folder; Mercury never migrates the schema itself. Without this table the content API still reads and saves, but answers 500 when it
-- tries to record a version, so apply it before enabling template editing.
--
-- One row per saved content: `version` counts from 1 per template, `reason` says why it was recorded (SAVE: a normal save; RESTORE: a restore of
-- an older version; BASELINE: the content the template had before its first edit from the API, kept so it can always be restored).
-- Mercury keeps the latest `umdc.templates.max-versions` (default 50) per template and deletes the older ones.

CREATE TABLE IF NOT EXISTS mercury.template_versions (
    id          uuid PRIMARY KEY DEFAULT general.uuid_generate_v4(),
    template_id uuid         NOT NULL REFERENCES mercury.templates (id),
    version     integer      NOT NULL,
    etag        varchar(128) NOT NULL,
    content     text         NOT NULL,
    size_bytes  integer      NOT NULL,
    reason      varchar(20)  NOT NULL DEFAULT 'SAVE',
    created_by  uuid,
    created_at  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT template_versions_template_version_uk UNIQUE (template_id, version),
    CONSTRAINT template_versions_reason_ck CHECK (reason IN ('SAVE', 'RESTORE', 'BASELINE'))
);

CREATE INDEX IF NOT EXISTS template_versions_template_idx ON mercury.template_versions (template_id, version DESC);
