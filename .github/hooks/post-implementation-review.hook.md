---
name: post-implementation-review
description: Automated review triggered after a developer completes an implementation — validates code quality before test-writer engagement
trigger: post-implementation
agents:
  - code-reviewer
  - test-writer
auto-block: true
---
# Post-Implementation Review Hook

## Trigger Conditions
- **Event:** Developer signals implementation complete on `feature/*` or `hotfix/*` branch
- **File Filters:** Changes to `src/main/java/com/umdc/mercury/**/*.java`
- **Branch Filters:** `feature/*`, `hotfix/*`

## Steps (Ordered)

### Step 1: Compilation Check
**Agent:** `code-reviewer`
```bash
mvn -U clean package -DskipTests
```
**Pass condition:** `BUILD SUCCESS`
**Fail behavior:** BLOCK — route back to `developer`. Emit compilation errors.
Do not proceed to further steps until this passes.

### Step 2: PMD Static Analysis
**Agent:** `code-reviewer`
**Prompt:** `.github/prompts/fix-lint-violations.prompt.md`
```bash
mvn pmd:check
```
**Pass condition:** 0 violations
**Fail behavior:** BLOCK — emit violations table. Route fixes to `developer`.

### Step 3: Architecture Layer Scan
**Agent:** `code-reviewer`
**Prompt:** `.github/prompts/review-code.prompt.md`

Run targeted checks on changed files:
1. OpenAPI annotations not on `*Controller`
2. No hardcoded Kafka topic strings (`${prx.consumer.topics.*}` only)
3. No hardcoded scheduler rates (`${prx.scheduler.*}` only)
4. No `new *MapperImpl()`
5. All new classes have explicit constructors
6. Write methods return `CompletableFuture<T>`
7. No `System.out.println` / `e.printStackTrace()`

**Pass condition:** All checks clean
**Fail behavior:** BLOCK — emit check violations. Route to `developer`.

### Step 4: Schema Change Verification
**Agent:** `code-reviewer`
If any JPA entity in `com.umdc.mercury.jpa.sql.entity` was modified:
- Verify corresponding `V<YYYYMMDD><seq>__*.sql` file exists in `src/main/resources/db/migration/`

**Pass condition:** Flyway migration present for every entity change
**Fail behavior:** BLOCK — "Schema change detected without Flyway migration. Consult `database-architect`."

### Step 5: Coverage Baseline
**Agent:** `test-writer`
**Prompt:** `.github/prompts/improve-coverage.prompt.md`
```bash
mvn -Pcoverage clean test
```
Generate coverage baseline report for all changed classes.
Output current line% and branch% per class.

**Pass condition:** (informational only at this stage — coverage enforcement in pre-PR hook)
**Fail behavior:** ADVISORY — flag classes with 0% coverage for immediate test-writer attention.

### Step 6: Test-Writer Engagement Signal
If Step 5 identifies classes with coverage < 70% line or < 50% branch:
Emit task for `test-writer`: "Write unit tests for: [class list]"
Reference prompt: `.github/prompts/write-unit-tests.prompt.md`

## Fail Behavior
- Steps 1–4: **auto-blocking** — implementation is not reviewable until all pass
- Step 5–6: **advisory** — informs test-writer of immediate priorities

## Output (Summary Comment Structure)
```markdown
## Post-Implementation Review

| Check | Status | Details |
|---|---|---|
| Compilation | ✅ PASS / ❌ FAIL | BUILD SUCCESS / errors |
| PMD | ✅ PASS / ❌ FAIL | 0 violations / N violations |
| Architecture | ✅ PASS / ❌ FAIL | N violations |
| Flyway Migration | ✅ PASS / ❌ FAIL / ➖ N/A | Migration present / missing |
| Coverage Baseline | ℹ️ INFO | Class: X% line / Y% branch |

**Implementation Review: READY FOR TESTS / BLOCKED**

### Blocking Issues
- [list with file:line:rule references]

### Test-Writer Tasks
- [ ] Write tests for: [class list]
  - Use prompt: `.github/prompts/write-unit-tests.prompt.md`
```
