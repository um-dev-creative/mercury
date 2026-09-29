# `session-token` and user-scoped authorization

How Mercury resolves the caller's identity from a `session-token` header, where that
token actually comes from, why one extraction path was a real vulnerability, and what's
still open.

## Where a `session-token` comes from

Mercury never authenticates end users itself — it only ever mints a `session-token` in
response to a caller (a registered backend, e.g. `directory-backend`) presenting a
`session-token-bkd`: a token backbone-rest already issued to the end user via its own
`POST /api/v1/session` (alias/password) or `POST /api/v1/session/token` (email/password).

Both operations below live in the shared `security-oauth` library, not in this repo
(`AuthAPi`/`AuthApiController`), mounted at `/api/v1/auth`. They look similar but do two
completely different things — **only one of them produces a token with a `uid` claim**:

| | `POST /api/v1/auth/token` (`accessToken`) | `POST /api/v1/auth/session-token` (`generateTokenSession`) |
|---|---|---|
| Headers | `session-token-bkd` (required) | `session-token-bkd` (required) |
| Body | `AuthRequest {alias, password}` — a registered login client's credentials (`umdc.mercury.login-clients`, e.g. `DIRECTORY_BACKEND_LOGIN_ALIAS`/`_PASSWORD`) | same |
| What it does with `session-token-bkd` | `AuthApiController.accessToken` calls `authService.validate(sessionTokenBkd)` → `BackbonePublicClient.validate(...)`, a **remote call to backbone-rest** to confirm the token is genuine — then discards it | Forwards it straight into `AuthServiceImpl.token(AuthRequest, String)`, which resolves its `uid` via `SessionJwtServiceImpl.getVerifiedUid` (local HS256 check, same shared secret — no network call needed) |
| Resulting Mercury `session-token` | **No `uid` claim** — only useful for pure M2M endpoints (`/api/v1/mail`, `/api/v1/verification-code`) | **Carries the verified `uid`** — this is the one Template Management API and Campaign's user-scoped endpoints (`getByApplication`, `updateCampaign`, `toggleCampaign`, `deleteCampaign`) require |

So: **to get a `session-token` you can use against `/api/v1/templates/**` or the
user-scoped campaign operations, call `POST /api/v1/auth/session-token`** with the end
user's backbone `sessionToken` in the `session-token-bkd` header and a registered login
client's alias/password in the body.

```bash
curl -s -X POST https://<mercury-host>/api/v1/auth/session-token \
  -H "Content-Type: application/json" \
  -H "session-token-bkd: <sessionToken issued by backbone-rest POST /api/v1/session>" \
  -d '{"alias":"'"$DIRECTORY_BACKEND_LOGIN_ALIAS"'","password":"'"$DIRECTORY_BACKEND_LOGIN_PASSWORD"'"}'
# → {"token": "<Mercury-signed session-token, carries uid>"}
```

## Two extraction mechanisms — only one is safe

| | `com.umdc.commons.util.JwtUtil.getUidFromToken` | `SessionJwtServiceImpl.getVerifiedUid` |
|---|---|---|
| Library | `com.umdc:commons` | `com.umdc:security-oauth` (Mercury-local wrapper) |
| Signature check | **None.** Splits the JWT on `.`, base64url-decodes the payload, parses it as a plain `org.json.JSONObject`, reads `uid`. | Verifies HS256 via `Jwts.parser().verifyWith(key)...parseSignedClaims(token)`, against the shared `APP_TOKEN_SECRET` — the same secret backbone-rest signs with. |
| Expiration check | None. | Enforced by the JJWT parser (`ExpiredJwtException` on parse). |
| Failure mode | Returns `null` silently on any malformed input. | Returns `Optional.empty()`. |
| Safe to use for an authorization decision? | **No — the caller controls every claim.** | Yes. |

`getVerifiedUid` was added to `SessionJwtServiceImpl` specifically so no call site needs
to duplicate this logic or fall back to the unverified helper. As of this writing,
`com.umdc.commons.util.JwtUtil.getUidFromToken` is not used anywhere in this codebase —
keep it that way. If you find yourself reaching for it, use `getVerifiedUid` instead.

## Why the unverified path was a real vulnerability

A `uid` claim is only trustworthy if something checked, cryptographically, that the
token was issued by Mercury or backbone-rest — not merely that a string shaped like a
JWT was present. Two call sites relied on the unverified path to make real
authorization decisions:

### 1. `AuthServiceImpl.token(AuthRequest, String sessionTokenBkd)` — token forging

