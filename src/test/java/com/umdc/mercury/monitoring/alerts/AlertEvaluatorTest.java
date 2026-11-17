package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.monitoring.QueueSnapshots;
import com.umdc.mercury.monitoring.SendEventRepository;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertEvaluator")
class AlertEvaluatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 17, 8, 0);
    private final UUID application = UUID.randomUUID();

    @Mock
    private SendEventRepository events;
    @Mock
    private QueueSnapshots queues;
    @Mock
    private ServiceHealthService health;
    @InjectMocks
    private AlertEvaluator evaluator;

    private AlertRuleEntity rule(AlertType type, String threshold, int window, int volume, String service) {
        var rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setApplicationId(application);
        rule.setType(type.name());
        rule.setThreshold(new BigDecimal(threshold));
        rule.setWindowMinutes(window);
        rule.setMinVolume(volume);
        rule.setService(service);
        return rule;
    }

    private static SendEventRepository.OutcomeCount outcome(String status, long total) {
        return new SendEventRepository.OutcomeCount() {
            @Override
            public String getChannel() {
                return "EMAIL";
            }

            @Override
            public String getKind() {
                return "CAMPAIGN";
            }

            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    @DisplayName("failure rate: breached when the share of failures passes the threshold with enough messages")
    void failureRateBreached() {
        when(events.countOutcomes(any(), any())).thenReturn(List.of(outcome("SENT", 70), outcome("FAILED", 30)));

        var reading = evaluator.read(rule(AlertType.FAILURE_RATE, "20.00", 15, 50, null), NOW);

        assertTrue(reading.breached());
        assertEquals(30.0, reading.value());
        assertEquals("30.0% of the messages failed in the last 15 min (30 of 100)", reading.detail());
    }

    @Test
    @DisplayName("failure rate: under the minimum volume, at the threshold, or with no messages at all, nothing fires")
    void failureRateNotBreached() {
        when(events.countOutcomes(any(), any())).thenReturn(List.of(outcome("SENT", 1), outcome("FAILED", 4)));
        assertFalse(evaluator.read(rule(AlertType.FAILURE_RATE, "20.00", 15, 50, null), NOW).breached(), "5 messages < volume 50");

        when(events.countOutcomes(any(), any())).thenReturn(List.of(outcome("SENT", 80), outcome("FAILED", 20)));
        assertFalse(evaluator.read(rule(AlertType.FAILURE_RATE, "20.00", 15, 0, null), NOW).breached(), "exactly the threshold does not exceed it");

        when(events.countOutcomes(any(), any())).thenReturn(List.of());
        var none = evaluator.read(rule(AlertType.FAILURE_RATE, "1.00", 15, 0, null), NOW);
        assertFalse(none.breached());
        assertEquals(0.0, none.value());
    }

    @Test
    @DisplayName("queue stuck: counts only channels with a provider, older than the window")
    void queueStuck() {
        var snapshot = mock(QueueSnapshots.QueueSnapshot.class);
        for (ChannelType channel : ChannelType.values()) {
            when(snapshot.countOlderThan(any(), any())).thenReturn(0L);
        }
        when(snapshot.countOlderThan(org.mockito.ArgumentMatchers.eq(ChannelType.EMAIL), any())).thenReturn(4L);
        when(snapshot.countOlderThan(org.mockito.ArgumentMatchers.eq(ChannelType.TELEGRAM), any())).thenReturn(2L);
        when(queues.of(application)).thenReturn(snapshot);

        var reading = evaluator.read(rule(AlertType.QUEUE_STUCK, "5.00", 30, 0, null), NOW);

        assertTrue(reading.breached());
        assertEquals(6.0, reading.value());
        assertEquals("6 message(s) have waited more than 30 min", reading.detail());
        assertFalse(evaluator.read(rule(AlertType.QUEUE_STUCK, "7.00", 30, 0, null), NOW).breached());
    }

    @Test
    @DisplayName("service down: a service in trouble for at least the window fires; a recent one, an up one and another service do not")
    void serviceDown() {
        var services = List.of(
                new MonitoringTO.ServiceHealth("SMTP", "DOWN", null, "TIMEOUT", "timed out", NOW.minusMinutes(12), NOW),
                new MonitoringTO.ServiceHealth("KAFKA", "DEGRADED", 10L, "SLOW", "slow", NOW.minusMinutes(2), NOW),
                new MonitoringTO.ServiceHealth("POSTGRESQL", "UP", 3L, null, null, null, NOW),
                new MonitoringTO.ServiceHealth("MONGODB", "DOWN", null, "CONNECTION_FAILED", "failed", null, NOW),
                new MonitoringTO.ServiceHealth("BACKBONE", "NOT_CONFIGURED", null, null, null, NOW.minusMinutes(99), NOW));
        when(health.health()).thenReturn(new MonitoringTO.HealthResponse(NOW, "OUTAGE", services));

        var any = evaluator.read(rule(AlertType.SERVICE_DOWN, "1.00", 10, 0, null), NOW);
        assertTrue(any.breached());
        assertEquals(12.0, any.value());
        assertEquals("SMTP DOWN for at least 10 min", any.detail());

        assertFalse(evaluator.read(rule(AlertType.SERVICE_DOWN, "1.00", 10, 0, "KAFKA"), NOW).breached());
        assertTrue(evaluator.read(rule(AlertType.SERVICE_DOWN, "1.00", 1, 0, "KAFKA"), NOW).breached());
        var quiet = evaluator.read(rule(AlertType.SERVICE_DOWN, "1.00", 60, 0, null), NOW);
        assertFalse(quiet.breached());
        assertEquals("No service has been degraded or down for 60 min", quiet.detail());
        assertEquals(Map.of(), Map.of());
    }
}
