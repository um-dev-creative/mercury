# ZeroBound in Mercury: login client, mail API and templates (T-71)

Mercury is the only place that talks to SMTP. zerobound-rest queues notifications in its own outbox and calls
`POST /api/v1/mail` (decision D-8, `zerobound-rest/docs/decisions/D-8-delivery-to-mercury.md`).

## 1. Who may call `/mail` and `/verification-code`
Only a **backend token Mercury minted itself** (`POST /api/v1/auth/token {alias, password}`): `iss`/`aud` are
Mercury's own (`umdc.security.jwt.issuer`/`audience`), there is no `uid`, and the subject is the login-client alias.
A backbone-rest user session (trusted by the generic session gate because of `trusted-issuers`) is answered `401` there.
`MachineTokenInterceptor` enforces it; the caller alias is available to controllers as the `mercury.caller` request attribute.

### Login client of zerobound
`application.yml` → `umdc.mercury.login-clients` has two ZeroBound entries, fed from Vault by the environment:

| Variable | Use |
|---|---|
| `ZEROBOUND_LOGIN_ALIAS` / `ZEROBOUND_LOGIN_PASSWORD` | the client zerobound-rest uses (its Vault keys `MERCURY_CLIENT_ALIAS` / `MERCURY_CLIENT_PASSWORD` hold the same pair) |
| `ZEROBOUND_NEXT_LOGIN_ALIAS` / `ZEROBOUND_NEXT_LOGIN_PASSWORD` | second slot for **rotation without downtime**: set the next pair here, switch zerobound-rest to it, then move it to the first slot and clear this one |

A client with an empty alias or password never matches. Tokens already minted live out their short expiry.

## 2. `POST /api/v1/mail`
Same body as before (`SendEmailRequest`), now implemented (it used to answer 501):

* checks that `templateDefinedId` exists (unknown → `400`), queues the message as `OPENED`; `SendEmailScheduler` renders the
  FreeMarker template and sends it, as for the other flows.
* `Idempotency-Key` (optional, scoped to the caller): the same key **and** the same request is accepted once, a repeat answers
  the first message with `Idempotent-Replayed: true`; the same key with a different request → `422`. Key and request are
  compared with a SHA-256 of every field, maps in key order.
* Answer: `{id, status: "OPENED", message}`; `id` is what the status call takes.

`GET /api/v1/mail/{id}` → `{id, status, createdAt, updatedAt}` with `OPENED`, `SENT` or `FAILED`; `404` for an unknown id, an id of
another caller or one past retention. The delivery log is the Mongo collection `mail_tracking` (no recipient, subject or
parameter; TTL `umdc.mail.tracking-retention`, default `30d`). `DELIVERED` is not reported: SMTP gives no receipt.

**Logging:** `SendEmailRequest.toString()` and `EmailMessageDocument.toString()` print ids, counts and dates only, never recipients,
subject, body or params, because the processor and the framework log them.

## 3. Templates (`src/main/resources/templates/zb_<kind>_<es|en>.ftl`)
The language is part of the file name, so zerobound picks the `templateDefinedId` of the user's language. Values are HTML-escaped
(`?html`); amounts arrive **already masked/formatted by zerobound** (`"≈ 300"` by default) and are printed as received. A missing
parameter makes the render fail instead of sending a half-empty mail. No uid, account number or free text of a transaction travels.

| kind | params |
|---|---|
| `debt_due` | `debtName`, `dueDate`, `amount` |
| `budget_exceeded` | `categoryName`, `period`, `spent`, `limit`, `percent` |
| `goal_reached` | `goalName`, `target` |
| `critical_insight` | `title`, `summary` |
| `price_alert` | `symbol`, `direction`, `threshold`, `price` |
| `weekly_summary` | `periodStart`, `periodEnd`, `income`, `spending`, `net` |
| `verification` | `vc`, `expiresInHours` |

`ZeroboundTemplatesTest` renders all 14 with exactly these params.

### Registering them in an environment
Templates are files (`TEMPLATE_PATH`, baked into the images from `src/main/resources/templates/`) **plus** rows in
`mercury.templates` / `mercury.template_defined`. `scripts/zerobound/seed-templates.sql` creates the rows (idempotent) and prints
the `template_defined` ids zerobound-rest needs. The schema is managed outside the application, so it was **not run** here: review it
against the target database first. The `application` row of ZeroBound in `general.application` must already exist.
