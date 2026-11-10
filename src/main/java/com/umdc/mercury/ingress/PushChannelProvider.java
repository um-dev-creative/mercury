package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Push notifications through the unified ingress; the target is the device token (or topic) of the platform. */
@Component
public class PushChannelProvider extends DocumentChannelProvider {

    public PushChannelProvider(NotificationEventConsumerService consumer, MessageNSRepository documents) {
        super(consumer, documents);
    }

    @Override
    public String code() {
        return "push";
    }

    @Override
    public boolean validTarget(String target) {
        return Objects.nonNull(target) && !target.isBlank();
    }
}
