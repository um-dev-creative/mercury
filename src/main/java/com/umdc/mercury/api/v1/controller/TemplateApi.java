package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * REST API contract for reusable base template management (the {@code TemplateEntity} in
 * {@code mercury.templates}) — create, update, deactivate, single lookup and
 * application-scoped search.
 *
 * <p>This is distinct from {@code TemplateDefinedEntity}, which represents an
 * application/user-specific usage of a template and is referenced by campaigns and message
 * records; that lifecycle is untouched by this API.</p>
 *
 * <p>All endpoints are rooted at {@code /api/v1/templates} and require a valid
 * {@code session-token} header. Every operation is additionally scoped to the caller's
 * authorized applications.</p>
 */
@Tag(name = "templates", description = "Template Management API")
public interface TemplateApi {

    String CODE_400 = "400";
    String CODE_401 = "401";
    String CODE_403 = "403";
    String CODE_404 = "404";
    String CODE_500 = "500";
    String MSG_UNAUTHORIZED = "Invalid or missing authentication token.";
    String MSG_FORBIDDEN = "Caller lacks permission to manage templates for this application.";
    String MSG_NOT_FOUND = "Template not found, or exists but is no longer active.";
    String MSG_INTERNAL_ERROR = "Unexpected internal error.";

    /**
     * Creates a new, always-active reusable template.
     *
     * @param request       the template creation request; must pass bean validation and
     *                      reference an existing application, template type and severity type.
     * @param sessionToken  the caller's session token, used to resolve and authorize the requester.
     * @return a ResponseEntity with the persisted template and HTTP status {@code 201 Created}.
     */
    @Operation(
            summary = "Create a new template",
            description = "Creates a new reusable base template, always persisted as active.",
            operationId = "createTemplate"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Template created successfully."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid request payload, or an unknown/inactive application, template type or severity type reference."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN),
            @ApiResponse(responseCode = CODE_500, description = MSG_INTERNAL_ERROR)
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TemplateDetailResponse> createTemplate(
            @RequestBody @Valid CreateTemplateRequest request,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    /**
     * Updates the mutable metadata of an existing, active template.
     *
     * @param id            the UUID of the template to update.
     * @param sessionToken  the caller's session token, used to resolve and authorize the requester.
     * @param request       the fields to update; fields left {@code null} are unchanged. The owning
     *                      application is immutable and cannot be changed through this operation.
     * @return a ResponseEntity with the updated template and HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Update template by ID",
            description = "Updates mutable metadata of an active template identified by UUID. Fields omitted from the request remain unchanged; the owning application is immutable.",
            operationId = "updateTemplate"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template updated successfully."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid request payload, or an unknown/inactive template type or severity type reference."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN),
            @ApiResponse(responseCode = CODE_404, description = MSG_NOT_FOUND),
            @ApiResponse(responseCode = CODE_500, description = MSG_INTERNAL_ERROR)
    })
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TemplateDetailResponse> updateTemplate(
            @PathVariable UUID id,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
            @RequestBody @Valid UpdateTemplateRequest request);

    /**
     * Soft-deletes a template, marking it inactive instead of removing the row so that
     * dependent {@code TemplateDefinedEntity} usage and campaign/message history stay intact.
     *
     * @param id            the UUID of the template to deactivate.
     * @param sessionToken  the caller's session token, used to resolve and authorize the requester.
     * @return a ResponseEntity with HTTP status {@code 204 No Content}.
     */
    @Operation(
            summary = "Deactivate template by ID",
            description = "Soft-deletes a template (sets active=false); the row and all dependent references are retained. Idempotent for an already-inactive template.",
            operationId = "deleteTemplate"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Template deactivated successfully (or was already inactive)."),
            @ApiResponse(responseCode = CODE_400, description = "Malformed id."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN),
            @ApiResponse(responseCode = CODE_404, description = "Template with the given id does not exist."),
            @ApiResponse(responseCode = CODE_500, description = MSG_INTERNAL_ERROR)
    })
    @DeleteMapping(value = "/{id}")
    ResponseEntity<Void> deleteTemplate(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    /**
     * Retrieves a single active template by its unique identifier.
     *
     * @param id            the UUID of the template to retrieve.
     * @param sessionToken  the caller's session token, used to resolve and authorize the requester.
     * @return a ResponseEntity with the template detail and HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Get template by ID",
            description = "Retrieves the full details of an active template identified by its UUID. Unknown and inactive ids are both reported as not found.",
            operationId = "getTemplateById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Template found and returned."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid UUID format supplied for id."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN),
            @ApiResponse(responseCode = CODE_404, description = MSG_NOT_FOUND),
            @ApiResponse(responseCode = CODE_500, description = MSG_INTERNAL_ERROR)
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TemplateDetailResponse> getTemplateById(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    /**
     * Searches templates scoped to a mandatory application, with optional text/classification
     * filters and bounded, sorted pagination.
     *
     * @param applicationId  mandatory application scope; results never leak another application's templates.
     * @param q              optional case-insensitive substring filter over description/location.
     * @param templateTypeId optional exact template type filter.
     * @param severityTypeId optional exact severity type filter.
     * @param active         optional exact active-state filter; omitted defaults to active-only.
     * @param page           zero-based page index; defaults to {@code 0}.
     * @param size           page size; defaults to {@code 20}, bounded to a safe maximum.
     * @param sort           {@code field,direction} sort spec (e.g. {@code createdAt,desc}); a stable
     *                       {@code id} tie-breaker is always appended.
     * @param sessionToken   the caller's session token, used to resolve and authorize the requester.
     * @return a ResponseEntity with the paginated search envelope and HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Search templates",
            description = "Searches templates within an application the caller may access. Always scoped by applicationId; defaults to active-only results; bounded and stably paginated/sorted.",
            operationId = "searchTemplates"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated search results returned."),
            @ApiResponse(responseCode = CODE_400, description = "Missing applicationId, invalid filter/UUID value, or out-of-bounds paging/sort parameter."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN),
            @ApiResponse(responseCode = CODE_500, description = MSG_INTERNAL_ERROR)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TemplateSearchResponse> searchTemplates(
            @RequestParam UUID applicationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID templateTypeId,
            @RequestParam(required = false) UUID severityTypeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);
}
