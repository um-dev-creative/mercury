package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.CampaignService;
import com.umdc.mercury.api.v1.to.*;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller that handles campaign lifecycle operations.
 *
 * <p>Implements the {@link CampaignApi} contract and delegates business logic
 * to {@link CampaignService}. Incoming {@link CreateCampaignRequest} objects
 * are adapted to the service-level {@link CampaignTO} transfer object before
 * the call is made. The response is built from the {@link CampaignProgressTO}
 * returned by the service.</p>
 *
 * <p>{@link CampaignApi} carries a class-level {@code @SkipSessionValidation} — its
 * {@code /api/v1/campaigns/**} routes are M2M-only and authenticated instead by
 * {@code ManagedClientSecurityConfig}'s backbone opaque-token chain, so Mercury's own
 * {@code SessionJwtInterceptor} never runs for this controller. Several operations here
 * still accept a {@code session-token} header purely to identify the end user the M2M
 * caller is acting on behalf of (ownership checks, query scoping) — since that header was
 * never verified by the interceptor, this controller verifies it itself via
 * {@link SessionJwtServiceImpl#getVerifiedUid(String)} before trusting its {@code uid}
 * claim for any authorization decision. Do not reintroduce
 * {@code com.umdc.commons.util.JwtUtil.getUidFromToken} here — it decodes the JWT payload
 * without checking the signature at all, which let any M2M caller impersonate any user by
 * self-crafting the header.</p>
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController implements CampaignApi {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or missing session token.";

    private final CampaignService campaignService;
    private final SessionJwtServiceImpl sessionJwtService;

    /**
     * Constructs a {@code CampaignController} with the required dependencies.
     *
     * @param campaignService   the campaign service; must not be {@code null}.
     * @param sessionJwtService used to verify the {@code session-token} header's signature
     *                          before trusting its {@code uid} claim; must not be {@code null}.
     */
    public CampaignController(CampaignService campaignService, SessionJwtServiceImpl sessionJwtService) {
        this.campaignService = campaignService;
        this.sessionJwtService = sessionJwtService;
    }

    /**
     * Verifies {@code sessionToken}'s signature and resolves its {@code uid} claim.
     *
     * @throws InvalidSessionTokenException if the token is missing, expired, tampered,
     *                                       or otherwise fails verification.
     */
    private UUID resolveVerifiedUserId(String sessionToken) {
        return sessionJwtService.getVerifiedUid(sessionToken)
                .orElseThrow(() -> new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE));
    }

    @Override
    public ResponseEntity<CreateCampaignResponse> createCampaign(CreateCampaignRequest request, String sessionToken) {
        UUID verifiedUserId = resolveVerifiedUserId(sessionToken);
        if (!verifiedUserId.equals(request.userId())) {
            throw new ForbiddenException("userId does not match the session-token user");
        }
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
    public ResponseEntity<CampaignDetailResponse> getById(UUID id, String sessionToken) {
        return ResponseEntity.ok(campaignService.getById(id, resolveVerifiedUserId(sessionToken)));
    }

    @Override
    public ResponseEntity<List<CampaignDetailResponse>> getByApplication(UUID applicationId, String sessionToken) {
        UUID userId = resolveVerifiedUserId(sessionToken);
        List<CampaignDetailResponse> responses = campaignService.getByUserIdAndApplicationId(userId, applicationId);
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<CampaignDetailResponse> updateCampaign(UUID id, String sessionToken, UpdateCampaignRequest request) {
        UUID userId = resolveVerifiedUserId(sessionToken);
        CampaignDetailResponse updated = campaignService.updateCampaign(id, request, userId);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<Void> toggleCampaign(UUID id, boolean enabled, String sessionToken) {
        UUID userId = resolveVerifiedUserId(sessionToken);
        campaignService.toggleCampaign(id, enabled, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCampaign(UUID id, String sessionToken) {
        UUID userId = resolveVerifiedUserId(sessionToken);
        campaignService.deleteCampaign(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CampaignProgressTO> getProgress(UUID id, String sessionToken) {
        return ResponseEntity.ok(campaignService.getProgress(id, resolveVerifiedUserId(sessionToken)));
    }
}
