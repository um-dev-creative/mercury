package com.prx.mercury.kafka.router;

import com.prx.mercury.kafka.consumer.service.EmailChannelService;
import com.prx.mercury.kafka.consumer.service.SmsChannelService;
import com.prx.mercury.kafka.consumer.service.TelegramChannelService;
import com.prx.mercury.kafka.consumer.service.WhatsAppChannelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("MessageChannelRouter constructor wiring")
class MessageChannelRouterTest {

    @Test
    @DisplayName("Constructor stores all channel services")
    void constructorStoresAllDependencies() throws Exception {
        EmailChannelService email = new EmailChannelService();
        SmsChannelService sms = new SmsChannelService();
        TelegramChannelService telegram = new TelegramChannelService();
        WhatsAppChannelService whatsapp = new WhatsAppChannelService();

        MessageChannelRouter router = new MessageChannelRouter(email, sms, telegram, whatsapp);

        assertSame(email, readField(router, "emailService"));
        assertSame(sms, readField(router, "smsService"));
        assertSame(telegram, readField(router, "telegramService"));
        assertSame(whatsapp, readField(router, "whatsAppChannelService"));
        assertNotNull(router);
    }

    private Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}

