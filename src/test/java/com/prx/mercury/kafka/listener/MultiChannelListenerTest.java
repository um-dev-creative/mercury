package com.prx.mercury.kafka.listener;

import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.kafka.listener.MultiChannelListener;
import com.umdc.mercury.kafka.router.MessageChannelRouter;
import com.umdc.mercury.kafka.to.PushNotificationMessageTO;
import com.umdc.mercury.kafka.to.WhatsAppMessageTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiChannelListener unit tests")
class MultiChannelListenerTest {

    @Mock
    private MessageChannelRouter messageChannelRouter;

    @InjectMocks
    private MultiChannelListener multiChannelListener;

    @Test
    @DisplayName("handleSms delegates the message to the router")
    void handleSms_delegatesToRouter() {
        SmsMessageDocument message = new SmsMessageDocument();
        message.setPhoneNumber("+15550001111");

        multiChannelListener.handleSms(message);

        verify(messageChannelRouter).routeSms(message);
    }

    @Test
    @DisplayName("handleTelegram delegates the message to the router")
    void handleTelegram_delegatesToRouter() {
        TelegramMessageDocument message = new TelegramMessageDocument();
        message.setChatId(12345L);

        multiChannelListener.handleTelegram(message);

        verify(messageChannelRouter).routeTelegram(message);
    }

    @Test
    @DisplayName("handleWhatsApp delegates the message to the router")
    void handleWhatsApp_delegatesToRouter() {
        WhatsAppMessageTO message = new WhatsAppMessageTO(
                UUID.randomUUID(), UUID.randomUUID(), "+15550001111", "text", "hello",
                null, null, null, null, null, LocalDateTime.now(), Map.of(), UUID.randomUUID());

        multiChannelListener.handleWhatsApp(message);

        verify(messageChannelRouter).routeWhatsApp(message);
    }

    @Test
    @DisplayName("handlePush delegates the message to the router")
    void handlePush_delegatesToRouter() {
        PushNotificationMessageTO message = new PushNotificationMessageTO(
                UUID.randomUUID(), UUID.randomUUID(), "device-token-1", "android", "Welcome", "Hello there",
                null, null, null, null, null, null, LocalDateTime.now(), Map.of(), UUID.randomUUID());

        multiChannelListener.handlePush(message);

        verify(messageChannelRouter).routePush(message);
    }
}
