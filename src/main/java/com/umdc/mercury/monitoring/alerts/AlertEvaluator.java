package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.api.v1.service.MonitoringServiceImpl;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.monitoring.QueueSnapshots;
import com.umdc.mercury.monitoring.SendEventRepository;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/** Reads what a rule watches and says whether it is breached. The detail is a fixed sentence with numbers and service names only. */
@Component
public class AlertEvaluator {

    private static final String SENT = "SENT";
    private static final String OUTAGE_DEGRADED = "DEGRADED";
    private static final String OUTAGE_DOWN = "DOWN";

    private final SendEventRepository events;
    private final QueueSnapshots queues;
    private final ServiceHealthService health;

    public AlertEvaluator(SendEventRepository events, QueueSnapshots queues, ServiceHealthService health) {
        this.events = events;
        this.queues = queues;
        this.health = health;
    }

    public Reading read(AlertRuleEntity rule, LocalDateTime now) {
        return switch (AlertType.valueOf(rule.getType())) {
            case FAILURE_RATE -> failureRate(rule, now);
            case QUEUE_STUCK -> queueStuck(rule, now);
            case SERVICE_DOWN -> serviceDown(rule, now);
        };
    }

    private Reading failureRate(AlertRuleEntity rule, LocalDateTime now) {
        long sent = 0;
        long failed = 0;
        for (var outcome : events.countOutcomes(rule.getApplicationId(), now.minusMinutes(rule.getWindowMinutes()))) {
            if (SENT.equals(outcome.getStatus())) {
                sent += outcome.getTotal();
            } else {
                failed += outcome.getTotal();
            }
        }
        long total = sent + failed;
        double rate = total == 0 ? 0 : failed * 100.0 / total;
        boolean breached = total > 0 && total >= rule.getMinVolume() && rate > rule.getThreshold().doubleValue();
        return new Reading(breached, rate, String.format(Locale.ROOT, "%.1f%% of the messages failed in the last %d min (%d of %d)", rate,
                rule.getWindowMinutes(), failed, total));
    }

    private Reading queueStuck(AlertRuleEntity rule, LocalDateTime now) {
        var snapshot = queues.of(rule.getApplicationId());
        var limit = now.minusMinutes(rule.getWindowMinutes());
        long waiting = 0;
        for (ChannelType channel : ChannelType.values()) {
            if (MonitoringServiceImpl.WITH_PROVIDER.contains(channel)) {
                waiting += snapshot.countOlderThan(channel, limit);
            }
        }
        return new Reading(waiting >= rule.getThreshold().doubleValue(), waiting,
                String.format(Locale.ROOT, "%d message(s) have waited more than %d min", waiting, rule.getWindowMinutes()));
    }

    private Reading serviceDown(AlertRuleEntity rule, LocalDateTime now) {
        long longest = 0;
        var names = new java.util.ArrayList<String>();
        for (var service : health.health().services()) {
            boolean outage = OUTAGE_DEGRADED.equals(service.status()) || OUTAGE_DOWN.equals(service.status());
            if (!outage || service.since() == null || rule.getService() != null && !rule.getService().equals(service.service())) {
                continue;
            }
            long minutes = Math.max(0, Duration.between(service.since(), now).toMinutes());
            if (minutes >= rule.getWindowMinutes()) {
                names.add(service.service() + " " + service.status());
                longest = Math.max(longest, minutes);
            }
        }
        return new Reading(!names.isEmpty(), longest, names.isEmpty() ? "No service has been degraded or down for " + rule.getWindowMinutes() + " min"
                : String.join(", ", names) + " for at least " + rule.getWindowMinutes() + " min");
    }

    /** The reading of one rule. */
    public record Reading(boolean breached, double value, String detail) {
    }
}
