package com.umdc.mercury.processor;

import com.umdc.mercury.api.v1.service.CampaignMetricsService;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.monitoring.SendEventService;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import com.umdc.mercury.kafka.consumer.service.PushChannelService;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.provider.ExpoPushDispatcher;
import com.umdc.mercury.provider.TelegramMessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Drives queued non-email channel messages to a terminal delivery status.
 *
 * <p>Mirrors what {@link MessageProcessor} does for email: the Kafka listener only persists the
 * message as {@link DeliveryStatusType#OPENED}, and this processor — driven by a scheduler —
 * performs the actual delivery attempt and reconciles the status.</p>
 *
 * <p>Telegram and Push (through Expo) are wired; SMS and WhatsApp stay queued until their providers
 * are integrated, which is why this deliberately dispatches per channel rather than looping
 * over every {@code OPENED} document.</p>
 */
@Component
public class MultiChannelMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MultiChannelMessageProcessor.class);

    private final TelegramChannelService telegramChannelService;
    private final TelegramMessageDispatcher telegramDispatcher;
    private final PushChannelService pushChannelService;
    private final ExpoPushDispatcher pushDispatcher;
    private final CampaignMetricsService campaignMetricsService;
    private final SendEventService sendEventService;

    public MultiChannelMessageProcessor(TelegramChannelService telegramChannelService,
                                        TelegramMessageDispatcher telegramDispatcher,
                                        PushChannelService pushChannelService,
                                        ExpoPushDispatcher pushDispatcher,
                                        CampaignMetricsService campaignMetricsService,
                                        SendEventService sendEventService) {
        this.telegramChannelService = telegramChannelService;
        this.telegramDispatcher = telegramDispatcher;
        this.pushChannelService = pushChannelService;
        this.pushDispatcher = pushDispatcher;
        this.campaignMetricsService = campaignMetricsService;
        this.sendEventService = sendEventService;
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
            sendEventService.record(ChannelType.TELEGRAM, SendEventService.kindOf(false, message.getCampaignId()), outcome, null,
                    message.getCampaignId(), message.getTemplateDefinedId(), String.valueOf(message.getChatId()), SendEventService.CAUSE_DELIVERY_FAILED);
        });
    }

    /**
     * Sends the queued push messages that are due ({@code sendDate} reached) through Expo and persists what changed. Terminal outcomes count
     * against the campaign and the monitoring; a message that failed for a transient reason stays {@code OPENED} (attempt counted) and is tried
     * again on the next run. Messages are left untouched while the Expo channel is off.
     */
    public void dispatchPush() {
        if (!pushDispatcher.isEnabled()) {
            logger.debug("Push dispatch skipped: the Expo channel is off.");
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<PushNotificationMessageDocument> due = pushChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED).stream()
                .filter(message -> Objects.isNull(message.getSendDate()) || !message.getSendDate().isAfter(now))
                .toList();
        if (due.isEmpty()) {
            return;
        }
        logger.info("Dispatching {} queued push message(s).", due.size());
        pushDispatcher.dispatch(due).forEach(message -> {
            pushChannelService.updateStatus(message);
            if (!DeliveryStatusType.OPENED.equals(message.getDeliveryStatus())) {
                campaignMetricsService.recordOutcome(message.getCampaignId(), message.getDeliveryStatus());
                recordPushEvent(message);
            }
        });
    }

    /**
     * Reads the Expo receipts of the push messages accepted at least {@code receipt-delay} ago. A receipt that reports an error (typically
     * {@code DeviceNotRegistered}) turns the message {@code FAILED} with its reason, which is what tells the caller a device token is dead.
     */
    public void checkPushReceipts() {
        if (!pushDispatcher.isEnabled()) {
            return;
        }
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minus(pushDispatcher.receiptDelay());
        List<PushNotificationMessageDocument> ripe = pushChannelService.findAwaitingReceipt().stream()
                .filter(message -> Objects.nonNull(message.getSentAt()) && !message.getSentAt().isAfter(threshold))
                .toList();
        if (ripe.isEmpty()) {
            return;
        }
        logger.info("Reading {} Expo receipt(s).", ripe.size());
        pushDispatcher.checkReceipts(ripe).forEach(message -> {
            pushChannelService.updateStatus(message);
            if (DeliveryStatusType.FAILED.equals(message.getDeliveryStatus())) {
                // the accepted message already counted as sent: only the monitoring learns it failed afterwards
                recordPushEvent(message);
            }
        });
    }

    private void recordPushEvent(PushNotificationMessageDocument message) {
        String cause = Objects.nonNull(message.getFailureReason()) ? message.getFailureReason().name() : SendEventService.CAUSE_DELIVERY_FAILED;
        sendEventService.record(ChannelType.PUSH, SendEventService.kindOf(false, message.getCampaignId()), message.getDeliveryStatus(), null,
                message.getCampaignId(), message.getTemplateDefinedId(), message.getDeviceToken(), cause);
    }
}
