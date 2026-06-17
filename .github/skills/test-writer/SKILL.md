# Test Writer SKILL

## Project-Specific Patterns

### Service Unit Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignMapper campaignMapper;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    @Test
    void shouldReturnCampaignWhenFound() {
        CampaignEntity entity = new CampaignEntity();
        CampaignResponse expected = new CampaignResponse(1L, "Test", "ACTIVE");
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(campaignMapper.toResponse(entity)).thenReturn(expected);

        CampaignResponse result = campaignService.getCampaign(1L);

        assertThat(result).isEqualTo(expected);
        verify(campaignRepository).findById(1L);
    }

    @Test
    void shouldThrowCampaignNotFoundExceptionWhenMissing() {
        when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CampaignNotFoundException.class,
            () -> campaignService.getCampaign(99L));
    }

    @Test
    void shouldCreateCampaignAndReturnFutureResult() throws Exception {
        CampaignRequest request = new CampaignRequest("Test", 1L);
        CampaignEntity entity = new CampaignEntity();
        CampaignResponse expected = new CampaignResponse(1L, "Test", "PENDING");
        when(campaignMapper.toEntity(request)).thenReturn(entity);
        when(campaignRepository.save(entity)).thenReturn(entity);
        when(campaignMapper.toResponse(entity)).thenReturn(expected);

        CompletableFuture<CampaignResponse> future = campaignService.createCampaign(request);

        assertThat(future.get()).isEqualTo(expected);
    }
}
```

### Controller Test Pattern (WebMvcTest slice)
```java
@WebMvcTest(CampaignController.class)
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignServiceImpl campaignService;

    @Test
    void shouldReturn200WhenCampaignExists() throws Exception {
        when(campaignService.getCampaign(1L))
            .thenReturn(new CampaignResponse(1L, "Test", "ACTIVE"));

        mockMvc.perform(get("/api/v1/campaigns/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void shouldReturn404WhenCampaignNotFound() throws Exception {
        when(campaignService.getCampaign(99L))
            .thenThrow(new CampaignNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/campaigns/99"))
               .andExpect(status().isNotFound());
    }
}
```

### Mapper Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class CampaignMapperTest {

    // Use Mappers.getMapper for interface-only mappers
    private final CampaignMapper mapper = Mappers.getMapper(CampaignMapper.class);

    @Test
    void shouldMapEntityToResponse() {
        CampaignEntity entity = new CampaignEntity();
        entity.setId(1L);
        entity.setName("Test");

        CampaignResponse response = mapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test");
    }
}
```

### Kafka Listener Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class MultiChannelListenerTest {

    @Mock
    private MessageChannelRouter router;

    @InjectMocks
    private MultiChannelListener listener;

    @Test
    void shouldDelegateToRouterOnConsume() {
        String message = "{\"type\":\"EMAIL\"}";
        listener.consume(message);
        verify(router).route(message);
    }
}
```

### Exception Coverage Pattern
Always test BOTH happy path AND each exception branch:
```java
// ForbiddenException → 403
assertThrows(ForbiddenException.class, () -> service.sensitiveOperation(invalidUser));

// IllegalStateException (channel disabled) → 422
assertThrows(IllegalStateException.class, () -> service.send(disabledChannel));

// IllegalArgumentException (bad input) → 400
assertThrows(IllegalArgumentException.class, () -> service.create(badRequest));
```

## Naming Conventions
| Source Class | Test Class | Location |
|---|---|---|
| `CampaignServiceImpl` | `CampaignServiceImplTest` | `src/test/java/com/umdc/mercury/api/v1/service/` |
| `CampaignController` | `CampaignControllerTest` | `src/test/java/com/umdc/mercury/api/v1/controller/` |
| `CampaignMapper` | `CampaignMapperTest` | `src/test/java/com/umdc/mercury/mapper/` |
| `MultiChannelListener` | `MultiChannelListenerTest` | `src/test/java/com/umdc/mercury/kafka/listener/` |
| `MessageChannelRouter` | `MessageChannelRouterTest` | `src/test/java/com/umdc/mercury/kafka/router/` |

Method naming: `should<Action>When<Condition>()` (e.g., `shouldThrowNotFoundWhenCampaignMissing`)

## Error Handling
- Test that `CampaignNotFoundException` is thrown when entity is absent
- Test that `ForbiddenException` is thrown when auth fails
- Test that `IllegalStateException` is thrown when channel is disabled
- Test that `IllegalArgumentException` is thrown on invalid input
- Use `assertThrows(ExceptionClass.class, () -> ...)` — never catch-and-check

## Key Files
- `src/test/java/com/umdc/mercury/` — test root (mirrors main package structure)
- `target/site/jacoco/jacoco.xml` — JaCoCo XML report (after `mvn -Pcoverage clean test`)
- `target/site/jacoco/index.html` — human-readable coverage report
- `pom.xml` — Surefire and JaCoCo plugin configuration
- `.github/tools/jacoco.tool.md` — coverage threshold reference
- `.github/tools/maven.tool.md` — test run commands

## Constraints
- NEVER write `new *MapperImpl()` in tests — use `Mappers.getMapper()` or `@InjectMocks`
- NEVER spin up full Spring context (`@SpringBootTest`) for unit tests — use slices
- NEVER use `Thread.sleep()` for async — use `CompletableFuture.get()` or `join()`
- NEVER suppress PMD warnings in test files without justification comment
- NEVER mock the class under test — only its dependencies
- NEVER skip testing exception paths

## Checklist
- [ ] Test class in same package as class under test (under `src/test/java/`)
- [ ] Test class named `<ClassName>Test.java`
- [ ] Each test method named `should<Action>When<Condition>()`
- [ ] Happy path tested
- [ ] All exception branches tested with `assertThrows`
- [ ] `CompletableFuture` results unwrapped via `.get()` or `.join()`
- [ ] `mvn clean test` passes with zero failures
- [ ] Line coverage ≥ 70% on changed classes (from `target/site/jacoco/jacoco.xml`)
- [ ] Branch coverage ≥ 50% on changed classes
