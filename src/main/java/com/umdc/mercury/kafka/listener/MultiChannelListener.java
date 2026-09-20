package com.umdc.mercury.kafka.listener;

import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.kafka.router.MessageChannelRouter;
import com.umdc.mercury.kafka.to.WhatsAppMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka entry points for the SMS, Telegram and WhatsApp channels.
 *
 * <p>Email is consumed separately by {@link MercuryEmailListener}; it is not part of
 * this listener because it already has its own dedicated persistence path.</p>
 */
@Service
public class MultiChannelListener {

    private static final Logger logger = LoggerFactory.getLogger(MultiChannelListener.class);

    private final MessageChannelRouter messageChannelRouter;

    public MultiChannelListener(MessageChannelRouter messageChannelRouter) {
        this.messageChannelRouter = messageChannelRouter;
    }

    @KafkaListener(
            topics = "${umdc.consumer.topics.sms}",
            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
            containerFactory = "smsMessageKafkaListenerContainerFactory")
    public void handleSms(SmsMessageDocument message) {
        logger.info("Received SMS message. phoneNumber={}, campaignId={}", message.getPhoneNumber(), message.getCampaignId());
        messageChannelRouter.routeSms(message);
    }

    @KafkaListener(
            topics = "${umdc.consumer.topics.telegram}",
            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
            containerFactory = "telegramMessageKafkaListenerContainerFactory")
    public void handleTelegram(TelegramMessageDocument message) {
        logger.info("Received Telegram message. chatId={}, campaignId={}", message.getChatId(), message.getCampaignId());
        messageChannelRouter.routeTelegram(message);
    }

    @KafkaListener(
            topics = "${umdc.consumer.topics.whatsapp}",
            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
            containerFactory = "whatsAppMessageKafkaListenerContainerFactory")
    public void handleWhatsApp(WhatsAppMessageTO message) {
        logger.info("Received WhatsApp message. phoneNumber={}, campaignId={}", message.phoneNumber(), message.campaignId());
        messageChannelRouter.routeWhatsApp(message);
    }
}
