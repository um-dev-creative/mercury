---
agent: developer
version: 1.0
---

## 1. Project-Specific Patterns

### Controller pattern
Every controller implements a `*Api` interface. OpenAPI annotations live on the interface only.

```java
// Interface (com.prx.mercury.api.v1.controller.CampaignApi)
@Tag(name = "campaigns", description = "Campaign Management API")
public interface CampaignApi {
    @Operation(summary = "...", operationId = "createCampaign")
    @ApiResponses(...)
    ResponseEntity<CreateCampaignResponse> createCampaign(CreateCampaignRequest request);
}

// Controller (com.prx.mercury.api.v1.controller.CampaignController)
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController implements CampaignApi { ... }
```

### Service pattern
```java
// Interface
public interface CampaignService {
    CompletableFuture<CreateCampaignResponse> createCampaign(CreateCampaignRequest request);
}

// Impl
@Service
public class CampaignServiceImpl implements CampaignService {
    private static final Logger logger = LoggerFactory.getLogger(CampaignServiceImpl.class);
    // constructor injection only
}
```

### DTO pattern
```java
// com.prx.mercury.api.v1.to
public record CreateCampaignRequest(
    @NotNull UUID channelTypeId,
    @NotNull UUID templateId,
    @NotBlank String name,
    @NotEmpty List<@Valid RecipientTO> recipients
) {}
```

### Kafka publish (in CampaignServiceImpl)
```java
kafkaTemplate.send(channelType.topicName(), emailMessageTO);
```

### New ChannelService
```java
@Service
public class SmsChannelService implements ChannelService<SmsMessageTO> {
    public SmsMessageTO send(SmsMessageTO message, TemplateDefinedTO template) { ... }
    public void updateStatus(SmsMessageTO message) { ... }
    public List<SmsMessageTO> findByDeliveryStatus(DeliveryStatusType status) { ... }
}
```

## 2. Naming Conventions

- Interfaces: `CampaignService`, `ChannelService<T>`, `CampaignApi`
- Implementations: `CampaignServiceImpl`, `EmailChannelService`
- Request records: `CreateCampaignRequest`, `UpdateCampaignRequest`, `SendEmailRequest`
- Response records: `CreateCampaignResponse`, `CampaignDetailResponse`, `SendEmailResponse`
- Transfer objects: `CampaignTO`, `ChannelTypeTO`, `RecipientTO`, `MessageRecordTO`
- Entities: `CampaignEntity`, `ChannelTypeEntity`, `MessageRecordEntity`
- Documents: `EmailMessageDocument`, `SmsMessageDocument`
- Mappers: `CampaignMapper`, `ChannelTypeMapper`, `MessageRecordMapper`
- Kafka TOs: `EmailMessageTO`, `SmsMessageTO`, `TelegramMessageTO`

## 3. Error Handling

| Scenario | Exception | HTTP |
|---|---|---|
| Campaign UUID not found | `CampaignNotFoundException` | 404 |
| Auth token invalid/missing | `ForbiddenException` | 403 |
| Channel type disabled | `IllegalStateException` | 422 |
| Invalid input / unknown template | `IllegalArgumentException` | 400 |
| Bean validation failure | `MethodArgumentNotValidException` | 400 |
| Async wrap | `CompletionException` | delegates to cause |

## 4. Key Files

- `src/main/java/com/prx/mercury/api/v1/controller/CampaignApi.java` — reference interface
- `src/main/java/com/prx/mercury/api/v1/service/CampaignServiceImpl.java` — reference service
- `src/main/java/com/prx/mercury/kafka/consumer/service/ChannelService.java` — channel contract
- `src/main/java/com/prx/mercury/kafka/router/MessageChannelRouter.java` — routing logic
- `src/main/java/com/prx/mercury/processor/MessageProcessor.java` — email lifecycle
- `src/main/resources/bootstrap.yml` — property placeholders

## 5. Constraints

- No `System.out.println` — use SLF4J only
- No MapStruct `new XxxMapper()` — always inject as Spring bean
- Scheduler `fixedRateString` must use `${prx.scheduler.*}` placeholder
- No hardcoded Kafka topic strings — use `${prx.consumer.topics.*}`
- Every class needs at least one explicit constructor (PMD `AtLeastOneConstructor`)

## 6. Checklist

- [ ] `*Api` interface created with full `@Operation` + `@ApiResponses`
- [ ] Controller implements interface, no OpenAPI annotations on controller class
- [ ] DTO record has Bean Validation annotations
- [ ] Service returns `CompletableFuture` for write operations
- [ ] Exception types match `GlobalExceptionHandler` mappings
- [ ] Logger: `private static final Logger logger = LoggerFactory.getLogger(ThisClass.class)`
- [ ] `mvn -U clean package -DskipTests` passes
