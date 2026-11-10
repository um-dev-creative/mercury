package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.exception.MailNotFoundException;
import com.umdc.mercury.api.v1.service.RequestHashes;
import com.umdc.mercury.api.v1.service.TemplateDefinedService;
import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.MailTrackingDocument;
import com.umdc.mercury.jpa.nosql.document.MessageDocument;
import com.umdc.mercury.jpa.nosql.repository.MailTrackingNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.jpa.sql.repository.ChannelTypeRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The one door for every channel ({@code POST /api/v1/messages}, decision D-14 of zerobound-rest): checks the channel (a provider exists and the catalogue
 * has not switched it off), the recipient and the template, claims the {@code Idempotency-Key} of the caller, and has the channel's {@link ChannelProvider}
 * queue the message ({@code OPENED}). It answers at once; the delivery is asynchronous and belongs to the channel's scheduler. The caller's alias scopes the
 * key and the status lookups, and the tracking row (the same collection as the mail's) holds no recipient, content or parameter.
 */
@Service
public class MessageIngressService {

    private final ChannelRegistry registry;
    private final ChannelTypeRepository catalogue;
    private final TemplateDefinedService templates;
    private final MailTrackingNSRepository tracking;
    private final MessageNSRepository documents;

    public MessageIngressService(ChannelRegistry registry, ChannelTypeRepository catalogue, TemplateDefinedService templates,
                                 MailTrackingNSRepository tracking, MessageNSRepository documents) {
        this.registry = registry;
        this.catalogue = catalogue;
        this.templates = templates;
        this.tracking = tracking;
        this.documents = documents;
    }

    /**
     * Queues {@code request} for {@code caller}.
     *
     * @throws IllegalArgumentException unknown or disabled channel, invalid target, a field the channel needs, or an unknown template (400)
     * @throws IllegalStateException    the key was used with a different request (422)
     */
    public Accepted accept(MessageRequest request, String caller, String idempotencyKey) {
        ChannelProvider provider = provider(request.channel());
        String target = request.target().strip();
        if (!provider.validTarget(target)) {
            throw new IllegalArgumentException("The target is not valid for the " + provider.code() + " channel");
        }
        if (Objects.nonNull(request.templateDefinedId())) {
            templates.find(request.templateDefinedId());
        }
        UUID messageId = UUID.randomUUID();
        var message = new OutboundMessage(messageId, provider.code(), request.userId(), target, request.templateDefinedId(), request.language(), request.from(),
                request.subject(), request.body(), request.sendDate(), request.params(), request.options(), request.mediaUrl(), request.mediaType());
        String missing = provider.missingField(message);
        if (Objects.nonNull(missing)) {
            throw new IllegalArgumentException(missing);
        }
        String hash = hash(request, provider.code());
        boolean keyed = Objects.nonNull(idempotencyKey) && !idempotencyKey.isBlank();
        String claimId = keyed ? caller + ":" + idempotencyKey.trim() : messageId.toString();
        Instant now = Instant.now();
        try {
            tracking.insert(new MailTrackingDocument(claimId, messageId, caller, hash, DeliveryStatusType.OPENED, now, now));
        } catch (DuplicateKeyException e) {
            return replay(claimId, hash);
        }
        try {
            provider.queue(message);
        } catch (RuntimeException e) {
            tracking.deleteById(claimId);   // nothing was queued: let the caller retry with the same key
            throw e;
        }
        return new Accepted(new MessageResponse(messageId, DeliveryStatusType.OPENED.name(), "Queued"), false);
    }

    private ChannelProvider provider(String channel) {
        ChannelProvider provider = registry.find(channel)
                .orElseThrow(() -> new IllegalArgumentException("Unknown channel '" + channel + "'. Available: " + String.join(", ", channels())));
        if (isSwitchedOff(provider.code())) {
            throw new IllegalArgumentException("The " + provider.code() + " channel is disabled");
        }
        return provider;
    }

    /** A channel the catalogue lists as disabled or inactive is off; one it does not list keeps its provider's default (on). */
    private boolean isSwitchedOff(String code) {
        return catalogue.findByCode(code)
                .map(entity -> !Boolean.TRUE.equals(entity.getEnabled()) || Boolean.FALSE.equals(entity.getActive()))
                .orElse(false);
    }

    /** The channel codes callers may use now: a provider exists and the catalogue has not switched it off. */
    public List<String> channels() {
        return registry.codes().stream().filter(code -> !isSwitchedOff(code)).toList();
    }

    private Accepted replay(String claimId, String hash) {
        MailTrackingDocument first = tracking.findById(claimId)
                .orElseThrow(() -> new IllegalStateException("Idempotency-Key is being processed, retry shortly"));
        if (!first.requestHash().equals(hash)) {
            throw new IllegalStateException("Idempotency-Key was already used with a different request");
        }
        return new Accepted(new MessageResponse(first.messageId(), first.status().name(), "Already accepted"), true);
    }

    /** State of a message {@code caller} submitted: the channel's own document when it has one, else the tracking row. */
    public MailStatusResponse status(UUID messageId, String caller) {
        MailTrackingDocument row = tracking.findByMessageId(messageId)
                .filter(t -> Objects.equals(t.caller(), caller))
                .orElseThrow(() -> new MailNotFoundException("Message not found"));
        DeliveryStatusType state = documents.findById(messageId.toString()).map(MessageDocument::getDeliveryStatus).orElse(row.status());
        return new MailStatusResponse(row.messageId(), state, row.createdAt(), row.updatedAt());
    }

    /** SHA-256 over the whole request, so a reused key with another body is told apart (equal requests hash equal, whatever the order of the maps). */
    static String hash(MessageRequest r, String channel) {
        return RequestHashes.of(List.of(channel, String.valueOf(r.userId()), r.target().strip().toLowerCase(Locale.ROOT), String.valueOf(r.templateDefinedId()),
                String.valueOf(r.language()), String.valueOf(r.from()), String.valueOf(r.subject()), String.valueOf(r.body()), String.valueOf(r.sendDate()),
                Objects.requireNonNullElse(r.params(), Map.of()), Objects.requireNonNullElse(r.options(), Map.of()), String.valueOf(r.mediaUrl()), String.valueOf(r.mediaType())));
    }

    /** The answer plus whether it replays an earlier request. */
    public record Accepted(MessageResponse response, boolean replayed) {
    }
}
