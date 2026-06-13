---
name: Full Feature Delivery
description: >
  Orchestrate end-to-end delivery of a Mercury feature: schema, API contract,
  implementation, tests, code review, and PR creation.
mode: agent
agent: orchestrator
tools: [run_subagent, read_file, run_in_terminal, codebase_search]
---

## Input Variables

- `${featureName}` — human-readable name (e.g., "Campaign Pause/Resume")
- `${issueId}` — issue ID (e.g., `ds-174`)
- `${httpMethod}` — HTTP method
- `${endpointPath}` — full REST path (e.g., `/api/v1/campaigns/{id}/pause`)
- `${channelScope}` — `ALL` or specific channel (EMAIL, SMS, TELEGRAM, WHATSAPP, PUSH)
- `${requiresNewTable}` — `yes` | `no`
- `${isUserScoped}` — `yes` | `no` (does it require `BackboneClient` auth validation?)

## Delivery Plan

Before delegating, produce this table:

```
Delivery Plan — ${featureName} (${issueId})
${httpMethod} ${endpointPath}

Step | Agent              | Prompt / Input                         | Blocking Next?
-----|--------------------|-----------------------------------------|---------------
1    | database-architect | Schema for ${featureName}               | yes (if requiresNewTable=yes)
2    | api-reviewer       | review-api-contract.prompt.md           | yes
3    | developer          | implement-feature.prompt.md             | yes
4    | test-writer        | write-unit-tests.prompt.md              | yes
5    | code-reviewer      | review-code.prompt.md                   | yes
6    | security-reviewer  | security-audit.prompt.md (auth-only)    | yes (if isUserScoped=yes)
```

## Execution

Run each step sequentially. For each:
- If PASS: proceed to next
- If FAIL: invoke the remediation prompt, fix, then re-run the step

Remediation map:
| Failure | Remediation |
|---|---|
| PMD violations | `fix-lint-violations.prompt.md` |
| JaCoCo below threshold | `improve-coverage.prompt.md` |
| API contract issues | `review-api-contract.prompt.md` + re-implement |
| Security findings | `security-audit.prompt.md` → fix → re-audit |

## Mercury Invariants to Enforce

1. OpenAPI annotations only on `*Api` interfaces
2. `${prx.scheduler.*}` placeholders for all scheduler rates
3. Kafka topics from `bootstrap.yml` — never hardcoded
4. `EmailMessageDocument` lifecycle: OPENED → SENT → deleted
5. `ForbiddenException` → 403 for auth failures
6. PMD `AtLeastOneConstructor` — explicit constructor in every class
7. DDL strategy `none` — SQL migration in `src/main/resources/db/` for schema changes

## Final Checklist

- [ ] `mvn -B -V -e clean verify` passes
- [ ] LINE coverage ≥ 70%, BRANCH coverage ≥ 50%
- [ ] PMD: zero violations
- [ ] API contract: APPROVED by api-reviewer
- [ ] Code: APPROVED by code-reviewer
- [ ] Security: PASS (if isUserScoped=yes)
- [ ] PR opened targeting `main` or `develop`

## Output

```
Delivery Summary — ${featureName} (${issueId})

Step results:
  Step 1 — database-architect: PASS / SKIP
  Step 2 — api-reviewer:       PASS
  Step 3 — developer:          PASS
  Step 4 — test-writer:        PASS
  Step 5 — code-reviewer:      APPROVED
  Step 6 — security-reviewer:  PASS / SKIP

Files created: N
Build: PASS
Coverage: LINE X% / BRANCH Y%
PR: #N opened
```
