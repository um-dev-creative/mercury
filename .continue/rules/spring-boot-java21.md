---
name: Spring Boot / Java 21 Standards
description: Spring Boot 3.5.8 and Java 21 patterns enforced in Mercury
applies-to: all
---

# Spring Boot 3.5.8 / Java 21 Patterns

## Records for DTOs

All transfer objects are immutable Java 21 records:

```java
// ✅ Correct
public record CreateCampaignRequest(
    @NotNull UUID channelTypeId,
    @NotNull UUID templateId,
    @NotBlank String name,
    @NotEmpty List<@Valid RecipientTO> recipients
) {}

// ❌ Wrong — do not use classes with setters for DTOs
public class CreateCampaignRequest { private String name; public void setName(...) {} }
```

## Async Pattern

Write operations return `CompletableFuture<T>`:

```java
public CompletableFuture<CreateCampaignResponse> createCampaign(CreateCampaignRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        // business logic
        return mapper.toResponse(saved);
    });
}
```

Callers unwrap with `.join()` in controllers (the controller's thread handles the join).

## Constructor Injection (PMD Required)

```java
// ✅ Constructor injection — required by PMD AtLeastOneConstructor
@Service
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository repository;
    private final CampaignMapper mapper;

    public CampaignServiceImpl(CampaignRepository repository, CampaignMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
}

// ❌ Field injection — violates PMD and makes testing harder
@Autowired private CampaignRepository repository;
```

## MapStruct

```java
// ✅ Define with componentModel = "spring"
@Mapper(componentModel = "spring")
public interface CampaignMapper {
    CampaignTO toTO(CampaignEntity entity);
    CampaignEntity toEntity(CreateCampaignRequest request);
}

// ✅ Inject as bean
@Autowired private CampaignMapper mapper;  // or constructor inject

// ❌ Never do this
CampaignMapper mapper = new CampaignMapperImpl();
```

## Scheduler Configuration

```java
// ✅ Property placeholder
@Scheduled(fixedRateString = "${prx.scheduler.email.rate}")
public void processEmails() { ... }

// ❌ Hardcoded
@Scheduled(fixedRate = 1500)
public void processEmails() { ... }
```

## Kafka Listeners

```java
// ✅ Topic from property
@KafkaListener(topics = "${prx.consumer.topics.email}", groupId = "${prx.consumer.group-id}")
public void onEmailMessage(EmailMessageTO message) { ... }

// ❌ Hardcoded topic
@KafkaListener(topics = "email-topic")
```

## New ChannelService Implementation

Any new channel (SMS, Telegram, etc.) must implement all three methods:

```java
public interface ChannelService<T> {
    T send(T message, TemplateDefinedTO template);
    void updateStatus(T message);
    List<T> findByDeliveryStatus(DeliveryStatusType status);
}
```

## Maven Build Commands Reference

```bash
mvn -U clean package -DskipTests         # Fast compile check
mvn clean test                            # PMD + JUnit 5
mvn -B -V -e clean verify                 # Full: PMD + tests + JaCoCo
mvn -Pcoverage clean test                 # JaCoCo XML for SonarCloud
mvn -Dtest=FullQualifiedName surefire:test # Single test class
mvn -Dtest=FullQualifiedName#method surefire:test  # Single test method
```

JaCoCo gates enforced at `verify`: LINE ≥ 70% (BUNDLE), BRANCH ≥ 50% (PACKAGE).
PMD runs at `test` phase via `ruleset.xml` — any violation fails the build before tests run.
