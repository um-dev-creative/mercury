package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.CampaignService;
import com.umdc.mercury.api.v1.to.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.umdc.commons.util.JwtUtil.getUidFromToken;

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
public class CampaignController implements CampaignApi {

    private final CampaignService campaignService;

    /**
     * Constructs a {@code CampaignController} with the required service dependency.
     *
     * @param campaignService the campaign service; must not be {@code null}.
     */
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Override
    public ResponseEntity<CreateCampaignResponse> createCampaign(CreateCampaignRequest request) {
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

    @Override
    public ResponseEntity<CampaignDetailResponse> getById(UUID id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @Override
    public ResponseEntity<List<CampaignDetailResponse>> getByApplication(UUID applicationId, String sessionToken) {
        UUID userId = getUidFromToken(sessionToken);
        List<CampaignDetailResponse> responses = campaignService.getByUserIdAndApplicationId(userId, applicationId);
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<CampaignDetailResponse> updateCampaign(UUID id, String sessionToken, UpdateCampaignRequest request) {
        UUID userId = getUidFromToken(sessionToken);
        CampaignDetailResponse updated = campaignService.updateCampaign(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<Void> toggleCampaign(UUID id, boolean enabled, String sessionToken) {
        UUID userId = getUidFromToken(sessionToken);
        campaignService.toggleCampaign(id, enabled, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCampaign(UUID id, String sessionToken) {
        UUID userId = getUidFromToken(sessionToken);
        campaignService.deleteCampaign(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CampaignProgressTO> getProgress(UUID id, String sessionToken) {
        return ResponseEntity.ok(campaignService.getProgress(id));
    }
}
