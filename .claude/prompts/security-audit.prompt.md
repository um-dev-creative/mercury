---
name: security-audit
description: Performs a security audit of Mercury changes focusing on auth, secrets, injection, and OWASP Top-10
mode: agent
agent: security-reviewer
tools: [Read, Bash]
---

## Input Variables

- `${auditScope}` — `PR` (audit current branch diff), `FULL` (audit entire codebase), or a specific package like `com.prx.mercury.security`
- `${prNumber}` — PR number if `${auditScope}` is `PR` (optional)

## Steps

1. **Get scope**:
   - For PR: `git diff main...HEAD`
   - For FULL: read all files in `src/main/java/com/prx/mercury/`

2. **Scan for hardcoded secrets**:
   ```bash
   grep -r "password\s*=\s*['\"]" src/main/
   grep -r "secret\s*=\s*['\"]" src/main/
   grep -r "token\s*=\s*['\"]" src/main/
   ```
   Any match that is not a `${ENV_VAR}` placeholder is a CRITICAL finding.

3. **Review auth flow** for any new or modified endpoints:
   - Does the endpoint access user-scoped data?
   - If yes: is `@RequestHeader("session-token")` present and validated via `BackboneClient`?
   - Is the auth validation done in `AuthServiceImpl` before the business operation?
   - Does auth failure throw `ForbiddenException` (not `IllegalArgumentException`)?

4. **Review JWT handling** in `SessionJwtServiceImpl`:
   - Is `${APP_TOKEN_SECRET}` the only source of the signing key?
   - Is `${APP_TOKEN_EXPIRATION}` used for token lifetime?
   - Are JWT claims validated properly?

5. **Review FeignClient interceptor**:
   - `BackendFeignClientInterceptor` must inject bearer token into outgoing Backbone requests
   - No plaintext credential logging in interceptor

6. **Check new `bootstrap.yml` entries**:
   - Any new `${ENV_VAR}` must be documented in `environment_variables.md`
   - No default values for secrets (e.g., `${APP_TOKEN_SECRET:hardcoded}` is forbidden)

7. **Check for injection vulnerabilities**:
   - JPQL queries: no string concatenation — use `@Query` with named parameters
   - No `Runtime.exec()` or `ProcessBuilder` with user-controlled input
   - FreeMarker templates: no `?eval` or `?interpret` with user input

8. **Check SMTP configuration**:
   - SMTP credentials must flow through `MailConfig` — not hardcoded in `EmailServiceImpl`
   - `MailConfig` reads from Vault-backed env vars

## Constraints

- Any CRITICAL finding (hardcoded secret, missing auth validation) must block merge
- HIGH findings must be remediated before merge
- MEDIUM findings should be tracked as follow-up issues
- Document all new `${ENV_VAR}` placeholders in `environment_variables.md`

## Output Format

Risk table:
| Finding | CWE | Severity | File:Line | Remediation |
|---|---|---|---|---|

Vault coverage:
| New ENV_VAR | Documented in environment_variables.md | Default Value Safe |
|---|---|---|

**Verdict: PASS / FAIL**
(FAIL = any CRITICAL or HIGH finding)
