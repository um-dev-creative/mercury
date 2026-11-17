package com.umdc.mercury.monitoring;

import com.umdc.mercury.monitoring.health.ServiceHealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @InjectMocks
    private SendEventRetentionScheduler scheduler;

    @Test
    @DisplayName("purges the expired events and the expired outages")
    void purges() {
        when(service.purgeExpired()).thenReturn(3);
        when(health.purgeExpired()).thenReturn(2);

        scheduler.purge();

        verify(service).purgeExpired();
        verify(health).purgeExpired();
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
