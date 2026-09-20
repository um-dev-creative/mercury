---
name: review-code
description: Perform a full code review of a Mercury PR or diff against all coding standards
mode: agent
agent: code-reviewer
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - get_errors
---
# Review Code

## Input Variables
- `${prBranch}` — branch name to review (e.g., `feature/campaign-progress`)
- `${baseBranch}` — base branch (default: `main`)
- `${changedFiles}` — optional: comma-separated list of changed files (or `auto-detect`)

## Step 1: Identify Changed Files
```bash
git --no-pager diff --name-only ${baseBranch}...${prBranch}
```

## Step 2: Run PMD
```bash
git checkout ${prBranch}
mvn pmd:check 2>&1
```
Expected: 0 violations. Any violation is a BLOCK.

## Step 3: Run Full Build and Tests
```bash
mvn -B -V -e clean verify
```
Must produce `BUILD SUCCESS`. Test failures or coverage failures are BLOCKs.

## Step 4: Architecture Layer Review

### Check 1: OpenAPI Annotation Placement
For each changed `*Controller` file:
```bash
grep -n "@Tag\|@Operation\|@ApiResponse" src/main/java/com/umdc/mercury/api/v1/controller/*Controller.java
```
Result must be empty. Any hit = BLOCK.

### Check 2: Hardcoded Kafka Topics
```bash
grep -rn 'topics\s*=\s*"[^$]' src/main/java/com/umdc/mercury/
```
Result must be empty. Any literal string topic = BLOCK.

### Check 3: Hardcoded Scheduler Rates
```bash
grep -rn 'fixedRate\s*=\s*[0-9]' src/main/java/com/umdc/mercury/
```
Result must be empty. Any literal rate = BLOCK.

### Check 4: MapStruct Instantiation
```bash
grep -rn 'new .*MapperImpl()' src/main/java/com/umdc/mercury/
```
Result must be empty. = BLOCK.

### Check 5: Logging
```bash
grep -rn 'System\.out\.\|printStackTrace' src/main/java/com/umdc/mercury/
```
Result must be empty. = BLOCK.

### Check 6: Explicit Constructors
For each new class in the diff — verify explicit constructor exists.

### Check 7: Async Return Types
For each new write service method — verify `CompletableFuture<T>` return type.

### Check 8: Exception Handling
Verify exceptions thrown match the mapping table:
- CampaignNotFoundException → 404
- ForbiddenException → 403
- IllegalStateException → 422
- IllegalArgumentException → 400

### Check 9: Secrets
```bash
grep -rn 'password\s*=\s*"[^$]\|secret\s*=\s*"[^$]\|token\s*=\s*"[^$]' \
  src/main/java/com/umdc/mercury/ src/main/resources/
```
Result must be empty. = BLOCK.

### Check 10: DDL Changes
If JPA entity changed without a corresponding Flyway migration → BLOCK.

## Step 5: Coverage Review
```bash
mvn -Pcoverage clean test
# Check target/site/jacoco/jacoco.xml for changed classes
# Line ≥ 70%, Branch ≥ 50% required
```

## Step 6: Naming Conventions
Verify all new files follow conventions:
- `*Api` / `*Controller` / `*ServiceImpl` / `*Entity` / `*Repository`
- `*Document` / `*Mapper` / `*Request` / `*Response` / `*TO`
- DTO types are records (not classes)

## Step 7: Compile Verdict

### APPROVE conditions (all must be true):
- PMD violations = 0
- All tests pass
- Line coverage ≥ 70%, branch coverage ≥ 50%
- No architecture violations (Checks 1–10 all clean)
- Naming conventions followed

### REQUEST_CHANGES conditions (any one triggers):
- PMD violations > 0
- Test failures
- Coverage below threshold
- Any BLOCK finding from Checks 1–10

## Constraints
- NEVER approve with PMD violations > 0
- NEVER approve missing coverage
- NEVER approve hardcoded topics, rates, or secrets

## Output Format
1. Changed files list
2. PMD result (0 violations or list)
3. Build result (PASS/FAIL)
4. Architecture checks table: Check | Result | Files | Lines
5. Coverage per changed class: Class | Line% | Branch%
6. Naming violations (if any)
7. **Verdict: APPROVE / REQUEST_CHANGES**
8. Blocking issues list (for REQUEST_CHANGES)
9. Advisory issues list
