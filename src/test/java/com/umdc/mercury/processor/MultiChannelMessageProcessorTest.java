package com.umdc.mercury.processor;

import com.umdc.mercury.api.v1.service.CampaignMetricsService;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.processor.MultiChannelMessageProcessor;
import com.umdc.mercury.provider.TelegramMessageDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiChannelMessageProcessor")
class MultiChannelMessageProcessorTest {

    @Mock
    private TelegramChannelService telegramChannelService;

    @Mock
    private TelegramMessageDispatcher telegramDispatcher;

    @Mock
    private com.umdc.mercury.kafka.consumer.service.PushChannelService pushChannelService;

    @Mock
    private com.umdc.mercury.provider.ExpoPushDispatcher pushDispatcher;

    @Mock
    private CampaignMetricsService campaignMetricsService;

    @Mock
    private com.umdc.mercury.monitoring.SendEventService sendEventService;

    @InjectMocks
    private MultiChannelMessageProcessor processor;

    private TelegramMessageDocument queued(UUID campaignId) {
        TelegramMessageDocument document = new TelegramMessageDocument();
        document.setChatId(12345L);
        document.setCampaignId(campaignId);
        document.setDeliveryStatus(DeliveryStatusType.OPENED);
        return document;
    }

    @Test
    @DisplayName("leaves messages queued when no bot token is configured")
    void skipsWhenDispatcherDisabled() {
        when(telegramDispatcher.isEnabled()).thenReturn(false);

        processor.dispatchTelegram();

        verifyNoInteractions(telegramChannelService, campaignMetricsService, sendEventService);
        verify(telegramDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("marks a delivered message SENT and records it against the campaign")
    void dispatchesAndRecordsSent() {
        UUID campaignId = UUID.randomUUID();
        TelegramMessageDocument document = queued(campaignId);
        when(telegramDispatcher.isEnabled()).thenReturn(true);
        when(telegramChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(document));
        when(telegramDispatcher.dispatch(document)).thenReturn(DeliveryStatusType.SENT);

        processor.dispatchTelegram();

        assertThat(document.getDeliveryStatus()).isEqualTo(DeliveryStatusType.SENT);
        verify(telegramChannelService).updateStatus(document);
        verify(campaignMetricsService).recordOutcome(campaignId, DeliveryStatusType.SENT);
        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.TELEGRAM, "CAMPAIGN", DeliveryStatusType.SENT, null, campaignId, null,
                "12345", "DELIVERY_FAILED");
    }

    @Test
    @DisplayName("marks a rejected message FAILED and records the failure")
    void dispatchesAndRecordsFailure() {
        UUID campaignId = UUID.randomUUID();
        TelegramMessageDocument document = queued(campaignId);
        when(telegramDispatcher.isEnabled()).thenReturn(true);
        when(telegramChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(document));
        when(telegramDispatcher.dispatch(document)).thenReturn(DeliveryStatusType.FAILED);

        processor.dispatchTelegram();

        assertThat(document.getDeliveryStatus()).isEqualTo(DeliveryStatusType.FAILED);
        verify(telegramChannelService).updateStatus(document);
        verify(campaignMetricsService).recordOutcome(campaignId, DeliveryStatusType.FAILED);
        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.TELEGRAM, "CAMPAIGN", DeliveryStatusType.FAILED, null, campaignId, null,
                "12345", "DELIVERY_FAILED");
    }

