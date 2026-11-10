-- A template has a language of its own (decision D-15 of zerobound-rest): callers name a template and a language and Mercury resolves the
-- definition, so no caller keeps template ids in its configuration. Until now the language only lived in the file name (zb_verification_es.ftl).
ALTER TABLE mercury.templates ADD COLUMN IF NOT EXISTS language varchar(16);

-- existing rows: the language is the suffix of the location before the extension (zb_verification_es.ftl -> es, zb_x_tg_pt-BR.ftl -> pt-BR)
UPDATE mercury.templates
SET language = substring(location from '_([A-Za-z]{2,3}(-[A-Za-z0-9]{2,4})?)\.[A-Za-z0-9]+$')
WHERE language IS NULL;

CREATE INDEX IF NOT EXISTS idx_templates_application_language ON mercury.templates (application_id, language);
