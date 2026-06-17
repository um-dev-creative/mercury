---
name: review-api-contract
description: Reviews a new or modified Mercury REST API contract for correctness, completeness, and consistency
mode: ask
agent: api-reviewer
tools: [Read, Bash]
---

## Input Variables

- `${apiInterface}` — name of the `*Api` interface to review (e.g., `CampaignApi`)
- `${changeType}` — `NEW` (new interface) or `MODIFIED` (additions to existing interface)

## Steps

1. **Read the API interface**:
   - `src/main/java/com/prx/mercury/api/v1/controller/${apiInterface}.java`

2. **Read the controller implementation**:
   - `src/main/java/com/prx/mercury/api/v1/controller/${apiInterface/Api/Controller}.java`
   - Verify no OpenAPI annotations exist on the controller class

3. **Read the request/response DTOs** for each endpoint:
   - `src/main/java/com/prx/mercury/api/v1/to/` — check all referenced record types

4. **Read `GlobalExceptionHandler`** to cross-reference status codes:
   - `../../src/main/java/com/umdc/mercury/api/v1/controller/GlobalExceptionHandler.java`

5. **Evaluate each endpoint method**:

   a. **OpenAPI annotations** — verify on interface, not controller:
      - `@Operation` with `summary`, `description`, `operationId`
      - `@ApiResponses` covering all reachable status codes

   b. **Status code completeness**:
      - 201 for POST create operations
      - 200 for GET and PATCH
      - 400 — validation failures always possible
      - 403 — required if endpoint is auth-gated (has `session-token`)
      - 404 — required if endpoint takes a `{id}` path variable
      - 422 — required if endpoint interacts with channel types
      - 500 — always required

   c. **Request DTO validation**:
      - All fields have appropriate Bean Validation annotations
      - Non-null UUIDs annotated with `@NotNull`
      - String fields annotated with `@NotBlank` (not `@NotNull`)
      - Nested objects annotated with `@Valid`

   d. **User-scoped endpoints**:
      - If filtering by user, is `@RequestHeader("session-token")` present?
      - Is user id extracted from token (not from request body)?

   e. **operationId uniqueness**:
      - Is `operationId` unique across all Mercury `*Api` interfaces?
      - Does it follow camelCase verb+noun pattern?

6. **Check reference implementation** `CampaignApi.java` for pattern conformance

## Constraints

- All `@Operation` must have `operationId` — this is required for OpenAPI code generation
- No `@Tag` on controller classes — only on `*Api` interfaces
- Response records must be Java `record` types — no mutable classes
- User ID must never come from request body in user-scoped endpoints

## Output Format

Endpoint review table:
| Method | Path | operationId | @Operation | @ApiResponses Complete | DTO Validation | Auth Header | Issues |
|---|---|---|---|---|---|---|---|

DTO review table:
| Record | Field | Annotation | Issue |
|---|---|---|---|

**Verdict: APPROVED / REQUEST_CHANGES**
(List specific changes required)
