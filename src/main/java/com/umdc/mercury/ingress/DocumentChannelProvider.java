package com.umdc.mercury.ingress;

import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.kafka.consumer.service.NotificationEventConsumerService;
import com.umdc.mercury.kafka.to.NotificationEventTO;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Base of the channels that mercury already stores as a {@link MessageDocument} (SMS, WhatsApp, Telegram, Push): the message becomes the same
 * {@link NotificationEventTO} the unified Kafka topic carries and goes through the same {@link NotificationEventConsumerService}, so a message
 * has one path whether it arrives over HTTP or over Kafka. The delivery state is the document's.
 */
abstract class DocumentChannelProvider implements ChannelProvider {

    static final Pattern E164_PHONE = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    static final Pattern CHAT_ID = Pattern.compile("^-?\\d{1,18}$");

    private final NotificationEventConsumerService consumer;
    private final MessageNSRepository documents;

    DocumentChannelProvider(NotificationEventConsumerService consumer, MessageNSRepository documents) {
        this.consumer = consumer;
        this.documents = documents;
    }

    @Override
    public void queue(OutboundMessage message) {
        consumer.process(event(message));
        if (!documents.existsById(message.id().toString())) {
            // the consumer discards what it cannot store without telling the caller: an HTTP caller must know
            throw new IllegalArgumentException("The " + code() + " message was not accepted");
        }
    }

    /** The unified-topic event of {@code message}. */
    NotificationEventTO event(OutboundMessage message) {
        Instant when = Objects.nonNull(message.sendDate()) ? message.sendDate().toInstant(ZoneOffset.UTC) : Instant.now();
        var header = new NotificationEventTO.Header(null, message.id().toString(), when.toString(), null);
        var routing = new NotificationEventTO.Routing(code(), Objects.toString(message.userId(), null), message.target().strip());
        var content = new NotificationEventTO.Content(Objects.toString(message.templateId(), null), null, message.language(), message.body(), message.mediaUrl(),
                Objects.requireNonNullElse(message.mediaType(), "NONE"));
        var payload = new NotificationEventTO.Payload(Objects.requireNonNullElse(message.params(), Map.of()), Objects.requireNonNullElse(message.options(), Map.of()));
        return new NotificationEventTO(header, routing, content, payload);
    }
}
