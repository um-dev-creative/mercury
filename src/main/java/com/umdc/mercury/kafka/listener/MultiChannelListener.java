package com.umdc.mercury.kafka.listener;

import org.springframework.stereotype.Service;

@Service
public class MultiChannelListener {

    private static final String CONTAINER_FACTORY = "emailMessageKafkaListenerContainerFactory";

//    @KafkaListener(
//            topics = "${umdc.consumer.topics.email}",
//            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
//            containerFactory = CONTAINER_FACTORY)
//    public void handleEmail(EmailMessageTO message) {
//        /* TODO document why this method is empty */
//    }
//
//    @KafkaListener(
//            topics = "${umdc.consumer.topics.sms}",
//            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
//            containerFactory = CONTAINER_FACTORY)
//    public void handleSms(SmsMessageTO message) {
//        /* TODO document why this method is empty */
//    }
//
//    @KafkaListener(
//            topics = "${umdc.consumer.topics.telegram}",
//            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
//            containerFactory = CONTAINER_FACTORY)
//    public void handleTelegram(TelegramMessageTO message) {
//        /* TODO document why this method is empty */
//    }
}
