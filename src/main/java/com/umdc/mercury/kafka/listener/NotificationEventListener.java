package com.umdc.mercury.kafka.listener;

import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationEventConsumerService notificationEventConsumerService;

    public NotificationEventListener(NotificationEventConsumerService notificationEventConsumerService) {
        this.notificationEventConsumerService = notificationEventConsumerService;
    }

    @KafkaListener(
            topics = "${umdc.consumer.topics.notification}",
            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
            containerFactory = "notificationEventKafkaListenerContainerFactory")
    public void listen(NotificationEventTO event) {
        if (Objects.isNull(event) || Objects.isNull(event.header()) || Objects.isNull(event.routing())) {
            logger.warn("Received null or incomplete NotificationEvent, discarding");
            return;
        }
        logger.info("Received NotificationEvent: messageId={}, channel={}",
                event.header().messageId(), event.routing().channel());
        notificationEventConsumerService.process(event);
    }
}
