---
name: write-unit-tests
description: Write comprehensive JUnit 5 + Mockito unit tests for a Mercury class
mode: agent
agent: test-writer
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
# Write Unit Tests

## Input Variables
- `${targetClass}` — fully qualified class name to test (e.g., `com.umdc.mercury.api.v1.service.CampaignServiceImpl`)
- `${testFocus}` — optional: specific methods or scenarios to focus on (or `all`)

## Step 1: Read the Class Under Test
Read `${targetClass}` source file and identify:
1. All public methods
2. Dependencies (constructor parameters)
3. Return types (T, List<T>, or CompletableFuture<T>)
4. Exception types thrown
5. Kafka interactions, DB calls, mapper calls

## Step 2: Determine Test File Location
```
Source: src/main/java/com/umdc/mercury/<subpackage>/<ClassName>.java
Test:   src/test/java/com/umdc/mercury/<subpackage>/<ClassName>Test.java
```

## Step 3: Write Test Class Structure
```java
@ExtendWith(MockitoExtension.class)
class ${ClassName}Test {

    // Mock all dependencies
    @Mock
    private DependencyA depA;

    @InjectMocks
    private ${ClassName} subject;

    // Setup if needed
    @BeforeEach
    void setUp() { }
}
```

## Step 4: Write Test Methods

### For each public read method (returns T or List<T>):
```java
@Test
void should<ReturnValue>When<HappyCondition>() {
    // given
    when(dependency.method(args)).thenReturn(result);
    // when
    ResultType result = subject.method(args);
    // then
    assertThat(result).isEqualTo(expected);
    verify(dependency).method(args);
}

@Test
void shouldThrow<ExceptionType>When<NotFoundCondition>() {
    when(dependency.findById(99L)).thenReturn(Optional.empty());
    assertThrows(CampaignNotFoundException.class, () -> subject.method(99L));
}
```

### For each public write method (returns CompletableFuture<T>):
```java
@Test
void should<CreateOrUpdate>When<ValidInput>() throws Exception {
    // given
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(savedEntity);
    when(mapper.toResponse(savedEntity)).thenReturn(expected);
    // when
    CompletableFuture<ResultType> future = subject.writeMethod(request);
    // then
    assertThat(future.get()).isEqualTo(expected);
}
```

### For ForbiddenException paths:
```java
@Test
void shouldThrowForbiddenExceptionWhenAccessDenied() {
    assertThrows(ForbiddenException.class,
        () -> subject.sensitiveMethod(unauthorizedUserId));
}
```

### For IllegalStateException (channel disabled):
```java
@Test
void shouldThrowIllegalStateExceptionWhenChannelDisabled() {
    when(channelService.isEnabled()).thenReturn(false);
    assertThrows(IllegalStateException.class,
        () -> subject.sendMessage(request));
}
```

## Step 5: Controller Tests (if ${targetClass} is a Controller)
Use `@WebMvcTest` slice instead of `@ExtendWith(MockitoExtension.class)`:
```java
@WebMvcTest(${ClassName}.class)
class ${ClassName}Test {
    @Autowired MockMvc mockMvc;
    @MockBean ServiceImpl service;

    @Test
    void shouldReturn200WhenFound() throws Exception {
        when(service.get(1L)).thenReturn(new Response(...));
        mockMvc.perform(get("/api/v1/resource/1"))
               .andExpect(status().isOk());
    }
}
```

## Step 6: Run Tests
```bash
mvn -Dtest=${targetClass}Test surefire:test
```
All tests must pass.

## Step 7: Check Coverage
```bash
mvn -Pcoverage clean test
# Check target/site/jacoco/jacoco.xml for ${targetClass} entry
# Line ≥ 70%, Branch ≥ 50% required
```

## Constraints
- NEVER use `new *MapperImpl()` — use `Mappers.getMapper()` or `@InjectMocks`
- NEVER use `@SpringBootTest` for unit tests — use `@ExtendWith(MockitoExtension.class)` or slices
- NEVER use `Thread.sleep()` — use `CompletableFuture.get()` for async
- NEVER skip exception branch tests
- Test method naming: `should<Action>When<Condition>()`

## Output Format
1. Test file path created
2. Test methods list with covered scenario
3. `mvn -Dtest=${targetClass}Test surefire:test` output (all pass)
4. Coverage for `${targetClass}`: line% and branch%
5. Uncovered branches list (if any remain)
