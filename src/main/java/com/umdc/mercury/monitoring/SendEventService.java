package com.umdc.mercury.monitoring;

import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Keeps the durable record of delivery outcomes for the monitoring views. Recording is best effort and isolated: it runs in its own
 * transaction and never throws, so a monitoring problem can never stop or undo a delivery.
 */
@Service
public class SendEventService {

    /** Kinds of communication told apart by the monitoring views. */
    public static final String KIND_TRANSACTIONAL = "TRANSACTIONAL";
    public static final String KIND_CAMPAIGN = "CAMPAIGN";
    public static final String KIND_VERIFICATION_CODE = "VERIFICATION_CODE";

    /** Failure causes recorded today (the delivery code reports only that it failed). */
    public static final String CAUSE_SEND_FAILED = "SEND_FAILED";
    public static final String CAUSE_DELIVERY_FAILED = "DELIVERY_FAILED";

    private static final Logger logger = LoggerFactory.getLogger(SendEventService.class);

    private final SendEventRepository events;
    private final TemplateDefinedRepository definitions;
    private final TransactionTemplate isolated;
    private final int retentionDays;

    public SendEventService(SendEventRepository events, TemplateDefinedRepository definitions, PlatformTransactionManager transactionManager,
                            @Value("${umdc.monitoring.retention-days:90}") int retentionDays) {
        this.events = events;
        this.definitions = definitions;
        this.isolated = new TransactionTemplate(transactionManager);
        this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.retentionDays = retentionDays;
    }

    /** The kind of a message: a verification code wins over a campaign, and anything else is transactional. */
    public static String kindOf(boolean verificationCode, UUID campaignId) {
        if (verificationCode) {
            return KIND_VERIFICATION_CODE;
        }
        return campaignId == null ? KIND_TRANSACTIONAL : KIND_CAMPAIGN;
    }

    /**
     * Records the outcome of a message. Only terminal outcomes count: {@code SENT} is recorded as sent and any other status as failed.
     *
     * @param recipient the recipient as the channel knows it; it is masked here and the original is never stored
     * @param cause     why it failed (short code), or {@code null} when it was sent
     */
    public void record(ChannelType channel, String kind, DeliveryStatusType outcome, UUID messageId, UUID campaignId, UUID templateDefinedId,
                       String recipient, String cause) {
        try {
            var sent = DeliveryStatusType.SENT.equals(outcome);
            var event = new SendEventEntity();
            event.setId(UUID.randomUUID());
            event.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
            event.setApplicationId(applicationOf(templateDefinedId));
            event.setChannel(channel.name());
            event.setKind(kind);
            event.setStatus(sent ? "SENT" : "FAILED");
            event.setMessageId(messageId);
            event.setCampaignId(campaignId);
            event.setTemplateDefinedId(templateDefinedId);
            event.setRecipientMasked(RecipientMasker.mask(recipient));
            event.setErrorCause(sent ? null : cause == null ? CAUSE_DELIVERY_FAILED : cause);
            isolated.executeWithoutResult(status -> events.save(event));
        } catch (RuntimeException e) {
            logger.warn("Could not record the send event of message {} (the delivery is not affected): {}", messageId, e.getMessage());
        }
    }

    /** Deletes the outcomes older than the retention period ({@code umdc.monitoring.retention-days}, 90 by default). */
    public int purgeExpired() {
        var before = LocalDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        var removed = isolated.execute(status -> events.deleteOlderThan(before));
        return removed == null ? 0 : removed;
    }

    private UUID applicationOf(UUID templateDefinedId) {
        if (templateDefinedId == null) {
            return null;
        }
        return definitions.findById(templateDefinedId).map(TemplateDefinedEntity::getApplicationId).orElse(null);
    }
}
