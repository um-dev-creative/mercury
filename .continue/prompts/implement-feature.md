---
name: Implement Feature
description: Implement a new Mercury REST endpoint across all required layers
---

Implement a new Mercury feature:
- Feature: {{{ featureName }}}
- HTTP Method: {{{ httpMethod }}}
- Endpoint: {{{ endpointPath }}}
- Issue: {{{ issueId }}}

## Instructions

Read these reference files first:
- `src/main/java/com/prx/mercury/api/v1/controller/CampaignApi.java`
- `src/main/java/com/prx/mercury/api/v1/service/CampaignServiceImpl.java`
- `src/main/java/com/prx/mercury/api/v1/to/CreateCampaignRequest.java`

Then implement in this exact order:

1. **`*Api` interface** — add the method with full `@Operation(summary, description, operationId)` and `@ApiResponses`. operationId = `{httpMethod}{FeatureName}` in camelCase. Status codes: 200/201, 400, 403, 404 (if `{id}` in path), 422 (if channel-related), 500.

2. **Request record** (if not GET/DELETE) — Java `record` in `com.prx.mercury.api.v1.to` with `@NotNull`/`@NotBlank`/`@Valid`/`@NotEmpty` on each field.

3. **`*Service` interface** — `CompletableFuture<ResponseRecord>` for writes, `ResponseRecord` or `List<ResponseRecord>` for reads.

4. **`*ServiceImpl`** — constructor injection, SLF4J logger (`LoggerFactory.getLogger(ThisClass.class)`), explicit constructor, correct exception types:
   - UUID not found → `CampaignNotFoundException`
   - Channel disabled → `IllegalStateException`
   - Auth failure → `ForbiddenException`

5. **`*Controller`** — implement the interface method, `@Valid` on request body, delegate to service, no business logic.

6. **MapStruct mapper** — update if new entity fields need mapping.

7. **Verify**:
   ```bash
   mvn -U clean package -DskipTests
   mvn clean test
   ```

List all files created/modified with their full paths. End with build result.
