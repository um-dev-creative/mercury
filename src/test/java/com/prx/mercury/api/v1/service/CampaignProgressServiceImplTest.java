package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.ChannelTypeTO;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.CampaignMetricsEntity;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import com.prx.mercury.jpa.sql.repository.CampaignMetricsRepository;
import com.prx.mercury.jpa.sql.repository.CampaignRepository;
import com.prx.mercury.mapper.CampaignMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignProgressServiceImpl unit tests")
class CampaignProgressServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignMetricsRepository metricsRepository;

    @Mock
    private CampaignMapper campaignMapper;

    @InjectMocks
    private CampaignProgressServiceImpl campaignProgressService;

    private CampaignEntity buildCampaign(UUID campaignId, int totalRecipients, String status) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId(campaignId);
        campaign.setName("Newsletters");
        campaign.setChannelType(new ChannelTypeEntity());
        campaign.setTotalRecipients(totalRecipients);
        campaign.setStatus(status);
        campaign.setCreatedAt(LocalDateTime.now().minusDays(1));
        return campaign;
    }

    private CampaignMetricsEntity buildMetrics(CampaignEntity campaign,
                                               int totalSent,
                                               int delivered,
                                               int failed,
                                               int opened,
                                               int clicked) {
        CampaignMetricsEntity metrics = new CampaignMetricsEntity();
        metrics.setCampaign(campaign);
        metrics.setTotalSent(totalSent);
        metrics.setDelivered(delivered);
        metrics.setFailed(failed);
        metrics.setOpened(opened);
        metrics.setClicked(clicked);
        metrics.setLastUpdated(LocalDateTime.now());
        return metrics;
    }

    @Nested
    @DisplayName("getProgress behavior")
    class GetProgress {

        @Test
        @DisplayName("returns mapped values when metrics exist")
        void returnsMetrics() {
            // Given
            UUID campaignId = UUID.randomUUID();
            CampaignEntity campaign = buildCampaign(campaignId, 50, "RUNNING");
            CampaignMetricsEntity metrics = buildMetrics(campaign, 20, 15, 3, 7, 5);

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(metricsRepository.findByCampaign_Id(campaignId)).thenReturn(Optional.of(metrics));
            when(campaignMapper.toChannelTypeTO(any(ChannelTypeEntity.class))).thenReturn(
                    new ChannelTypeTO(UUID.randomUUID(), "email", "Email", "Transactional", null, true, null, LocalDateTime.now(), LocalDateTime.now(), true)
            );

            // When
            CampaignProgressTO result = campaignProgressService.getProgress(campaignId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.campaignId()).isEqualTo(campaignId);
            assertThat(result.totalSent()).isEqualTo(20);
            assertThat(result.failed()).isEqualTo(3);
        }

        @Test
        @DisplayName("uses empty metrics when none exist")
        void missingMetricsUsesDefaults() {
            // Given
            UUID campaignId = UUID.randomUUID();
            CampaignEntity campaign = buildCampaign(campaignId, 10, "DRAFT");

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
            when(metricsRepository.findByCampaign_Id(campaignId)).thenReturn(Optional.empty());
            when(campaignMapper.toChannelTypeTO(any(ChannelTypeEntity.class))).thenReturn(null);

            // When
            CampaignProgressTO result = campaignProgressService.getProgress(campaignId);

            // Then - defaults expected
            assertThat(result).isNotNull();
            assertThat(result.totalRecipients()).isEqualTo(10);
            assertThat(result.totalSent()).isZero();
            assertThat(result.pending()).isEqualTo(10);
        }

        @Test
        @DisplayName("throws when campaign is missing")
        void missingCampaignThrows() {
            // Given
            UUID campaignId = UUID.randomUUID();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

            // Then
            assertThrows(IllegalArgumentException.class, () -> campaignProgressService.getProgress(campaignId));
        }
    }
}
