# Shared Skill: Persistence

**Used by:** `developer`, `database-architect`

## PostgreSQL Persistence (JPA + Flyway)

### DDL Strategy
```yaml
# bootstrap.yml — ALWAYS none; schema managed by Flyway only
spring:
  jpa:
    hibernate:
      ddl-auto: none
```

### Flyway Migration Naming
```
Format:  V<YYYYMMDD><3-digit-seq>__<snake_case_description>.sql
Example: V20250315001__add_campaign_channel_type_index.sql

Location: src/main/resources/db/migration/
Rules:
  - Append-only: never edit a migration after it has been applied
  - Sequential: seq resets per date (001, 002, 003 per YYYYMMDD)
  - Description: lowercase, underscores, no spaces
```

### JPA Entity Template
```java
@Entity
@Table(name = "entity_name")           // always explicit
public class SomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    // Relationships — always LAZY for collections
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    private List<ChildEntity> children;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private ParentEntity parent;

    public SomeEntity() { }          // explicit no-arg (PMD)
    // getters / setters
}
```

### Existing Entity Inventory
| Entity | Table | Key Relationships |
|---|---|---|
| `CampaignEntity` | `campaign` | → `ChannelTypeEntity`, → `FrequencyTypeEntity` |
| `CampaignMetricsEntity` | `campaign_metrics` | → `CampaignEntity` |
| `ChannelTypeEntity` | `channel_type` | — |
| `TemplateEntity` | `template` | → `TemplateTypeEntity` |
| `TemplateDefinedEntity` | `template_defined` | → `TemplateEntity` |
| `TemplateTypeEntity` | `template_type` | — |
| `MessageRecordEntity` | `message_record` | → `CampaignEntity` |
| `VerificationCodeEntity` | `verification_code` | — |
| `FrequencyTypeEntity` | `frequency_type` | — |

### Repository Pattern
```java
// JPA repository
@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {
    List<CampaignEntity> findByStatus(String status);
    Optional<CampaignEntity> findById(Long id);  // inherited from JpaRepository
}

// Pagination for large result sets
Page<CampaignEntity> findByChannelTypeId(Long channelTypeId, Pageable pageable);
```

## MongoDB Persistence

### Document Template
```java
@Document(collection = "collection_name")
public class SomeDocument {

    @Id
    private String id;

    @Field("field_name")
    private String fieldName;

    @Field("created_at")
    private OffsetDateTime createdAt;

    public SomeDocument() { }   // explicit no-arg (PMD)
    // getters / setters
}
```

### MongoDB Document Inventory
| Document | Collection | Special Lifecycle |
|---|---|---|
| `EmailMessageDocument` | `email_messages` | OPENED → SENT → deleted |
| `SmsMessageDocument` | `sms_messages` | — |
| `TelegramMessageDocument` | `telegram_messages` | — |
| `MessageDocument` | `messages` | — |

### EmailMessageDocument Lifecycle
```
1. Created with status=OPENED when message is ready to send
2. Updated to status=SENT after successful channel delivery
3. Deleted from collection after confirmed sent
```

### MongoDB Repository Pattern
```java
@Repository
public interface EmailMessageDocumentRepository
        extends MongoRepository<EmailMessageDocument, String> {
    List<EmailMessageDocument> findByStatus(String status);
    void deleteByStatusAndCampaignId(String status, Long campaignId);
}
```

## Transaction Management
```java
// Service methods that modify multiple entities must be @Transactional
@Transactional
public CompletableFuture<CampaignResponse> createCampaign(CampaignRequest request) { ... }

// Read-only queries benefit from readOnly=true
@Transactional(readOnly = true)
public CampaignResponse getCampaign(Long id) { ... }
```

## Checklist
- [ ] `spring.jpa.hibernate.ddl-auto=none` in bootstrap.yml
- [ ] Every schema change has a `V<YYYYMMDD><seq>__*.sql` Flyway file
- [ ] Flyway file in `src/main/resources/db/migration/`
- [ ] JPA entity has explicit `@Table(name=...)` and `@Column(name=...)`
- [ ] All collection relationships are `FetchType.LAZY`
- [ ] Every entity and document has an explicit no-arg constructor
- [ ] Repository extends `JpaRepository<Entity, Long>` or `MongoRepository<Doc, String>`
- [ ] Large result sets use `Pageable` pagination
- [ ] Multi-entity writes wrapped in `@Transactional`
- [ ] Read-only queries use `@Transactional(readOnly = true)`
