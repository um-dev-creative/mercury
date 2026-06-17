---
name: full-feature-delivery
description: End-to-end orchestrated delivery of a Mercury feature from plan to merged PR
mode: agent
agent: orchestrator
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - insert_edit_into_file
  - replace_string_in_file
  - create_file
  - get_errors
  - run_subagent
---
# Full Feature Delivery

## Input Variables
- `${featureName}` — short feature identifier (e.g., `campaign-progress`)
- `${featureDescription}` — full description of what to build
- `${layersAffected}` — comma-separated: `persistence,api,service,controller,kafka,mapper,scheduler`
- `${securitySensitive}` — `true` | `false` (involves auth/JWT/Vault/ForbiddenException)
- `${newDependencies}` — `none` | comma-separated new Maven dependencies

## Phase 0: Delivery Plan

Produce the delivery plan table BEFORE any delegation:

| Step | Agent | Input | Expected Output | Blocking |
|---|---|---|---|---|
| 1 | database-architect | Feature description + affected entities | Flyway migration + Entity/Repository | Steps 2, 3 |
| 2 | api-reviewer | Feature description + DTO requirements | `*Api` interface + DTO records | Step 3 |
| 3 | developer | Migration ✓ + API contract ✓ + feature description | All source layers implemented | Step 4 |
| 4 | test-writer | Implemented source | Unit tests, ≥70% line, ≥50% branch | Step 5 |
| 5 | code-reviewer | Full diff | APPROVE / REQUEST_CHANGES | Step 6 |
| 6 | security-reviewer | Full diff (if ${securitySensitive}=true) | APPROVE / BLOCK | Step 7 |
| 7 | devops-engineer | bootstrap.yml changes (if Kafka/scheduler) | Config validated | Step 8 |
| 8 | orchestrator | All steps passed | PR created | — |

Adjust table: remove steps for layers not in `${layersAffected}`.
Add security-reviewer step only if `${securitySensitive}=true`.
Add devops-engineer step only if Kafka topics or scheduler rates need adding.

## Phase 1: Persistence (if `persistence` in ${layersAffected})
Delegate to `database-architect` via prompt `.github/prompts/implement-feature.prompt.md` (persistence section).

Wait for:
- Flyway migration file created
- JPA entities / MongoDB documents defined
- Repositories created

## Phase 2: API Contract (if `api` or `controller` in ${layersAffected})
Delegate to `api-reviewer` via prompt `.github/prompts/review-api-contract.prompt.md`.

Wait for:
- `*Api` interface with OpenAPI annotations
- DTO records defined
- HTTP status codes confirmed

## Phase 3: Implementation
Delegate to `developer` via prompt `.github/prompts/implement-feature.prompt.md`.

Inputs: Phase 1 artifacts + Phase 2 artifacts.

Wait for:
- All source layers compiled successfully
- PMD = 0 violations
- `mvn -U clean package -DskipTests` → BUILD SUCCESS

## Phase 4: Tests
Delegate to `test-writer` via prompt `.github/prompts/write-unit-tests.prompt.md` for each new/changed class.

Wait for:
- All tests pass
- Line coverage ≥ 70%, branch coverage ≥ 50%
- `mvn clean test` → BUILD SUCCESS

If coverage insufficient: re-delegate to `test-writer` via `.github/prompts/improve-coverage.prompt.md`.

## Phase 5: Code Review
Delegate to `code-reviewer` via prompt `.github/prompts/review-code.prompt.md`.

If APPROVE → proceed to Phase 6.
If REQUEST_CHANGES → route specific fixes back to `developer` or `test-writer`, then re-review.

## Phase 6: Security Review (if ${securitySensitive}=true)
Delegate to `security-reviewer` via prompt `.github/prompts/security-audit.prompt.md`.

If APPROVE → proceed to Phase 7.
If BLOCK → halt; route remediation to `developer`, repeat from Phase 5.

## Phase 7: DevOps Config (if Kafka or scheduler changes)
Delegate to `devops-engineer`:
- Add new Kafka topics to `bootstrap.yml` under `prx.consumer.topics.*`
- Add new scheduler rates to `bootstrap.yml` under `prx.scheduler.*`

## Phase 8: Create Pull Request
```bash
gh pr create \
  --base main \
  --head feature/${featureName} \
  --title "feat(${featureName}): ${featureDescription}" \
  --body "$(cat .github/PULL_REQUEST_TEMPLATE.md)"
```

## Constraints
- NEVER proceed to Phase 3 without Phase 1 AND Phase 2 complete
- NEVER create PR if any phase returned FAIL/BLOCK/REQUEST_CHANGES unresolved
- NEVER skip security review if `${securitySensitive}=true`
- NEVER create PR without `mvn -B -V -e clean verify` passing

## Output Format
1. Delivery Plan table (Phase 0)
2. Per-phase execution summary: Phase | Agent | Status | Artifacts
3. Final build: `mvn -B -V -e clean verify` → BUILD SUCCESS
4. Coverage summary: overall line% and branch%
5. PMD: 0 violations
6. PR URL
7. **Delivery Status: COMPLETE / BLOCKED** (with reason if blocked)
