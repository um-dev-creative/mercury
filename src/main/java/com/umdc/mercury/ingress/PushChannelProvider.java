package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import com.umdc.mercury.kafka.to.NotificationEventTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Push notifications through the unified ingress; the target is the Expo push token of the device (the delivery is
 * {@link com.umdc.mercury.provider.ExpoPushDispatcher}'s).
 *
 * <p>The title of the notification is, in this order: {@code options.title}, the request {@code subject}, the subject the template defines (rendered
 * with the message parameters, like the e-mail's). {@code options.data} (a JSON object, ids only) is delivered to the app with the notification.</p>
 */
@Component
public class PushChannelProvider extends DocumentChannelProvider {

    private final TemplateDefinedRepository definitions;
    private final SubjectRenderer subjects;

    public PushChannelProvider(NotificationEventConsumerService consumer, MessageNSRepository documents, TemplateDefinedRepository definitions,
                               SubjectRenderer subjects) {
        super(consumer, documents);
        this.definitions = definitions;
        this.subjects = subjects;
    }

    @Override
    NotificationEventTO event(OutboundMessage message) {
        NotificationEventTO event = super.event(message);
        Optional<String> title = title(message);
        if (title.isEmpty()) {
            return event;
        }
        var instructions = new LinkedHashMap<String, Object>(event.payload().delivery_instructions());
        instructions.put("title", title.get());
        return new NotificationEventTO(event.header(), event.routing(), event.content(), new NotificationEventTO.Payload(event.payload().context(), instructions));
    }

    /** The title to add, or empty when {@code options.title} already says it or there is none. */
    private Optional<String> title(OutboundMessage message) {
        if (Objects.nonNull(message.options()) && message.options().get("title") instanceof String given && !given.isBlank()) {
            return Optional.empty();
        }
        if (Objects.nonNull(message.subject()) && !message.subject().isBlank()) {
            return Optional.of(message.subject().strip());
        }
        if (Objects.isNull(message.templateId())) {
            return Optional.empty();
        }
        return definitions.findById(message.templateId())
                .map(definition -> definition.getTemplate())
                .map(template -> template.getSubject())
                .filter(text -> !text.isBlank())
                .map(text -> subjects.render(text, message.params()))
                .filter(text -> !text.isBlank());
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
