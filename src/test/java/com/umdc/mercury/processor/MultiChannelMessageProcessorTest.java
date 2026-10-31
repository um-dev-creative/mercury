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

        verifyNoInteractions(telegramChannelService, campaignMetricsService);
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
    }

    @Test
    @DisplayName("does nothing when there is nothing queued")
    void noQueuedMessages() {
        when(telegramDispatcher.isEnabled()).thenReturn(true);
        when(telegramChannelService.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of());

        processor.dispatchTelegram();

        verify(telegramDispatcher, never()).dispatch(any());
        verifyNoInteractions(campaignMetricsService);
    }
}
