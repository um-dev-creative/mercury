package com.prx.mercury.kafka.listener;

import com.prx.mercury.kafka.to.EmailMessageTO;
import com.prx.mercury.kafka.to.SmsMessageTO;
import com.prx.mercury.kafka.to.TelegramMessageTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MultiChannelListener {

    @KafkaListener(topics = "${prx.consumer.topics.email}")
    public void handleEmail(EmailMessageTO message) {
        /* TODO document why this method is empty */
    }

    @KafkaListener(topics = "${prx.consumer.topics.sms}")
    public void handleSms(SmsMessageTO message) {
        /* TODO document why this method is empty */
    }

    @KafkaListener(topics = "${prx.consumer.topics.telegram}")
    public void handleTelegram(TelegramMessageTO message) {
        /* TODO document why this method is empty */
    }
}
