package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.to.CampaignDetailResponse;
import com.umdc.mercury.api.v1.to.CampaignProgressTO;
import com.umdc.mercury.api.v1.to.CreateCampaignRequest;
import com.umdc.mercury.api.v1.to.CreateCampaignResponse;
import com.umdc.mercury.api.v1.to.UpdateCampaignRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * REST API contract for campaign management operations.
 *
 * <p>All endpoints are rooted at {@code /api/v1/campaigns}.</p>
 */
@Tag(name = "campaigns", description = "Campaign Management API")
public interface CampaignApi {

    String CODE_400 = "400";
    String CODE_401 = "401";
    String CODE_403 = "403";
    String CODE_404 = "404";
    String MSG_UNAUTHORIZED = "Invalid or missing authentication token.";
    String MSG_NOT_FOUND_OR_DELETED = "Campaign not found or already soft-deleted.";

    /**
     * Creates a new messaging campaign and starts its execution asynchronously.
     *
     * <p>The method validates the request, persists the campaign, publishes
     * per-recipient messages to the appropriate Kafka topic and returns the
     * initial campaign state. Progress can be tracked via the
     * {@code GET /api/v1/campaigns/{id}/progress} endpoint.</p>
     *
     * @param request the campaign creation request containing the channel,
     *                template, recipients and optional scheduling information.
     *                Must not be {@code null} and must pass bean validation.
     * @return a ResponseEntity containing a CreateCampaignResponse
     *         with the newly created campaign details and HTTP status {@code 201 Created}.
     */
    @Operation(
            summary = "Create a new campaign",
            description = "Creates a new messaging campaign and publishes messages to recipients via the specified channel.",
            operationId = "createCampaign"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Campaign created successfully."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid request payload or missing required fields."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = "Caller lacks permission to create campaigns."),
            @ApiResponse(responseCode = "422", description = "Channel type not found or disabled."),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CreateCampaignResponse> createCampaign(@RequestBody @Valid CreateCampaignRequest request);

    /**
     * Retrieves a campaign by its unique identifier.
     *
     * <p>Returns the full campaign detail including channel type, template reference,
     * audit timestamps, status and optional metadata.</p>
     *
     * @param id the UUID of the campaign to retrieve; must be a valid UUID.
     * @return a ResponseEntity containing a CampaignDetailResponse
     *         with HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Get campaign by ID",
            description = "Retrieves the full details of a campaign identified by its UUID.",
            operationId = "getCampaignById"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign found and returned."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid UUID format supplied for id."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = "Caller lacks permission to view this campaign."),
            @ApiResponse(responseCode = CODE_404, description = "Campaign with the given id does not exist."),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error.")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CampaignDetailResponse> getById(@PathVariable UUID id);

    /**
     * Retrieves campaigns for the authenticated user filtered by application id.
     *
     * <p>The user id is extracted from the session token provided in the request
     * header identified by the session token header key.</p>
     *
     * @param applicationId the application UUID used to filter campaigns
     * @param sessionToken the session token header containing the user id
     * @return a list of {@link CampaignDetailResponse} for the user and application
     */
    @Operation(
            summary = "Get campaigns by application for current user",
            description = "Retrieves campaigns filtered by application id for the authenticated user. User id is obtained from the session token header."
            ,operationId = "getCampaignsByApplication"
    )
    @GetMapping(value = "/application/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<CampaignDetailResponse>> getByApplication(@PathVariable UUID applicationId, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    /**
     * Updates mutable fields of an existing campaign.
     *
     * @param id the UUID of the campaign to update.
     * @param sessionToken the session token header containing the requesting user id.
     * @param request the fields to update; unset fields are left unchanged.
     * @return a ResponseEntity containing the updated CampaignDetailResponse
     *         with HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Update campaign by ID",
            description = "Updates mutable fields of a campaign identified by UUID. Fields omitted from the request remain unchanged.",
            operationId = "updateCampaign"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign updated successfully."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid request payload."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = "Caller lacks permission to update this campaign."),
            @ApiResponse(responseCode = CODE_404, description = MSG_NOT_FOUND_OR_DELETED),
            @ApiResponse(responseCode = "422", description = "Business rule violation.")
    })
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CampaignDetailResponse> updateCampaign(
            @PathVariable UUID id,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
            @RequestBody @Valid UpdateCampaignRequest request);

    /**
     * Enables or disables (pauses/resumes) a campaign.
     *
     * @param id the UUID of the campaign to toggle.
     * @param enabled desired enabled state.
     * @param sessionToken the session token header containing the requesting user id.
     * @return a ResponseEntity with HTTP status {@code 204 No Content}.
     */
    @Operation(
            summary = "Enable or disable a campaign",
            description = "Enables or disables (pauses/resumes) a campaign by id.",
            operationId = "toggleCampaign"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Campaign toggled successfully."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid parameters."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = "Caller lacks permission."),
            @ApiResponse(responseCode = CODE_404, description = MSG_NOT_FOUND_OR_DELETED),
            @ApiResponse(responseCode = "422", description = "Business rule violation, e.g. enabling without a template.")
    })
    @PatchMapping(value = "/{id}/toggle")
    ResponseEntity<Void> toggleCampaign(
            @PathVariable UUID id,
            @RequestParam(name = "enabled") boolean enabled,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    /**
     * Soft-deletes a campaign, marking it as deleted instead of removing the row.
     *
     * @param id the UUID of the campaign to delete.
     * @param sessionToken the session token header containing the requesting user id.
     * @return a ResponseEntity with HTTP status {@code 204 No Content}.
     */
    @Operation(
            summary = "Soft-delete campaign by ID",
            description = "Soft deletes a campaign (marks it as deleted; data is retained for audits).",
            operationId = "deleteCampaign"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Campaign soft-deleted successfully."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_403, description = "Caller lacks permission."),
            @ApiResponse(responseCode = CODE_404, description = MSG_NOT_FOUND_OR_DELETED)
    })
    @DeleteMapping(value = "/{id}")
    ResponseEntity<Void> deleteCampaign(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    /**
     * Retrieves progress metrics for a campaign, including counts of sent, delivered,
     * failed and other channel-specific statistics.
     *
     * @param id the UUID of the campaign.
     * @param sessionToken the session token header containing the requesting user id.
     * @return a ResponseEntity containing the CampaignProgressTO with HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Get campaign progress",
            description = "Retrieves progress metrics for a campaign (sent, delivered, failed, pending, etc.), computed from stored metrics.",
            operationId = "getCampaignProgress"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progress projection returned."),
            @ApiResponse(responseCode = CODE_400, description = "Invalid UUID format supplied for id."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED),
            @ApiResponse(responseCode = CODE_404, description = "Campaign not found.")
    })
    @GetMapping(value = "/{id}/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CampaignProgressTO> getProgress(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);
}
