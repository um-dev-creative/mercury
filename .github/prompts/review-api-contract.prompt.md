---
name: review-api-contract
description: Define and validate the OpenAPI contract for a new or modified Mercury endpoint
mode: agent
agent: api-reviewer
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
# Review API Contract

## Input Variables
- `${endpointDescription}` — what the endpoint does
- `${httpMethod}` — GET | POST | PUT | PATCH | DELETE
- `${resourcePath}` — path relative to `/api/v1/` (e.g., `campaigns/{id}/progress`)
- `${requestDtoFields}` — field definitions for request DTO (or `none` for GET)
- `${responseDtoFields}` — field definitions for response DTO
- `${apiInterfaceName}` — existing or new `*Api` interface (e.g., `CampaignApi`)
- `${controllerName}` — existing or new `*Controller` (e.g., `CampaignController`)

## Step 1: Verify Existing Contract (if modifying)
```bash
cat src/main/java/com/umdc/mercury/api/v1/controller/${apiInterfaceName}.java
```
Identify existing endpoints and ensure new endpoint doesn't conflict.

## Step 2: Define DTO Records

### Request DTO (if ${httpMethod} is POST, PUT, or PATCH)
File: `src/main/java/com/umdc/mercury/api/v1/to/<Resource>Request.java`
```java
public record <Resource>Request(
    @NotBlank(message = "<Field> is required") String fieldName,
    @NotNull(message = "<Field> is required") Long numericField
    // ${requestDtoFields}
) {}
```

### Response DTO
File: `src/main/java/com/umdc/mercury/api/v1/to/<Resource>Response.java`
```java
public record <Resource>Response(
    Long id,
    // ${responseDtoFields}
) {}
```

## Step 3: Determine HTTP Response Codes
For `${httpMethod}`:
- GET → 200 (found), 404 (not found), 403 (forbidden)
- POST → 201 (created), 400 (bad input), 403 (forbidden)
- PUT / PATCH → 200 (updated), 400 (bad input), 404 (not found), 403 (forbidden)
- DELETE → 204 (deleted), 404 (not found), 403 (forbidden)

Additional codes:
- 422 if channel can be disabled (`IllegalStateException`)

## Step 4: Write/Update `*Api` Interface
Add to `src/main/java/com/umdc/mercury/api/v1/controller/${apiInterfaceName}.java`:

```java
@Operation(summary = "${endpointDescription}")
@ApiResponse(responseCode = "200", description = "Success",
    content = @Content(schema = @Schema(implementation = <Resource>Response.class)))
@ApiResponse(responseCode = "404", description = "Not found")
@ApiResponse(responseCode = "403", description = "Access denied")
// (add all applicable codes)
@${httpMethod}Mapping("/${resourcePath}")
<ReturnType> <methodName>(
    @Parameter(description = "...", required = true) @PathVariable Long id  // if applicable
    @RequestBody @Valid <Resource>Request request  // if POST/PUT/PATCH
);
```

Return type rules:
- GET → `<Resource>Response` or `List<<Resource>Response>`
- POST/PUT/PATCH/DELETE → `CompletableFuture<<Resource>Response>` or `CompletableFuture<Void>`

## Step 5: Verify `*Controller` Has No OpenAPI Annotations
```bash
grep -n "@Tag\|@Operation\|@ApiResponse\|@Parameter" \
  src/main/java/com/umdc/mercury/api/v1/controller/${controllerName}.java
```
Must return empty. If not → remove annotations from controller.

## Step 6: API Contract Completeness Checklist
- [ ] `*Api` interface exists with `@Tag` on class
- [ ] All endpoints have `@Operation(summary=...)`
- [ ] All endpoints declare ALL applicable `@ApiResponse` codes
- [ ] Request DTOs use `@NotBlank`/`@NotNull` on required fields
- [ ] `@Valid` on `@RequestBody` parameters
- [ ] Path prefix is `/api/v1/`
- [ ] Write operations return `CompletableFuture<*Response>`
- [ ] Read operations return `*Response` or `List<*Response>`
- [ ] `*Controller` has ZERO OpenAPI annotations

## Step 7: Compile Verification
```bash
mvn -U clean package -DskipTests
```
Must produce `BUILD SUCCESS`.

## Constraints
- NEVER place OpenAPI annotations on `*Controller`
- NEVER use classes for DTOs — records only
- NEVER return `void` from write operations — use `CompletableFuture<Void>` minimum
- NEVER declare a POST endpoint without `@RequestBody @Valid`
- NEVER declare a `@ApiResponse` without `responseCode` AND `description`

## Output Format
1. API contract table: Method | Path | Request DTO | Response DTO | HTTP Codes
2. DTO record definitions (field, type, validation)
3. `*Api` interface code for new/modified methods
4. OpenAPI annotation completeness: COMPLETE / INCOMPLETE (with missing items)
5. `mvn -U clean package -DskipTests` → BUILD SUCCESS
6. **Verdict: APPROVED / NEEDS_REVISION** with itemized issues
