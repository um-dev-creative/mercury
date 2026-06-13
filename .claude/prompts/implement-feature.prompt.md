---
name: implement-feature
description: Implements a new Mercury feature across all required layers following project conventions
mode: agent
agent: developer
tools: [Read, Edit, Write, Bash]
---

## Input Variables

- `${featureName}` — human-readable feature name (e.g., "Campaign Pause/Resume")
- `${httpMethod}` — HTTP method (GET, POST, PUT, PATCH, DELETE)
- `${endpointPath}` — full path (e.g., `/api/v1/campaigns/{id}/pause`)
- `${issueId}` — YouTrack issue ID (e.g., `ds-172`)
- `${channelType}` — affected channel(s), or `ALL` if not channel-specific
- `${requestRecord}` — name of the request DTO record (e.g., `PauseCampaignRequest`)
- `${responseRecord}` — name of the response DTO record (e.g., `CampaignDetailResponse`)

## Steps

1. **Read existing reference files** to understand the current pattern before writing anything:
   - `src/main/java/com/prx/mercury/api/v1/controller/CampaignApi.java`
   - `src/main/java/com/prx/mercury/api/v1/service/CampaignServiceImpl.java`
   - `src/main/java/com/prx/mercury/api/v1/to/CreateCampaignRequest.java`

2. **Create or update the `*Api` interface** at `src/main/java/com/prx/mercury/api/v1/controller/`:
   - Add method with `@Operation(summary, description, operationId)` and `@ApiResponses`
   - Include status codes: 200/201, 400, 403, 404 (if by ID), 422 (if channel involved), 500
   - `operationId` must be camelCase: `${httpMethod.toLowerCase}${featureName.camelCase}`

3. **Create the request DTO** at `src/main/java/com/prx/mercury/api/v1/to/${requestRecord}.java`:
   - Java `record` with Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Valid`)
   - No mutable state — records only

4. **Create or update the `*Service` interface** at `src/main/java/com/prx/mercury/api/v1/service/`:
   - Method returns `CompletableFuture<${responseRecord}>` for write operations
   - Method returns `${responseRecord}` or `List<${responseRecord}>` for read operations

5. **Implement `*ServiceImpl`** at `src/main/java/com/prx/mercury/api/v1/service/`:
   - `private static final Logger logger = LoggerFactory.getLogger(XxxServiceImpl.class)`
   - Inject dependencies via constructor (no `@Autowired` on fields)
   - Throw `CampaignNotFoundException` for missing UUIDs
   - Throw `IllegalStateException` for disabled channel type
   - Throw `ForbiddenException` for auth failures
   - Include explicit no-arg or all-arg constructor (PMD `AtLeastOneConstructor`)

6. **Update the `*Controller`** at `src/main/java/com/prx/mercury/api/v1/controller/`:
   - Implement the interface method
   - Add `@Valid` annotation on request body parameter
   - Delegate to service, wrap result in `ResponseEntity`
   - No business logic in controller

7. **Update MapStruct mapper** if new entity fields are mapped:
   - `src/main/java/com/prx/mercury/mapper/CampaignMapper.java` or create new mapper

8. **Verify compilation**:
   ```bash
   mvn -U clean package -DskipTests
   ```

9. **Run PMD**:
   ```bash
   mvn clean test
   ```
   Fix any PMD violations before proceeding.

## Constraints

- OpenAPI annotations ONLY on `*Api` interface — never on `*Controller`
- Scheduler rates use `${prx.scheduler.*}` placeholders — no hardcoded milliseconds
- Logger: `LoggerFactory.getLogger()` only — no Log4j, no `java.util.logging`
- Kafka topics referenced from `bootstrap.yml` properties — never hardcoded strings
- No `new CampaignMapper()` — mappers are Spring beans

## Output Format

List of files created/modified:
- `src/main/java/com/prx/mercury/api/v1/controller/<Interface>.java` — created/updated
- `src/main/java/com/prx/mercury/api/v1/controller/<Controller>.java` — created/updated
- `src/main/java/com/prx/mercury/api/v1/to/<RequestRecord>.java` — created
- `src/main/java/com/prx/mercury/api/v1/service/<Service>.java` — created/updated
- `src/main/java/com/prx/mercury/api/v1/service/<ServiceImpl>.java` — created/updated
- Build result: PASS / FAIL with details
