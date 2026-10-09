# `session-token` and user-scoped authorization

How Mercury resolves the caller's identity from a `session-token` header, where that
token actually comes from, why one extraction path was a real vulnerability, and what's
still open.

## Where a `session-token` comes from

Mercury never authenticates end users itself, and it no longer mints a user-scoped token.
The end user's `session-token` is the one **backbone-rest issues** via its own
`POST /api/v1/session` (alias/password) or `POST /api/v1/session/token` (email/password), and
the client sends it **directly** in the `session-token` header. Mercury verifies it locally
(`SessionJwtServiceImpl.getVerifiedUid`: HS256 against the shared `APP_TOKEN_SECRET`, `exp`,
`iss`/`aud`, revocation — no network call) and reads the `uid` claim from it.

| Credential | Header | What it proves | Where it is checked |
|---|---|---|---|
| End user's backbone `session-token` | `session-token` | Which **user** the call is on behalf of (`uid`) | `SessionJwtInterceptor` (Template Management) / `getVerifiedUid` in `CampaignController` and `TemplateController` |
| Backbone opaque access token | `Authorization: Bearer <token>` | Which **service** is calling, and its scope (`mercury:message:read`/`write`) | `ManagedClientSecurityConfig` (`/api/v1/campaigns/**`, `/api/v1/channel-types/**`, `/api/v1/templates/**`; GET needs `mercury:message:read`, everything else `mercury:message:write`) |

```bash
curl -s https://<mercury-host>/api/v1/campaigns/application/<app-uuid> \
  -H "Authorization: Bearer <opaque access token>" \
  -H "session-token: <sessionToken issued by backbone-rest POST /api/v1/session>"
```

The former `POST /api/v1/auth/session-token` exchange (`session-token-bkd` → Mercury token with
`uid`) was removed: the exchange added a second hop with no benefit, since Mercury can verify
backbone's token itself.

`POST /api/v1/auth/token` (`accessToken`) no longer takes a `session-token-bkd`: it is served by
Mercury's own `AuthTokenController`, and `security-oauth`'s `AuthApiController` (which required
that header and also mapped the removed `/auth/session-token`) is excluded from the component
scan in `MercuryApplication`. The caller authenticates with its registered alias/password
(`umdc.mercury.login-clients`) alone and gets a token **without** a `uid`, for the pure M2M
endpoints (`/api/v1/mail`, `/api/v1/verification-code`) only. `/api/v1/auth/session-token` no
longer exists (404).

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

### 1. `AuthServiceImpl.token(AuthRequest, String sessionTokenBkd)` — token forging (route since removed)

`POST /api/v1/auth/session-token` (removed, see above) exchanged a backbone-issued token for a
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

## Claim validation and revocation (MER-11)

Signature + `exp` alone is not enough while the HS256 secret is shared with backbone-rest: any token
signed with it would be accepted, whoever issued it, whoever it was meant for, and even after the user
logged out. `SessionJwtServiceImpl.getTokenClaims` is the single trust gate (`SessionJwtInterceptor`
reaches it through the default `isValid`; `getVerifiedUid`, `getUsernameFromToken` and `revokeToken`
call it directly). In order, it checks:

1. **HS256 signature and `exp`**, and **`type=session-token`**. Backbone signs its 7-day refresh tokens
   (`type=refresh-token`, also carrying `uid`) with the same key; without the type check a refresh token
   would be accepted as a session.
2. **`iss`** is Mercury's (`umdc.security.jwt.issuer`) or one of `trusted-issuers`, and **`aud`** is
   Mercury's (`audience`) or one of `trusted-audiences`. Both are **mandatory** (`allow-missing-claims`
   defaults to `false`). `generateSessionToken` stamps both and discards caller-supplied `iss`/`aud`.
3. **`jti` not on Mercury's denylist** (`TokenRevocationService`, MongoDB `revoked_tokens`, TTL-purged at
   the token's own `exp`; needs `spring.data.mongodb.auto-index-creation: true`, set in `application.yml`).
4. **Still active in backbone-rest**, for any token whose `iss` is not Mercury's own
   (`BackboneSessionValidator` → `GET /api/v1/session/validate`). Backbone keeps its **own** `jti`
   deny-list, so this is what makes a logout at backbone-rest take effect in Mercury.

Every failure surfaces as `CertificateSecurityException` → the existing 401 contract, with no cause leaked
to the client. Both remote/IO checks **fail closed**: if MongoDB or backbone-rest cannot be reached the
token is rejected, never let through.

