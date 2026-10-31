package com.umdc.mercury.scheduler;

import com.umdc.mercury.processor.MessageProcessor;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SendEmailSchedulerTest {

    private final MessageProcessor processor = mock(MessageProcessor.class);
    private final SendEmailScheduler scheduler = new SendEmailScheduler(processor);

    @Test
    void sendEmailOnlyProcessesPendingMessages() {
        scheduler.sendEmail();

        verify(processor).processMessage();
        verifyNoMoreInteractions(processor);
    }

    @Test
    void saveMessageProcessedOnlyUpdatesStatuses() {
        scheduler.saveMessageProcessed();

        verify(processor).updateMessageStatus();
        verifyNoMoreInteractions(processor);
    }
}
