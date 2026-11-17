package com.umdc.mercury.monitoring;

import com.umdc.mercury.monitoring.health.ServiceHealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendEventRetentionScheduler")
class SendEventRetentionSchedulerTest {

    @Mock
    private SendEventService service;
    @Mock
    private ServiceHealthService health;
    @Mock
    private com.umdc.mercury.monitoring.alerts.AlertEngine alerts;
    private SendEventRetentionScheduler scheduler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        scheduler = new SendEventRetentionScheduler(service, health, alerts, 90);
    }

    @Test
    @DisplayName("purges the expired events and the expired outages")
    void purges() {
        when(service.purgeExpired()).thenReturn(3);
        when(health.purgeExpired()).thenReturn(2);
        when(alerts.purgeResolvedBefore(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        scheduler.purge();

        verify(service).purgeExpired();
        verify(health).purgeExpired();
        var cutoff = org.mockito.ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        verify(alerts).purgeResolvedBefore(cutoff.capture());
        org.junit.jupiter.api.Assertions.assertTrue(cutoff.getValue().isBefore(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(89)));
    }

    @Test
    @DisplayName("a failing purge is logged and does not stop the scheduler")
    void failureIsSwallowed() {
        when(service.purgeExpired()).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(scheduler::purge);
    }

    @Test
    @DisplayName("an empty purge is fine")
    void nothingToPurge() {
        when(service.purgeExpired()).thenReturn(0);
        when(health.purgeExpired()).thenReturn(0);

        assertDoesNotThrow(scheduler::purge);
    }
}
