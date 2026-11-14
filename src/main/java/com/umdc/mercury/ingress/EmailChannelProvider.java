package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * E-mail through the unified ingress: queues the same {@link EmailMessageDocument} {@code POST /api/v1/mail} does, which the e-mail scheduler sends.
 * The subject is the one the <b>template</b> defines (edited from Iris, rendered with the message parameters); a {@code subject} sent in the request
 * overrides it, and a message with neither is refused.
 */
@Component
public class EmailChannelProvider implements ChannelProvider {

    private static final Pattern ADDRESS = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmailMessageNSRepository messages;
    private final TemplateDefinedRepository definitions;
    private final SubjectRenderer subjects;

    public EmailChannelProvider(EmailMessageNSRepository messages, TemplateDefinedRepository definitions, SubjectRenderer subjects) {
        this.messages = messages;
        this.definitions = definitions;
        this.subjects = subjects;
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
            return "a template (template + applicationId + language, or templateDefinedId) is required for email";
        }
        if (Objects.isNull(message.from()) || message.from().isBlank()) {
            return "from is required for email";
        }
        if (isBlank(message.subject()) && templateSubject(message).isEmpty()) {
            return "subject is required for email: send it or define it in the template";
        }
        return null;
    }

    @Override
    public void queue(OutboundMessage message) {
        String address = message.target().strip();
        String subject = isBlank(message.subject()) ? subjects.render(templateSubject(message).orElseThrow(), message.params()) : message.subject();
        messages.save(new EmailMessageDocument(null, message.id(), message.templateId(), message.userId(), message.from(),
                List.of(new EmailContact(address, address, address)), List.of(), subject,
                Objects.requireNonNullElse(message.body(), "email"), message.sendDate(), message.params(), DeliveryStatusType.OPENED));
    }

    /** The subject of the template the message goes with, when it defines one. */
    private Optional<String> templateSubject(OutboundMessage message) {
        return definitions.findById(message.templateId())
                .map(definition -> definition.getTemplate())
                .map(template -> template.getSubject())
                .filter(text -> !text.isBlank());
    }

    private static boolean isBlank(String text) {
        return Objects.isNull(text) || text.isBlank();
    }
}
