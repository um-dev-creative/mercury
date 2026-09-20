package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsChannelService implements ChannelService<SmsMessageDocument> {

    private static final Logger logger = LoggerFactory.getLogger(SmsChannelService.class);

    private final MessageNSRepository messageRepository;

    public SmsChannelService(MessageNSRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Persists the SMS message. Actual delivery through an SMS gateway (e.g. Twilio)
     * is not yet integrated; this stores the message so it can be picked up once a
     * provider is wired in.
     */
    @Override
    public SmsMessageDocument send(SmsMessageDocument message, TemplateDefinedTO template) {
        logger.info("Persisting SMS message. phoneNumber={}, templateId={}, campaignId={}",
                message.getPhoneNumber(), template != null ? template.id() : null, message.getCampaignId());
        return messageRepository.save(message);
    }

    @Override
    public void updateStatus(SmsMessageDocument message) {
        logger.debug("Updating SMS message status. id={}, status={}", message.getId(), message.getDeliveryStatus());
        messageRepository.save(message);
    }

    @Override
    public List<SmsMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return messageRepository.findByDeliveryStatus(status).stream()
                .filter(SmsMessageDocument.class::isInstance)
                .map(SmsMessageDocument.class::cast)
                .toList();
    }
}
