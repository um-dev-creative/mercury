---
name: fix-bug
description: Diagnose and fix a bug in Mercury with full test coverage for the fix
mode: agent
agent: developer
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - insert_edit_into_file
  - replace_string_in_file
  - create_file
  - get_errors
---
# Fix Bug

## Input Variables
- `${bugDescription}` — description of the bug (symptoms, error message, stack trace)
- `${affectedClass}` — fully qualified class name where bug is suspected (e.g., `com.umdc.mercury.api.v1.service.CampaignServiceImpl`)
- `${reproducedBy}` — steps or test case that reproduces the bug
- `${ticketId}` — optional issue/ticket ID

## Step 1: Create Hotfix Branch
```bash
git checkout main && git pull origin main
git checkout -b hotfix/${ticketId:-fix}-${bugDescription// /-}
```

## Step 2: Reproduce the Bug
Attempt to write a failing test that demonstrates the bug:
```bash
mvn -Dtest=${affectedClass}Test surefire:test
```
If no test exists for the failing scenario, note it — you will add one in Step 4.

## Step 3: Locate Root Cause
1. Read `${affectedClass}` source file
2. Identify the incorrect logic branch
3. Check if the bug relates to a known Mercury exception:
   - `CampaignNotFoundException` not thrown → resource silently missing
   - `ForbiddenException` not thrown → access incorrectly granted
   - `IllegalStateException` not thrown → disabled channel processed
   - `IllegalArgumentException` not thrown → invalid input accepted
4. Check related files: mapper, entity, repository, controller

## Step 4: Apply Fix
Apply minimal change to fix the root cause:
- If exception not thrown: add `throw new <AppropriateException>(...)` 
- If wrong return value: correct the mapping or query
- If race condition: ensure `CompletableFuture<T>` pattern used correctly
- If DB issue: check if a Flyway migration is needed (consult `database-architect`)

Rules:
- NEVER break existing behavior (other tests must still pass)
- NEVER add `System.out.println` for debugging — use `log.debug()`
- NEVER skip the explicit constructor requirement
- Add `log.warn` or `log.error` at the fix point for observability

## Step 5: Write/Update Tests
Add or update test in `src/test/java/.../` to:
1. Reproduce the original bug (should FAIL before fix)
2. Verify the fix (should PASS after fix)
3. Name: `shouldFix${BugName}When${Condition}()`

```bash
mvn -Dtest=${affectedClass}Test surefire:test
```

## Step 6: Run Full Test Suite
```bash
mvn clean test
```
All tests must pass. Fix any regressions introduced by the change.

## Step 7: PMD Check
```bash
mvn pmd:check
```
Must be 0 violations.

## Step 8: Verify Coverage
```bash
mvn -Pcoverage clean test
# Check target/site/jacoco/index.html — affected class must meet 70% line / 50% branch
```

## Constraints
- NEVER apply a fix that introduces a PMD violation
- NEVER merge without a test that proves the bug is fixed
- NEVER downgrade coverage below 70% line / 50% branch
- If schema change is needed for the fix: create Flyway migration first via `database-architect`

## Output Format
1. Root cause description (1–3 sentences)
2. Files modified with change summary
3. New/updated test class and method name
4. `mvn clean test` output (all pass)
5. PMD violations = 0 confirmation
6. Coverage delta for affected class
