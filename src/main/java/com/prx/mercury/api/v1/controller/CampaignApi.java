package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CreateCampaignRequest;
import com.prx.mercury.api.v1.to.CreateCampaignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
     * @return a {@link ResponseEntity} containing a {@link CreateCampaignResponse}
     *         with the newly created campaign details and HTTP status {@code 201 Created}.
     */
    @Operation(
            summary = "Create a new campaign",
            description = "Creates a new messaging campaign and publishes messages to recipients via the specified channel."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Campaign created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields."),
            @ApiResponse(responseCode = "401", description = "Invalid or missing authentication token."),
            @ApiResponse(responseCode = "403", description = "Caller lacks permission to create campaigns."),
            @ApiResponse(responseCode = "422", description = "Channel type not found or disabled."),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error.")
    })
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<CreateCampaignResponse> createCampaign(@RequestBody @Valid CreateCampaignRequest request);

    /**
     * Retrieves a campaign by its unique identifier.
     *
     * <p>Returns the full campaign detail including channel type, template reference,
     * audit timestamps, status and optional metadata.</p>
     *
     * @param id the UUID of the campaign to retrieve; must be a valid UUID.
     * @return a {@link ResponseEntity} containing a {@link CampaignDetailResponse}
     *         with HTTP status {@code 200 OK}.
     */
    @Operation(
            summary = "Get campaign by ID",
            description = "Retrieves the full details of a campaign identified by its UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign found and returned."),
            @ApiResponse(responseCode = "400", description = "Invalid UUID format supplied for id."),
            @ApiResponse(responseCode = "401", description = "Invalid or missing authentication token."),
            @ApiResponse(responseCode = "403", description = "Caller lacks permission to view this campaign."),
            @ApiResponse(responseCode = "404", description = "Campaign with the given id does not exist."),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error.")
    })
    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<CampaignDetailResponse> getById(@PathVariable UUID id);
}
