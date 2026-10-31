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

## Links (T-113)

Run `src/main/resources/db/migration/V2028__telegram_links.sql` against the `mercury` schema (tables `telegram_links`, `telegram_link_tokens`).
M2M routes (mercury-minted token; the caller's login alias is the application, so a caller never sees another's rows):

| Route | Answer |
|---|---|
| `POST /api/v1/telegram/links` `{userId, language?}` | 201 `{deepLink, expiresAt}`; 400 unsupported language; 503 bot off/unreachable |
| `GET /api/v1/telegram/links/{userId}` | `{status: NONE\|PENDING\|ACTIVE\|BLOCKED, linkedAt?, language?}`, never the chat id |
| `DELETE /api/v1/telegram/links/{userId}` | 204, idempotent |

Token: 32 random bytes, base64url (43 chars), stored as SHA-256, single use, 10 minutes; a new link invalidates the user's earlier tokens. One chat
per user and one user per chat per application: linking again replaces. The `/start <token>` side (`TelegramLinkService.redeem`) is wired in T-114.

## Receiving updates (T-114)

Run `V2029__telegram_inbox_state.sql` (last handled update per application, and the poller's lease). Settings: `TELEGRAM_APPLICATION` (the
login alias of the application, e.g. ZeroBound's `ZEROBOUND_LOGIN_ALIAS`: links are stored under it), `TELEGRAM_MODE` (`poll` | `webhook`),
`TELEGRAM_WEBHOOK_URL` (public HTTPS URL of `/api/v1/telegram/webhook/<alias>`, T-117).

| Mode | Where | How |
|---|---|---|
| `poll` | dev / QA (mercury is not reachable from the Internet) | `getUpdates` long polling; only the instance holding the lease polls; resumes after the last handled update |
| `webhook` | production | `setWebhook` at start-up with `secret_token`; `POST /api/v1/telegram/webhook/{alias}` checks `X-Telegram-Bot-Api-Secret-Token` in constant time (401), body ≤ 1 MB (413), alias must be the application (404) |

Every update is handled once (`update_id` only grows). Only private chats are read; the text is used to find the command and is never stored or
logged. Commands: `/start <token>` (links; a new link replaces the old chat and tells it), `/start` (how to link), `/ayuda` `/help`, `/estado`
`/status`, `/desvincular` `/unlink`. `my_chat_member` with `kicked`/`left`, or a 403 on a reply, marks the link `BLOCKED`.
