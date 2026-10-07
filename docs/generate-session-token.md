## POST /api/v1/auth/token and POST /api/v1/auth/session-token
Example: `POST /api/v1/auth/session-token`

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Document both session-token-issuing operations (implemented by security-oauth's AuthAPi, not by a controller in this repo) | 2026-09-29 |

---

### Related Story/Requirement Link(s)
See `docs/architecture/session-token-authorization.md` for the full trust-model writeup.

---

## Description
Mercury never authenticates end users itself. Every `session-token` a caller presents to
this app's own endpoints was minted by one of these two operations, both implemented by
`AuthAPi`/`AuthApiController` in the shared `security-oauth` library (mounted at
`/api/v1/auth` — **not** a controller class in this repo, which is why it never appears
in the hand-maintained `src/main/resources/api/openapi.yaml` unless someone adds it by
hand, as done here). Both are `@SkipSessionValidation`, since by definition a caller
requesting a token doesn't have one yet.

They look similar — same base path, same request/response shape — but only one of them
produces a token you can actually use against a user-scoped endpoint:

| | `POST /api/v1/auth/token` (`accessToken`) | `POST /api/v1/auth/session-token` (`generateTokenSession`) |
|---|---|---|
| What it verifies | Calls `authService.validate(sessionTokenBkd)` → a **remote** call to backbone-rest (`BackbonePublicClient.validate`) to confirm the backbone token is genuine — then **discards** it | Resolves `sessionTokenBkd`'s `uid` via `SessionJwtServiceImpl.getVerifiedUid` — a **local** HS256 check against the shared `APP_TOKEN_SECRET`, no network call |
| Resulting token | No `uid` claim | Carries the verified `uid` claim |
| Use it for | Pure M2M endpoints: `/api/v1/mail`, `/api/v1/verification-code` | User-scoped endpoints: `GET/PUT/DELETE /api/v1/templates/**`, and Campaign's `getByApplication`/`updateCampaign`/`toggleCampaign`/`deleteCampaign` |

**If you need a `session-token` for Template Management or the user-scoped Campaign
endpoints, call `POST /api/v1/auth/session-token`, not `POST /api/v1/auth/token`.**

### Implementation Notes
* Neither operation validates the request body's `alias`/`password` against an end
  user's credentials — they check `umdc.mercury.login-clients` (`LoginClientProperties`),
  a small static registry of backend services allowed to call this endpoint (e.g.
  `DIRECTORY_BACKEND_LOGIN_ALIAS`/`DIRECTORY_BACKEND_LOGIN_PASSWORD`). The end user's own
  authentication already happened at backbone-rest (`POST /api/v1/session` or
  `POST /api/v1/session/token`) — that's where `sessionTokenBkd` comes from.
* `session-token-bkd` is a **required** header on both operations, even though
  `accessToken` never forwards it into the response token — it's still needed there for
  the `validate()` check.
* `generateTokenSession`'s `AuthServiceImpl.token(AuthRequest, String)` used to trust
  `sessionTokenBkd`'s `uid` claim without verifying it at all — a real vulnerability
  (any registered login client could mint a legitimate Mercury token for an arbitrary
  `uid`). Fixed by verifying the signature locally before trusting the claim; see
  `docs/architecture/session-token-authorization.md` and the `CHANGELOG`.

---

### Endpoint
```bash
POST /api/v1/auth/token
POST /api/v1/auth/session-token
```

### Parameters
*N/A — no path or query parameters.*

---

## Request
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | `application/json` |
| `session-token-bkd` | string | Required on both operations. The token to validate/exchange — a backbone-rest-issued session token for `POST /api/v1/auth/session-token`. |

### Body
```json
{
  "alias": "directory-backend",
  "password": "<login-client-password>"
}
```

---

## Response
### Body (200 OK)
```json
{
  "token": "<newly-minted Mercury-signed session-token>"
}
```

### Code Response
| Status Code | Description |
| --- | --- |
| 200 OK | Token generated successfully |
| 400 Bad Request | Blank `alias`, unregistered login client, or (`session-token` only) `session-token-bkd` failed signature verification |
| 406 Not Acceptable | Token generation unexpectedly produced a blank token |

---

## Examples (curl)

**Get a `session-token` usable against Template Management / user-scoped Campaign endpoints:**
```bash
curl -s -X POST https://<mercury-host>/api/v1/auth/session-token \
  -H "Content-Type: application/json" \
  -H "session-token-bkd: <sessionToken issued by backbone-rest POST /api/v1/session>" \
  -d '{"alias":"directory-backend","password":"<login-client-password>"}'
# → {"token": "<Mercury-signed session-token, carries uid>"}
```

**Get a `session-token` for pure M2M endpoints only (no `uid`):**
```bash
curl -s -X POST https://<mercury-host>/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -H "session-token-bkd: <any backbone-issued token, only validated remotely>" \
  -d '{"alias":"directory-backend","password":"<login-client-password>"}'
# → {"token": "<Mercury-signed session-token, no uid>"}
```

**Use the resulting token:**
```bash
curl -s https://<mercury-host>/api/v1/templates?applicationId=<app-uuid> \
  -H "session-token: <token>"
```

---

## Where to find the live, complete spec
This file and `src/main/resources/api/openapi.yaml` document these two operations by
hand, since they're implemented by an external library and were previously missing from
both. The authoritative, always-current spec — reflecting every `@RestController` bean
in the running app, including these — is served live via springdoc (see
`springdoc.*` in `application.yml`):
* `GET /v3/api-docs` — full OpenAPI JSON
* `GET /swagger-ui.html` — interactive UI
