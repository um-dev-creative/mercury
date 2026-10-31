package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushChannelService implements ChannelService<PushNotificationMessageDocument> {

    private static final Logger logger = LoggerFactory.getLogger(PushChannelService.class);

    private final MessageNSRepository messageRepository;

    public PushChannelService(MessageNSRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Persists the push notification. Actual delivery through a push provider
     * (e.g. FCM/APNs) is not yet integrated; this stores the message so it can
     * be picked up once a provider is wired in.
     */
    @Override
    public PushNotificationMessageDocument send(PushNotificationMessageDocument message, TemplateDefinedTO template) {
        logger.info("Persisting Push message. platform={}, templateId={}, campaignId={}",
                message.getPlatform(), template != null ? template.id() : null, message.getCampaignId());
        return messageRepository.save(message);
    }

    @Override
    public void updateStatus(PushNotificationMessageDocument message) {
        logger.debug("Updating Push message status. id={}, status={}", message.getId(), message.getDeliveryStatus());
        messageRepository.save(message);
    }

    @Override
    public List<PushNotificationMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return messageRepository.findByDeliveryStatus(status).stream()
                .filter(PushNotificationMessageDocument.class::isInstance)
                .map(PushNotificationMessageDocument.class::cast)
                .toList();
    }
}
