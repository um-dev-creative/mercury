package com.prx.mercury.kafka.consumer.service;

import com.prx.commons.exception.StandardException;
import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.kafka.to.EmailMessageTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EmailMessageConsumerService default methods")
class EmailMessageConsumerServiceDefaultsTest {

    private final EmailMessageConsumerService service = new EmailMessageConsumerService() {
    };

    @Test
    @DisplayName("findAll default implementation throws StandardException")
    void findAllThrows() {
        assertThrows(StandardException.class, service::findAll);
    }

    @Test
    @DisplayName("findById default implementation throws StandardException")
    void findByIdThrows() {
        assertThrows(StandardException.class, () -> service.findById("id-1"));
    }

    @Test
    @DisplayName("save default implementation throws StandardException")
    void saveThrows() {
        EmailMessageTO message = sampleEmailMessageTO();
        assertThrows(StandardException.class, () -> service.save(message));
    }

    @Test
    @DisplayName("saveAll default implementation throws StandardException")
    void saveAllThrows() {
        List<EmailMessageTO> messages = List.of(sampleEmailMessageTO());
        assertThrows(StandardException.class, () -> service.saveAll(messages));
    }

    @Test
    @DisplayName("deleteById default implementation throws StandardException")
    void deleteByIdThrows() {
        assertThrows(StandardException.class, () -> service.deleteById("id-1"));
    }

    @Test
    @DisplayName("deleteAll default implementation throws StandardException")
    void deleteAllThrows() {
        assertThrows(StandardException.class, service::deleteAll);
    }

    @Test
    @DisplayName("update default implementation throws StandardException")
    void updateThrows() {
        EmailMessageTO message = sampleEmailMessageTO();
        assertThrows(StandardException.class, () -> service.update(message));
    }

    private EmailMessageTO sampleEmailMessageTO() {
        return new EmailMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "from@test.com",
                List.of(new EmailContact("to@test.com", "to", null)),
                List.of(),
                "subject",
                "body",
                LocalDateTime.now(),
                Map.of("k", "v")
        );
    }
}
