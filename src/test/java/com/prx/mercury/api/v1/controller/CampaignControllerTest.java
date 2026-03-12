package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.exception.CampaignNotFoundException;
import com.prx.mercury.api.v1.service.CampaignService;
import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CreateCampaignRequest;
import com.prx.mercury.api.v1.to.CreateCampaignResponse;
import com.prx.mercury.api.v1.to.RecipientTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignController unit tests")
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController campaignController;

    private CreateCampaignRequest validRequest;
    private UUID templateId;
    private UUID userId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        userId = UUID.randomUUID();
        applicationId = UUID.randomUUID();
        validRequest = new CreateCampaignRequest(
                "Spring Promotion 2026",
                "email",
                templateId,
                userId,
                List.of(new RecipientTO("alice@example.com", "Alice", Map.of())),
                Map.of("promoCode", "SPRING20"),
                null,
                "DRAFT",
                applicationId
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CampaignProgressTO buildProgress(UUID campaignId, String name, String status) {
        LocalDateTime now = LocalDateTime.now();
        return new CampaignProgressTO(campaignId, name, null, 1, 0, 0, 0, 0, 0, 0,
                0.0, 0.0, now, now, status);
    }

    // ── createCampaign ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCampaign – success scenarios")
    class CreateCampaignSuccess {

        @Test
        @DisplayName("returns 201 Created with populated response body")
        void createCampaign_returns201WithBody() {
            UUID campaignId = UUID.randomUUID();
            CampaignProgressTO progress = buildProgress(campaignId, "Spring Promotion 2026", "DRAFT");
            when(campaignService.createCampaign(any(CampaignTO.class)))
                    .thenReturn(CompletableFuture.completedFuture(progress));

            ResponseEntity<CreateCampaignResponse> response = campaignController.createCampaign(validRequest);

            assertAll("response",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().campaignId()).isEqualTo(campaignId),
                    () -> assertThat(response.getBody().name()).isEqualTo("Spring Promotion 2026"),
                    () -> assertThat(response.getBody().status()).isEqualTo("DRAFT"),
                    () -> assertThat(response.getBody().totalRecipients()).isEqualTo(1),
                    () -> assertThat(response.getBody().createdAt()).isNotNull()
            );
        }

        @Test
        @DisplayName("maps all request fields to CampaignTO before delegating to service")
        void createCampaign_mapsRequestToCampaignTO() {
            UUID campaignId = UUID.randomUUID();
            CampaignProgressTO progress = buildProgress(campaignId, validRequest.name(), "DRAFT");
            ArgumentCaptor<CampaignTO> captor = ArgumentCaptor.forClass(CampaignTO.class);
            when(campaignService.createCampaign(captor.capture()))
                    .thenReturn(CompletableFuture.completedFuture(progress));

            campaignController.createCampaign(validRequest);

            CampaignTO captured = captor.getValue();
            assertAll("CampaignTO mapping",
                    () -> assertThat(captured.name()).isEqualTo(validRequest.name()),
                    () -> assertThat(captured.channelTypeCode()).isEqualTo(validRequest.channelTypeCode()),
                    () -> assertThat(captured.templateId()).isEqualTo(templateId),
                    () -> assertThat(captured.userId()).isEqualTo(userId),
                    () -> assertThat(captured.applicationId()).isEqualTo(applicationId),
                    () -> assertThat(captured.recipients()).hasSize(1),
                    () -> assertThat(captured.templateParams()).containsEntry("promoCode", "SPRING20")
            );
        }

        @Test
        @DisplayName("scheduledAt from request is propagated to response when provided")
        void createCampaign_scheduledAtPropagated() {
            LocalDateTime scheduledAt = LocalDateTime.now().plusDays(5);
            CreateCampaignRequest scheduledRequest = new CreateCampaignRequest(
                    "Scheduled Campaign", "sms", templateId, userId,
                    List.of(new RecipientTO("+15551234567", "Bob", Map.of())),
                    Map.of(), scheduledAt, "DRAFT", applicationId);

            UUID campaignId = UUID.randomUUID();
            CampaignProgressTO progress = buildProgress(campaignId, "Scheduled Campaign", "DRAFT");
            when(campaignService.createCampaign(any(CampaignTO.class)))
                    .thenReturn(CompletableFuture.completedFuture(progress));

            ResponseEntity<CreateCampaignResponse> response = campaignController.createCampaign(scheduledRequest);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().scheduledAt()).isEqualTo(scheduledAt);
        }

        @Test
        @DisplayName("delegates to campaignService exactly once")
        void createCampaign_delegatesToServiceOnce() {
            UUID campaignId = UUID.randomUUID();
            CampaignProgressTO progress = buildProgress(campaignId, validRequest.name(), "DRAFT");
            when(campaignService.createCampaign(any(CampaignTO.class)))
                    .thenReturn(CompletableFuture.completedFuture(progress));

            campaignController.createCampaign(validRequest);

            verify(campaignService).createCampaign(any(CampaignTO.class));
        }
    }

    @Nested
    @DisplayName("createCampaign – failure scenarios")
    class CreateCampaignFailure {

        @Test
        @DisplayName("propagates CompletionException when service reports channel not found")
        void createCampaign_channelNotFound_throwsCompletionException() {
            when(campaignService.createCampaign(any(CampaignTO.class)))
                    .thenReturn(CompletableFuture.failedFuture(
                            new IllegalArgumentException("Channel type not found: email")));

            assertThrows(CompletionException.class,
                    () -> campaignController.createCampaign(validRequest));
        }

        @Test
        @DisplayName("propagates CompletionException when service reports disabled channel")
        void createCampaign_disabledChannel_throwsCompletionException() {
            when(campaignService.createCampaign(any(CampaignTO.class)))
                    .thenReturn(CompletableFuture.failedFuture(
                            new IllegalStateException("Channel type is disabled: Email")));

            assertThrows(CompletionException.class,
                    () -> campaignController.createCampaign(validRequest));
        }

        @Test
        @DisplayName("propagates CompletionException when service reports template not found")
        void createCampaign_templateNotFound_throwsCompletionException() {
            when(campaignService.createCampaign(any(CampaignTO.class)))
                    .thenReturn(CompletableFuture.failedFuture(
                            new IllegalArgumentException("Template not found: " + templateId)));

            assertThrows(CompletionException.class,
                    () -> campaignController.createCampaign(validRequest));
        }

        @Test
        @DisplayName("throws NullPointerException when request is null")
        void createCampaign_nullRequest_throwsNPE() {
            assertThrows(NullPointerException.class,
                    () -> campaignController.createCampaign(null));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById – success scenarios")
    class GetByIdSuccess {

        @Test
        @DisplayName("returns 200 OK with campaign detail body")
        void getById_returns200WithBody() {
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            CampaignDetailResponse detail = new CampaignDetailResponse(
                    id, "Summer Promo 2026", "email", UUID.randomUUID(),
                    "DRAFT", 50, null, now, now, Map.of("ownerId", "abc"));
            when(campaignService.getById(id)).thenReturn(detail);

            ResponseEntity<CampaignDetailResponse> response = campaignController.getById(id);

            assertAll("getById response",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().id()).isEqualTo(id),
                    () -> assertThat(response.getBody().name()).isEqualTo("Summer Promo 2026"),
                    () -> assertThat(response.getBody().channelType()).isEqualTo("email"),
                    () -> assertThat(response.getBody().status()).isEqualTo("DRAFT"),
                    () -> assertThat(response.getBody().totalRecipients()).isEqualTo(50),
                    () -> assertThat(response.getBody().createdAt()).isEqualTo(now),
                    () -> assertThat(response.getBody().updatedAt()).isEqualTo(now)
            );
        }

        @Test
        @DisplayName("delegates to campaignService.getById exactly once")
        void getById_delegatesToServiceOnce() {
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            when(campaignService.getById(id)).thenReturn(
                    new CampaignDetailResponse(id, "N", "sms", null, "DRAFT", 0, null, now, now, null));

            campaignController.getById(id);

            verify(campaignService).getById(id);
        }
    }

    @Nested
    @DisplayName("getById – failure scenarios")
    class GetByIdFailure {

        @Test
        @DisplayName("propagates CampaignNotFoundException when campaign does not exist")
        void getById_notFound_throwsCampaignNotFoundException() {
            UUID id = UUID.randomUUID();
            when(campaignService.getById(id))
                    .thenThrow(new CampaignNotFoundException("Campaign not found: " + id));

            assertThrows(CampaignNotFoundException.class,
                    () -> campaignController.getById(id));
        }
    }
}
