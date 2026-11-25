package com.umdc.mercury.scheduler;

import com.umdc.mercury.processor.MultiChannelMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled entry point for the non-email channels, the counterpart of
 * {@link SendEmailScheduler}.
 */
@Component
public class MultiChannelScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MultiChannelScheduler.class);

    private final MultiChannelMessageProcessor multiChannelMessageProcessor;

    public MultiChannelScheduler(MultiChannelMessageProcessor multiChannelMessageProcessor) {
        this.multiChannelMessageProcessor = multiChannelMessageProcessor;
    }

    /**
     * Dispatches queued Telegram messages.
     * Rate is controlled by {@code umdc.scheduler.dispatch-telegram.fixed-rate} (default 60 000 ms).
     */
    @Scheduled(fixedRateString = "${umdc.scheduler.dispatch-telegram.fixed-rate:60000}")
    public void dispatchTelegram() {
        logger.debug("Initiating Telegram dispatch task...");
        multiChannelMessageProcessor.dispatchTelegram();
        logger.debug("Telegram dispatch task completed.");
    }

    /**
     * Sends the queued push messages through Expo.
     * Rate is controlled by {@code umdc.scheduler.dispatch-push.fixed-rate} (default 30 000 ms).
     */
    @Scheduled(fixedRateString = "${umdc.scheduler.dispatch-push.fixed-rate:30000}")
    public void dispatchPush() {
        logger.debug("Initiating push dispatch task...");
        multiChannelMessageProcessor.dispatchPush();
        logger.debug("Push dispatch task completed.");
    }

    /**
     * Reads the Expo receipts (final answers, such as a dead device token) of the push messages already accepted.
     * Rate is controlled by {@code umdc.scheduler.push-receipts.fixed-rate} (default 300 000 ms).
     */
    @Scheduled(fixedRateString = "${umdc.scheduler.push-receipts.fixed-rate:300000}")
    public void checkPushReceipts() {
        logger.debug("Initiating push receipts task...");
        multiChannelMessageProcessor.checkPushReceipts();
        logger.debug("Push receipts task completed.");
    }
}
