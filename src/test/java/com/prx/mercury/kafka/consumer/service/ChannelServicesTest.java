package com.prx.mercury.kafka.consumer.service;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.MessageDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.kafka.consumer.service.SmsChannelService;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.kafka.consumer.service.WhatsAppChannelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Channel Services persistence behavior")
class ChannelServicesTest {

    @Mock
    private MessageNSRepository messageRepository;

    @Test
    @DisplayName("SmsChannelService persists on send and delegates status updates")
    void smsChannelServicePersists() {
        SmsChannelService service = new SmsChannelService(messageRepository);
        SmsMessageDocument message = new SmsMessageDocument();
        message.setPhoneNumber("+15550001111");
        message.setMessage("hello");
        message.setDeliveryStatus(DeliveryStatusType.OPENED);
        when(messageRepository.save(message)).thenReturn(message);

        SmsMessageDocument result = service.send(message, null);

        assertSame(message, result);
        verify(messageRepository).save(message);

        service.updateStatus(message);
        verify(messageRepository, times(2)).save(message);
    }

    @Test
    @DisplayName("SmsChannelService findByDeliveryStatus filters to SMS documents only")
    void smsChannelServiceFindFiltersByType() {
        SmsChannelService service = new SmsChannelService(messageRepository);
        SmsMessageDocument sms = new SmsMessageDocument();
        TelegramMessageDocument telegram = new TelegramMessageDocument();
        when(messageRepository.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.<MessageDocument>of(sms, telegram));

        List<SmsMessageDocument> result = service.findByDeliveryStatus(DeliveryStatusType.OPENED);

        assertEquals(List.of(sms), result);
    }

    @Test
    @DisplayName("TelegramChannelService persists on send and delegates status updates")
    void telegramChannelServicePersists() {
        TelegramChannelService service = new TelegramChannelService(messageRepository);
        TelegramMessageDocument message = new TelegramMessageDocument();
        message.setChatId(12345L);
        message.setMessage("hello");
        message.setDeliveryStatus(DeliveryStatusType.OPENED);
        when(messageRepository.save(message)).thenReturn(message);

        TelegramMessageDocument result = service.send(message, null);

        assertSame(message, result);
        verify(messageRepository).save(message);

        service.updateStatus(message);
        verify(messageRepository, times(2)).save(message);
    }

    @Test
    @DisplayName("TelegramChannelService findByDeliveryStatus filters to Telegram documents only")
    void telegramChannelServiceFindFiltersByType() {
        TelegramChannelService service = new TelegramChannelService(messageRepository);
        TelegramMessageDocument telegram = new TelegramMessageDocument();
        SmsMessageDocument sms = new SmsMessageDocument();
        when(messageRepository.findByDeliveryStatus(DeliveryStatusType.DELIVERED)).thenReturn(List.<MessageDocument>of(sms, telegram));

        List<TelegramMessageDocument> result = service.findByDeliveryStatus(DeliveryStatusType.DELIVERED);

        assertEquals(List.of(telegram), result);
    }

    @Test
    @DisplayName("WhatsAppChannelService persists on send and delegates status updates")
    void whatsAppChannelServicePersists() {
        WhatsAppChannelService service = new WhatsAppChannelService(messageRepository);
        WhatsAppMessageDocument message = new WhatsAppMessageDocument();
        message.setPhoneNumber("+15550001111");
        message.setContent("hello");
        message.setDeliveryStatus(DeliveryStatusType.OPENED);
        when(messageRepository.save(message)).thenReturn(message);

        WhatsAppMessageDocument result = service.send(message, null);

        assertSame(message, result);
        verify(messageRepository).save(message);

        service.updateStatus(message);
        verify(messageRepository, times(2)).save(message);
    }

    @Test
    @DisplayName("WhatsAppChannelService findByDeliveryStatus filters to WhatsApp documents only")
    void whatsAppChannelServiceFindFiltersByType() {
        WhatsAppChannelService service = new WhatsAppChannelService(messageRepository);
        WhatsAppMessageDocument whatsApp = new WhatsAppMessageDocument();
        SmsMessageDocument sms = new SmsMessageDocument();
        when(messageRepository.findByDeliveryStatus(DeliveryStatusType.FAILED)).thenReturn(List.<MessageDocument>of(sms, whatsApp));

        List<WhatsAppMessageDocument> result = service.findByDeliveryStatus(DeliveryStatusType.FAILED);

        assertEquals(List.of(whatsApp), result);
    }
}
