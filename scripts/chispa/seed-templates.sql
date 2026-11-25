-- CHISPA push templates. Registers the 14 FreeMarker files ch_<kind>_<es|en>.ftl (kinds: invitation, challenge, surprise, plan, date_confirmed,
-- memory, reward; copies in src/test/resources/templates, which must be in the template storage) as mercury.templates + mercury.template_defined
-- for the CHISPA application, with the subject (the notification title) CHISPA. Idempotent: a template whose location already exists for the
-- application is skipped, and a subject already set is never overwritten.
--
-- Not run by the application (the schema is managed outside it). Run with psql, giving the ids that already exist in your environment:
--   psql "$MERCURY_DB" -v ON_ERROR_STOP=1 \
--        -v app_id=<general.application id of CHISPA: the BACKBONE_APPLICATION_ID of chispa-rest> \
--        -v owner_id=<user id that owns the definitions> \
--        -v type_id=<mercury.template_types id of PUSH_NOTIFICATION (scripts/templates/seed-template-types.sql)> \
--        -v severity_id=<mercury.severity_type id> -v frequency_id=<mercury.frequency_type id> \
--        -f scripts/chispa/seed-templates.sql
-- type_id MUST NOT be the verification-code type: those templates make mercury store a confirmable code and need the parameter `vc`.
-- chispa-rest names the templates (ch_<kind> + language + applicationId) and mercury resolves them (decision D-15), so no id goes in any
-- configuration; the script prints each template_defined id for reference only. The texts are fixed and generic on purpose (a lock screen is
-- public): they carry no parameter. Verify on a copy of the database first.

\set ON_ERROR_STOP on
\if :{?app_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'app_id is required: psql -v app_id=<uuid> (see the header of this file)'; END $$;
\endif
\if :{?owner_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'owner_id is required: psql -v owner_id=<uuid> (see the header of this file)'; END $$;
\endif
\if :{?type_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'type_id is required: psql -v type_id=<uuid> (see the header of this file)'; END $$;
\endif
\if :{?severity_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'severity_id is required: psql -v severity_id=<uuid> (see the header of this file)'; END $$;
\endif
\if :{?frequency_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'frequency_id is required: psql -v frequency_id=<uuid> (see the header of this file)'; END $$;
\endif

BEGIN;

WITH kinds(kind, descr) AS (VALUES
    ('invitation', 'Invitation'), ('challenge', 'Challenge'), ('surprise', 'Surprise'), ('plan', 'Plan'),
    ('date_confirmed', 'Plan confirmed'), ('memory', 'Memory'), ('reward', 'Reward')),
     langs(lang) AS (VALUES ('es'), ('en')),
     wanted AS (
         SELECT 'ch_' || k.kind || '_' || l.lang || '.ftl' AS location,
                'CHISPA push - ' || k.descr || ' (' || l.lang || ')' AS description
         FROM kinds k CROSS JOIN langs l)
INSERT INTO mercury.templates (description, location, file_format, language, template_type_id, application_id, severity_type_id, subject)
SELECT w.description, w.location, 'ftl', substring(w.location from '_([a-z]{2,3})\.ftl$'), :'type_id'::uuid, :'app_id'::uuid, :'severity_id'::uuid, 'CHISPA'
FROM wanted w
WHERE NOT EXISTS (SELECT 1 FROM mercury.templates t WHERE t.location = w.location AND t.application_id = :'app_id'::uuid);

-- rows created without a subject: the notification title is the template's subject
UPDATE mercury.templates
SET subject = 'CHISPA'
WHERE application_id = :'app_id'::uuid AND location LIKE 'ch\_%.ftl' AND (subject IS NULL OR subject = '');

INSERT INTO mercury.template_defined (template_id, user_id, application_id, frequency_type)
SELECT t.id, :'owner_id'::uuid, :'app_id'::uuid, :'frequency_id'::uuid
FROM mercury.templates t
WHERE t.application_id = :'app_id'::uuid AND t.location LIKE 'ch\_%.ftl'
  AND NOT EXISTS (SELECT 1 FROM mercury.template_defined d WHERE d.template_id = t.id);

SELECT t.location, d.id AS template_defined_id
FROM mercury.templates t JOIN mercury.template_defined d ON d.template_id = t.id
WHERE t.application_id = :'app_id'::uuid AND t.location LIKE 'ch\_%.ftl'
ORDER BY t.location;

COMMIT;
