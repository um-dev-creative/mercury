package com.umdc.mercury.monitoring;

import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.sql.entity.TemplateDefinedEntity;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendEventService")
class SendEventServiceTest {

    @Mock
    private SendEventRepository events;
    @Mock
    private TemplateDefinedRepository definitions;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final MonitoringStorage storage = org.mockito.Mockito.mock(MonitoringStorage.class);
    private SendEventService service;
    private final UUID application = UUID.randomUUID();
    private final UUID template = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new SendEventService(events, definitions, transactionManager, 90, storage);
    }

    private SendEventEntity saved() {
        var captor = ArgumentCaptor.forClass(SendEventEntity.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }

    private void knownTemplate() {
        var definition = new TemplateDefinedEntity();
        definition.setApplicationId(application);
        when(definitions.findById(template)).thenReturn(Optional.of(definition));
    }

    @Test
    @DisplayName("kindOf: a verification code wins over a campaign; no campaign is transactional")
    void kindOf() {
        assertEquals("VERIFICATION_CODE", SendEventService.kindOf(true, UUID.randomUUID()));
        assertEquals("CAMPAIGN", SendEventService.kindOf(false, UUID.randomUUID()));
        assertEquals("TRANSACTIONAL", SendEventService.kindOf(false, null));
    }

    @Test
    @DisplayName("a sent message is stored with its application, masked recipient and no cause")
    void recordsSent() {
        knownTemplate();
        var message = UUID.randomUUID();
        var campaign = UUID.randomUUID();

        service.record(ChannelType.EMAIL, "CAMPAIGN", DeliveryStatusType.SENT, message, campaign, template, "john.doe@example.com", "SEND_FAILED");

        var event = saved();
        assertNotNull(event.getId());
        assertEquals(application, event.getApplicationId());
        assertEquals("EMAIL", event.getChannel());
        assertEquals("CAMPAIGN", event.getKind());
        assertEquals("SENT", event.getStatus());
        assertEquals(message, event.getMessageId());
        assertEquals(campaign, event.getCampaignId());
        assertEquals(template, event.getTemplateDefinedId());
        assertEquals("j***@e***.com", event.getRecipientMasked());
        assertNull(event.getErrorCause(), "a cause only goes with a failure");
        var now = LocalDateTime.now(java.time.ZoneOffset.UTC);
        assertTrue(event.getOccurredAt().isAfter(now.minusMinutes(1)) && !event.getOccurredAt().isAfter(now.plusSeconds(1)), "stored in UTC, now");
    }

    @Test
    @DisplayName("any status other than SENT is stored as FAILED with the cause, or a default one")
    void recordsFailed() {
        knownTemplate();

        service.record(ChannelType.TELEGRAM, "TRANSACTIONAL", DeliveryStatusType.FAILED, null, null, template, "12345", "BLOCKED_BY_USER");

        var event = saved();
        assertEquals("FAILED", event.getStatus());
        assertEquals("BLOCKED_BY_USER", event.getErrorCause());
        assertEquals("***345", event.getRecipientMasked());
    }

    @Test
    @DisplayName("a failure without a cause gets DELIVERY_FAILED")
    void defaultCause() {
        service.record(ChannelType.SMS, "TRANSACTIONAL", DeliveryStatusType.REJECTED, null, null, null, null, null);

        var event = saved();
        assertEquals("DELIVERY_FAILED", event.getErrorCause());
        assertNull(event.getApplicationId(), "no template, no application");
        assertNull(event.getRecipientMasked());
    }

    @Test
    @DisplayName("a template that no longer exists leaves the application empty instead of failing")
    void unknownTemplate() {
        when(definitions.findById(template)).thenReturn(Optional.empty());

        service.record(ChannelType.PUSH, "CAMPAIGN", DeliveryStatusType.SENT, null, null, template, "token", null);

        assertNull(saved().getApplicationId());
    }

    @Test
    @DisplayName("a database error is swallowed: recording can never stop a delivery")
    void neverThrows() {
        doThrow(new IllegalStateException("db down")).when(events).save(any());

        service.record(ChannelType.EMAIL, "TRANSACTIONAL", DeliveryStatusType.SENT, null, null, null, "a@b.co", null);

        verify(events).save(any());
    }

    @Test
    @DisplayName("purgeExpired deletes what is older than the retention period and reports how many")
    void purges() {
        when(events.deleteOlderThan(any())).thenReturn(7);

        assertEquals(7, service.purgeExpired());

        var captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(events).deleteOlderThan(captor.capture());
        assertTrue(captor.getValue().isBefore(LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(89)));
        assertTrue(captor.getValue().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(91)));
    }

    @Test
    @DisplayName("a recording that fails is counted for the dashboard and still never throws")
    void failedRecordingIsCounted() {
        org.mockito.Mockito.when(events.save(any())).thenThrow(new IllegalStateException("relation mercury.send_events does not exist"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.record(ChannelType.EMAIL, "TRANSACTIONAL", DeliveryStatusType.SENT, null, null, null, "a@b.co", null));

        org.mockito.Mockito.verify(storage).recordFailed();
    }
}
