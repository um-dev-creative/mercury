package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.MessageDocument;

import java.util.List;

public interface ChannelService<T extends MessageDocument> {

    T send(T message, TemplateDefinedTO template);

    void updateStatus(T message);

    List<T> findByDeliveryStatus(DeliveryStatusType status);
}

