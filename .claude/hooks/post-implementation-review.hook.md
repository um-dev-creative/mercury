---
name: Post-Implementation Review
description: Non-blocking review triggered after a push to a feature branch. Provides early feedback on code quality without blocking development.
trigger: push-feature-branch
agents: [code-reviewer]
auto-block: false
---

## Trigger Conditions

Activates when:
- A commit is pushed to any `feature/*` branch
- Changed files include `.java` source files

## Steps

1. **Quick compile check**:
   ```bash
   mvn -U clean package -DskipTests
   ```
   If compile fails, report as WARNING — developer may be mid-implementation.

2. **PMD quick scan**:
   ```bash
   mvn pmd:check
   ```
   Surface violations as early warnings so they can be fixed before PR.

3. **Quick code review** — invoke **code-reviewer** with `.claude/prompts/review-code.prompt.md`:
   - Scope: `${reviewDepth}=quick` (BLOCKER findings only)
   - Focus: OpenAPI annotation placement, hardcoded scheduler rates, missing constructors

4. **Test existence check**:
   - For each new `*ServiceImpl.java` file, check if a corresponding `*ServiceImplTest.java` exists
   - For each new `*Controller.java`, check if `*ControllerTest.java` exists
   - Report missing test files as WARNINGS

## Fail Behavior

- This hook is non-blocking — it never prevents a push or commit
- Findings are surfaced as informational annotations
- PMD violations and missing tests are flagged as early warnings for pre-PR resolution

## Output

```
Post-Push Review (non-blocking):
  [INFO] Compile: PASS / WARNING (compile error)
  [INFO] PMD quick: PASS / N warnings
  [INFO] Convention check: N issues found
  [INFO] Missing tests: [list of classes without tests]

Action recommended: Run mvn clean test before opening PR
```
