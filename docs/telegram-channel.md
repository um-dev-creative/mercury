# Telegram channel (T-111, zerobound decision D-11)

mercury owns the bot token, the `chat_id` of every user and the delivery. This page covers the bot and its configuration; the rest of
the channel (T-112…T-117) is described in zerobound-rest `docs/TELEGRAM_CHANNEL_PLAN.md`.

## Configuration

| Property | Variable | Owner | Default |
|---|---|---|---|
| `umdc.mercury.telegram.enabled` | `TELEGRAM_ENABLED` | env file | `false` |
| `umdc.mercury.telegram.bot-token` | `TELEGRAM_BOT_TOKEN` | **Vault** | empty (= channel off) |
| `umdc.mercury.telegram.webhook-secret` | `TELEGRAM_WEBHOOK_SECRET` | **Vault** | empty |
| `umdc.mercury.telegram.api-base-url` | `TELEGRAM_API_BASE_URL` | env file | `https://api.telegram.org` |

Never put the token or the secret in an env file, the repository or a log: an env var shadows Vault, and the token travels in the Bot API
URL, so errors are logged by class only. At start-up mercury calls `getMe` once and logs `Telegram bot @<name> reachable` (or a warning
that never stops the service).

## Creating the bot (manual, once per environment)

1. In Telegram, talk to `@BotFather`: `/newbot` (name, username ending in `bot`). One bot per environment (QA and production differ).
2. `/setdescription`, `/setabouttext`, `/setuserpic`; `/setjoingroups` → *Disable* (private chats only).
3. `/setcommands` (Spanish; `/setcommands` with language code `en` for English): `start`, `ayuda`, `estado`, `desvincular`.
4. Store the token as `TELEGRAM_BOT_TOKEN` and a random 32-byte secret as `TELEGRAM_WEBHOOK_SECRET` in mercury's Vault path; set `TELEGRAM_ENABLED=true`.
5. Register the application's row: `scripts/telegram/bootstrap-channel-config.sql` (dry run first; `app_id` = the ZeroBound application in
   backbone, `bot_username` without `@`). It adds the `TELEGRAM` channel type and the `channel_config` row (`settings.botUsername`, languages, limits).

## Rotating the token

`/revoke` in BotFather issues a new token and invalidates the old one at once: update Vault, restart mercury, check the `reachable` line.
