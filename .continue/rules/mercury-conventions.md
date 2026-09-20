---
name: Mercury Conventions
description: Project-specific patterns and invariants for the Mercury Spring Boot microservice
applies-to: all
---

# Mercury Project Conventions

## Architecture

Mercury is a multi-channel messaging microservice. Every code change must respect this layered flow:

```
REST Controller (*Api interface + *Controller)
  └─> Service (*ServiceImpl, CompletableFuture for writes)
        ├─> JPA Repositories (PostgreSQL) ─> MapStruct Mappers ─> *TO records
        ├─> MongoDB Repositories (EmailMessageDocument lifecycle)
        └─> KafkaTemplate (publish per-recipient to channel topics)

Kafka Listeners (MultiChannelListener)
  └─> MessageChannelRouter ─> ChannelService impls

SendEmailScheduler ─> MessageProcessor (OPENED → SENT → PostgreSQL → deleted)
```

## Naming Conventions

| Layer | Pattern | Example |
|---|---|---|
| API interface | `*Api` | `CampaignApi` |
| Controller | `*Controller implements *Api` | `CampaignController` |
| Service interface | `*Service` | `CampaignService` |
| Service impl | `*ServiceImpl` | `CampaignServiceImpl` |
| Request DTO | `*Request` | `CreateCampaignRequest` |
| Response DTO | `*Response` | `CreateCampaignResponse` |
| Transfer object | `*TO` | `CampaignTO`, `RecipientTO` |
| JPA entity | `*Entity` | `CampaignEntity` |
| MongoDB document | `*Document` | `EmailMessageDocument` |
| MapStruct mapper | `*Mapper` | `CampaignMapper` |
| Kafka message | `*MessageTO` | `EmailMessageTO` |

## Controller Pattern

```java
// ✅ Annotations on the INTERFACE
@Tag(name = "campaigns")
public interface CampaignApi {
    @Operation(summary = "...", operationId = "createCampaign")
    @ApiResponses({ @ApiResponse(responseCode = "200", ...) })
    ResponseEntity<CreateCampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request);
}

// ✅ Controller is THIN — delegate only
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController implements CampaignApi {
    public ResponseEntity<CreateCampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(service.createCampaign(request).join());
    }
}
```

## Service Pattern

```java
@Service
public class CampaignServiceImpl implements CampaignService {
    private static final Logger logger = LoggerFactory.getLogger(CampaignServiceImpl.class);

    private final CampaignRepository campaignRepository;

    public CampaignServiceImpl(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    public CompletableFuture<CreateCampaignResponse> createCampaign(CreateCampaignRequest request) {
        return CompletableFuture.supplyAsync(() -> { ... });
    }
}
```

## Exception → HTTP Mapping

| Exception | HTTP Status |
|---|---|
| `CampaignNotFoundException` | 404 Not Found |
| `ForbiddenException` | 403 Forbidden |
| `IllegalStateException` (channel disabled) | 422 Unprocessable Entity |
| `IllegalArgumentException` (bad input) | 400 Bad Request |
| `MethodArgumentNotValidException` | 400 Bad Request |

## Hard Rules

1. Never write `System.out.println` — use `LoggerFactory.getLogger()`
2. Never instantiate `new CampaignMapper()` — always inject as Spring bean
3. Never hardcode Kafka topic strings — use `${prx.consumer.topics.*}` from `bootstrap.yml`
4. Never hardcode scheduler rates — use `${prx.scheduler.*}` placeholders
5. Never commit real secrets — `bootstrap.yml` uses Vault-backed `${ENV_VAR}` references
6. Never break `EmailMessageDocument` lifecycle: OPENED → SENT → deleted
7. DDL strategy is `none` — every schema change needs a SQL script in `src/main/resources/db/`
