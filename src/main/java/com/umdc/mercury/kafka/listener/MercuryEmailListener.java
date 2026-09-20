package com.umdc.mercury.kafka.listener;

import com.umdc.mercury.kafka.consumer.service.EmailMessageConsumerService;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MercuryEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(MercuryEmailListener.class);

    private final EmailMessageConsumerService emailMessageConsumerService;

    /**
     * MessageListener.
     */
    public MercuryEmailListener(EmailMessageConsumerService emailMessageConsumerService) {
        this.emailMessageConsumerService = emailMessageConsumerService;
    }

    @KafkaListener(
            topics = "${umdc.consumer.topics.email}",
            groupId = "${umdc.consumer.group-id:mercury-multi-channel}",
            containerFactory = "emailMessageKafkaListenerContainerFactory")
    public void listenerEmailTopic(EmailMessageTO message) {
        if (Objects.isNull(message)) {
            logger.warn("Received null Email message, discarding");
            return;
        }
        if (Objects.nonNull(message.sendDate()) && Objects.nonNull(message.to())
                && Objects.nonNull(message.templateDefinedId()) && Objects.nonNull(message.userId())
                && Objects.nonNull(message.from())) {
            emailMessageConsumerService.save(message);
            logger.info("Email message saved for processing. templateDefinedId={}, userId={}",
                    message.templateDefinedId(), message.userId());
        } else {
            logger.warn("Received incomplete Email message, discarding: {}", message);
        }
    }
}
