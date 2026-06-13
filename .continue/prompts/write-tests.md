---
name: Write Tests
description: Write JUnit 5 unit tests for a Mercury class targeting ≥75% line coverage
---

Write unit tests for: {{{ targetClass }}}

## Instructions

1. Read the production class and identify all public methods, dependencies, and exceptions thrown.

2. Create the test file at `src/test/java/` mirroring the package structure: `{{{ targetClass }}}Test.java`

3. Test class setup:
   ```java
   @ExtendWith(MockitoExtension.class)
   @DisplayName("{ClassName}")
   class {ClassName}Test {
       @Mock private DependencyA dependencyA;
       @InjectMocks private {ClassName} subject;
   }
   ```

4. Write one test per logical case using Arrange/Act/Assert:
   - Happy path (success response)
   - Not found → `CampaignNotFoundException`
   - Auth failure → `ForbiddenException`
   - Channel disabled → `IllegalStateException`
   - Async: unwrap with `.join()`, assert `CompletionException` for error paths

5. Use AssertJ (`assertThat(...)`). Use `@ParameterizedTest` for boundary values.

6. Run and verify:
   ```bash
   mvn -Dtest={ClassName}Test surefire:test
   mvn -Pcoverage clean test
   ```
   Target: LINE ≥ 75% for the target class.

Output: test file path, list of @DisplayName values, coverage result.
