---
name: Implement Feature
description: >
  Implement a new Mercury REST endpoint end-to-end: *Api interface, *Controller,
  *ServiceImpl, DTOs, MapStruct mapper update, and Maven verification.
mode: agent
agent: developer
tools: [read_file, grep_search, file_search, create_file, insert_edit_into_file, replace_string_in_file, run_in_terminal]
---

## Input Variables

- `${featureName}` — human-readable feature name (e.g., "Campaign Pause")
- `${httpMethod}` — HTTP method: GET | POST | PUT | PATCH | DELETE
- `${endpointPath}` — full path (e.g., `/api/v1/campaigns/{id}/pause`)
- `${issueId}` — issue ID (e.g., `ds-172`)
- `${requestRecord}` — name of the request record (e.g., `PauseCampaignRequest`) — omit for GET/DELETE
- `${responseRecord}` — name of the response record (e.g., `CampaignDetailResponse`)

## Steps

1. **Read the reference files** before writing anything:
   - `src/main/java/com/prx/mercury/api/v1/controller/CampaignApi.java`
   - `src/main/java/com/prx/mercury/api/v1/service/CampaignServiceImpl.java`
   - `src/main/java/com/prx/mercury/api/v1/to/CreateCampaignRequest.java`

2. **Create or update the `*Api` interface** in `src/main/java/com/prx/mercury/api/v1/controller/`:
   - `@Operation(summary, description, operationId)` — operationId is `${httpMethod.toLowerCase}${featureName}` camelCase
   - `@ApiResponses` covering: 200/201, 400, 403, 404 (if `{id}`), 422 (if channel), 500
   - Do NOT add these annotations to the controller class

3. **Create the request DTO** (if not GET/DELETE) in `src/main/java/com/prx/mercury/api/v1/to/${requestRecord}.java`:
   - Java `record` with Bean Validation: `@NotNull`, `@NotBlank`, `@Valid`, `@NotEmpty`

4. **Create or update the `*Service` interface**:
   - Write operations: `CompletableFuture<${responseRecord}>`
   - Read operations: `${responseRecord}` or `List<${responseRecord}>`

5. **Implement `*ServiceImpl`**:
   - `private static final Logger logger = LoggerFactory.getLogger(XxxServiceImpl.class)`
   - Constructor injection only — no `@Autowired` on fields
   - Throw `CampaignNotFoundException` for missing UUIDs → 404
   - Throw `IllegalStateException` for disabled channel type → 422
   - Throw `ForbiddenException` for auth failures → 403
   - Explicit constructor (PMD `AtLeastOneConstructor`)

6. **Update the `*Controller`**:
   - Implement the interface method
   - `@Valid` on request body parameter
   - Delegate to service, wrap in `ResponseEntity`
   - No business logic

7. **Update MapStruct mapper** if new entity fields are mapped

8. **Verify**:
   ```bash
   mvn -U clean package -DskipTests
   mvn clean test
   ```

## Constraints

- `@Operation` / `@ApiResponse` on `*Api` interface only
- SLF4J `LoggerFactory` only — no other logging
- Kafka topic strings from `bootstrap.yml` properties — never hardcoded
- Scheduler rates use `${prx.scheduler.*}` placeholders
- No `new CampaignMapper()` — inject mappers as Spring beans

## Output

List every file created or modified with its full path and a one-line summary of the change. End with `mvn clean test` result: PASS / FAIL.
