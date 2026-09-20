---
name: write-unit-tests
description: Writes comprehensive JUnit 5 + Mockito unit tests for a Mercury class to achieve full branch coverage
mode: agent
agent: test-writer
tools: [Read, Write, Bash]
---

## Input Variables

- `${targetClass}` — fully qualified class name (e.g., `com.prx.mercury.api.v1.service.CampaignServiceImpl`)
- `${testScope}` — `unit` (Mockito, no Spring context) or `slice` (WebMvcTest / DataJpaTest)

## Steps

1. **Read the target class**:
   - `src/main/java/${targetClass.toPath()}.java`
   - Identify: all public methods, all branches (if/else, try/catch, Optional.orElseThrow), async paths

2. **Read the existing test file** if it exists at:
   - `src/test/java/${targetClass.toPath()}Test.java`
   - Note which methods already have tests and what's missing

3. **Read a reference test** for the same layer type:
   - Service: `src/test/java/com/prx/mercury/api/v1/service/CampaignServiceImplTest.java`
   - Controller: `src/test/java/com/prx/mercury/api/v1/controller/CampaignControllerTest.java`
   - Mapper: `src/test/java/com/prx/mercury/mapper/CampaignMapperTest.java`
   - Processor: `src/test/java/com/prx/mercury/processor/MessageProcessorTest.java`

4. **Write the test class** at `src/test/java/${targetClass.toPath()}Test.java`:

   For `unit` scope:
   ```java
   @ExtendWith(MockitoExtension.class)
   class ${ClassName}Test {
       @Mock <DependencyType> <dependency>;
       @InjectMocks ${ClassName} subject;

       @Test
       void <method>_should<Expected>_when<Condition>() { ... }
   }
   ```

   For `slice` scope (controller):
   ```java
   @WebMvcTest(${ControllerClass}.class)
   class ${ControllerClass}Test {
       @Autowired MockMvc mockMvc;
       @MockBean <ServiceType> service;
   }
   ```

5. **Cover these scenarios for every public method**:
   - Happy path with valid input
   - `Optional.empty()` → `CampaignNotFoundException` where applicable
   - `IllegalArgumentException` for invalid business arguments
   - `IllegalStateException` for disabled channel type where applicable
   - `CompletableFuture` completion and exception paths (call `.join()` in assertions)
   - Null/empty list inputs where applicable

6. **Run the test class in isolation**:
   ```bash
   mvn -Dtest=${ClassName}Test surefire:test
   ```

7. **Run full test suite to check for regressions**:
   ```bash
   mvn clean test
   ```

## Constraints

- Do not use `@SpringBootTest` for pure unit tests — it loads full context
- Async `CompletableFuture` tests must call `.join()` or `.get()` to assert results
- Test method naming: `<method>_should<Expected>_when<Condition>()`
- No assertions on `LoggerFactory` — SLF4J logs are not testable via Mockito
- Do not import `org.junit.Test` (JUnit 4) — use `org.junit.jupiter.api.Test` only

## Output Format

- Test file path: `src/test/java/com/prx/mercury/<package>/${ClassName}Test.java`
- Methods covered: table with Method | Happy Path | Exception Path | Async Path
- Run result: `mvn -Dtest=${ClassName}Test surefire:test` PASS / FAIL
