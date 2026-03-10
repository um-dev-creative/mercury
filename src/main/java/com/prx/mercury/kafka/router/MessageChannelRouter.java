package com.prx.mercury.kafka.router;

import com.prx.mercury.kafka.consumer.service.EmailChannelService;
import com.prx.mercury.kafka.consumer.service.SmsChannelService;
import com.prx.mercury.kafka.consumer.service.TelegramChannelService;
import com.prx.mercury.kafka.consumer.service.WhatsAppChannelService;
import org.springframework.stereotype.Service;

@Service
public class MessageChannelRouter {

    private final EmailChannelService emailService;
    private final SmsChannelService smsService;
    private final TelegramChannelService telegramService;
    private final WhatsAppChannelService whatsAppChannelService;


    public MessageChannelRouter(EmailChannelService emailService, SmsChannelService smsService, TelegramChannelService telegramService, WhatsAppChannelService whatsAppChannelService) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.telegramService = telegramService;
        this.whatsAppChannelService = whatsAppChannelService;
    }
}
