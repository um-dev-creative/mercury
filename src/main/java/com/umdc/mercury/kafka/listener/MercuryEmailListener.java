package com.umdc.mercury.kafka.listener;

import com.umdc.mercury.kafka.consumer.service.EmailMessageConsumerService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;

@Service
public class MercuryEmailListener {
//    private static final String TOPIC = "mercury-multi-channel";
//    private final Logger logger = LoggerFactory.getLogger(MercuryEmailListener.class);
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

//    @KafkaListener(topics = "${umdc.consumer.mercury.topic}", groupId = TOPIC, containerFactory = "emailMessageKafkaListenerContainerFactory")
//    public void listenerEmailTopic(EmailMessageTO message) {
//        logger.info("Received Email Message in group '{}': {}", TOPIC, message);
//        if (Objects.nonNull(message) && Objects.nonNull(message.sendDate()) && Objects.nonNull(message.to())
//                && Objects.nonNull(message.templateDefinedId()) && Objects.nonNull(message.userId()) && Objects.nonNull(message.from())
//        ) {
//            emailMessageConsumerService.save(message);
//            logger.info("Sending Email Message: {}", message);
//        } else {
//            logger.info("Received Email Message in group '{}': NULL", TOPIC);
//        }
//        emailMessageLatch.countDown();
//    }

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
