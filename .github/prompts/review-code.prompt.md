---
name: Review Code
description: >
  Review Mercury source changes for correctness, PMD compliance, Spring Boot
  conventions, and layer separation. Returns APPROVED or REQUEST_CHANGES.
mode: ask
agent: code-reviewer
tools: [read_file, grep_search, codebase_search, run_in_terminal]
---

## Input Variables

- `${prScope}` — what to review: PR number | branch name | file list
- `${changeType}` — `feature` | `bugfix` | `refactor` | `config`

## Review Process

1. **Read changed files** identified by the PR diff or branch comparison
2. **Run quality gates**:
   ```bash
   mvn clean test          # PMD must pass
   mvn -B -V -e clean verify  # JaCoCo must pass
   ```
3. **Apply the checklist** from `code-reviewer.agent.md`
4. **Classify findings** as BLOCKING or MINOR

## Checklist Summary

**Layer Separation**
- [ ] OpenAPI annotations only on `*Api` interfaces
- [ ] Controllers are thin delegates — no business logic
- [ ] Services return `CompletableFuture` for write operations

**DTOs**
- [ ] Records in `com.prx.mercury.api.v1.to` with correct suffixes
- [ ] Bean Validation annotations on all input fields

**PMD**
- [ ] Every class has explicit constructor
- [ ] No unused imports, empty catch blocks, or hardcoded literals
- [ ] MapStruct mappers not instantiated with `new`

**Configuration**
- [ ] No hardcoded Kafka topics, scheduler rates, or secrets
- [ ] Vault-backed properties referenced via `${...}` placeholders

**Exception Mapping**
- [ ] `CampaignNotFoundException` → 404
- [ ] `ForbiddenException` → 403
- [ ] `IllegalStateException` → 422
- [ ] `IllegalArgumentException` → 400

## Output

```
Code Review — ${prScope} (${changeType})

BLOCKING:
- [file:line] Issue

MINOR:
- [file:line] Suggestion

PMD: PASS / N violations
JaCoCo: LINE X% BRANCH Y% — PASS / FAIL
Overall: APPROVED / REQUEST_CHANGES
```
