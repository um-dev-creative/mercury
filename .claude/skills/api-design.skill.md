---
name: api-design
used-by: [developer, api-reviewer]
version: 1.0
---

## API Design Shared Skill — Mercury `/api/v1/` Conventions

### URL Structure
```
GET    /api/v1/campaigns                        # list
POST   /api/v1/campaigns                        # create
GET    /api/v1/campaigns/{id}                   # get by id (UUID)
PUT    /api/v1/campaigns/{id}                   # full update
PATCH  /api/v1/campaigns/{id}/toggle            # partial state change
GET    /api/v1/campaigns?applicationId={uuid}   # filter by query param
GET    /api/v1/channel-types                    # list
POST   /api/v1/mail                             # send email
GET    /api/v1/verification-codes               # verification code ops
```

### HTTP Method Semantics
- `POST` — creates a resource, returns `201 Created`
- `GET` — reads, returns `200 OK`
- `PUT` — full replacement, returns `200 OK`
- `PATCH` — partial update or state toggle, returns `200 OK`
- `DELETE` — not currently used in Mercury

### Response Wrapper Pattern
Mercury returns typed records directly in `ResponseEntity<T>` — no generic wrapper envelope:
```java
ResponseEntity<CreateCampaignResponse> createCampaign(...)
ResponseEntity<CampaignDetailResponse> getById(...)
ResponseEntity<List<CampaignDetailResponse>> getByApplication(...)
```

### Error Response
`ApiError` record:
```java
public record ApiError(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
)
```

### OpenAPI Annotation Placement
- `@Tag` — on `*Api` interface at class level
- `@Operation(summary, description, operationId)` — on each method of `*Api`
- `@ApiResponses(@ApiResponse(...))` — on each method of `*Api`
- NEVER on `*Controller` class or methods

### operationId Naming
camelCase, verb + noun:
- `createCampaign`, `getCampaignById`, `updateCampaign`, `toggleCampaign`
- `sendEmail`, `getChannelTypes`, `createVerificationCode`

### Bean Validation on Request Records
```java
public record CreateCampaignRequest(
    @NotNull(message = "channelTypeId is required") UUID channelTypeId,
    @NotNull(message = "templateId is required") UUID templateId,
    @NotBlank(message = "name is required") String name,
    @NotEmpty(message = "recipients must not be empty") List<@Valid RecipientTO> recipients
) {}
```
