package com.prx.mercury.kafka.listener;

import com.prx.mercury.kafka.to.EmailMessageTO;
import com.prx.mercury.kafka.to.SmsMessageTO;
import com.prx.mercury.kafka.to.TelegramMessageTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MultiChannelListener {

    private static final String CONTAINER_FACTORY = "emailMessageKafkaListenerContainerFactory";

    @KafkaListener(
            topics = "${prx.consumer.topics.email}",
            groupId = "${prx.consumer.group-id:mercury-multi-channel}",
            containerFactory = CONTAINER_FACTORY)
    public void handleEmail(EmailMessageTO message) {
        /* TODO document why this method is empty */
    }

    @KafkaListener(
            topics = "${prx.consumer.topics.sms}",
            groupId = "${prx.consumer.group-id:mercury-multi-channel}",
            containerFactory = CONTAINER_FACTORY)
    public void handleSms(SmsMessageTO message) {
        /* TODO document why this method is empty */
    }

    @KafkaListener(
            topics = "${prx.consumer.topics.telegram}",
            groupId = "${prx.consumer.group-id:mercury-multi-channel}",
            containerFactory = CONTAINER_FACTORY)
    public void handleTelegram(TelegramMessageTO message) {
        /* TODO document why this method is empty */
    }
}
