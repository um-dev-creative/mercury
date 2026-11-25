package com.umdc.mercury.scheduler;

import com.umdc.mercury.processor.MultiChannelMessageProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("MultiChannelScheduler")
class MultiChannelSchedulerTest {

    private final MultiChannelMessageProcessor processor = mock(MultiChannelMessageProcessor.class);
    private final MultiChannelScheduler scheduler = new MultiChannelScheduler(processor);

    @Test
    @DisplayName("each task delegates to its processor method")
    void delegates() {
        scheduler.dispatchTelegram();
        scheduler.dispatchPush();
        scheduler.checkPushReceipts();

        verify(processor).dispatchTelegram();
        verify(processor).dispatchPush();
        verify(processor).checkPushReceipts();
    }

    @Test
    @DisplayName("the push tasks run at the configurable rates the settings document")
    void rates() throws NoSuchMethodException {
        assertTrue(MultiChannelScheduler.class.getMethod("dispatchPush").getAnnotation(Scheduled.class).fixedRateString()
                .contains("umdc.scheduler.dispatch-push.fixed-rate"));
        assertTrue(MultiChannelScheduler.class.getMethod("checkPushReceipts").getAnnotation(Scheduled.class).fixedRateString()
                .contains("umdc.scheduler.push-receipts.fixed-rate"));
    }
}
