---
name: Write Unit Tests
description: >
  Write JUnit 5 unit tests for a Mercury class or feature using Mockito,
  following project conventions and targeting ≥75% line coverage.
mode: agent
agent: test-writer
tools: [read_file, grep_search, codebase_search, create_file, insert_edit_into_file, run_in_terminal]
---

## Input Variables

- `${targetClass}` — fully qualified class name (e.g., `com.prx.mercury.api.v1.service.CampaignServiceImpl`)
- `${testScope}` — what to cover: `all` | `happy-path` | `error-paths` | `specific: ${methodNames}`

## Steps

1. **Read the production class** and identify:
   - All public methods
   - Dependencies (constructor-injected)
   - Exception types thrown
   - `CompletableFuture` methods (require `.get()` or `.join()` in tests)

2. **Locate or create the test file** at `src/test/java/` mirroring the production package:
   - File: `${targetClass}Test.java`
   - Annotate class with `@DisplayName("CampaignServiceImpl")`

3. **Set up the test class**:
   ```java
   @ExtendWith(MockitoExtension.class)
   @DisplayName("${ClassName}")
   class ${ClassName}Test {

       @Mock private CampaignRepository campaignRepository;
       // ... other mocks

       @InjectMocks private ${ClassName} subject;

       @BeforeEach
       void setUp() { /* reset state if needed */ }
   }
   ```

4. **Write test methods** — one logical assertion per test:
   ```java
   @Test
   @DisplayName("createCampaign — returns response when channel type is enabled")
   void createCampaign_channelEnabled_returnsResponse() {
       // Arrange
       var request = new CreateCampaignRequest(...);
       given(channelTypeRepository.findById(any())).willReturn(Optional.of(enabledChannel));
       // Act
       var result = subject.createCampaign(request).join();
       // Assert
       assertThat(result).isNotNull();
       assertThat(result.id()).isNotNull();
   }
   ```

5. **Cover these paths for each method**:
   - Happy path (success)
   - Not found → `CampaignNotFoundException` thrown
   - Auth failure → `ForbiddenException` thrown
   - Channel disabled → `IllegalStateException` thrown
   - Null / invalid input → `IllegalArgumentException`
   - Async: unwrap `CompletionException` to assert inner cause

6. **Run targeted test**:
   ```bash
   mvn -Dtest=${targetClass}Test surefire:test
   ```

7. **Run coverage check**:
   ```bash
   mvn -Pcoverage clean test
   ```
   Target: LINE ≥ 75% for `${targetClass}` (5% safety above the 70% gate).

## Constraints

- JUnit 5 (`@ExtendWith(MockitoExtension.class)`) — no JUnit 4
- AssertJ for assertions (`assertThat(...)`)
- `@ParameterizedTest` when the same behavior spans multiple inputs
- No `Thread.sleep` — use `CompletableFuture.join()` for async
- Never modify production code to make it more testable

## Output

- Test file path
- List of test methods added with `@DisplayName` values
- Coverage result: LINE X%, BRANCH Y% for the target class
- `mvn clean test`: PASS / FAIL
