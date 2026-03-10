package com.prx.mercury.scheduler;

import com.prx.mercury.processor.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for scheduling email-related tasks.
 * This scheduler handles periodic execution of email sending and status updates.
 */
@Service
public class SendEmailScheduler {

    private final Logger logger = LoggerFactory.getLogger(SendEmailScheduler.class);
    private final MessageProcessor messageProcessor;

    /**
     * Constructs a SendEmailScheduler with the required message processor.
     *
     * @param messageProcessor The processor responsible for email message operations
     */
    public SendEmailScheduler(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    /**
     * Scheduled task that triggers email sending process.
     * Rate is controlled by {@code prx.scheduler.send-email.fixed-rate} (default 60 000 ms).
     */
    @Scheduled(fixedRateString = "${prx.scheduler.send-email.fixed-rate:60000}")
    public void sendEmail() {
        logger.debug("Initiating email sending task...");
        messageProcessor.processMessage();
        logger.debug("Email sending task completed.");
    }

    /**
     * Scheduled task that updates the status of processed messages.
     * Rate is controlled by {@code prx.scheduler.save-message-processed.fixed-rate} (default 300 000 ms).
     */
    @Scheduled(fixedRateString = "${prx.scheduler.save-message-processed.fixed-rate:300000}")
    public void saveMessageProcessed() {
        logger.debug("Initiating message processed saving task...");
        messageProcessor.updateMessageStatus();
        logger.debug("Message processed saving task completed.");
    }

}