### Backbone logout is honoured, with a bounded delay

The call goes through Mercury's own `BackboneSessionClient`, **not** `security-oauth`'s
`BackbonePublicClient.validate`: backbone-rest reads the token from the `Authorization` header (raw JWT, no
`Bearer`), while the jar sends `session-token`, so the jar's client would get `400` and every Backbone token
would look invalid. `BackboneSessionClientContractTest` pins path and header.

Backbone answers `validate` with `true`/`false` (`false`: wrong type or revoked) or `401` (expired or
malformed). Only **positive** answers are cached, by SHA-256 of the token (never the token), for
`umdc.security.jwt.backbone-validation-cache-ttl` (default `30s`). That TTL is the worst-case delay before a
logout at backbone-rest is reflected here, and it bounds the extra remote call to one per token per TTL.
`umdc.security.jwt.validate-with-backbone=false` turns the check off.

### Trusted issuers / audiences

The end user's `session-token` is issued by backbone-rest, so its `iss`/`aud` must be accepted:
`umdc.security.jwt.trusted-issuers` / `trusted-audiences` (env `APP_TOKEN_TRUSTED_ISSUERS` /
`APP_TOKEN_TRUSTED_AUDIENCES`, comma-separated), on top of Mercury's own. The defaults (`backbone-rest` /
`backbone-rest-client`) are backbone-rest's own (`JWT_ISSUER` / `JWT_AUDIENCE` in its `application.yml`);
backbone-rest only stamps them when they are configured. If an environment's backbone-rest does not stamp
them, set `APP_TOKEN_ALLOW_MISSING_CLAIMS=true` there (an absent claim is then tolerated; a present but
different one is still rejected) — and fix backbone-rest rather than leaving it on.

### Revoking a token: `POST /api/v1/auth/logout`

`POST /api/v1/auth/logout` with the `session-token` header verifies the token and adds its `jti` to
Mercury's denylist until its `exp` (`SessionJwtServiceImpl.revokeToken`), answering `204`. It revokes in
Mercury only; log the user out at backbone-rest as well to invalidate the token everywhere. A token with no
`jti` cannot be revoked individually and is refused.

## Authenticating client applications: `POST /api/v1/auth/token`

Callers (e.g. directory-backend) authenticate with a registered alias/password
(`umdc.mercury.login-clients`); no `session-token-bkd` is needed (an extra header is ignored). Because that
credential is now the only barrier, failed attempts are **throttled** per alias + caller address
(`umdc.security.login-throttle.max-attempts` = 5, `window` = 15m): beyond that the endpoint answers `429`
with `Retry-After` without evaluating the password, a success clears the counter, and every success/failure
is logged (alias + address, never the password). The counter is per instance (N replicas ⇒ up to N× the
limit) and keyed by the address Spring sees: behind a proxy that hides it, all clients share one address, so
size `max-attempts` accordingly.

## Secrets

`umdc.security.jwt.secret` (`APP_TOKEN_SECRET`) has **no default**. It used to carry a committed dev value,
which would have let anyone with the repo forge a session-token in any environment where the variable was
unset. Now the app refuses to start without it. It must be base64, at least 256 bits, and the same value
backbone-rest signs with; supply it from Vault, or locally via `secrets/local.env` (see
`secrets/local.env.example`; `openssl rand -base64 48`).

## Closed design questions

- **`createCampaign`** now requires the end user's `session-token`; the verified `uid` must equal the
  request's `userId` (else `403`), so a campaign can no longer be attributed to an arbitrary user.
- **`getById` / `getProgress`** now require the `session-token` and enforce ownership like update / toggle /
  delete (`403` for a non-owner). One deliberate carry-over: a legacy campaign with no recorded owner
  (`createdBy` null) stays open to any authenticated caller on every operation, as before.
- **`/api/v1/templates/**`** sits behind the same Bearer chain as Campaign and ChannelType (read scope for
  GET, write scope otherwise) in addition to the end user's `session-token`.

## Verification

`SecurityFilterChainsTest` runs the real `ManagedClientSecurityConfig` / `SessionTokenSecurityConfig` chains
under `@WebMvcTest` with probe controllers: no Bearer → 401, inactive Bearer → 401, read scope → GET only,
write scope → writes only (403 otherwise), and `/auth/**` + `/mail` stay `permitAll` at the filter level.
