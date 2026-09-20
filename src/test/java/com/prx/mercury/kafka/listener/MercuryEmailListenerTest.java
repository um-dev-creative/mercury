package com.prx.mercury.kafka.listener;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.kafka.consumer.service.EmailMessageConsumerService;
import com.umdc.mercury.kafka.listener.MercuryEmailListener;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercuryEmailListener unit tests")
class MercuryEmailListenerTest {

    @Mock
    private EmailMessageConsumerService emailMessageConsumerService;

    @InjectMocks
    private MercuryEmailListener mercuryEmailListener;

    private EmailMessageTO completeMessage() {
        return new EmailMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "noreply@example.com",
                List.of(new EmailContact("john@example.com", "John", null)),
                List.of(),
                "Welcome",
                "Hello body",
                LocalDateTime.now(),
                Map.of()
        );
    }

    @Test
    @DisplayName("saves the message when all required fields are present")
    void listenerEmailTopic_completeMessage_saves() {
        EmailMessageTO message = completeMessage();

        mercuryEmailListener.listenerEmailTopic(message);

        verify(emailMessageConsumerService).save(message);
    }

    @Test
    @DisplayName("discards a null message without invoking the consumer service")
    void listenerEmailTopic_nullMessage_discarded() {
        mercuryEmailListener.listenerEmailTopic(null);

        verify(emailMessageConsumerService, never()).save(any());
    }

    @Test
    @DisplayName("discards an incomplete message (missing 'to') without invoking the consumer service")
    void listenerEmailTopic_missingTo_discarded() {
        EmailMessageTO message = new EmailMessageTO(
                UUID.randomUUID(), UUID.randomUUID(), "noreply@example.com",
                null, List.of(), "Welcome", "Hello body", LocalDateTime.now(), Map.of());

        mercuryEmailListener.listenerEmailTopic(message);

        verify(emailMessageConsumerService, never()).save(any());
    }
}
