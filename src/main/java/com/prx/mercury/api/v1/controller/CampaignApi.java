package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CreateCampaignRequest;
import com.prx.mercury.api.v1.to.CreateCampaignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.UUID;

/**
 * REST API contract for campaign management operations.
 *
 * <p>All endpoints are rooted at {@code /api/v1/campaigns}.</p>
 */
@Tag(name = "campaigns", description = "Campaign Management API")
public interface CampaignApi {

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
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields."),
            @ApiResponse(responseCode = "401", description = "Invalid or missing authentication token."),
            @ApiResponse(responseCode = "403", description = "Caller lacks permission to create campaigns."),
            @ApiResponse(responseCode = "422", description = "Channel type not found or disabled."),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error.")
    })
    ResponseEntity<CreateCampaignResponse> createCampaign(CreateCampaignRequest request);

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
            @ApiResponse(responseCode = "400", description = "Invalid UUID format supplied for id."),
            @ApiResponse(responseCode = "401", description = "Invalid or missing authentication token."),
            @ApiResponse(responseCode = "403", description = "Caller lacks permission to view this campaign."),
            @ApiResponse(responseCode = "404", description = "Campaign with the given id does not exist."),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error.")
    })
    ResponseEntity<CampaignDetailResponse> getById(UUID id);

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
    ResponseEntity<List<CampaignDetailResponse>> getByApplication(UUID applicationId, @RequestHeader("session-token") String sessionToken);
}
