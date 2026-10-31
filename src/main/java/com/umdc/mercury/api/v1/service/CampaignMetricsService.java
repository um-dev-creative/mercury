package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.sql.repository.CampaignMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Advances the per-campaign delivery counters that back
 * {@code GET /api/v1/campaigns/{id}/progress}.
 *
 * <p>Until a channel could actually deliver, nothing ever moved these counters and every
 * campaign reported all recipients as pending. They are updated here, at the point a message
 * reaches a terminal delivery status.</p>
 *
 * <p>Metrics are observability, not payload: a failure to record them is logged and swallowed
 * so it never causes a Kafka redelivery of a message that was already handed to the provider.
 * MongoDB and PostgreSQL are separate datastores here, so there is no shared transaction to
 * roll back either way.</p>
 */
@Service
public class CampaignMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignMetricsService.class);

    private final CampaignMetricsRepository metricsRepository;

    public CampaignMetricsService(CampaignMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    /**
     * Records the outcome of a delivery attempt against its campaign.
     *
     * @param campaignId the campaign the message belongs to; {@code null} for messages sent
     *                   outside a campaign (e.g. verification codes), which are skipped
     * @param status     the terminal status reached by the message
     */
    public void recordOutcome(UUID campaignId, DeliveryStatusType status) {
        if (Objects.isNull(campaignId)) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now(ZoneId.of(ZoneOffset.UTC.getId()));
            int updated = DeliveryStatusType.SENT.equals(status)
                    ? metricsRepository.incrementTotalSent(campaignId, now)
                    : metricsRepository.incrementFailed(campaignId, now);
            if (updated == 0) {
                logger.warn("No campaign metrics row to update. campaignId={}, status={}", campaignId, status);
            }
        } catch (RuntimeException e) {
            logger.warn("Unable to record campaign metrics. campaignId={}, status={}", campaignId, status, e);
        }
    }
}
