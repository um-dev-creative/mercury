---
name: implement-feature
description: Implement a complete Mercury feature from scratch across all required layers
mode: agent
agent: developer
tools:
  - run_in_terminal
  - read_file
  - grep_search
  - file_search
  - insert_edit_into_file
  - replace_string_in_file
  - create_file
  - get_errors
---
# Implement Feature

## Input Variables
- `${featureName}` — short name for the feature (e.g., `campaign-progress`)
- `${featureDescription}` — what the feature does
- `${layersAffected}` — comma-separated layers: `persistence,service,controller,kafka,mapper,scheduler`
- `${ticketId}` — optional YouTrack/issue ID

## Pre-Implementation Checklist
Before writing any code, confirm:
1. `database-architect` has approved schema changes (if persistence layer affected)
2. `api-reviewer` has approved the API contract (if controller layer affected)
3. Feature branch `feature/${featureName}` has been created off `main`

## Step 1: Set Up Feature Branch
```bash
git checkout main && git pull origin main
git checkout -b feature/${featureName}
```

## Step 2: Persistence Layer (if applicable)
Only proceed if `database-architect` has already provided the Flyway migration and entity definition.
- Verify `src/main/resources/db/migration/V<YYYYMMDD><seq>__*.sql` exists
- Implement/update JPA entity in `com.umdc.mercury.jpa.sql.entity`
- Implement/update repository in `com.umdc.mercury.jpa.sql.repository`
- Or MongoDB document in `com.umdc.mercury.jpa.nosql.document` + repository

## Step 3: DTO Records (if new endpoint)
Create request/response records in `com.umdc.mercury.api.v1.to`:
```java
// *Request — with Bean Validation
// *Response — clean output record
// *TO — internal transfer if needed
```
Rules: Java records only. `@NotBlank`/`@NotNull` on request components.

## Step 4: MapStruct Mapper (if new entity ↔ DTO mapping)
Create mapper interface in `com.umdc.mercury.mapper`:
```java
@Mapper(componentModel = "spring")
public interface ${FeatureName}Mapper {
    // toResponse, toEntity, toDocument methods
}
```
Inject in service as Spring bean — never `new *MapperImpl()`.

## Step 5: Service Implementation
Create/update `${FeatureName}ServiceImpl` in `com.umdc.mercury.api.v1.service`:
- Explicit constructor with all dependencies
- `private static final Logger log = LoggerFactory.getLogger(...)`
- Read methods return `T` or `List<T>` directly
- Write methods return `CompletableFuture<T>`
- Throw `CampaignNotFoundException` (404), `ForbiddenException` (403), etc. as appropriate

## Step 6: Controller (if new endpoint)
- Update `${FeatureName}Api` interface in `com.umdc.mercury.api.v1.controller` — add OpenAPI annotations here
- Update `${FeatureName}Controller` — implements `${FeatureName}Api`, zero OpenAPI annotations

## Step 7: Kafka / Scheduler / Processor (if applicable)
- Kafka listeners: topics from `${prx.consumer.topics.*}` only
- Scheduler rates: from `${prx.scheduler.*}` only
- Add new property keys to `bootstrap.yml` if introducing new topics/rates

## Step 8: Compile Verification
```bash
mvn -U clean package -DskipTests
```
Must produce `BUILD SUCCESS`. Fix all compilation errors before proceeding.

## Step 9: PMD Check
```bash
mvn pmd:check
```
Must produce 0 violations. Fix all violations before proceeding.

## Constraints
- NEVER place OpenAPI annotations on `*Controller`
- NEVER hardcode Kafka topic strings or scheduler rates
- NEVER use `new *MapperImpl()`
- NEVER omit explicit constructors
- NEVER use `System.out.println` — SLF4J only
- NEVER put secrets in source

## Output Format
1. List of all created/modified files with full paths
2. `mvn -U clean package -DskipTests` output (BUILD SUCCESS required)
3. PMD violations count (must be 0)
4. Summary of changes per layer
5. Ready-for-test-writer confirmation
