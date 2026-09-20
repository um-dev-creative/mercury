package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventConsumerServiceImpl implements NotificationEventConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumerServiceImpl.class);

    /**
     * Default constructor.
     */
    public NotificationEventConsumerServiceImpl() {
        // no-arg constructor required by AtLeastOneConstructor rule
    }

    @Override
    public void process(NotificationEventTO event) {
        String rawChannel = event.routing().channel();
        logger.info("Processing NotificationEvent: campaignId={}, messageId={}, channel={}, traceId={}",
                event.header().campaignId(), event.header().messageId(), rawChannel, event.header().traceId());

        ChannelType channelType;
        try {
            channelType = ChannelType.fromCode(rawChannel);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown channel type '{}' for messageId={}, discarding", rawChannel, event.header().messageId());
            return;
        }

        switch (channelType) {
            case EMAIL -> handleEmail(event);
            case SMS -> handleSms(event);
            case TELEGRAM -> handleTelegram(event);
            case WHATSAPP -> handleWhatsApp(event);
            case PUSH -> handlePush(event);
        }
    }

    private void handleEmail(NotificationEventTO event) {
        logger.info("Routing to EMAIL channel: recipient={}, templateId={}",
                event.routing().target_identifier(), event.content().templateId());
    }

    private void handleSms(NotificationEventTO event) {
        logger.info("Routing to SMS channel: recipient={}", event.routing().target_identifier());
    }

    private void handleTelegram(NotificationEventTO event) {
        logger.info("Routing to TELEGRAM channel: recipient={}", event.routing().target_identifier());
    }

    private void handleWhatsApp(NotificationEventTO event) {
        logger.info("Routing to WHATSAPP channel: recipient={}", event.routing().target_identifier());
    }

    private void handlePush(NotificationEventTO event) {
        logger.info("Routing to PUSH channel: recipient={}", event.routing().target_identifier());
    }
}
