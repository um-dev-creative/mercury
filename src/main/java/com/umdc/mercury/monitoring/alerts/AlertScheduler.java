package com.umdc.mercury.monitoring.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the alert rules, once a minute by default ({@code umdc.scheduler.alert-evaluation.fixed-rate}). */
@Component
public class AlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    private final AlertEngine engine;

    public AlertScheduler(AlertEngine engine) {
        this.engine = engine;
    }

    @Scheduled(fixedRateString = "${umdc.scheduler.alert-evaluation.fixed-rate:60000}", initialDelayString = "${umdc.scheduler.alert-evaluation.initial-delay:30000}")
    public void evaluate() {
        try {
            engine.evaluateAll();
        } catch (RuntimeException e) {
            logger.warn("The alert evaluation failed: {}", e.getClass().getSimpleName());
        }
    }
}
