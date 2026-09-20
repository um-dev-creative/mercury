package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelegramChannelService implements ChannelService<TelegramMessageDocument> {

    private static final Logger logger = LoggerFactory.getLogger(TelegramChannelService.class);

    private final MessageNSRepository messageRepository;

    public TelegramChannelService(MessageNSRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Persists the Telegram message. Actual delivery through the Telegram Bot API
     * is not yet integrated; this stores the message so it can be picked up once a
     * provider is wired in.
     */
    @Override
    public TelegramMessageDocument send(TelegramMessageDocument message, TemplateDefinedTO template) {
        logger.info("Persisting Telegram message. chatId={}, templateId={}, campaignId={}",
                message.getChatId(), template != null ? template.id() : null, message.getCampaignId());
        return messageRepository.save(message);
    }

    @Override
    public void updateStatus(TelegramMessageDocument message) {
        logger.debug("Updating Telegram message status. id={}, status={}", message.getId(), message.getDeliveryStatus());
        messageRepository.save(message);
    }

    @Override
    public List<TelegramMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return messageRepository.findByDeliveryStatus(status).stream()
                .filter(TelegramMessageDocument.class::isInstance)
                .map(TelegramMessageDocument.class::cast)
                .toList();
    }
}