    @Test
    @DisplayName("a message outside any campaign is recorded as transactional")
    void recordsTransactionalOutsideCampaign() {
        TelegramMessageDocument document = queued(null);
        when(telegramDispatcher.isEnabled()).thenReturn(true);
        when(telegramChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(document));
        when(telegramDispatcher.dispatch(document)).thenReturn(DeliveryStatusType.SENT);

        processor.dispatchTelegram();

        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.TELEGRAM, "TRANSACTIONAL", DeliveryStatusType.SENT, null, null, null,
                "12345", "DELIVERY_FAILED");
    }

    @Test
    @DisplayName("does nothing when there is nothing queued")
    void noQueuedMessages() {
        when(telegramDispatcher.isEnabled()).thenReturn(true);
        when(telegramChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of());

        processor.dispatchTelegram();

        verify(telegramDispatcher, never()).dispatch(any());
        verifyNoInteractions(campaignMetricsService, sendEventService);
    }

    private com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument push(UUID campaignId, java.time.LocalDateTime sendDate) {
        var document = new com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument();
        document.setDeviceToken("ExponentPushToken[abc123]");
        document.setCampaignId(campaignId);
        document.setSendDate(sendDate);
        document.setDeliveryStatus(DeliveryStatusType.OPENED);
        return document;
    }

    @Test
    @DisplayName("push stays queued while the Expo channel is off")
    void pushSkippedWhenOff() {
        when(pushDispatcher.isEnabled()).thenReturn(false);

        processor.dispatchPush();
        processor.checkPushReceipts();

        verifyNoInteractions(pushChannelService, campaignMetricsService, sendEventService);
    }

    @Test
    @DisplayName("push sends only the messages whose sendDate is reached, persists them and counts a SENT one")
    void pushDispatchesDueMessages() {
        UUID campaignId = UUID.randomUUID();
        var now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        var due = push(campaignId, now.minusMinutes(1));
        var noDate = push(null, null);
        var later = push(campaignId, now.plusHours(2));
        when(pushDispatcher.isEnabled()).thenReturn(true);
        when(pushChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(due, noDate, later));
        when(pushDispatcher.dispatch(List.of(due, noDate))).thenAnswer(call -> {
            due.setDeliveryStatus(DeliveryStatusType.SENT);
            noDate.setDeliveryStatus(DeliveryStatusType.SENT);
            return List.of(due, noDate);
        });

        processor.dispatchPush();

        verify(pushChannelService).updateStatus(due);
        verify(pushChannelService).updateStatus(noDate);
        verify(pushChannelService, never()).updateStatus(later);
        verify(campaignMetricsService).recordOutcome(campaignId, DeliveryStatusType.SENT);
        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.PUSH, "CAMPAIGN", DeliveryStatusType.SENT, null, campaignId, null,
                "ExponentPushToken[abc123]", "DELIVERY_FAILED");
        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.PUSH, "TRANSACTIONAL", DeliveryStatusType.SENT, null, null, null,
                "ExponentPushToken[abc123]", "DELIVERY_FAILED");
    }

    @Test
    @DisplayName("push with nothing due does not call Expo")
    void pushNothingDue() {
        when(pushDispatcher.isEnabled()).thenReturn(true);
        when(pushChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED))
                .thenReturn(List.of(push(null, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(1))));

        processor.dispatchPush();

        verify(pushDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("a push that failed for a dead token is recorded FAILED with its reason; one still retrying is persisted but not counted")
    void pushFailureAndRetry() {
        UUID campaignId = UUID.randomUUID();
        var dead = push(campaignId, null);
        var retrying = push(campaignId, null);
        when(pushDispatcher.isEnabled()).thenReturn(true);
        when(pushChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(dead, retrying));
        when(pushDispatcher.dispatch(any())).thenAnswer(call -> {
            dead.setDeliveryStatus(DeliveryStatusType.FAILED);
            dead.setFailureReason(com.umdc.mercury.constant.DeliveryFailureReason.DEVICE_NOT_REGISTERED);
            retrying.setDeliveryAttempts(1);
            return List.of(dead, retrying);
        });

        processor.dispatchPush();

        verify(pushChannelService).updateStatus(retrying);
        verify(campaignMetricsService).recordOutcome(campaignId, DeliveryStatusType.FAILED);
        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.PUSH, "CAMPAIGN", DeliveryStatusType.FAILED, null, campaignId, null,
                "ExponentPushToken[abc123]", "DEVICE_NOT_REGISTERED");
        verify(campaignMetricsService, org.mockito.Mockito.times(1)).recordOutcome(any(), any());
    }

    @Test
    @DisplayName("receipts: only messages accepted at least receipt-delay ago are read, and a dead token is recorded without counting the campaign twice")
    void receiptsReadRipeMessages() {
        var now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        var ripeDead = push(UUID.randomUUID(), null);
        ripeDead.setSentAt(now.minusMinutes(30));
        var ripeOk = push(null, null);
        ripeOk.setSentAt(now.minusMinutes(20));
        var fresh = push(null, null);
        fresh.setSentAt(now.minusMinutes(1));
        var unknown = push(null, null);
        when(pushDispatcher.isEnabled()).thenReturn(true);
        when(pushDispatcher.receiptDelay()).thenReturn(java.time.Duration.ofMinutes(15));
        when(pushChannelService.findAwaitingReceipt()).thenReturn(List.of(ripeDead, ripeOk, fresh, unknown));
        when(pushDispatcher.checkReceipts(List.of(ripeDead, ripeOk))).thenAnswer(call -> {
            ripeDead.setDeliveryStatus(DeliveryStatusType.FAILED);
            ripeDead.setFailureReason(com.umdc.mercury.constant.DeliveryFailureReason.DEVICE_NOT_REGISTERED);
            ripeOk.setDeliveryStatus(DeliveryStatusType.SENT);
            return List.of(ripeDead, ripeOk);
        });

        processor.checkPushReceipts();

        verify(pushChannelService).updateStatus(ripeDead);
        verify(pushChannelService).updateStatus(ripeOk);
        verify(sendEventService).record(com.umdc.mercury.constant.ChannelType.PUSH, "CAMPAIGN", DeliveryStatusType.FAILED, null, ripeDead.getCampaignId(), null,
                "ExponentPushToken[abc123]", "DEVICE_NOT_REGISTERED");
        verifyNoInteractions(campaignMetricsService);
        verify(sendEventService, org.mockito.Mockito.times(1)).record(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("receipts: nothing ripe means no call to Expo")
    void receiptsNothingRipe() {
        var fresh = push(null, null);
        fresh.setSentAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        when(pushDispatcher.isEnabled()).thenReturn(true);
        when(pushDispatcher.receiptDelay()).thenReturn(java.time.Duration.ofMinutes(15));
        when(pushChannelService.findAwaitingReceipt()).thenReturn(List.of(fresh));

        processor.checkPushReceipts();

        verify(pushDispatcher, never()).checkReceipts(any());
    }
}
