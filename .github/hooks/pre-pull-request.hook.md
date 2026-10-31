---
name: pre-pull-request
description: Gate that runs before a Mercury PR is created or merged — validates build, PMD, coverage, and architecture
trigger: pre-merge
agents:
  - code-reviewer
  - test-writer
auto-block: true
---
# Pre-Pull-Request Hook

## Trigger Conditions
- **Event:** Pull request creation or update targeting `main` or `develop`
- **File Filters:** Any change under `src/` or `pom.xml` or `ruleset.xml`
- **Branch Filters:** `feature/*`, `hotfix/*`

## Steps (Ordered — stop on first failure)

### Step 1: Full Build Verification
**Agent:** `code-reviewer`
**Prompt:** (inline — no external prompt needed)
```bash
mvn -B -V -e clean verify
```
**Pass condition:** `BUILD SUCCESS` with exit code 0
**Fail behavior:** BLOCK — emit "Build failed. Fix compile errors and test failures before PR."

### Step 2: PMD Zero-Violation Gate
**Agent:** `code-reviewer`
**Prompt:** `.github/prompts/fix-lint-violations.prompt.md`
```bash
mvn pmd:check
```
**Pass condition:** 0 PMD violations
**Fail behavior:** BLOCK — emit list of PMD violations with file/line/rule. Route to developer for fix.

### Step 3: JaCoCo Coverage Gate
**Agent:** `test-writer`
**Prompt:** `.github/prompts/improve-coverage.prompt.md`
Verify from `target/site/jacoco/jacoco.xml`:
- Line coverage ≥ 70%
- Branch coverage ≥ 50%
**Pass condition:** Both thresholds met (enforced by `mvn verify` in Step 1)
**Fail behavior:** BLOCK — emit per-class coverage gaps. Route to `test-writer` for additional tests.

### Step 4: Architecture Conventions Check
**Agent:** `code-reviewer`
**Prompt:** `.github/prompts/review-code.prompt.md`
Run checks:
- No OpenAPI annotations on `*Controller` files
- No hardcoded Kafka topic strings
- No hardcoded scheduler rates
- No `new *MapperImpl()` instantiations
- No `System.out.println` / `e.printStackTrace()`
- No secrets in source
**Pass condition:** All checks clean
**Fail behavior:** BLOCK — emit violation table with file/line/description.

### Step 5: PR Template Completeness
**Agent:** `code-reviewer`
Verify PR description references `.github/PULL_REQUEST_TEMPLATE.md` checklist and checklist items are filled.
**Pass condition:** Checklist items present in PR body
**Fail behavior:** ADVISORY — emit reminder to complete PR template.

## Fail Behavior
- Steps 1–4 are **auto-blocking** — PR cannot merge until all pass
- Step 5 is **advisory** — warning comment only, does not block

## Output (Summary Comment Structure)
```markdown
## Pre-PR Gate Results

| Check | Status | Details |
|---|---|---|
| Build | ✅ PASS / ❌ FAIL | BUILD SUCCESS / error summary |
| PMD | ✅ PASS / ❌ FAIL | 0 violations / N violations |
| Line Coverage | ✅ PASS / ❌ FAIL | X% (≥70% required) |
| Branch Coverage | ✅ PASS / ❌ FAIL | X% (≥50% required) |
| Architecture | ✅ PASS / ❌ FAIL | N violations |
| PR Template | ✅ PASS / ⚠️ WARN | Complete / Incomplete |

**Gate: PASSED / BLOCKED**

### Blocking Issues (fix required)
- [list of blocking issues with file:line references]

### Advisory (fix recommended)
- [list of advisory items]
```
