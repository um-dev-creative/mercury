---
agent: api-reviewer
version: 1.0
---

## 1. Project-Specific Patterns

### Interface + controller split
All OpenAPI annotations on interface:
```java
// CampaignApi.java
@Tag(name = "campaigns", description = "Campaign Management API")
public interface CampaignApi {
    @Operation(summary = "Create a new campaign", operationId = "createCampaign")
    @ApiResponses({ @ApiResponse(responseCode = "201", ...), ... })
    ResponseEntity<CreateCampaignResponse> createCampaign(CreateCampaignRequest request);
}

// CampaignController.java — no OpenAPI annotations here
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController implements CampaignApi { ... }
```

### Standard HTTP status codes used in Mercury
- `201 Created` — POST that creates a resource
- `200 OK` — GET, PATCH toggle
- `400 Bad Request` — validation errors, `MethodArgumentNotValidException`, `IllegalArgumentException`
- `401` — not used in Mercury (auth failures → 403)
- `403 Forbidden` — `ForbiddenException` from BackboneClient validation
- `404 Not Found` — `CampaignNotFoundException`
- `422 Unprocessable Entity` — `IllegalStateException` (disabled channel type)
- `500 Internal Server Error` — unhandled exceptions

### Session-scoped endpoints
```java
ResponseEntity<List<CampaignDetailResponse>> getByApplication(
    UUID applicationId,
    @RequestHeader("session-token") String sessionToken
);
```

## 2. Naming Conventions

- API interfaces: `CampaignApi`, `MailApi`, `ChannelTypeApi`, `VerificationCodeApi`
- `operationId` format: camelCase verb + noun (e.g., `createCampaign`, `getCampaignById`, `toggleCampaign`)
- Request records: `Create<Resource>Request`, `Update<Resource>Request`
- Response records: `Create<Resource>Response`, `<Resource>DetailResponse`

## 3. Error Handling

Every `@ApiResponses` block must include the responses that apply. Standard minimum set:
```
201/200, 400, 403, 500
```
Add 404 for endpoints that retrieve by ID. Add 422 for endpoints that interact with channel types.

## 4. Key Files

- `../../../src/main/java/com/umdc/mercury/api/v1/controller/CampaignApi.java` — reference for complete OpenAPI annotation pattern
- `../../../src/main/java/com/umdc/mercury/api/v1/controller/MailApi.java` — mail endpoint contract
- `../../../src/main/java/com/umdc/mercury/api/v1/controller/GlobalExceptionHandler.java` — exception-to-status mapping
- `src/main/java/com/prx/mercury/api/v1/to/` — all DTO records

## 5. Constraints

- `operationId` is required on every `@Operation`
- `@ApiResponse` codes must match what `GlobalExceptionHandler` actually returns for that endpoint
- Request body records must use Bean Validation (`@NotNull`, `@NotBlank`, `@Valid`, `@Size`)
- Response records are immutable Java `record` types — no setters or mutable fields

## 6. Checklist

- [ ] `@Tag` on interface
- [ ] `@Operation` with `summary`, `description`, and `operationId` on every method
- [ ] `@ApiResponses` covering all reachable status codes
- [ ] No OpenAPI annotations on the `*Controller` class
- [ ] Request DTOs have Bean Validation
- [ ] Response DTOs are Java records
- [ ] Status codes align with `GlobalExceptionHandler` exception mappings
- [ ] User-scoped endpoints use `@RequestHeader("session-token")`
