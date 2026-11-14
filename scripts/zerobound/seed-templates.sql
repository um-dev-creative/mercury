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
-- It prints each template_defined id for reference only: since decision D-15 zerobound-rest names its templates (zb_<kind> + language) and mercury resolves
-- them, so no id goes in any configuration. The `language` column is set here (migration V2034 fills it for older rows). Verify on a copy of the database first.

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
INSERT INTO mercury.templates (description, location, file_format, language, template_type_id, application_id, severity_type_id)
SELECT w.description, w.location, 'ftl', substring(w.location from '_([a-z]{2,3})\.ftl$'),
       CASE WHEN w.location LIKE 'zb\_verification\_%' THEN :'verification_type_id'::uuid ELSE :'type_id'::uuid END,
       :'app_id'::uuid, :'severity_id'::uuid
FROM wanted w
WHERE NOT EXISTS (SELECT 1 FROM mercury.templates t WHERE t.location = w.location AND t.application_id = :'app_id'::uuid);

-- rows created before T-96 carry the ordinary type: move the verification templates to the verification-code type
UPDATE mercury.templates
SET template_type_id = :'verification_type_id'::uuid
WHERE application_id = :'app_id'::uuid AND location LIKE 'zb\_verification\_%.ftl'
  AND template_type_id <> :'verification_type_id'::uuid;

-- The subject of each e-mail lives in the template (V2035; editable from Iris), not in zerobound-rest. Fill the ones that have none yet with the
-- subjects zerobound used to send; a subject already set (edited from Iris) is never overwritten.
WITH subjects(kind, lang, subject) AS (VALUES
    ('verification', 'es', 'Verifica tu correo en ZeroBound'), ('verification', 'en', 'Verify your email on ZeroBound'),
    ('debt_due', 'es', 'Una deuda vence pronto'), ('debt_due', 'en', 'A debt is due soon'),
    ('budget_exceeded', 'es', 'Superaste un presupuesto'), ('budget_exceeded', 'en', 'You went over a budget'),
    ('goal_reached', 'es', 'Alcanzaste una meta'), ('goal_reached', 'en', 'You reached a goal'),
    ('critical_insight', 'es', 'Aviso importante sobre tus finanzas'), ('critical_insight', 'en', 'Important notice about your finances'),
    ('price_alert', 'es', 'Alerta de precio'), ('price_alert', 'en', 'Price alert'),
    ('weekly_summary', 'es', 'Tu resumen semanal'), ('weekly_summary', 'en', 'Your weekly summary'))
UPDATE mercury.templates t
SET subject = s.subject
FROM subjects s
WHERE t.application_id = :'app_id'::uuid AND t.location = 'zb_' || s.kind || '_' || s.lang || '.ftl'
  AND (t.subject IS NULL OR t.subject = '');

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
