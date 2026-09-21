package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.CampaignMetricsService;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.sql.repository.CampaignMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignMetricsService")
class CampaignMetricsServiceTest {

    @Mock
    private CampaignMetricsRepository metricsRepository;

    @InjectMocks
    private CampaignMetricsService service;

    @Test
    @DisplayName("increments the sent counter for a delivered message")
    void recordsSent() {
        UUID campaignId = UUID.randomUUID();
        when(metricsRepository.incrementTotalSent(eq(campaignId), any(LocalDateTime.class))).thenReturn(1);

        service.recordOutcome(campaignId, DeliveryStatusType.SENT);

        verify(metricsRepository).incrementTotalSent(eq(campaignId), any(LocalDateTime.class));
        verify(metricsRepository, never()).incrementFailed(any(), any());
    }

    @Test
    @DisplayName("increments the failed counter for any non-SENT outcome")
    void recordsFailed() {
        UUID campaignId = UUID.randomUUID();
        when(metricsRepository.incrementFailed(eq(campaignId), any(LocalDateTime.class))).thenReturn(1);

        service.recordOutcome(campaignId, DeliveryStatusType.FAILED);

        verify(metricsRepository).incrementFailed(eq(campaignId), any(LocalDateTime.class));
        verify(metricsRepository, never()).incrementTotalSent(any(), any());
    }

    @Test
    @DisplayName("skips messages that do not belong to a campaign")
    void skipsNullCampaign() {
        service.recordOutcome(null, DeliveryStatusType.SENT);

        verifyNoInteractions(metricsRepository);
    }

    @Test
    @DisplayName("swallows persistence failures so metrics never break the delivery path")
    void swallowsRepositoryFailure() {
        UUID campaignId = UUID.randomUUID();
        when(metricsRepository.incrementTotalSent(eq(campaignId), any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> service.recordOutcome(campaignId, DeliveryStatusType.SENT))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("tolerates a campaign with no metrics row")
    void toleratesMissingMetricsRow() {
        UUID campaignId = UUID.randomUUID();
        when(metricsRepository.incrementTotalSent(eq(campaignId), any(LocalDateTime.class))).thenReturn(0);

        assertThatCode(() -> service.recordOutcome(campaignId, DeliveryStatusType.SENT))
                .doesNotThrowAnyException();
    }
}
