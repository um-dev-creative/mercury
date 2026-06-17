# Developer SKILL

## Project-Specific Patterns

### REST Layer Pattern
```java
// Api interface (annotations here only)
@Tag(name = "Campaign")
public interface CampaignApi {
    @Operation(summary = "Get campaign")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/api/v1/campaigns/{id}")
    CampaignResponse getCampaign(@PathVariable Long id);
}

// Controller (zero OpenAPI annotations)
@RestController
@RequiredArgsConstructor
public class CampaignController implements CampaignApi {
    private final CampaignServiceImpl campaignService;

    public CampaignController(CampaignServiceImpl campaignService) {
        this.campaignService = campaignService;
    }

    @Override
    public CampaignResponse getCampaign(Long id) {
        return campaignService.getCampaign(id);
    }
}
```

### Service Layer Pattern
```java
@Service
public class CampaignServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);
    private final CampaignRepository campaignRepository;
    private final CampaignMapper campaignMapper;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                                CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.campaignMapper = campaignMapper;
    }

    // Read → returns T
    public CampaignResponse getCampaign(Long id) {
        return campaignRepository.findById(id)
            .map(campaignMapper::toResponse)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + id));
    }

    // Write → returns CompletableFuture<T>
    public CompletableFuture<CampaignResponse> createCampaign(CampaignRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignEntity entity = campaignMapper.toEntity(request);
            return campaignMapper.toResponse(campaignRepository.save(entity));
        });
    }
}
```

### Kafka Listener Pattern
```java
@Component
public class MultiChannelListener {
    private static final Logger log = LoggerFactory.getLogger(MultiChannelListener.class);
    private final MessageChannelRouter router;

    public MultiChannelListener(MessageChannelRouter router) {
        this.router = router;
    }

    @KafkaListener(topics = "${prx.consumer.topics.notifications}")
    public void consume(String message) {
        router.route(message);
    }
}
```

### Scheduler Pattern
```java
@Component
public class CampaignProcessor {
    private static final Logger log = LoggerFactory.getLogger(CampaignProcessor.class);

    public CampaignProcessor() { }

    @Scheduled(fixedRateString = "${prx.scheduler.campaign.rate}")
    public void process() {
        log.info("Processing campaigns");
    }
}
```

### MapStruct Mapper Pattern
```java
@Mapper(componentModel = "spring")
public interface CampaignMapper {
    CampaignResponse toResponse(CampaignEntity entity);
    CampaignEntity toEntity(CampaignRequest request);
}
// Injected as: private final CampaignMapper campaignMapper; (Spring bean)
// NEVER: new CampaignMapperImpl()
```

### DTO Record Pattern
```java
// in com.umdc.mercury.api.v1.to
public record CampaignRequest(
    @NotBlank String name,
    @NotNull Long channelTypeId
) {}

public record CampaignResponse(
    Long id,
    String name,
    String status
) {}
```

## Naming Conventions
| Layer | Pattern | Example |
|---|---|---|
| REST interface | `*Api` | `CampaignApi` |
| REST controller | `*Controller` | `CampaignController` |
| Service impl | `*ServiceImpl` | `CampaignServiceImpl` |
| JPA entity | `*Entity` | `CampaignEntity` |
| JPA repository | `*Repository` | `CampaignRepository` |
| MongoDB document | `*Document` | `EmailMessageDocument` |
| Mapper | `*Mapper` | `CampaignMapper` |
| Request DTO | `*Request` | `CampaignRequest` |
| Response DTO | `*Response` | `CampaignResponse` |
| Transfer object | `*TO` | `MessageTO` |
| Kafka TO | lives in `kafka/to/` | `MessageChannelTO` |

## Error Handling
| Exception | Package | HTTP |
|---|---|---|
| `CampaignNotFoundException` | `com.umdc.mercury.api.v1.exception` | 404 |
| `ForbiddenException` | `com.umdc.mercury.api.v1.exception` | 403 |
| `IllegalStateException` | java.lang | 422 (channel disabled) |
| `IllegalArgumentException` | java.lang | 400 (bad input) |
| `MethodArgumentNotValidException` | spring-web | 400 |

Always throw exceptions with descriptive messages. Never return null from service methods — throw or return Optional explicitly handled.

## Key Files
- `src/main/java/com/umdc/mercury/api/v1/controller/` — controllers
- `src/main/java/com/umdc/mercury/api/v1/service/` — services
- `src/main/java/com/umdc/mercury/api/v1/to/` — DTOs
- `src/main/java/com/umdc/mercury/api/v1/exception/` — exceptions
- `src/main/java/com/umdc/mercury/jpa/sql/entity/` — JPA entities
- `src/main/java/com/umdc/mercury/jpa/sql/repository/` — JPA repos
- `src/main/java/com/umdc/mercury/jpa/nosql/document/` — MongoDB docs
- `src/main/java/com/umdc/mercury/jpa/nosql/repository/` — MongoDB repos
- `src/main/java/com/umdc/mercury/mapper/` — MapStruct mappers
- `src/main/java/com/umdc/mercury/kafka/listener/` — Kafka listeners
- `src/main/java/com/umdc/mercury/kafka/consumer/service/` — channel services
- `src/main/java/com/umdc/mercury/kafka/router/` — message router
- `src/main/java/com/umdc/mercury/scheduler/` — schedulers
- `src/main/java/com/umdc/mercury/processor/` — processors
- `src/main/java/com/umdc/mercury/security/` — security config
- `src/main/java/com/umdc/mercury/config/` — app config
- `src/main/resources/bootstrap.yml` — config + Kafka topics + scheduler rates
- `src/main/resources/db/migration/` — Flyway scripts
- `pom.xml` — Maven POM
- `ruleset.xml` — PMD rules

## Constraints
- NEVER place OpenAPI annotations on `*Controller` — only on `*Api`
- NEVER hardcode Kafka topic strings — always `${prx.consumer.topics.*}`
- NEVER hardcode scheduler rates — always `${prx.scheduler.*}`
- NEVER use `new *MapperImpl()` — inject as Spring bean
- NEVER skip the explicit constructor (PMD will fail)
- NEVER use `System.out.println` or `e.printStackTrace()` — use SLF4J
- NEVER put secrets in source or config files — Vault-backed only
- NEVER modify Flyway migration files after they have been applied

## Checklist
- [ ] All new classes have explicit constructors
- [ ] SLF4J logger declared as `private static final Logger log = LoggerFactory.getLogger(...)`
- [ ] Write operations return `CompletableFuture<T>`
- [ ] Read operations return `T` or `List<T>` directly
- [ ] Kafka topics use `${prx.consumer.topics.*}` property
- [ ] Scheduler rates use `${prx.scheduler.*}` property
- [ ] MapStruct mappers injected — never instantiated
- [ ] OpenAPI annotations on `*Api` only
- [ ] `mvn -U clean package -DskipTests` → BUILD SUCCESS
- [ ] PMD violations = 0