`POST /api/v1/auth/session-token` (see above) exchanges a backbone-issued token for a
Mercury-signed one. The whole `AuthAPi` interface (in `security-oauth`) is
`@SkipSessionValidation` — reasonable for the sibling `accessToken` operation, whose
whole point is a caller that doesn't have a token yet — but this class-level annotation
silently exempted `generateTokenSession` too, even though *that* operation's entire job
is to trust a caller-supplied token. Unlike `accessToken` (which calls
`authService.validate(sessionTokenBkd)`, a real remote check against backbone-rest),
`generateTokenSession` never validated `sessionTokenBkd` at all — locally or remotely —
before this fix.

The old code decoded `sessionTokenBkd`'s `uid` without checking its signature, then
embedded that value in a **newly minted, validly-signed** Mercury session token. Since
Mercury and backbone-rest share `APP_TOKEN_SECRET`, that new token is indistinguishable
from a genuine one everywhere else in the system. Net effect: any caller who could
satisfy the static registered-login-client alias/password check (`LoginClientProperties`)
could mint a legitimate Mercury token for *any* `uid` of their choosing — full user
impersonation, propagating to every endpoint that trusts a Mercury session token (e.g.
the Template Management API, whose real authorization is delegated to backbone-rest's
ACL via that very token).

**Fix:** `sessionJwtService.getVerifiedUid(sessionTokenBkd)` — verified with the same
shared secret, so this is safe even though the token was issued by a different service.
Returns `400 Bad Request` (matching this endpoint's already-documented OpenAPI contract)
instead of minting a token when verification fails.

### 2. `CampaignController` — M2M identity substitution

`CampaignApi` is also `@SkipSessionValidation`, but for a different, legitimate reason:
`/api/v1/campaigns/**` is authenticated by `ManagedClientSecurityConfig`'s backbone
opaque-token chain, which verifies the *calling service's* identity (an M2M client like
directory-backend) and OAuth2 scope (`mercury:message:read`/`write`) — not an end user's.

Several operations *also* accept a `session-token` header to identify which end user
the M2M caller is acting on behalf of:

- `getByApplication` scopes the query itself: `campaignRepository.findByCreatedByAndApplicationId(userId, applicationId)`.
- `updateCampaign`/`toggleCampaign`/`deleteCampaign` gate a real ownership check in
  `CampaignServiceImpl`: `entity.getCreatedBy().equals(requesterId)`, throwing
  `ForbiddenException` otherwise.

Because that header was never verified (the interceptor was skipped, and the M2M
opaque-token check authenticates a completely different identity), any caller holding a
valid `mercury:message:*` scope token could self-craft a `session-token` with an
arbitrary `uid` and read, update, pause, or soft-delete *any other user's* campaigns.

**Fix:** `CampaignController` now injects `SessionJwtServiceImpl` and calls
`getVerifiedUid` before trusting the claim, throwing a new
`InvalidSessionTokenException` (mapped to `401`, matching `CampaignApi`'s
already-documented `401` response) when it fails.

### 3. `TemplateController` — hardened for defense in depth, not because it was exploitable

`TemplateApi` is *not* `@SkipSessionValidation`, so `SessionJwtInterceptor` already
verifies `session-token`'s signature before any of these controller methods run — by the
time `getUidFromToken` executed, the token was already known-good. This wasn't
exploitable today, but it was fragile: it silently depended on interceptor coverage
never changing, duplicated JWT-parsing logic the interceptor had already done, and
returned `null` on a malformed `uid` claim rather than failing loudly (a `null`
`requesterId` would have flowed into `TemplateServiceImpl` as an audit field). Switched
to `getVerifiedUid` for consistency and to retire the unverified helper from the
codebase entirely — real authorization for this API is delegated to backbone-rest's ACL
via `ApplicationAuthorizationService.assertPermission`, unaffected by this change.

## Open design questions (not changed in this pass)

These weren't touched because fixing them changes API/trust-boundary behavior rather
than hardening a check the code already intended to make — they need a product
decision, not just a security hardening pass:

- **`CampaignController.createCampaign`** attributes a new campaign to whatever
  `request.userId()` the caller supplies in the request body — no `session-token` is
  even accepted on this endpoint, so there's no separate claim to cross-check it
  against. If M2M callers are meant to be a trusted broker acting on behalf of any user
  they name, this is fine as-is; if not, the endpoint needs a verified identity source
  the same way the other four operations now have one.
- **`getById`/`getProgress`** perform no ownership check at all — any caller with a
  valid M2M scope can read any campaign or its progress by id, regardless of who created
  it. This may be intentional (M2M services often need broad read access), but it's
  inconsistent with the "only the owner can mutate" model enforced everywhere else in
  this controller. `getProgress` additionally still accepts a `session-token` header in
  its signature that it never reads — either wire it into an ownership check or drop the
  parameter.
