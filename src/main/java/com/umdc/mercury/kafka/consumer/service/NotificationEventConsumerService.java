package com.umdc.mercury.kafka.consumer.service;

import com.umdc.mercury.kafka.to.NotificationEventTO;

public interface NotificationEventConsumerService {

    void process(NotificationEventTO event);
}
