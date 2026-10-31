# Shared Skill: API Design

**Used by:** `developer`, `api-reviewer`

## REST Design Principles for Mercury

### URL Structure
```
/api/v1/{resource}           GET    — list all
/api/v1/{resource}/{id}      GET    — get by id
/api/v1/{resource}           POST   — create
/api/v1/{resource}/{id}      PUT    — full update
/api/v1/{resource}/{id}      PATCH  — partial update
/api/v1/{resource}/{id}      DELETE — delete

Examples:
  GET  /api/v1/campaigns
  GET  /api/v1/campaigns/{id}
  POST /api/v1/campaigns
  GET  /api/v1/channel-types
  POST /api/v1/mail/send
  POST /api/v1/verification/send
```

### OpenAPI Annotation Placement Rule
```
*Api interface  → ALL @Tag, @Operation, @ApiResponse, @Parameter annotations
*Controller     → ZERO OpenAPI annotations (implements *Api only)
```

### DTO Design Rules
```java
// Records only — no POJO classes for DTOs
// Request records: input validation annotations on components
public record CampaignRequest(
    @NotBlank String name,
    @NotNull Long channelTypeId
) {}

// Response records: no validation annotations needed
public record CampaignResponse(Long id, String name, String status) {}

// Transfer objects: internal data movement
public record CampaignTO(Long id, String name, ChannelTypeTO channelType) {}
```

### HTTP Status Code Reference
| Scenario | Code | Notes |
|---|---|---|
| Successful read | 200 | GET, PUT, PATCH |
| Created | 201 | POST |
| No content | 204 | DELETE |
| Bad request | 400 | Validation failure |
| Forbidden | 403 | `ForbiddenException` |
| Not found | 404 | `CampaignNotFoundException` |
| Unprocessable | 422 | Channel disabled (`IllegalStateException`) |
| Server error | 500 | Unhandled exception |

### Exception → HTTP Mapping (GlobalExceptionHandler)
```java
@ExceptionHandler(CampaignNotFoundException.class) → 404
@ExceptionHandler(ForbiddenException.class)         → 403
@ExceptionHandler(IllegalStateException.class)      → 422
@ExceptionHandler(IllegalArgumentException.class)   → 400
@ExceptionHandler(MethodArgumentNotValidException.class) → 400
```

### Async Return Type Rule
```
POST / PUT / PATCH / DELETE operations in *Api → CompletableFuture<*Response>
GET operations → *Response or List<*Response> (synchronous)
```

### Bean Validation Annotations (on DTO record components)
```java
@NotNull    — object field must not be null
@NotBlank   — String must not be blank
@NotEmpty   — collection/string must not be empty
@Min / @Max — numeric range
@Size       — String/collection size
@Email      — email format
@Pattern    — regex constraint
@Valid      — cascade validation on nested records
```

## Checklist
- [ ] All endpoints follow `/api/v1/` prefix
- [ ] `*Api` interface carries all OpenAPI annotations
- [ ] `*Controller` has zero OpenAPI annotations
- [ ] DTOs are Java records (not POJOs)
- [ ] All applicable HTTP codes declared in `@ApiResponse`
- [ ] `@Valid` on all `@RequestBody` parameters
- [ ] Write operations return `CompletableFuture<*Response>`
- [ ] Exception → HTTP mapping matches `GlobalExceptionHandler`
