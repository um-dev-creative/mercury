package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.EmailMessageDocument;
import com.prx.mercury.jpa.nosql.document.SmsMessageDocument;
import com.prx.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.prx.mercury.kafka.to.WhatsAppMessageTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Channel Services basic behavior")
class ChannelServicesTest {

    @Test
    @DisplayName("EmailChannelService returns null on send and empty list on find")
    void emailChannelServiceBasicFlow() {
        EmailChannelService service = new EmailChannelService();
        EmailMessageDocument message = new EmailMessageDocument(
                "1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "from@test.com",
                List.of(), List.of(), "subject", "body", LocalDateTime.now(), Map.of(), DeliveryStatusType.PENDING
        );

        assertNull(service.send(message, null));
        service.updateStatus(message);
        assertTrue(service.findByDeliveryStatus(DeliveryStatusType.SENT).isEmpty());
    }

    @Test
    @DisplayName("SmsChannelService handles send update and find without exceptions")
    void smsChannelServiceBasicFlow() {
        SmsChannelService service = new SmsChannelService();
        SmsMessageDocument message = new SmsMessageDocument();
        message.setPhoneNumber("+15550001111");
        message.setMessage("hello");
        message.setDeliveryStatus(DeliveryStatusType.PENDING);

        assertNull(service.send(message, null));
        service.updateStatus(message);
        assertTrue(service.findByDeliveryStatus(DeliveryStatusType.OPENED).isEmpty());
    }

    @Test
    @DisplayName("TelegramChannelService handles send update and find without exceptions")
    void telegramChannelServiceBasicFlow() {
        TelegramChannelService service = new TelegramChannelService();
        TelegramMessageDocument message = new TelegramMessageDocument();
        message.setChatId(12345L);
        message.setMessage("hello");
        message.setDeliveryStatus(DeliveryStatusType.PENDING);

        assertNull(service.send(message, null));
        service.updateStatus(message);
        assertTrue(service.findByDeliveryStatus(DeliveryStatusType.DELIVERED).isEmpty());
    }

    @Test
    @DisplayName("WhatsAppChannelService handles send update and find without exceptions")
    void whatsAppChannelServiceBasicFlow() {
        WhatsAppChannelService service = new WhatsAppChannelService();
        WhatsAppMessageTO message = new WhatsAppMessageTO(
                UUID.randomUUID(), UUID.randomUUID(), "+15550001111", "text", "hello",
                null, null, List.of(), null, null, LocalDateTime.now(), Map.of(), UUID.randomUUID()
        );

        assertNull(service.send(message, null));
        service.updateStatus(message);
        assertTrue(service.findByDeliveryStatus(DeliveryStatusType.FAILED).isEmpty());
    }
}
