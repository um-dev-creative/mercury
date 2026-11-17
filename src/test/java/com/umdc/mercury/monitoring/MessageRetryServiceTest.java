package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.service.CampaignMetricsService;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageRetryService")
class MessageRetryServiceTest {

    private final UUID application = UUID.randomUUID();
    private final UUID message = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();

    @Mock
    private SendEventRepository events;
    @Mock
    private EmailMessageNSRepository emailQueue;
    @Mock
    private CampaignMetricsService metrics;
    @InjectMocks
    private MessageRetryService service;

    private EmailMessageDocument failed(UUID campaign) {
        return new EmailMessageDocument("doc-1", message, UUID.randomUUID(), UUID.randomUUID(), "from@x.com", List.of(), List.of(), "subject", "body",
                LocalDateTime.of(2026, 11, 17, 8, 0), Map.of("k", "v"), DeliveryStatusType.FAILED, campaign);
    }

    @Test
    @DisplayName("a failed e-mail of the application goes back to OPENED with everything else untouched, and its campaign failure is taken back")
    void queuesAgain() {
        var campaign = UUID.randomUUID();
        var document = failed(campaign);
        when(events.existsByApplicationIdAndMessageIdAndChannelAndStatus(application, message, "EMAIL", "FAILED")).thenReturn(true);
        when(emailQueue.findFailedByMessageId(message)).thenReturn(List.of(document));

        assertEquals(MessageRetryService.Result.QUEUED, service.retry(application, message, user));

        var saved = ArgumentCaptor.forClass(EmailMessageDocument.class);
        verify(emailQueue).save(saved.capture());
        assertEquals(DeliveryStatusType.OPENED, saved.getValue().deliveryStatus());
        assertEquals(document.id(), saved.getValue().id());
        assertEquals(document.body(), saved.getValue().body());
        assertEquals(document.params(), saved.getValue().params());
        assertEquals(campaign, saved.getValue().campaignId());
        verify(metrics).revokeFailure(campaign);
    }

    @Test
    @DisplayName("a message of another application (or an unknown one) is NOT_FOUND and nothing is read from the queue")
    void notFound() {
        when(events.existsByApplicationIdAndMessageIdAndChannelAndStatus(application, message, "EMAIL", "FAILED")).thenReturn(false);

        assertEquals(MessageRetryService.Result.NOT_FOUND, service.retry(application, message, user));

        verifyNoInteractions(emailQueue, metrics);
    }

    @Test
    @DisplayName("when the e-mail is no longer held as FAILED (already queued, sent or purged) it is NOT_RETRYABLE and nothing changes")
    void notRetryable() {
        when(events.existsByApplicationIdAndMessageIdAndChannelAndStatus(application, message, "EMAIL", "FAILED")).thenReturn(true);
        when(emailQueue.findFailedByMessageId(message)).thenReturn(List.of());

        assertEquals(MessageRetryService.Result.NOT_RETRYABLE, service.retry(application, message, user));

        verify(emailQueue, never()).save(any());
        verifyNoInteractions(metrics);
    }
}
