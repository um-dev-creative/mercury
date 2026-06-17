# Tool: Flyway Database Migrations

**Purpose:** Manage PostgreSQL schema changes for Mercury via Flyway SQL migrations.

## Core Principles
- DDL strategy in `bootstrap.yml` is always `none` — Flyway is the ONLY mechanism for schema changes
- Migration files are **append-only** — never modify an existing file after it has been applied
- Each migration is atomic and must be idempotent where possible

## File Location
```
src/main/resources/db/migration/
```

## Naming Convention
```
Format:  V<YYYYMMDD><3-digit-seq>__<snake_case_description>.sql

Examples:
  V20250101001__initial_schema.sql
  V20250115001__add_campaign_metrics_table.sql
  V20250315001__add_campaign_status_column.sql
  V20250315002__create_frequency_type_index.sql

Rules:
  - Date: YYYYMMDD (year + month + day, zero-padded)
  - Seq:  3-digit sequence, resets per date (001, 002, 003 ...)
  - Description: lowercase, underscores only, no spaces
  - Double underscore (__) separates version from description (Flyway requirement)
```

## Migration Script Template
```sql
-- V20250315001__add_campaign_status_column.sql
-- Description: Adds status column to campaign table with default value
-- Author: database-architect
-- Date: 2025-03-15

ALTER TABLE campaign
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

CREATE INDEX IF NOT EXISTS idx_campaign_status ON campaign(status);
```

## Flyway via Maven (Runs Automatically)
Flyway migrations run automatically at application startup via Spring Boot auto-configuration.
They also run during integration tests that start an application context.

```bash
# Verify migrations apply cleanly (requires DB connection)
mvn flyway:info

# Run pending migrations manually
mvn flyway:migrate

# Validate applied checksums match files
mvn flyway:validate

# Repair checksum mismatch (use carefully — only for dev environments)
mvn flyway:repair
```

## Bootstrap.yml Flyway Configuration
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
  jpa:
    hibernate:
      ddl-auto: none   # ALWAYS none — Flyway manages schema
```

## Common Migration Patterns

### Add Column
```sql
ALTER TABLE campaign
    ADD COLUMN IF NOT EXISTS channel_config JSONB;
```

### Create Table
```sql
CREATE TABLE IF NOT EXISTS frequency_type (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Add Index
```sql
CREATE INDEX IF NOT EXISTS idx_message_record_campaign_id
    ON message_record(campaign_id);
```

### Add Foreign Key
```sql
ALTER TABLE campaign
    ADD CONSTRAINT fk_campaign_channel_type
    FOREIGN KEY (channel_type_id) REFERENCES channel_type(id);
```

### Rename Column (careful — check all JPA entities)
```sql
ALTER TABLE campaign RENAME COLUMN old_name TO new_name;
```

## Existing Migration Files
Located at: `src/main/resources/db/migration/`
List with: `ls -la src/main/resources/db/migration/`

## Error Recovery
| Error | Resolution |
|---|---|
| Checksum mismatch | NEVER edit the file; create a corrective `V<date><seq>__fix_*.sql` |
| Migration already applied | Check `flyway_schema_history` table; use `flyway:repair` if needed |
| SQL syntax error | Fix in a NEW migration file; do not touch applied migrations |
| Duplicate version | Rename with incremented sequence number |

## Constraints
- NEVER modify an existing migration file after application to any environment
- NEVER use Hibernate DDL (`create`, `update`, `validate`) — always `none`
- NEVER include DML (data updates) in schema migrations unless strictly required
- ALWAYS use `IF NOT EXISTS` / `IF EXISTS` for safety
- ALWAYS include a comment header with description, author, and date
