package com.umdc.mercury.monitoring.alerts;

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
@DisplayName("AlertScheduler")
class AlertSchedulerTest {

    @Mock
    private AlertEngine engine;
    @InjectMocks
    private AlertScheduler scheduler;

    @Test
    @DisplayName("each tick evaluates the rules")
    void evaluates() {
        scheduler.evaluate();

        verify(engine).evaluateAll();
    }

    @Test
    @DisplayName("a failing evaluation does not stop the scheduler")
    void failureIsSwallowed() {
        when(engine.evaluateAll()).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(scheduler::evaluate);
    }
}
