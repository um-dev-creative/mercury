package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.SmsMessageDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsChannelService implements ChannelService<SmsMessageDocument> {
    @Override
    public SmsMessageDocument send(SmsMessageDocument message, TemplateDefinedTO template) {
        return null;
    }

    @Override
    public void updateStatus(SmsMessageDocument message) {
        // TODO document why this method is empty
    }

    @Override
    public List<SmsMessageDocument> findByDeliveryStatus(DeliveryStatusType status) {
        return List.of();
    }
}
