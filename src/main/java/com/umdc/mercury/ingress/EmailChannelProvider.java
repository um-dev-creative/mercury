package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** E-mail through the unified ingress: queues the same {@link EmailMessageDocument} {@code POST /api/v1/mail} does, which the e-mail scheduler sends. */
@Component
public class EmailChannelProvider implements ChannelProvider {

    private static final Pattern ADDRESS = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmailMessageNSRepository messages;

    public EmailChannelProvider(EmailMessageNSRepository messages) {
        this.messages = messages;
    }

    @Override
    public String code() {
        return "email";
    }

    @Override
    public boolean validTarget(String target) {
        return Objects.nonNull(target) && ADDRESS.matcher(target.strip()).matches();
    }

    @Override
    public String missingField(OutboundMessage message) {
        if (Objects.isNull(message.templateId())) {
            return "templateDefinedId is required for email";
        }
        if (Objects.isNull(message.from()) || message.from().isBlank()) {
            return "from is required for email";
        }
        if (Objects.isNull(message.subject()) || message.subject().isBlank()) {
            return "subject is required for email";
        }
        return null;
    }

    @Override
    public void queue(OutboundMessage message) {
        String address = message.target().strip();
        messages.save(new EmailMessageDocument(null, message.id(), message.templateId(), message.userId(), message.from(),
                List.of(new EmailContact(address, address, address)), List.of(), message.subject(),
                Objects.requireNonNullElse(message.body(), "email"), message.sendDate(), message.params(), DeliveryStatusType.OPENED));
    }
}
