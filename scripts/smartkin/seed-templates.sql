-- SmartKin mail templates. Registers the two FreeMarker files sk_verification_{es,en}.ftl as mercury.templates + mercury.template_defined
-- for the SmartKin application. Idempotent: a template whose location already exists for the application is skipped.
--
-- Not run by the application (the schema is managed outside it). Run with psql, giving the ids that already exist in your environment. If
-- the template type, severity and frequency rows do not exist yet, scripts/smartkin/bootstrap-reference-data.sql creates them and prints those
-- three ids; the application and the owner user come from backbone-rest.
--   psql "$MERCURY_DB" -v ON_ERROR_STOP=1 \
--        -v app_id=<general.application id of SmartKin: smartkin-backend's umdc.smartkin.application-id / Vault APPLICATION_ID> \
--        -v owner_id=<user id that owns the definitions> \
--        -v verification_type_id=<mercury.template_types id configured in mercury as PRX_VERIFICATION_CODE_TEMPLATE_ID> \
--        -v severity_id=<mercury.severity_type id> -v frequency_id=<mercury.frequency_type id> \
--        -f scripts/smartkin/seed-templates.sql
--
-- Both templates are of the VERIFICATION-CODE type: mercury stores a confirmable code (user, application, expiry, attempts) only for a
-- message whose template is of that type, and smartkin-backend later confirms it through POST /api/v1/verification-code. Mercury's
-- umdc.verification.code.template.id (env PRX_VERIFICATION_CODE_TEMPLATE_ID) is that TYPE id; smartkin-backend's property with the same
-- name (umdc.verification.code.template.id, Vault PRX_VERIFICATION_CODE_TEMPLATE_ID) is the template_defined id this script prints.
-- Two different ids with the same name: do not mix them up.
-- It prints each template_defined id. smartkin-backend sends ONE id for every user, so put the Spanish one (the app's language) in its
-- configuration; the English one is there to switch to. Verify on a copy of the database first.

\set ON_ERROR_STOP on
-- every variable is required; say which one is missing instead of a syntax error
\if :{?app_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'app_id is required: psql -v app_id=<uuid> (see the header of this file)'; END $$;
\endif
\if :{?owner_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'owner_id is required: psql -v owner_id=<uuid> (see the header of this file)'; END $$;
\endif
\if :{?verification_type_id}
\else
    DO $$ BEGIN RAISE EXCEPTION 'verification_type_id is required: psql -v verification_type_id=<uuid> (see the header of this file)'; END $$;
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

-- Fail with a message instead of a foreign-key error when an id does not exist (psql does not substitute variables inside DO $$ ... $$,
-- hence \gset and \if)
SELECT EXISTS (SELECT 1 FROM general.application WHERE id = :'app_id'::uuid) AS app_ok,
       EXISTS (SELECT 1 FROM mercury.template_types WHERE id = :'verification_type_id'::uuid) AS type_ok,
       EXISTS (SELECT 1 FROM mercury.severity_type WHERE id = :'severity_id'::uuid) AS severity_ok,
       EXISTS (SELECT 1 FROM mercury.frequency_type WHERE id = :'frequency_id'::uuid) AS frequency_ok \gset
\if :app_ok
\else
    DO $$ BEGIN RAISE EXCEPTION 'app_id is not a row of general.application (create the SmartKin application first)'; END $$;
\endif
\if :type_ok
\else
    DO $$ BEGIN RAISE EXCEPTION 'verification_type_id is not a row of mercury.template_types'; END $$;
\endif
\if :severity_ok
\else
    DO $$ BEGIN RAISE EXCEPTION 'severity_id is not a row of mercury.severity_type'; END $$;
\endif
\if :frequency_ok
\else
    DO $$ BEGIN RAISE EXCEPTION 'frequency_id is not a row of mercury.frequency_type'; END $$;
\endif

WITH wanted(location, description) AS (VALUES
    ('sk_verification_es.ftl', 'SmartKin - Email verification code (es)'),
    ('sk_verification_en.ftl', 'SmartKin - Email verification code (en)'))
INSERT INTO mercury.templates (description, location, file_format, template_type_id, application_id, severity_type_id)
SELECT w.description, w.location, 'ftl', :'verification_type_id'::uuid, :'app_id'::uuid, :'severity_id'::uuid
FROM wanted w
WHERE NOT EXISTS (SELECT 1 FROM mercury.templates t WHERE t.location = w.location AND t.application_id = :'app_id'::uuid);

-- a row registered with another type would never store the code: move it to the verification-code type
UPDATE mercury.templates
SET template_type_id = :'verification_type_id'::uuid
WHERE application_id = :'app_id'::uuid AND location LIKE 'sk\_verification\_%.ftl'
  AND template_type_id <> :'verification_type_id'::uuid;

INSERT INTO mercury.template_defined (template_id, user_id, application_id, frequency_type)
SELECT t.id, :'owner_id'::uuid, :'app_id'::uuid, :'frequency_id'::uuid
FROM mercury.templates t
WHERE t.application_id = :'app_id'::uuid AND t.location LIKE 'sk\_verification\_%.ftl'
  AND NOT EXISTS (SELECT 1 FROM mercury.template_defined d WHERE d.template_id = t.id);

SELECT t.location, d.id AS template_defined_id
FROM mercury.templates t JOIN mercury.template_defined d ON d.template_id = t.id
WHERE t.application_id = :'app_id'::uuid AND t.location LIKE 'sk\_verification\_%.ftl'
ORDER BY t.location;

COMMIT;
