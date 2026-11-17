package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.service.CampaignMetricsService;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Puts a failed e-mail back in the queue so the next run of {@code MessageProcessor} sends it again. Only an e-mail that the application's own
 * monitoring record shows as failed and that is still held in MongoDB (status {@code FAILED}) can be retried; Telegram and the channels without
 * a provider have nothing to retry here.
 */
@Service
public class MessageRetryService {

    /** What a retry did. */
    public enum Result { QUEUED, NOT_FOUND, NOT_RETRYABLE }

    private static final Logger logger = LoggerFactory.getLogger(MessageRetryService.class);

    private final SendEventRepository events;
    private final EmailMessageNSRepository emailQueue;
    private final CampaignMetricsService campaignMetrics;

    public MessageRetryService(SendEventRepository events, EmailMessageNSRepository emailQueue, CampaignMetricsService campaignMetrics) {
        this.events = events;
        this.emailQueue = emailQueue;
        this.campaignMetrics = campaignMetrics;
    }

    /**
     * @param applicationId the application the caller acts for: a message of another application is {@link Result#NOT_FOUND}, never revealed
     * @param messageId     the id shown in the message table
     * @param requestedBy   the verified user, for the log
     */
    public Result retry(UUID applicationId, UUID messageId, UUID requestedBy) {
        if (!events.existsByApplicationIdAndMessageIdAndChannelAndStatus(applicationId, messageId, ChannelType.EMAIL.name(), "FAILED")) {
            return Result.NOT_FOUND;
        }
        var failed = emailQueue.findFailedByMessageId(messageId);
        if (failed.isEmpty()) {
            // already queued again, sent, or no longer held
            return Result.NOT_RETRYABLE;
        }
        for (var document : failed) {
            emailQueue.save(reopened(document));
            campaignMetrics.revokeFailure(document.campaignId());
        }
        logger.info("Message {} of application {} queued again by user {}", messageId, applicationId, requestedBy);
        return Result.QUEUED;
    }

    private static EmailMessageDocument reopened(EmailMessageDocument d) {
        return new EmailMessageDocument(d.id(), d.messageId(), d.templateDefinedId(), d.userId(), d.from(), d.to(), d.cc(), d.subject(), d.body(),
                d.sendDate(), d.params(), DeliveryStatusType.OPENED, d.campaignId());
    }
}
