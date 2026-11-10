package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Sms through the unified ingress; the target is a phone number in E.164 format (+34600112233). */
@Component
public class SmsChannelProvider extends DocumentChannelProvider {

    public SmsChannelProvider(NotificationEventConsumerService consumer, MessageNSRepository documents) {
        super(consumer, documents);
    }

    @Override
    public String code() {
        return "sms";
    }

    @Override
    public boolean validTarget(String target) {
        return Objects.nonNull(target) && E164_PHONE.matcher(target.strip()).matches();
    }
}
