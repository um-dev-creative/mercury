# Database Architect SKILL

## Project-Specific Patterns

### Flyway Migration File Pattern
```
src/main/resources/db/migration/
├── V20250101001__initial_schema.sql
├── V20250115001__add_campaign_metrics.sql
└── V20250201001__add_verification_code_expiry.sql

Naming rule: V<YYYYMMDD><3-digit-seq>__<snake_case_description>.sql
Examples:
  V20250315001__add_message_record_channel_type.sql
  V20250315002__create_frequency_type_table.sql
```

### Flyway Migration Content Pattern
```sql
-- V20250315001__add_campaign_status_column.sql
-- Description: Adds status column to campaign table
-- Author: database-architect
-- Date: 2025-03-15

ALTER TABLE campaign
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

CREATE INDEX idx_campaign_status ON campaign(status);
```

### JPA Entity Pattern
```java
// com.umdc.mercury.jpa.sql.entity
@Entity
@Table(name = "campaign")
public class CampaignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_type_id", nullable = false)
    private ChannelTypeEntity channelType;

    // Explicit no-arg constructor (PMD AtLeastOneConstructor)
    public CampaignEntity() { }

    // All-args constructor
    public CampaignEntity(String name, String status) {
        this.name = name;
        this.status = status;
    }
    // getters/setters
}
```

### Spring Data JPA Repository Pattern
```java
// com.umdc.mercury.jpa.sql.repository
@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {

    List<CampaignEntity> findByStatus(String status);

    Optional<CampaignEntity> findByNameAndChannelTypeId(String name, Long channelTypeId);

    @Query("SELECT c FROM CampaignEntity c WHERE c.channelType.id = :channelTypeId")
    List<CampaignEntity> findAllByChannelTypeId(@Param("channelTypeId") Long channelTypeId);
}
```

### MongoDB Document Pattern
```java
// com.umdc.mercury.jpa.nosql.document
@Document(collection = "email_messages")
public class EmailMessageDocument {

    @Id
    private String id;

    @Field("status")
    private String status;  // Lifecycle: OPENED → SENT → deleted

    @Field("campaign_id")
    private Long campaignId;

    @Field("recipient")
    private String recipient;

    @Field("created_at")
    private OffsetDateTime createdAt;

    public EmailMessageDocument() { }
    // getters/setters
}
// Lifecycle: OPENED → SENT → document deleted from collection
```

### MongoDB Repository Pattern
```java
// com.umdc.mercury.jpa.nosql.repository
@Repository
public interface EmailMessageDocumentRepository
        extends MongoRepository<EmailMessageDocument, String> {

    List<EmailMessageDocument> findByStatus(String status);
    List<EmailMessageDocument> findByCampaignId(Long campaignId);
    void deleteByStatusAndCampaignId(String status, Long campaignId);
}
```

### Existing Entity → Table Mapping
| Entity Class | Table Name | Repository |
|---|---|---|
| `CampaignEntity` | `campaign` | `CampaignRepository` |
| `CampaignMetricsEntity` | `campaign_metrics` | — |
| `ChannelTypeEntity` | `channel_type` | `ChannelTypeRepository` |
| `TemplateEntity` | `template` | `TemplateRepository` |
| `TemplateDefinedEntity` | `template_defined` | `TemplateDefinedRepository` |
| `TemplateTypeEntity` | `template_type` | `TemplateTypeEntityRepository` |
| `MessageRecordEntity` | `message_record` | `MessageRecordRepository` |
| `VerificationCodeEntity` | `verification_code` | `VerificationCodeRepository` |
| `FrequencyTypeEntity` | `frequency_type` | `FrequencyTypeRepository` |

### MongoDB Documents Inventory
| Document Class | Collection | Lifecycle |
|---|---|---|
| `EmailMessageDocument` | `email_messages` | OPENED → SENT → deleted |
| `SmsMessageDocument` | `sms_messages` | — |
| `TelegramMessageDocument` | `telegram_messages` | — |
| `MessageDocument` | `messages` | — |

## Naming Conventions
- JPA Entity: `*Entity` in `com.umdc.mercury.jpa.sql.entity`
- JPA Repository: `*Repository` in `com.umdc.mercury.jpa.sql.repository`
- MongoDB Document: `*Document` in `com.umdc.mercury.jpa.nosql.document`
- MongoDB Repository: `*Repository` in `com.umdc.mercury.jpa.nosql.repository`
- Flyway files: `V<YYYYMMDD><seq>__<description>.sql` (see pattern above)
- Table names: `snake_case` (explicit `@Table(name = "...")`)
- Column names: `snake_case` (explicit `@Column(name = "...")`)

## Error Handling
- Flyway checksum mismatch → NEVER modify existing migration; create a new corrective one
- Entity relationship cycle → document and break with `FetchType.LAZY` + explicit join
- MongoDB document not found → repository returns `Optional` or empty list, service throws appropriate exception
- DDL applied without Flyway → revert immediately; apply via migration script

## Key Files
- `src/main/java/com/umdc/mercury/jpa/sql/entity/` — all JPA entities
- `src/main/java/com/umdc/mercury/jpa/sql/repository/` — JPA repositories
- `src/main/java/com/umdc/mercury/jpa/nosql/document/` — MongoDB documents
- `src/main/java/com/umdc/mercury/jpa/nosql/repository/` — MongoDB repositories
- `src/main/resources/db/migration/` — Flyway SQL migration files
- `src/main/resources/bootstrap.yml` — datasource + MongoDB config
- `.github/tools/flyway.tool.md` — migration commands and validation
- `.github/skills/persistence.skill.md` — shared persistence patterns

## Constraints
- NEVER set `spring.jpa.hibernate.ddl-auto` to anything other than `none`
- NEVER modify an existing Flyway migration file after it has run
- NEVER create schema changes without a corresponding Flyway migration
- NEVER use implicit column/table names — always explicit `@Table(name=)` and `@Column(name=)`
- NEVER use `FetchType.EAGER` for collection associations
- NEVER delete MongoDB documents outside the defined lifecycle (OPENED → SENT → deleted)
- NEVER create a repository method that bypasses pagination for potentially large result sets

## Checklist
- [ ] Flyway migration file created with correct `V<YYYYMMDD><seq>__*.sql` naming
- [ ] Migration file placed in `src/main/resources/db/migration/`
- [ ] No modification to existing migration files
- [ ] JPA entity has `@Table(name=...)` and `@Column(name=...)` explicit annotations
- [ ] Every entity has an explicit no-arg constructor
- [ ] `ddl-auto` remains `none` in `bootstrap.yml`
- [ ] MongoDB document lifecycle matches OPENED → SENT → deleted (for email)
- [ ] Repository interface extends `JpaRepository` or `MongoRepository`
- [ ] No `FetchType.EAGER` on collection associations
- [ ] Impact analysis documented (existing queries, index needs)
