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
}
