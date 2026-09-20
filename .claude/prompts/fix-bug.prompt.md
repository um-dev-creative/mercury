---
name: fix-bug
description: Diagnoses and fixes a bug in Mercury, ensuring the fix compiles, passes PMD, and does not reduce coverage
mode: agent
agent: developer
tools: [Read, Edit, Bash]
---

## Input Variables

- `${bugDescription}` — clear description of the incorrect behavior
- `${affectedEndpoint}` — endpoint or component where the bug manifests (e.g., `POST /api/v1/campaigns`)
- `${expectedBehavior}` — what should happen
- `${actualBehavior}` — what actually happens
- `${stackTrace}` — exception stack trace if available (optional)

## Steps

1. **Locate the defect** by reading the relevant layer files:
   - If `${affectedEndpoint}` is a REST endpoint: read controller, then service impl
   - If Kafka-related: read `MultiChannelListener`, `MessageChannelRouter`, and the affected `*ChannelService`
   - If scheduler-related: read `SendEmailScheduler`, `MessageProcessor`
   - If mapper-related: read the `*Mapper` interface in `src/main/java/com/prx/mercury/mapper/`

2. **Identify the root cause** by tracing the call chain:
   ```
   Controller → ServiceImpl → Repository / KafkaTemplate
   MultiChannelListener → MessageChannelRouter → ChannelService
   SendEmailScheduler → MessageProcessor → EmailMessageNSRepository / MessageRecordRepository
   ```

3. **Check exception handling**:
   - Is the exception type correct for the scenario? (See `GlobalExceptionHandler` for mappings)
   - Is `CompletionException` wrapping an async exception correctly unwrapped?
   - Are `Optional<T>` results from repositories handled with `.orElseThrow(CampaignNotFoundException::new)`?

4. **Apply the minimal fix**:
   - Change only the files necessary to fix `${bugDescription}`
   - Do not refactor surrounding code unless required to fix the bug
   - Preserve the existing `EmailMessageDocument` lifecycle (OPENED → SENT → deleted)

5. **Verify the fix compiles**:
   ```bash
   mvn -U clean package -DskipTests
   ```

6. **Run PMD and tests**:
   ```bash
   mvn clean test
   ```

7. **Add or update a regression test** in the corresponding `*Test` class:
   - Test class at `src/test/java/com/prx/mercury/<matching-package>/`
   - Test method name: `<method>_should<ExpectedBehavior>_when<BugCondition>()`

## Constraints

- Do not change API contracts (request/response records) unless the bug is in the contract itself
- Do not change HTTP status codes unless they are provably wrong per `GlobalExceptionHandler`
- Preserve `EmailMessageDocument` lifecycle integrity
- SLF4J logging only — do not add `System.err.println` or other debugging output

## Output Format

- Root cause analysis: 2-3 sentences
- Files modified: list with brief reason for each change
- Regression test added: class and method name
- Build result: `mvn clean test` PASS / FAIL
