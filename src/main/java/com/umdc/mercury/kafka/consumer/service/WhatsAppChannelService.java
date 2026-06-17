package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.kafka.to.WhatsAppMessageTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WhatsAppChannelService implements ChannelService<WhatsAppMessageTO> {

    private WhatsAppMessageTO whatsAppMessageTO;
    private TemplateDefinedTO templateDefinedTO;

    public WhatsAppChannelService() {
        // Default constructor
    }

    @Override
    public WhatsAppMessageTO send(WhatsAppMessageTO message, TemplateDefinedTO template) {
        this.whatsAppMessageTO = message;
        this.templateDefinedTO = template;
        return null;
    }

    @Override
    public void updateStatus(WhatsAppMessageTO message) {
        this.whatsAppMessageTO = message;
    }

    @Override
    public List<WhatsAppMessageTO> findByDeliveryStatus(DeliveryStatusType status) {
        return List.of();
    }
}

