---
name: Fix Bug
description: >
  Diagnose and fix a Mercury bug: identify the root cause across layers,
  apply a minimal targeted fix, and verify with the relevant test.
mode: agent
agent: developer
tools: [read_file, grep_search, codebase_search, replace_string_in_file, run_in_terminal]
---

## Input Variables

- `${bugDescription}` — what is broken and observed symptom (e.g., "Campaign toggle returns 500 when campaign is disabled")
- `${affectedEndpoint}` — REST path or component (e.g., `PATCH /api/v1/campaigns/{id}/toggle`)
- `${errorOrStackTrace}` — exception message or stack trace excerpt (paste inline)
- `${issueId}` — issue ID (e.g., `ds-173`)

## Steps

1. **Locate the bug**:
   - Search for the endpoint or component: `grep -rn "${affectedEndpoint}" src/`
   - Read the controller, service, and relevant entity/repository files
   - Identify which layer the failure originates in

2. **Understand Mercury's layer flow**:
   ```
   Controller (*Api + *Controller)
     └─> Service (*ServiceImpl, CompletableFuture for writes)
           ├─> JPA Repository (PostgreSQL)
           ├─> MongoDB Repository
           └─> KafkaTemplate
   ```

3. **Diagnose the root cause**:
   - Check exception handling — does the thrown exception match `GlobalExceptionHandler` mappings?
   - Check async unwrapping — `CompletionException` must unwrap to expose the real cause
   - Check `CampaignNotFoundException`, `ForbiddenException`, `IllegalStateException` usage

4. **Apply a minimal fix** — change only what is necessary:
   - Do not refactor surrounding code
   - Do not change public API signatures without updating the `*Api` interface and tests

5. **Run the targeted test**:
   ```bash
   mvn -Dtest=com.prx.mercury.api.v1.service.${ServiceClass}Test surefire:test
   ```

6. **If no test exists for this case, write one**:
   - File in `src/test/java/com/prx/mercury/...` mirroring production package
   - JUnit 5 + Mockito, `@DisplayName`, Arrange/Act/Assert

7. **Verify full build**:
   ```bash
   mvn clean test
   ```

## Constraints

- Fix must be targeted — no collateral refactoring
- Exception types must match `GlobalExceptionHandler` mappings (404/403/422/400)
- SLF4J only for any added logging
- No new hardcoded Kafka topics or scheduler rates

## Output

- Root cause: one sentence
- Files changed: list with full paths
- Fix summary: what changed and why
- Test result: PASS / FAIL
