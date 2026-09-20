---
name: Pre-Pull-Request Gate
description: Blocking gate that runs before any PR is opened from a feature branch to main or develop. Enforces build health, PMD compliance, JaCoCo coverage, and code review sign-off.
trigger: pr-open
agents: [code-reviewer, test-writer]
auto-block: true
---

## Trigger Conditions

Activates when:
- A PR is opened or marked ready-for-review targeting `main` or `develop`
- Any `feature/*` branch is ready for merge

## Steps

1. **Build check** — must pass before any review:
   ```bash
   mvn -B -V -e clean verify
   ```
   If `BUILD FAILURE`: block PR, report PMD violations or JaCoCo failures.

2. **PMD check** (included in verify, but surface separately):
   - Parse `target/pmd.xml` for violations
   - Any violation → BLOCK with violation list

3. **JaCoCo threshold check** (included in verify):
   - LINE coverage ≥ 70% (BUNDLE)
   - BRANCH coverage ≥ 50% (PACKAGE)
   - Below threshold → BLOCK, invoke **test-writer** with `.claude/prompts/improve-coverage.prompt.md`

4. **Code review** — invoke **code-reviewer** with `.claude/prompts/review-code.prompt.md`:
   - Scope: `PR` with the PR number
   - Must return APPROVED

5. **API contract check** — if diff touches `*Api.java` files:
   - Invoke **api-reviewer** with `.claude/prompts/review-api-contract.prompt.md`
   - Must return APPROVED

## Fail Behavior

- Any BLOCK condition prevents PR merge
- Report includes: failing step, command output, and remediation prompt to run
- Non-blocking findings (MINOR from code-reviewer) are surfaced as PR comments, not blocks

## Output

```
Pre-PR Gate Results:
  [PASS/FAIL] mvn clean verify
  [PASS/FAIL] PMD — N violations
  [PASS/FAIL] JaCoCo — LINE: X%, BRANCH: Y%
  [PASS/FAIL] Code Review — APPROVED / REQUEST_CHANGES
  [PASS/FAIL] API Contract Review (if applicable)

Gate Status: PASS / BLOCKED
```
