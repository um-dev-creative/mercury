-- Inserts the template types that the shipped templates (the .ftl files of the template storage) belong to, into mercury.template_types.
-- The types were defined from what each template says and from the parameters it needs, not from its file name:
--
--   VERIFICATION_CODE  The message carries a single-use confirmation code (`vc`) that expires (`expiresInMinutes`). Mercury keeps a
--                      confirmable code (user, application, expiry, attempts) ONLY for templates of this type, so there must be ONE such
--                      type and its id is the value of PRX_VERIFICATION_CODE_TEMPLATE_ID (umdc.verification.code.template.id).
--   FINANCIAL_ALERT    A warning triggered when something crosses a limit or needs attention now: budget exceeded (spent, limit, percent),
--                      price alert (symbol, threshold, price) and critical insight (title, summary).
--   PAYMENT_REMINDER   A reminder tied to a due date: debt due (debtName, dueDate, amount).
--   GOAL_ACHIEVEMENT   A congratulation when the person reaches something: goal reached (goalName, target).
--   PERIODIC_SUMMARY   A recurring digest of a period: weekly summary (periodStart, periodEnd, income, spending, net).
--   GENERIC_MESSAGE    A plain greeting with no business meaning (first_message_template: name, userEntity.alias); used to try the channel.
--
-- Which template kind belongs to which type (the "MAP" lines are read by TemplateTypesScriptTest, which fails if a template of the
-- storage has no type here). Every kind exists in Spanish and English (`_es`, `_en`); the zb_* notices also in Telegram (`_tg_es`, `_tg_en`).
--   MAP zb_verification                    VERIFICATION_CODE
--   MAP sk_verification                    VERIFICATION_CODE
--   MAP iris_verification                  VERIFICATION_CODE
--   MAP ds_new_user_verification_code      VERIFICATION_CODE
--   MAP zb_budget_exceeded                 FINANCIAL_ALERT
--   MAP zb_price_alert                     FINANCIAL_ALERT
--   MAP zb_critical_insight                FINANCIAL_ALERT
--   MAP zb_debt_due                        PAYMENT_REMINDER
--   MAP zb_goal_reached                    GOAL_ACHIEVEMENT
--   MAP zb_weekly_summary                  PERIODIC_SUMMARY
--   MAP first_message_template             GENERIC_MESSAGE
--
-- Safe to run more than once and on a database that already has types: a type whose name already exists (any case) is left as it is, so no
-- row is changed or duplicated, and nothing is deleted. If your environment already has a verification-code type under another name, do
-- NOT add this one: keep using that one (Mercury has ONE verification-code type) and delete the VERIFICATION_CODE line below before running.
-- It prints the types with their ids at the end: use the id of VERIFICATION_CODE as PRX_VERIFICATION_CODE_TEMPLATE_ID, and the others as
-- `type_id` of scripts/zerobound/seed-templates.sql (type_id and verification_type_id MUST differ).
--
--   psql "$MERCURY_DB" -f scripts/templates/seed-template-types.sql
-- Verify it on a copy of the database first. Run it after Flyway has created mercury.template_types.

BEGIN;

INSERT INTO mercury.template_types (name, description)
SELECT v.name, v.description
FROM (VALUES
    ('VERIFICATION_CODE', 'Messages that carry a single-use confirmation code that expires (parameters vc and expiresInMinutes); Mercury keeps a confirmable code only for this type (umdc.verification.code.template.id)'),
    ('FINANCIAL_ALERT',   'Warnings triggered when a limit is crossed or something needs attention now: budget exceeded, price alert, critical insight'),
    ('PAYMENT_REMINDER',  'Reminders tied to a due date, such as a debt that is about to fall due'),
    ('GOAL_ACHIEVEMENT',  'Congratulations when the person reaches a goal'),
    ('PERIODIC_SUMMARY',  'Recurring digest of a period, such as the weekly summary of income, spending and net'),
    ('GENERIC_MESSAGE',   'Plain messages with no business meaning, such as a greeting used to try a channel')
) AS v(name, description)
WHERE NOT EXISTS (SELECT 1 FROM mercury.template_types t WHERE lower(t.name) = lower(v.name));

SELECT id, name, active, created_at FROM mercury.template_types ORDER BY name;

COMMIT;
