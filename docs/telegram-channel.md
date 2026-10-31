# Telegram channel (T-111, zerobound decision D-11)

End-user behavior: [user guide](user-guide/en/telegram.md).

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

## Sending (T-115)

Run `V2030__telegram_messages.sql`. `POST /api/v1/telegram/messages` `{userId, kind, language?, params?, disableNotification?}` with an
`Idempotency-Key` (scoped to the caller) answers 202 `{id, status}` (a replay adds `Idempotent-Replayed: true`); 400 unknown template/language,
409 `{code: TELEGRAM_NOT_LINKED | TELEGRAM_BLOCKED}`, 422 key reused with another request. `GET /api/v1/telegram/messages/{id}` →
`QUEUED | SENT | FAILED | BLOCKED` (404 for another caller's).

`TelegramSender` (every second, `umdc.mercury.telegram.send-delay`) claims due rows with `FOR UPDATE SKIP LOCKED` and a 2-minute lease, renders
`zb_<kind>_tg_<lang>.ftl` (T-116), splits texts over 4096 characters at line breaks (it resumes after the parts already sent), and sends with the
Bot API. Rates per instance: 1 message/s per chat and 30/s per bot (a message that does not fit waits a second, no attempt spent); 429 waits
`retry_after`; 403 blocks the link (the next send answers 409); other 4xx fail; 5xx and timeouts retry after 1 min, 5 min, 30 min, 2 h, 12 h
(the 6th failure or 48 h is `FAILED`). Final rows lose their parameters. The Kafka path (`mercury-unified-message`, campaigns by `chatId`) is a
separate flow: it still only persists, and ZeroBound's channel does not use it.

## Exposing the webhook in production (T-117)

Telegram only posts to **HTTPS on 443/8443 with a certificate from a public CA** (not the internal `umdc-local-ca`), so this needs the production
host name; QA keeps `TELEGRAM_MODE=poll`. Nothing else of mercury may become public: expose exactly one path.

nginx (a `server` block on the public name, separate from the internal one; adjust the upstream to the production mercury):

```nginx
location = /telegram-webhook/zerobound {           # alias = TELEGRAM_APPLICATION
    limit_except POST { deny all; }
    client_max_body_size 1m;                       # mercury also refuses > 1 MB with 413
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass https://mercury/api/v1/telegram/webhook/zerobound;
}
location / { return 404; }                         # no other mercury route on this name
```

Optional hardening: allow only Telegram's published ranges (`149.154.160.0/20`, `91.108.4.0/22`; check the current list in Telegram's docs)
with `allow`/`deny`. The secret header is the real authentication: without a correct `X-Telegram-Bot-Api-Secret-Token` mercury answers 401.

Procedure (once per environment, secrets only in Vault, never in the shell history or a chat):
1. Set in Vault `TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_SECRET` (32 random bytes, `A-Z a-z 0-9 _ -`, 1–256 chars); in the environment
   `TELEGRAM_ENABLED=true`, `TELEGRAM_MODE=webhook`, `TELEGRAM_APPLICATION=<login alias>`,
   `TELEGRAM_WEBHOOK_URL=https://<public host>/telegram-webhook/<alias>`.
2. Restart mercury: it calls `setWebhook` and logs `Telegram webhook registered`.
3. Check: `getWebhookInfo` (via the Bot API, token from Vault) shows the URL, `pending_update_count` near 0 and no `last_error_message`;
   `/start` to the bot answers; `curl -i -X POST <url> -H 'Content-Type: application/json' -d '{}'` without the secret answers **401**;
   any other path of the public host answers 404.
4. Rotating the secret: change `TELEGRAM_WEBHOOK_SECRET` in Vault and restart (mercury re-registers with the new one). Rotating the token:
   BotFather `/revoke`, update Vault, restart. To go back to polling: `TELEGRAM_MODE=poll` (the poller deletes the webhook).
