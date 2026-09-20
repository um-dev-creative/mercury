---
name: full-feature-delivery
description: Orchestrates end-to-end delivery of a Mercury feature from requirement to merged PR using all specialized agents
mode: agent
agent: orchestrator
tools: [Read, Edit, Write, Bash]
---

## Input Variables

- `${featureName}` — feature name (e.g., "Campaign Pause/Resume Toggle")
- `${issueId}` — YouTrack issue ID (e.g., `ds-173`)
- `${httpMethod}` — primary HTTP method (e.g., `PATCH`)
- `${endpointPath}` — endpoint path (e.g., `/api/v1/campaigns/{id}/pause`)
- `${channelScope}` — `ALL`, `EMAIL`, `SMS`, `TELEGRAM`, `WHATSAPP`, or `PUSH`
- `${requiresNewTable}` — `YES` or `NO` — whether a new database table or column is needed
- `${isUserScoped}` — `YES` or `NO` — whether the endpoint requires session-token auth

## Steps

### Phase 1 — Analysis (run first)

1. Read `../../src/main/java/com/umdc/mercury/api/v1/controller/CampaignApi.java` to understand existing patterns
2. Read `../../src/main/java/com/umdc/mercury/jpa/sql/entity/CampaignEntity.java` to understand current schema
3. Read `src/main/resources/bootstrap.yml` for config patterns
4. Produce a Delivery Plan table before any code changes

### Phase 2 — Persistence (if ${requiresNewTable} is YES)

Invoke **database-architect** via `.claude/prompts/implement-feature.prompt.md` scoped to:
- New entity fields or table
- SQL migration script in `src/main/resources/db/`
- Repository method additions
- MapStruct mapper updates

### Phase 3 — API Contract

Invoke **api-reviewer** via `.claude/prompts/review-api-contract.prompt.md`:
- Define `*Api` interface method signature
- Define request/response DTOs
- Get APPROVED before proceeding to implementation

### Phase 4 — Implementation

Invoke **developer** via `.claude/prompts/implement-feature.prompt.md`:
- Implement `*Api` interface update
- Implement `*ServiceImpl` method
- Implement `*Controller` method
- Run: `mvn -U clean package -DskipTests`

### Phase 5 — Tests

Invoke **test-writer** via `.claude/prompts/write-unit-tests.prompt.md`:
- Write unit tests for the new service method
- Write controller slice tests
- Run: `mvn clean test`

### Phase 6 — Coverage Check

If `mvn clean verify` fails on JaCoCo:
- Invoke **test-writer** via `.claude/prompts/improve-coverage.prompt.md`
- Target: 70% line / 50% branch

### Phase 7 — Code Review

Invoke **code-reviewer** via `.claude/prompts/review-code.prompt.md`:
- Must return APPROVED before Phase 8

### Phase 8 — Security Review (if ${isUserScoped} is YES)

Invoke **security-reviewer** via `.claude/prompts/security-audit.prompt.md` with `${auditScope}=PR`:
- Must return PASS before Phase 9

### Phase 9 — PR Creation

Invoke **devops-engineer**:
```bash
git checkout -b feature/${issueId}-${featureName.toLower.toHyphenated}
git add -p
git commit -m "feat: ${featureName} — ${httpMethod} ${endpointPath}"
gh pr create --title "feat: ${featureName}" --body "..."
```

## Constraints

- Phases execute in order — Phase N does not start until Phase N-1 passes
- A FAIL from code-reviewer or security-reviewer halts the pipeline
- `mvn clean verify` must pass at end of Phase 6 before code review
- No direct push to `main` — always via PR

## Output Format

Delivery Plan:
| Phase | Agent | Prompt | Status | Notes |
|---|---|---|---|---|

Final checklist:
- [ ] Schema migration created (if applicable)
- [ ] API contract reviewed and APPROVED
- [ ] Implementation compiles: `mvn -U clean package -DskipTests`
- [ ] Tests pass: `mvn clean test`
- [ ] Coverage thresholds met: `mvn clean verify`
- [ ] Code review: APPROVED
- [ ] Security review: PASS (if user-scoped)
- [ ] PR created: URL
