## POST /api/v1/auth/token — M2M session-token (no `uid`)

### Modification Log
| Name              | Detail | Date |
|-------------------| --- | --- |
| Luis Antonio Mata | Document the session-token-issuing operation (implemented by security-oauth's AuthAPi, not by a controller in this repo) | 2026-09-29 |
| Luis Antonio Mata | MER-11: remove `POST /api/v1/auth/session-token`; the end user's backbone `session-token` is now sent directly | 2026-10-06 |
| Luis Antonio Mata | MER-11: `POST /api/v1/auth/token` served by Mercury's own `AuthTokenController` — `session-token-bkd` no longer required | 2026-10-07 |

---

### Related Story/Requirement Link(s)
See `docs/architecture/session-token-authorization.md` for the full trust-model writeup.

---

## Description
Mercury never authenticates end users itself. This operation is implemented by `AuthTokenController` (`/api/v1/auth`), which replaces
`security-oauth`'s `AuthApiController` (excluded from the component scan in `MercuryApplication`
because it demanded a `session-token-bkd` header). It is `@SkipSessionValidation`, since a caller
requesting a token doesn't have one yet.

It issues a Mercury-signed `session-token` **without a `uid` claim**, for the pure M2M endpoints
`/api/v1/mail` and `/api/v1/verification-code`.

**User-scoped endpoints do not use this token.** Template Management
(`GET/PUT/DELETE /api/v1/templates/**`) and Campaign's `getByApplication`/`updateCampaign`/
`toggleCampaign`/`deleteCampaign` take the **end user's backbone-rest session token directly**
in the `session-token` header, plus the calling service's `Authorization: Bearer` token
(Campaign, ChannelType and Template Management all require both). The former `POST /api/v1/auth/session-token` exchange was removed.

### Implementation Notes
* The body's `alias`/`password` are checked against `umdc.mercury.login-clients`
  (`LoginClientProperties`), a small static registry of backend services allowed to call this
  endpoint (e.g. `DIRECTORY_BACKEND_LOGIN_ALIAS`/`DIRECTORY_BACKEND_LOGIN_PASSWORD`) — not an
  end user's credentials.
* No `session-token-bkd` is needed (an extra header is ignored): the registered alias/password is the
  caller's authentication, so failed attempts are throttled per alias + address (5 per 15 min by default,
  `umdc.security.login-throttle.*`) and audited. See `docs/architecture/session-token-authorization.md`.
* `POST /api/v1/auth/logout` (header `session-token`) revokes the presented token in Mercury (`204`).

---

### Endpoint
```bash
POST /api/v1/auth/token
```

## Request
### Header
| Field | Type | Description |
| --- | --- | --- |
| `Content-Type` | string | `application/json` |

### Body
```json
{
  "alias": "directory-backend",
  "password": "<login-client-password>"
}
```

## Response
### Body (200 OK)
```json
{
  "token": "<newly-minted Mercury-signed session-token, no uid>"
}
```

### Code Response
| Status Code | Description |
| --- | --- |
| 200 OK | Token generated successfully |
| 400 Bad Request | Blank `alias` |
| 401 Unauthorized | Alias/password not in the registered login-client registry |
| 429 Too Many Requests | Too many failed attempts for this alias and address; wait for `Retry-After` seconds |
| 406 Not Acceptable | Token generation unexpectedly produced a blank token |

## Examples (curl)

**Get a `session-token` for pure M2M endpoints (no `uid`):**
```bash
curl -s -X POST https://<mercury-host>/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"alias":"directory-backend","password":"<login-client-password>"}'
```

**Call a user-scoped endpoint** (the end user's backbone token, sent directly):
```bash
curl -s "https://<mercury-host>/api/v1/templates?applicationId=<app-uuid>" \
  -H "session-token: <sessionToken issued by backbone-rest POST /api/v1/session>"
```

## Where to find the live, complete spec
The authoritative spec — every `@RestController` bean in the running app — is served live via
springdoc (see `springdoc.*` in `application.yml`): `GET /v3/api-docs` and `GET /swagger-ui.html`.
