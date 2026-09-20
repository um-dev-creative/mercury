---
name: improve-coverage
description: Analyzes JaCoCo coverage report and adds targeted tests to reach 70% line / 50% branch thresholds in Mercury
mode: agent
agent: test-writer
tools: [Read, Write, Bash]
---

## Input Variables

- `${coverageTarget}` — package or class to improve (e.g., `com.prx.mercury.processor` or `ALL`)
- `${currentLineCoverage}` — current line coverage percentage (e.g., `62%`)
- `${currentBranchCoverage}` — current branch coverage percentage (e.g., `44%`)

## Steps

1. **Generate current coverage report**:
   ```bash
   mvn clean test
   ```
   Open `target/site/jacoco/index.html` and identify packages/classes below threshold.

2. **Find the lowest-coverage classes** in the target package:
   - Focus on: `com.prx.mercury.api.v1.service`, `com.prx.mercury.processor`, `com.prx.mercury.kafka`, `com.prx.mercury.mapper`
   - Priority: classes with 0% coverage first, then classes below 50%

3. **Read each low-coverage class** to identify untested branches:
   - `if/else` blocks
   - `Optional.orElseThrow()` — test both present and empty cases
   - `try/catch` — test both success and exception paths
   - `CompletableFuture` — test both `.join()` success and `CompletionException` paths
   - Early return conditions

4. **Find or create the test class**:
   - Existing: `src/test/java/com/prx/mercury/<package>/<Class>Test.java`
   - New: create following the pattern from `CampaignServiceImplTest.java`

5. **Add targeted tests** for each uncovered branch. Focus on:
   - `MessageProcessor`: OPENED → SENT lifecycle, SMTP failure path
   - `CampaignServiceImpl`: channel disabled path, empty recipients path, Kafka publish failure
   - `GlobalExceptionHandler`: each `@ExceptionHandler` method
   - Mapper null input edge cases

6. **Verify improved coverage**:
   ```bash
   mvn clean verify
   ```
   Confirm thresholds pass: 70% LINE (BUNDLE), 50% BRANCH (PACKAGE).

7. **If verify still fails**, identify remaining gaps:
   ```bash
   mvn -Pcoverage clean test
   ```
   Check `target/site/jacoco/jacoco.xml` for specific missed instruction counts.

## Constraints

- Do not add trivial tests (testing getters/setters on records) to inflate coverage — focus on real branch logic
- Tests must assert meaningful results, not just call methods
- Do not modify production code to remove branches just to make coverage easier
- JMH benchmark classes in `src/test/java/com/prx/mercury/benchmark/` do NOT count toward coverage

## Output Format

- Coverage before: LINE ${currentLineCoverage}%, BRANCH ${currentBranchCoverage}%
- Classes targeted: list with current % and target %
- Tests added: table with Class | Method | Branch Covered
- Coverage after: `mvn clean verify` result with new percentages
- Threshold check: PASS / FAIL
