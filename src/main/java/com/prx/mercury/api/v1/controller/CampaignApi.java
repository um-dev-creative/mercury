package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.to.CreateCampaignRequest;
import com.prx.mercury.api.v1.to.CreateCampaignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
     *         with the newly created campaign details and HTTP status {@code 202 Accepted}.
     */
    @Operation(
            summary = "Create a new campaign",
            description = "Creates a new messaging campaign and publishes messages to recipients via the specified channel."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Campaign accepted and processing started."),
            @ApiResponse(responseCode = "400", description = "Invalid request payload."),
            @ApiResponse(responseCode = "422", description = "Channel type not found or disabled.")
    })
    @PostMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<CreateCampaignResponse> createCampaign(@RequestBody @Valid CreateCampaignRequest request);
}
