-- ZeroBound mail templates (T-71). Registers the 14 FreeMarker files zb_<kind>_<lang>.ftl as
-- mercury.templates + mercury.template_defined for the ZeroBound application. Idempotent: a template
-- whose location already exists is skipped.
--
-- Not run by the application (the schema is managed outside it). Run with psql, giving the ids that
-- already exist in your environment:
--   psql "$MERCURY_DB" -v app_id=<general.application id of ZeroBound> -v owner_id=<user id that owns the definitions> \
--        -v type_id=<mercury.template_types id of the ordinary notices> \
--        -v verification_type_id=<the template type configured as PRX_VERIFICATION_CODE_TEMPLATE_ID> \
--        -v severity_id=<mercury.severity_type id> -v frequency_id=<mercury.frequency_type id> \
--        -f scripts/zerobound/seed-templates.sql
-- type_id and verification_type_id MUST differ. mercury stores a confirmable code (user, application, expiry, attempts) only
-- for a message whose template is of the verification-code type, and then needs the parameter `vc`; the other zb_* templates
-- have none, so giving them that type would make every notice fail. Only zb_verification_{es,en} gets verification_type_id
-- (T-96); a database seeded before that is corrected by the UPDATE below.
-- It prints each template_defined id: those are the `templateDefinedId` values zerobound-rest puts in
-- its configuration (docs/zerobound-mail-templates.md). Verify on a copy of the database first.

BEGIN;

WITH kinds(kind, descr) AS (VALUES
    ('debt_due', 'Debt due date'), ('budget_exceeded', 'Budget exceeded'), ('goal_reached', 'Goal reached'),
    ('critical_insight', 'Critical insight'), ('price_alert', 'Price alert'), ('weekly_summary', 'Weekly summary'),
    ('verification', 'Email verification code')),
     langs(lang) AS (VALUES ('es'), ('en')),
     wanted AS (
         SELECT 'zb_' || k.kind || '_' || l.lang || '.ftl' AS location,
                'ZeroBound - ' || k.descr || ' (' || l.lang || ')' AS description
         FROM kinds k CROSS JOIN langs l)
INSERT INTO mercury.templates (description, location, file_format, template_type_id, application_id, severity_type_id)
SELECT w.description, w.location, 'ftl',
       CASE WHEN w.location LIKE 'zb\_verification\_%' THEN :'verification_type_id'::uuid ELSE :'type_id'::uuid END,
       :'app_id'::uuid, :'severity_id'::uuid
FROM wanted w
WHERE NOT EXISTS (SELECT 1 FROM mercury.templates t WHERE t.location = w.location AND t.application_id = :'app_id'::uuid);

-- rows created before T-96 carry the ordinary type: move the verification templates to the verification-code type
UPDATE mercury.templates
SET template_type_id = :'verification_type_id'::uuid
WHERE application_id = :'app_id'::uuid AND location LIKE 'zb\_verification\_%.ftl'
  AND template_type_id <> :'verification_type_id'::uuid;

INSERT INTO mercury.template_defined (template_id, user_id, application_id, frequency_type)
SELECT t.id, :'owner_id'::uuid, :'app_id'::uuid, :'frequency_id'::uuid
FROM mercury.templates t
WHERE t.application_id = :'app_id'::uuid AND t.location LIKE 'zb\_%.ftl'
  AND NOT EXISTS (SELECT 1 FROM mercury.template_defined d WHERE d.template_id = t.id);

SELECT t.location, d.id AS template_defined_id
FROM mercury.templates t JOIN mercury.template_defined d ON d.template_id = t.id
WHERE t.application_id = :'app_id'::uuid AND t.location LIKE 'zb\_%.ftl'
ORDER BY t.location;

COMMIT;
