package com.prx.mercury.kafka.consumer.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.constant.DeliveryStatusType;
import java.util.List;

public interface ChannelService<T> {

    T send(T message, TemplateDefinedTO template);

    void updateStatus(T message);

    List<T> findByDeliveryStatus(DeliveryStatusType status);
}

