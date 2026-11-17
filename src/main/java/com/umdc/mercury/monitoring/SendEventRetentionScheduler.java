package com.umdc.mercury.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes the delivery outcomes older than the retention period, once a day by default. */
@Component
public class SendEventRetentionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SendEventRetentionScheduler.class);

    private final SendEventService sendEventService;

    public SendEventRetentionScheduler(SendEventService sendEventService) {
        this.sendEventService = sendEventService;
    }

    /** Rate: {@code umdc.scheduler.send-event-purge.fixed-rate} (default 86 400 000 ms), first run one hour after start. */
    @Scheduled(fixedRateString = "${umdc.scheduler.send-event-purge.fixed-rate:86400000}", initialDelayString = "${umdc.scheduler.send-event-purge.initial-delay:3600000}")
    public void purge() {
        try {
            int removed = sendEventService.purgeExpired();
            if (removed > 0) {
                logger.info("Removed {} expired send event(s)", removed);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not purge the expired send events: {}", e.getMessage());
        }
    }
}
