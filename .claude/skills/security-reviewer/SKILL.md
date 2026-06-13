---
agent: security-reviewer
version: 1.0
---

## 1. Project-Specific Patterns

### Auth flow
1. Client sends `session-token` request header
2. `AuthServiceImpl` calls `BackboneClient.validateSession(token)`
3. `BackboneClient` is a Feign client pointing to `https://prx-qa.backbone.tst/backbone`
4. `BackendFeignClientInterceptor` injects bearer token into outgoing Backbone requests
5. If validation fails → throw `ForbiddenException` → `GlobalExceptionHandler` maps to 403

### JWT
- `SessionJwtServiceImpl` generates session tokens
- Secret: `${APP_TOKEN_SECRET}` from Vault
- Expiration: `${APP_TOKEN_EXPIRATION}` from Vault
- Never log JWT secret values

### Vault-backed credentials
All secrets come through `bootstrap.yml` environment variable placeholders:
- `${APP_TOKEN_SECRET}` — JWT signing key
- `${BACKBONE_CLIENT_SECRET}` — OAuth client secret for Backbone
- `${MERCURY_CLIENT_SECRET}` — OAuth client secret for Mercury
- `${BACKBONE_PASSWORD}` / `${MERCURY_PASSWORD}` — client credentials
- SMTP: configured through `MailConfig` using Vault-backed env vars

## 2. Naming Conventions

- `ForbiddenException` (not `UnauthorizedException` — Mercury uses 403 for auth failures)
- `AuthServiceImpl` for auth validation logic
- `BackboneClient` for Feign client to auth service
- `BackendFeignClientInterceptor` for outgoing request auth injection

## 3. Error Handling

| Security Event | Exception | HTTP | Log Level |
|---|---|---|---|
| Invalid/missing session token | `ForbiddenException` | 403 | WARN |
| Missing required header | `ForbiddenException` | 403 | WARN |
| Expired token | `ForbiddenException` | 403 | WARN |

Never return 401 from Mercury — `ForbiddenException` → 403 is the project convention.

## 4. Key Files

- `src/main/java/com/prx/mercury/security/SessionJwtServiceImpl.java` — JWT token generation
- `src/main/java/com/prx/mercury/client/BackboneClient.java` — Feign client for session validation
- `src/main/java/com/prx/mercury/client/interceptor/BackendFeignClientInterceptor.java` — request auth injection
- `src/main/java/com/prx/mercury/api/v1/service/AuthServiceImpl.java` — auth orchestration
- `src/main/java/com/prx/mercury/api/v1/exception/ForbiddenException.java` — auth exception
- `src/main/resources/bootstrap.yml` — Vault env var placeholders
- `environment_variables.md` — documentation of all env vars

## 5. Constraints

- No secrets, tokens, or credentials in source code — all must be `${ENV_VAR}` placeholders
- User identity must come from validated session token, not from request body/query params
- SMTP credentials must flow through `MailConfig` — never in `EmailServiceImpl` directly
- New Vault-backed variables must be added to `environment_variables.md`

## 6. Checklist

- [ ] No hardcoded secrets in any `.java`, `.yml`, or `.properties` file
- [ ] User-scoped endpoints use `@RequestHeader("session-token")` and validate via `BackboneClient`
- [ ] Authorization failures throw `ForbiddenException` (not `IllegalArgumentException`)
- [ ] New `${ENV_VAR}` placeholders documented in `environment_variables.md`
- [ ] JWT secret sourced from `${APP_TOKEN_SECRET}` only
- [ ] No sensitive data (tokens, passwords) written to logs
