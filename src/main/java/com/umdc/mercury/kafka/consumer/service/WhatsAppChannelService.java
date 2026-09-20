package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.WhatsAppMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WhatsAppChannelService implements ChannelService<WhatsAppMessageDocument> {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppChannelService.class);

    private final MessageNSRepository messageRepository;

    public WhatsAppChannelService(MessageNSRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Persists the WhatsApp message. Actual delivery through the WhatsApp Business
     * (Cloud API) is not yet integrated; this stores the message so it can be picked
     * up once a provider is wired in.
     */
    @Override
    public WhatsAppMessageDocument send(WhatsAppMessageDocument message, TemplateDefinedTO template) {
        logger.info("Persisting WhatsApp message. phoneNumber={}, templateId={}, campaignId={}",
                message.getPhoneNumber(), template != null ? template.id() : null, message.getCampaignId());
        return messageRepository.save(message);
    }

    @Override
    public void updateStatus(WhatsAppMessageDocument message) {
        logger.debug("Updating WhatsApp message status. id={}, status={}", message.getId(), message.getDeliveryStatus());
        messageRepository.save(message);
    }

    @Override
    public List<WhatsAppMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return messageRepository.findByDeliveryStatus(status).stream()
                .filter(WhatsAppMessageDocument.class::isInstance)
                .map(WhatsAppMessageDocument.class::cast)
                .toList();
    }
}
