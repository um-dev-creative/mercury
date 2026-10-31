-- Mercury reference data that scripts/smartkin/seed-templates.sql needs before it can register SmartKin's templates, and that mercury needs
-- to send any e-mail. There is no API and no other script that creates these rows: the schema is managed outside the application.
--
--   mercury.template_types   a type for verification-code templates (mercury keeps a confirmable code only for templates of that type)
--   mercury.severity_type    INFO
--   mercury.frequency_type   ONCE
--   mercury.message_status_type   one row per DeliveryStatusType (OPENED, PENDING, IN_PROGRESS, SENT, DELIVERED, REJECTED, CANCELED, ABORTED,
--                            FAILED): MessageProcessor looks the status of each message up by name, so without them no mail is processed
--
-- It only ADDS what is missing (nothing existing is changed or deleted), looks rows up by name ignoring case, and prints the ids that
-- seed-templates.sql takes. NOT part of the application; run it with psql, first as a dry run, on a copy of the database if you can:
--
--   psql "$MERCURY_DB" -v ON_ERROR_STOP=1 -v dry_run=1 -f scripts/smartkin/bootstrap-reference-data.sql     # shows what exists, writes nothing
--   psql "$MERCURY_DB" -v ON_ERROR_STOP=1 -f scripts/smartkin/bootstrap-reference-data.sql                    # writes
--
-- Optional variables
--   dry_run=1                      run everything, print what would be created, and roll back
--   verification_type_id=<uuid>    REUSE this template type as the verification-code type (it must exist and be active)
--   create_new_type=1              create a verification-code type even though verification templates of another type exist
--   verification_type_name=...     name of the type to find or create          (default VERIFICATION_CODE)
--   severity_name=...              (default INFO)      frequency_name=...      (default ONCE)
--
-- MERCURY HAS ONE VERIFICATION-CODE TYPE: the one set in its configuration (umdc.verification.code.template.id, env
-- PRX_VERIFICATION_CODE_TEMPLATE_ID, which holds a template TYPE id). If other applications (zerobound's zb_verification_*) already have verification
-- templates, reuse their type with verification_type_id: a second type would leave one of the applications without a stored code. The script
-- refuses to create a new type in that situation unless you say create_new_type=1.

\set ON_ERROR_STOP on
\if :{?verification_type_name}
\else
    \set verification_type_name VERIFICATION_CODE
\endif
\if :{?severity_name}
\else
    \set severity_name INFO
\endif
\if :{?frequency_name}
\else
    \set frequency_name ONCE
\endif

BEGIN;

\echo
\echo '== What exists now'
\echo 'template types:'
SELECT id, name, active FROM mercury.template_types ORDER BY created_at;
\echo 'severity types:'
SELECT id, name, active FROM mercury.severity_type ORDER BY created_at;
\echo 'frequency types:'
SELECT id, name, active FROM mercury.frequency_type ORDER BY created_at;
\echo 'message status types:'
SELECT name, active FROM mercury.message_status_type ORDER BY name;
\echo 'templates that look like verification templates, and the type each one has:'
SELECT t.location, t.template_type_id, tt.name AS type_name
FROM mercury.templates t LEFT JOIN mercury.template_types tt ON tt.id = t.template_type_id
WHERE t.location ILIKE '%verification%' ORDER BY t.location;

-- 1. The verification-code type -------------------------------------------------------------------------------------------------------------
SELECT (SELECT count(DISTINCT template_type_id) FROM mercury.templates WHERE location ILIKE '%verification%') > 0 AS has_candidates \gset

\if :{?verification_type_id}
    SELECT EXISTS (SELECT 1 FROM mercury.template_types WHERE id = :'verification_type_id'::uuid AND active) AS explicit_ok \gset
    \if :explicit_ok
        \set vt :verification_type_id
        \set vt_how reused
    \else
        DO $$ BEGIN RAISE EXCEPTION 'verification_type_id is not an ACTIVE row of mercury.template_types (see the list above)'; END $$;
    \endif
\else
    \if :has_candidates
        \if :{?create_new_type}
            \set vt_how created
        \else
            DO $$ BEGIN RAISE EXCEPTION 'Verification templates of another type already exist (listed above). Mercury has ONE verification-code type: re-run with -v verification_type_id=<that type id> to reuse it, or -v create_new_type=1 to create a separate one on purpose.'; END $$;
        \endif
    \else
        \set vt_how created
    \endif
    SELECT EXISTS (SELECT 1 FROM mercury.template_types WHERE lower(name) = lower(:'verification_type_name') AND active) AS vt_named \gset
    \if :vt_named
        \set vt_how reused
    \endif
    INSERT INTO mercury.template_types (name, description)
    SELECT :'verification_type_name', 'Templates whose message carries a confirmable verification code (umdc.verification.code.template.id of mercury)'
    WHERE NOT EXISTS (SELECT 1 FROM mercury.template_types WHERE lower(name) = lower(:'verification_type_name') AND active);
    SELECT id AS vt FROM mercury.template_types WHERE lower(name) = lower(:'verification_type_name') AND active ORDER BY created_at LIMIT 1 \gset
\endif

-- 2. Severity and frequency -----------------------------------------------------------------------------------------------------------------
SELECT EXISTS (SELECT 1 FROM mercury.severity_type WHERE lower(name) = lower(:'severity_name') AND active) AS sev_existed,
       EXISTS (SELECT 1 FROM mercury.frequency_type WHERE lower(name) = lower(:'frequency_name') AND active) AS freq_existed \gset

INSERT INTO mercury.severity_type (name, description)
SELECT :'severity_name', 'Informational messages'
WHERE NOT EXISTS (SELECT 1 FROM mercury.severity_type WHERE lower(name) = lower(:'severity_name') AND active);
SELECT id AS sev FROM mercury.severity_type WHERE lower(name) = lower(:'severity_name') AND active ORDER BY created_at LIMIT 1 \gset

INSERT INTO mercury.frequency_type (name, description)
SELECT :'frequency_name', 'Sent once'
WHERE NOT EXISTS (SELECT 1 FROM mercury.frequency_type WHERE lower(name) = lower(:'frequency_name') AND active);
SELECT id AS freq FROM mercury.frequency_type WHERE lower(name) = lower(:'frequency_name') AND active ORDER BY created_at LIMIT 1 \gset

-- 3. Message status types: the names are the constants of DeliveryStatusType, which MessageProcessor looks up --------------------------------------
INSERT INTO mercury.message_status_type (name, description, created_at, updated_at, active)
SELECT s, 'Delivery status ' || s, now(), now(), true
FROM unnest(ARRAY['OPENED', 'PENDING', 'IN_PROGRESS', 'SENT', 'DELIVERED', 'REJECTED', 'CANCELED', 'ABORTED', 'FAILED']) AS s
WHERE NOT EXISTS (SELECT 1 FROM mercury.message_status_type m WHERE m.name = s);

-- 4. Result ---------------------------------------------------------------------------------------------------------------------------------
\echo
\echo '== Ids for scripts/smartkin/seed-templates.sql'
SELECT 'verification_type_id' AS variable, :'vt' AS id, :'vt_how' AS verification_type
UNION ALL SELECT 'severity_id', :'sev', CASE WHEN :'sev_existed'::boolean THEN 'reused' ELSE 'created' END
UNION ALL SELECT 'frequency_id', :'freq', CASE WHEN :'freq_existed'::boolean THEN 'reused' ELSE 'created' END;
\echo
\echo 'Next:'
\echo '  1. mercury configuration: PRX_VERIFICATION_CODE_TEMPLATE_ID =' :vt '(a template TYPE id; restart mercury if you changed it)'
\echo '  2. seed-templates.sql with -v verification_type_id=' :vt ' -v severity_id=' :sev ' -v frequency_id=' :freq
\echo '     plus -v app_id (SmartKin in backbone: APPLICATION_ID) and -v owner_id (a user of that application)'

\if :{?dry_run}
    ROLLBACK;
    \echo
    \echo 'DRY RUN: nothing was written.'
\else
    COMMIT;
\endif
