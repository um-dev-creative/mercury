package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.TelegramMessageDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelegramChannelService implements ChannelService<TelegramMessageDocument> {

    private TelegramMessageDocument telegramMessageDocument;
    private TemplateDefinedTO templateDefinedTO;

    public TelegramChannelService() {
        //Default constructor
    }

    @Override
    public TelegramMessageDocument send(TelegramMessageDocument message, TemplateDefinedTO template) {
        this.telegramMessageDocument =message;
        this.templateDefinedTO = template;
        return null;
    }

    @Override
    public void updateStatus(TelegramMessageDocument message) {
        // TODO document why this method is empty
        this.telegramMessageDocument =message;
    }

    @Override
    public List<TelegramMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return List.of();
    }
}
