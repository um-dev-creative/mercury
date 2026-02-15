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
     * Executes at a fixed rate of every 60 seconds (60,000 milliseconds).
     */
    @Scheduled(fixedRate = 1500)
    public void sendEmail() {
        logger.debug("Initiating email sending task...");
        messageProcessor.processMessage();
        logger.debug("Email sending task completed.");
    }

    /**
     * Scheduled task that updates the status of processed messages.
     * Executes at a fixed rate of every 5 minutes (300,000 milliseconds).
     */
    @Scheduled(fixedRate = 1500)
    public void saveMessageProcessed() {
        logger.debug("Initiating message processed saving task...");
        messageProcessor.updateMessageStatus();
        logger.debug("Message processed saving task completed.");
    }

}
