package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.kafka.to.WhatsAppMessageTO;
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

