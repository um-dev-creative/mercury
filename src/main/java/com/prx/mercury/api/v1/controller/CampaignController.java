package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.CampaignService;
import com.prx.mercury.api.v1.to.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.prx.commons.util.JwtUtil.getUidFromToken;
import static com.prx.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * REST controller that handles campaign lifecycle operations.
 *
 * <p>Implements the {@link CampaignApi} contract and delegates business logic
 * to {@link CampaignService}. Incoming {@link CreateCampaignRequest} objects
 * are adapted to the service-level {@link CampaignTO} transfer object before
 * the call is made. The response is built from the {@link CampaignProgressTO}
 * returned by the service.</p>
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    /**
     * Constructs a {@code CampaignController} with the required service dependency.
     *
     * @param campaignService the campaign service; must not be {@code null}.
     */
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converts the incoming {@link CreateCampaignRequest} to a {@link CampaignTO},
     * delegates to {@link CampaignService#createCampaign(CampaignTO)} and maps the
     * resulting {@link CampaignProgressTO} to a {@link CreateCampaignResponse}.</p>
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateCampaignResponse> createCampaign(@RequestBody @Valid CreateCampaignRequest request) {
        CampaignTO campaignTO = new CampaignTO(
                request.name(),
                request.channelTypeCode(),
                request.templateId(),
                request.userId(),
                request.recipients(),
                request.templateParams(),
                request.scheduledAt(),
                request.status(),
                request.applicationId()
        );

        CampaignProgressTO progress = campaignService.createCampaign(campaignTO).join();

        CreateCampaignResponse response = new CreateCampaignResponse(
                progress.campaignId(),
                progress.name(),
                progress.status(),
                progress.totalRecipients(),
                progress.createdAt(),
                request.scheduledAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link CampaignService#getById(UUID)} and wraps the result
     * in a {@code 200 OK} response.</p>
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CampaignDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CampaignDetailResponse> updateCampaign(
            @PathVariable UUID id,
            @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
            @RequestBody @Valid UpdateCampaignRequest request) {
        UUID userId = getUidFromToken(sessionToken);
        CampaignDetailResponse updated = campaignService.updateCampaign(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping(value = "/application/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CampaignDetailResponse>> getByApplication(@PathVariable UUID applicationId, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken) {
        // extract user id from token
        UUID userId = getUidFromToken(sessionToken);
        // delegate to service
        List<CampaignDetailResponse> responses = campaignService.getByUserIdAndApplicationId(userId, applicationId);
        return ResponseEntity.ok(responses);
    }
}

