package com.umdc.mercury.kafka.router;

import com.umdc.mercury.kafka.consumer.service.EmailChannelService;
import com.umdc.mercury.kafka.consumer.service.SmsChannelService;
import com.umdc.mercury.kafka.consumer.service.TelegramChannelService;
import com.umdc.mercury.kafka.consumer.service.WhatsAppChannelService;
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
