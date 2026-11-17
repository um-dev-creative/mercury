package com.umdc.mercury.monitoring;

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
    @InjectMocks
    private SendEventRetentionScheduler scheduler;

    @Test
    @DisplayName("purges the expired events")
    void purges() {
        when(service.purgeExpired()).thenReturn(3);

        scheduler.purge();

        verify(service).purgeExpired();
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

        assertDoesNotThrow(scheduler::purge);
    }
}
