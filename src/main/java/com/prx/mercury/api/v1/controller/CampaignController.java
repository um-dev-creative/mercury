package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.CampaignService;
import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CreateCampaignRequest;
import com.prx.mercury.api.v1.to.CreateCampaignResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    /**
     * {@inheritDoc}
     *
     * <p>Converts the incoming {@link CreateCampaignRequest} to a {@link CampaignTO},
     * delegates to {@link CampaignService#createCampaign(CampaignTO)} and maps the
     * resulting {@link CampaignProgressTO} to a {@link CreateCampaignResponse}.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link CampaignService#getById(UUID)} and wraps the result
     * in a {@code 200 OK} response.</p>
     */
    @Override
    public ResponseEntity<CampaignDetailResponse> getById(UUID id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }
}
