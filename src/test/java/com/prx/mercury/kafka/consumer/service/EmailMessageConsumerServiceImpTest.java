package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.jpa.nosql.document.EmailMessageDocument;
import com.prx.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.prx.mercury.kafka.to.EmailMessageTO;
import com.prx.mercury.mapper.EmailMessageMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailMessageConsumerServiceImp behavior")
class EmailMessageConsumerServiceImpTest {

    @Mock
    private EmailMessageNSRepository emailMessageNSRepository;

    @Mock
    private EmailMessageMapper emailMessageMapper;

    @InjectMocks
    private EmailMessageConsumerServiceImp service;

    @Test
    @DisplayName("saveAll returns the same input list")
    void saveAllReturnsInput() {
        List<EmailMessageTO> input = List.of(sampleEmailMessageTO());

        List<EmailMessageTO> result = service.saveAll(input);

        assertSame(input, result);
    }

    @Test
    @DisplayName("save maps and persists document then returns original message")
    void saveMapsAndPersists() {
        EmailMessageTO input = sampleEmailMessageTO();
        EmailMessageDocument mapped = new EmailMessageDocument(
                "id-1",
                UUID.randomUUID(),
                input.templateDefinedId(),
                input.userId(),
                input.from(),
                input.to(),
                input.cc(),
                input.subject(),
                input.body(),
                input.sendDate(),
                input.params(),
                null
        );

        when(emailMessageMapper.toEmailMessageDocument(input)).thenReturn(mapped);

        EmailMessageTO result = service.save(input);

        assertEquals(input, result);
        verify(emailMessageMapper).toEmailMessageDocument(input);
        verify(emailMessageNSRepository).save(mapped);
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

