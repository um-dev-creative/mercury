package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsChannelService implements ChannelService<SmsMessageDocument> {

    private SmsMessageDocument  smsMessageDocument;
    private TemplateDefinedTO templateDefinedTO;

    public SmsChannelService() {
        // Default constructor
    }
    @Override
    public SmsMessageDocument send(SmsMessageDocument message, TemplateDefinedTO template) {
        this.smsMessageDocument =message;
        this.templateDefinedTO = template;
        return null;
    }

    @Override
    public void updateStatus(SmsMessageDocument message) {
        this.smsMessageDocument =message;
    }

    @Override
    public List<SmsMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return List.of();
    }
}
