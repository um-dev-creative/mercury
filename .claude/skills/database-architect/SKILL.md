---
agent: database-architect
version: 1.0
---

## 1. Project-Specific Patterns

### JPA entity pattern
```java
@Entity
@Table(name = "campaign")
public class CampaignEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // ... fields
    // explicit constructor required (PMD AtLeastOneConstructor)
    public CampaignEntity() {}
}
```

### MongoDB document lifecycle
`EmailMessageDocument` is the primary transient document:
1. Written when `MultiChannelListener` / `EmailChannelService` receives a Kafka message
2. Read by `MessageProcessor.processMessage()` — finds `OPENED` status docs, sends via SMTP
3. Updated to `SENT` status by `MessageProcessor.processMessage()`
4. Read by `MessageProcessor.updateMessageStatus()` — finds `SENT` docs
5. Converted to `MessageRecordEntity` (PostgreSQL) and deleted from MongoDB

Breaking this lifecycle (e.g., deleting documents prematurely, changing status constants) is a critical bug.

### DDL None pattern
Hibernate DDL is `none`. All schema changes require SQL migration scripts:
```sql
-- src/main/resources/db/V1__add_frequency_type.sql
CREATE TABLE frequency_type (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL
);
```

## 2. Naming Conventions

- JPA entities: `<Entity>Entity` (e.g., `CampaignEntity`, `ChannelTypeEntity`)
- MongoDB documents: `<Type>MessageDocument` or `<Type>Document`
- SQL repositories: `<Entity>Repository` extending `JpaRepository<XxxEntity, UUID>`
- MongoDB repositories: `<Type>NSRepository` (NS = NoSQL) extending `MongoRepository`
- Migration scripts: `src/main/resources/db/V<N>__<description>.sql`

## 3. Error Handling

- Missing entity by UUID → `CampaignNotFoundException` thrown in service layer (not repository layer)
- MongoDB document not found → return empty `Optional` / empty `List` — do not throw
- Repository methods returning `Optional<T>` must be handled with `.orElseThrow()` or `.orElse()` in service layer

## 4. Key Files

- `src/main/java/com/prx/mercury/jpa/sql/entity/` — all JPA entities
- `src/main/java/com/prx/mercury/jpa/sql/repository/` — all SQL repositories
- `src/main/java/com/prx/mercury/jpa/nosql/document/EmailMessageDocument.java` — primary MongoDB document
- `src/main/java/com/prx/mercury/jpa/nosql/repository/EmailMessageNSRepository.java` — MongoDB repo
- `src/main/java/com/prx/mercury/mapper/CampaignMapper.java` — entity ↔ TO reference mapper
- `src/main/java/com/prx/mercury/config/DatabaseConfig.java` — dual-datasource config
- `src/main/resources/db/` — SQL migration scripts

## 5. Constraints

- DDL strategy is `none` — Hibernate must not manage schema
- `EmailMessageDocument` must maintain `OPENED` → `SENT` → deleted lifecycle
- All JPA entities must have `@Table(name = "...")` explicit annotation
- UUID primary keys preferred (`@GeneratedValue(strategy = GenerationType.UUID)`)
- No business logic in entity classes — entities are pure data holders

## 6. Checklist

- [ ] New entity has `@Entity`, `@Table(name = "...")`, explicit no-arg constructor
- [ ] New repository extends `JpaRepository<XxxEntity, UUID>` or `MongoRepository<XxxDocument, String>`
- [ ] SQL migration script created in `src/main/resources/db/`
- [ ] MapStruct mapper updated for new fields
- [ ] `EmailMessageDocument` lifecycle not broken
- [ ] Repository `Optional<T>` returns handled in service layer with `.orElseThrow()`
