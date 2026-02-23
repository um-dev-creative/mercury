package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.EmailMessageDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailChannelService implements ChannelService<EmailMessageDocument> {

    @Override
    public EmailMessageDocument send(EmailMessageDocument message, TemplateDefinedTO template) {
        return null;
    }

    @Override
    public void updateStatus(EmailMessageDocument message) {
        // TODO document why this method is empty
    }

    @Override
    public List<EmailMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return List.of();
    }
}
