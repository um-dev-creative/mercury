package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import java.util.List;

public interface ChannelService<T> {

    T send(T message, TemplateDefinedTO template);

    void updateStatus(T message);

    List<T> findByDeliveryStatus(DeliveryStatusType status);
}

