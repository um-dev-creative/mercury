-- Reference data for the Telegram channel (T-111, zerobound decision D-11): the TELEGRAM channel type and the channel_config row of one
-- application. NOT part of the application (the schema is managed outside it, like scripts/smartkin): run it with psql, first as a dry run.
--
--   psql "$MERCURY_DB" -v ON_ERROR_STOP=1 -v dry_run=1 -v app_id=<ZEROBOUND application uuid> -v bot_username=<bot> -f scripts/telegram/bootstrap-channel-config.sql
--   psql "$MERCURY_DB" -v ON_ERROR_STOP=1 -v app_id=<uuid> -v bot_username=<bot> -f scripts/telegram/bootstrap-channel-config.sql
--
-- It only ADDS what is missing. The bot token and the webhook secret are NOT stored here: they live in Vault (TELEGRAM_BOT_TOKEN,
-- TELEGRAM_WEBHOOK_SECRET), so `credentials` stays NULL. `settings` holds the public bot username and the languages; `limits` the Bot API
-- rates T-115 respects (about 1 message/s per chat, about 30/s per bot).

\set ON_ERROR_STOP on
\if :{?app_id}
\else
    \echo 'app_id is required (the ZeroBound application uuid in backbone)'
    \quit
\endif
\if :{?bot_username}
\else
    \echo 'bot_username is required (without @)'
    \quit
\endif

BEGIN;

INSERT INTO mercury.channel_type (code, name, description, enabled, active)
SELECT 'TELEGRAM', 'Telegram', 'Telegram Bot API (private chats)', true, true
WHERE NOT EXISTS (SELECT 1 FROM mercury.channel_type WHERE upper(code) = 'TELEGRAM');

INSERT INTO mercury.channel_config (channel_type_id, application_id, settings, limits)
SELECT ct.id, :'app_id'::uuid,
       jsonb_build_object('botUsername', :'bot_username', 'languages', jsonb_build_array('es', 'en')),
       jsonb_build_object('messagesPerSecondPerChat', 1, 'messagesPerSecondPerBot', 30)
FROM mercury.channel_type ct
WHERE upper(ct.code) = 'TELEGRAM'
  AND NOT EXISTS (SELECT 1 FROM mercury.channel_config c WHERE c.channel_type_id = ct.id AND c.application_id = :'app_id'::uuid);

SELECT c.id, ct.code, c.application_id, c.settings, c.limits
FROM mercury.channel_config c JOIN mercury.channel_type ct ON ct.id = c.channel_type_id
WHERE upper(ct.code) = 'TELEGRAM' AND c.application_id = :'app_id'::uuid;

\if :{?dry_run}
ROLLBACK;
\echo 'dry run: nothing written'
\else
COMMIT;
\endif
