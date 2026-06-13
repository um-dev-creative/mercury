---
name: persistence-patterns
used-by: [developer, database-architect]
version: 1.0
---

## Persistence Patterns Shared Skill — Mercury Dual-Store Model

### PostgreSQL (JPA) — Permanent Storage

**Entity base pattern:**
```java
@Entity
@Table(name = "campaign")
public class CampaignEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_type_id")
    private ChannelTypeEntity channelType;

    public CampaignEntity() {}   // PMD AtLeastOneConstructor
}
```

**Repository pattern:**
```java
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID> {
    List<CampaignEntity> findByUserIdAndApplicationId(UUID userId, UUID applicationId);
    Optional<CampaignEntity> findByIdAndActiveTrue(UUID id);
}
```

**Current entities:** `CampaignEntity`, `CampaignMetricsEntity`, `ChannelTypeEntity`, `ChannelConfigEntity`, `FrequencyTypeEntity`, `MessageDeliveryLogEntity`, `MessageRecordEntity`, `MessageStatusTypeEntity`, `SeverityTypeEntity`, `TemplateDefinedEntity`, `TemplateEntity`, `TemplateTypeEntity`, `UserEntity`, `ApplicationEntity`, `VerificationCodeEntity`

### MongoDB — Transient In-Flight Messages

**Document pattern:**
```java
@Document(collection = "email_message")
public class EmailMessageDocument {
    @Id
    private String id;
    private String status;   // "OPENED" → "SENT" → (deleted)
    private String recipient;
    private String subject;
    // ...
}
```

**Repository pattern:**
```java
public interface EmailMessageNSRepository extends MongoRepository<EmailMessageDocument, String> {
    List<EmailMessageDocument> findByStatus(String status);
}
```

**Lifecycle must be preserved:**
1. Kafka consumer writes `EmailMessageDocument` with `status = OPENED`
2. `MessageProcessor.processMessage()` reads `OPENED`, sends SMTP, updates to `SENT`
3. `MessageProcessor.updateMessageStatus()` reads `SENT`, writes `MessageRecordEntity` to PostgreSQL, deletes MongoDB doc

### DDL-None Rule
Hibernate DDL is `none`. Schema changes require SQL migration files in `src/main/resources/db/`.

### MapStruct Bridge Pattern
```java
@Mapper(componentModel = "spring")
public interface CampaignMapper {
    CampaignTO toTO(CampaignEntity entity);
    CampaignEntity toEntity(CreateCampaignRequest request);
    List<CampaignTO> toTOList(List<CampaignEntity> entities);
}
```
MapStruct mappers are Spring beans — inject with `@Autowired` / constructor injection, never `Mappers.getMapper()` in production code.

### DatabaseConfig
`com.prx.mercury.config.DatabaseConfig` configures dual datasource (PostgreSQL + MongoDB). Do not bypass this with manual datasource creation.
