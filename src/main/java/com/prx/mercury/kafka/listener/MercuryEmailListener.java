package com.prx.mercury.kafka.listener;

import com.prx.mercury.kafka.consumer.EmailMessageConsumerService;
import com.prx.mercury.kafka.to.EmailMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

@Service
public class MercuryEmailListener {
    private static final String TOPIC = "emails-messages";
    private final Logger logger = LoggerFactory.getLogger(MercuryEmailListener.class);
    private final CountDownLatch latch = new CountDownLatch(3);
    private final CountDownLatch partitionLatch = new CountDownLatch(2);
    private final CountDownLatch filterLatch = new CountDownLatch(2);
    private final CountDownLatch emailMessageLatch = new CountDownLatch(1);
    private final EmailMessageConsumerService emailMessageConsumerService;

    /**
     * MessageListener.
     */
    public MercuryEmailListener(EmailMessageConsumerService emailMessageConsumerService) {
        this.emailMessageConsumerService = emailMessageConsumerService;
    }

    @KafkaListener(topics = "${prx.consumer.mercury.topic}", groupId = TOPIC, containerFactory = "emailMessageKafkaListenerContainerFactory")
    public void listenerEmailTopic(EmailMessageTO message) {
        logger.info("Received Email Message in group '{}': {}", TOPIC, message);
        if (Objects.nonNull(message) && Objects.nonNull(message.sendDate()) && Objects.nonNull(message.to())
                && Objects.nonNull(message.templateDefinedId()) && Objects.nonNull(message.userId()) && Objects.nonNull(message.from())
        ) {
            emailMessageConsumerService.save(message);
            logger.info("Sending Email Message: {}", message);
        } else {
            logger.info("Received Email Message in group '{}': NULL", TOPIC);
        }
        emailMessageLatch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public CountDownLatch getPartitionLatch() {
        return partitionLatch;
    }

    public CountDownLatch getFilterLatch() {
        return filterLatch;
    }

    public CountDownLatch getEmailMessageLatch() {
        return emailMessageLatch;
    }
}
