---
name: Fix Bug
description: Diagnose and apply a minimal fix for a Mercury bug
---

Fix the following Mercury bug:
- Description: {{{ bugDescription }}}
- Affected: {{{ affectedEndpoint }}}
- Error: {{{ errorOrStackTrace }}}

## Instructions

1. Locate the source of the error:
   ```bash
   grep -rn "{{{ affectedEndpoint }}}" src/
   ```
   Read the controller, service, and repository involved.

2. Trace through Mercury's layers to find the root cause:
   - Controller → Service → Repository/Kafka
   - Check `CompletionException` unwrapping for async failures
   - Check exception handler mappings in `GlobalExceptionHandler`

3. Apply a minimal fix — change only what is broken. Do not refactor.

4. Run the relevant test:
   ```bash
   mvn -Dtest=<AffectedClassTest> surefire:test
   ```

5. If no test covers this case, write one using JUnit 5 + Mockito.

6. Verify full build:
   ```bash
   mvn clean test
   ```

Output: root cause (one sentence), files changed, test result.
