# API Reviewer SKILL

## Project-Specific Patterns

### Api Interface Pattern (CORRECT)
```java
// com.umdc.mercury.api.v1.controller — interface with ALL OpenAPI annotations
@Tag(name = "Campaign", description = "Campaign management operations")
@RequestMapping("/api/v1/campaigns")
public interface CampaignApi {

    @Operation(summary = "Get campaign by ID", description = "Returns a single campaign")
    @ApiResponse(responseCode = "200", description = "Campaign found",
        content = @Content(schema = @Schema(implementation = CampaignResponse.class)))
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @GetMapping("/{id}")
    CampaignResponse getCampaign(
        @Parameter(description = "Campaign ID", required = true) @PathVariable Long id
    );

    @Operation(summary = "Create campaign")
    @ApiResponse(responseCode = "201", description = "Campaign created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CompletableFuture<CampaignResponse> createCampaign(
        @RequestBody @Valid CampaignRequest request
    );
}
```

### Controller Pattern (CORRECT — zero OpenAPI annotations)
```java
// com.umdc.mercury.api.v1.controller — implements Api, NO @Tag/@Operation/@ApiResponse
@RestController
public class CampaignController implements CampaignApi {

    private final CampaignServiceImpl campaignService;

    public CampaignController(CampaignServiceImpl campaignService) {
        this.campaignService = campaignService;
    }

    @Override
    public CampaignResponse getCampaign(Long id) {
        return campaignService.getCampaign(id);
    }

    @Override
    public CompletableFuture<CampaignResponse> createCampaign(CampaignRequest request) {
        return campaignService.createCampaign(request);
    }
}
```

### DTO Record Pattern
```java
// com.umdc.mercury.api.v1.to
public record CampaignRequest(
    @NotBlank(message = "Campaign name is required")
    String name,

    @NotNull(message = "Channel type ID is required")
    Long channelTypeId,

    @NotNull(message = "Frequency type is required")
    Long frequencyTypeId
) {}

public record CampaignResponse(
    Long id,
    String name,
    String status,
    Long channelTypeId,
    OffsetDateTime createdAt
) {}
```

### Existing Endpoint Inventory
| Interface | Controller | Path Prefix | DTOs |
|---|---|---|---|
| `CampaignApi` | `CampaignController` | `/api/v1/campaigns` | `CampaignRequest/Response` |
| `ChannelTypeApi` | `ChannelTypeController` | `/api/v1/channel-types` | `ChannelTypeResponse` |
| `MailApi` | `MailController` | `/api/v1/mail` | mail DTOs |
| `VerificationCodeApi` | `VerificationCodeController` | `/api/v1/verification` | `VerificationCodeRequest/Response` |

### HTTP Status Code Rules
| Scenario | Status | Exception |
|---|---|---|
| Successful read | 200 | — |
| Successful create | 201 | — |
| Successful update | 200 | — |
| Successful delete | 204 | — |
| Resource not found | 404 | `CampaignNotFoundException` |
| Auth denied | 403 | `ForbiddenException` |
| Channel disabled | 422 | `IllegalStateException` |
| Bad input | 400 | `IllegalArgumentException` / `MethodArgumentNotValidException` |

## Naming Conventions
- Interface: `*Api` (e.g., `CampaignApi`) in `com.umdc.mercury.api.v1.controller`
- Controller: `*Controller` (e.g., `CampaignController`) in `com.umdc.mercury.api.v1.controller`
- Request DTO: `*Request` record (e.g., `CampaignRequest`) in `com.umdc.mercury.api.v1.to`
- Response DTO: `*Response` record (e.g., `CampaignResponse`) in `com.umdc.mercury.api.v1.to`
- Transfer object: `*TO` record in `com.umdc.mercury.api.v1.to`
- Path: always prefixed `/api/v1/`

## Error Handling
When reviewing an API contract:
- Missing `@ApiResponse` for documented exception → **NEEDS_REVISION**
- Wrong HTTP status code for known exception → **NEEDS_REVISION**
- OpenAPI annotation on `*Controller` → **NEEDS_REVISION** (must move to `*Api`)
- `@RequestBody` on GET endpoint → **NEEDS_REVISION**
- DTO not a Java record → **NEEDS_REVISION**
- Missing `@Valid` on `@RequestBody` → **NEEDS_REVISION**

## Key Files
- `src/main/java/com/umdc/mercury/api/v1/controller/` — Api interfaces + Controllers
- `src/main/java/com/umdc/mercury/api/v1/to/` — DTO records
- `src/main/java/com/umdc/mercury/api/v1/exception/` — exception classes
- `src/main/java/com/umdc/mercury/config/` — `GlobalExceptionHandler`
- `.github/skills/api-design.skill.md` — shared API design patterns

## Constraints
- NEVER place `@Tag`, `@Operation`, or `@ApiResponse` on `*Controller` — `*Api` only
- NEVER use classes instead of records for DTOs
- NEVER omit `@ApiResponse` for 403, 404, 400 where applicable
- NEVER use path prefixes other than `/api/v1/`
- NEVER approve a contract without all error response codes declared
- NEVER approve a POST/PUT without `@Valid @RequestBody`

## Checklist
- [ ] `*Api` interface exists and carries all `@Tag`, `@Operation`, `@ApiResponse` annotations
- [ ] `*Controller` implements `*Api` and has zero OpenAPI annotations
- [ ] All DTOs are Java records in `com.umdc.mercury.api.v1.to`
- [ ] All applicable HTTP codes declared: 200/201/204, 400, 403, 404, 422
- [ ] `@Valid` present on `@RequestBody` parameters
- [ ] Path prefix is `/api/v1/`
- [ ] Write operations return `CompletableFuture<*Response>` in interface
- [ ] DTO field validations present (`@NotBlank`, `@NotNull`, etc.)
- [ ] Verdict: APPROVED or NEEDS_REVISION with itemized list
