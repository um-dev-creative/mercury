package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Telegram through the unified ingress; the target is a numeric chat id. */
@Component
public class TelegramChannelProvider extends DocumentChannelProvider {

    public TelegramChannelProvider(NotificationEventConsumerService consumer, MessageNSRepository documents) {
        super(consumer, documents);
    }

    @Override
    public String code() {
        return "telegram";
    }

    @Override
    public boolean validTarget(String target) {
        return Objects.nonNull(target) && CHAT_ID.matcher(target.strip()).matches();
    }
}
