package com.umdc.mercury.processor;

import com.umdc.mercury.api.v1.service.CampaignMetricsService;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.provider.TelegramMessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drives queued non-email channel messages to a terminal delivery status.
 *
 * <p>Mirrors what {@link MessageProcessor} does for email: the Kafka listener only persists the
 * message as {@link DeliveryStatusType#OPENED}, and this processor — driven by a scheduler —
 * performs the actual delivery attempt and reconciles the status.</p>
 *
 * <p>Only Telegram is wired so far; SMS, WhatsApp and Push stay queued until their providers
 * are integrated, which is why this deliberately dispatches per channel rather than looping
 * over every {@code OPENED} document.</p>
 */
@Component
public class MultiChannelMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MultiChannelMessageProcessor.class);

    private final TelegramChannelService telegramChannelService;
    private final TelegramMessageDispatcher telegramDispatcher;
    private final CampaignMetricsService campaignMetricsService;

    public MultiChannelMessageProcessor(TelegramChannelService telegramChannelService,
                                        TelegramMessageDispatcher telegramDispatcher,
                                        CampaignMetricsService campaignMetricsService) {
        this.telegramChannelService = telegramChannelService;
        this.telegramDispatcher = telegramDispatcher;
        this.campaignMetricsService = campaignMetricsService;
    }

    /**
     * Dispatches every queued Telegram message, updating its delivery status and the campaign
     * counters. Messages are left untouched when no bot token is configured, so they are picked
     * up once one is, instead of being failed.
     */
    public void dispatchTelegram() {
        if (!telegramDispatcher.isEnabled()) {
            logger.debug("Telegram dispatch skipped: no bot token configured.");
            return;
        }

        List<TelegramMessageDocument> queued = telegramChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED);
        if (queued.isEmpty()) {
            return;
        }
        logger.info("Dispatching {} queued Telegram message(s).", queued.size());

        queued.forEach(message -> {
            DeliveryStatusType outcome = telegramDispatcher.dispatch(message);
            message.setDeliveryStatus(outcome);
            telegramChannelService.updateStatus(message);
            campaignMetricsService.recordOutcome(message.getCampaignId(), outcome);
        });
    }
}
