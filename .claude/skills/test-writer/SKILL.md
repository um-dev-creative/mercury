---
agent: test-writer
version: 1.0
---

## 1. Project-Specific Patterns

### Service unit test pattern
```java
@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock CampaignRepository campaignRepository;
    @Mock CampaignMapper campaignMapper;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks CampaignServiceImpl campaignService;

    @Test
    void createCampaign_shouldReturnResponse_whenValidRequest() {
        // arrange
        // act
        CreateCampaignResponse result = campaignService.createCampaign(request).join();
        // assert
    }
}
```

### Controller slice test pattern
```java
@WebMvcTest(CampaignController.class)
class CampaignControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean CampaignService campaignService;
    // ...
}
```

### Mapper test pattern
```java
@ExtendWith(MockitoExtension.class)
class CampaignMapperTest {
    CampaignMapper mapper = Mappers.getMapper(CampaignMapper.class);
    // verify entity → TO and TO → entity mappings
}
```

### Benchmark pattern
```java
// src/test/java/com/prx/mercury/benchmark/
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
public class ConfirmCodeStreamBenchmark { ... }
```

## 2. Naming Conventions

- Test class: `<ProductionClass>Test` in same sub-package under `src/test/java/com/prx/mercury/`
- Test methods: `<methodName>_should<Expected>_when<Condition>()` pattern
- Existing tests: `CampaignServiceImplTest`, `CampaignControllerTest`, `MessageProcessorTest`, `CampaignMapperTest`, `VerificationCodeServiceTest`

## 3. Error Handling

- Test that `CampaignNotFoundException` is thrown for missing campaign UUID and results in 404 in controller tests
- Test that `IllegalStateException` from disabled channel produces 422 in controller tests
- Test that `CompletionException` wrapping is handled in async service methods
- Test `GlobalExceptionHandlerTest` for each exception path

## 4. Key Files

- `src/test/java/com/prx/mercury/api/v1/service/CampaignServiceImplTest.java` — service test reference
- `src/test/java/com/prx/mercury/api/v1/controller/CampaignControllerTest.java` — controller test reference
- `src/test/java/com/prx/mercury/mapper/CampaignMapperTest.java` — mapper test reference
- `src/test/java/com/prx/mercury/processor/MessageProcessorTest.java` — processor test reference
- `target/site/jacoco/index.html` — coverage report after `mvn verify`
- `target/site/jacoco/jacoco.xml` — machine-readable for SonarCloud via `-Pcoverage`

## 5. Constraints

- JaCoCo thresholds: 70% line (BUNDLE), 50% branch (PACKAGE) — enforced at `mvn verify`
- Do not use `@SpringBootTest` for pure unit tests — it loads the full context unnecessarily
- Benchmark tests must be in `src/test/java/com/prx/mercury/benchmark/` and run via `mvn -Pbenchmark clean test`
- Do not mock `LoggerFactory` — SLF4J loggers are not part of test assertions

## 6. Checklist

- [ ] Test class in correct package under `src/test/java/com/prx/mercury/`
- [ ] Both happy-path and exception branches covered
- [ ] Async `CompletableFuture` tests call `.join()` or `.get()`
- [ ] `mvn -Dtest=<ClassName> surefire:test` passes in isolation
- [ ] `mvn clean verify` passes with coverage thresholds
- [ ] New test classes added for any new production class
