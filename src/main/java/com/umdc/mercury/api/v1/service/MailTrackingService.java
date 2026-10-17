package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.MailNotFoundException;
import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.api.v1.to.SendEmailResponse;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.document.MailTrackingDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MailTrackingNSRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Accepts the mail of {@code POST /api/v1/mail}: checks the template, queues the message for the
 * scheduler ({@code OPENED}) and keeps the delivery log. With an {@code Idempotency-Key} the same
 * request is accepted once (a replay answers the first message); the same key with another request is
 * refused. The caller's alias scopes both the key and the status lookups.
 */
@Service
public class MailTrackingService {

    private final EmailMessageNSRepository messages;
    private final MailTrackingNSRepository tracking;
    private final TemplateDefinedService templates;

    public MailTrackingService(EmailMessageNSRepository messages, MailTrackingNSRepository tracking,
                               TemplateDefinedService templates) {
        this.messages = messages;
        this.tracking = tracking;
        this.templates = templates;
    }

    /**
     * Queues {@code request} for {@code caller}.
     *
     * @throws IllegalArgumentException the template does not exist (400)
     * @throws IllegalStateException    the key was used with a different request (422)
     */
    public Accepted accept(SendEmailRequest request, String caller, String idempotencyKey) {
        templates.find(request.templateDefinedId());
        String hash = hash(request);
        boolean keyed = Objects.nonNull(idempotencyKey) && !idempotencyKey.isBlank();
        UUID messageId = UUID.randomUUID();
        String claimId = keyed ? caller + ":" + idempotencyKey.trim() : messageId.toString();
        Instant now = Instant.now();
        var claim = new MailTrackingDocument(claimId, messageId, caller, hash, DeliveryStatusType.OPENED, now, now);
        try {
            tracking.insert(claim);
        } catch (DuplicateKeyException e) {
            return replay(claimId, hash);
        }
        try {
            messages.save(new EmailMessageDocument(null, messageId, request.templateDefinedId(), request.userId(),
                    request.from(), request.to(), request.cc(), request.subject(), request.body(),
                    request.sendDate(), request.params(), DeliveryStatusType.OPENED));
        } catch (RuntimeException e) {
            tracking.deleteById(claimId);   // nothing was queued: let the caller retry with the same key
            throw e;
        }
        return new Accepted(new SendEmailResponse(messageId, DeliveryStatusType.OPENED.name(), "Queued"), false);
    }

    private Accepted replay(String claimId, String hash) {
        MailTrackingDocument first = tracking.findById(claimId)
                .orElseThrow(() -> new IllegalStateException("Idempotency-Key is being processed, retry shortly"));
        if (!first.requestHash().equals(hash)) {
            throw new IllegalStateException("Idempotency-Key was already used with a different request");
        }
        return new Accepted(new SendEmailResponse(first.messageId(), first.status().name(),
                "Already accepted"), true);
    }

    /** State of a mail {@code caller} submitted. */
    public MailStatusResponse status(UUID messageId, String caller) {
        return tracking.findByMessageId(messageId)
                .filter(t -> Objects.equals(t.caller(), caller))
                .map(t -> new MailStatusResponse(t.messageId(), t.status(), t.createdAt(), t.updatedAt()))
                .orElseThrow(() -> new MailNotFoundException("Mail not found"));
    }

    /** Records the state the scheduler reached; a mail that was not submitted through the API is ignored. */
    public void recordStatus(UUID messageId, DeliveryStatusType status) {
        if (Objects.isNull(messageId) || Objects.isNull(status)) {
            return;
        }
        Optional<MailTrackingDocument> found = tracking.findByMessageId(messageId);
        found.ifPresent(t -> tracking.save(t.withStatus(status, Instant.now())));
    }

    /** SHA-256 over every field of the request, with maps in key order so equal requests hash equal. */
    static String hash(SendEmailRequest r) {
        String canonical = canonical(List.of(String.valueOf(r.templateDefinedId()), String.valueOf(r.userId()),
                String.valueOf(r.from()), String.valueOf(r.to()), String.valueOf(r.cc()),
                String.valueOf(r.subject()), String.valueOf(r.body()), String.valueOf(r.sendDate()),
                Objects.requireNonNullElse(r.params(), Map.of())));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            var sorted = new TreeMap<String, String>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), canonical(v)));
            return sorted.toString();
        }
        if (value instanceof Iterable<?> items) {
            var out = new StringBuilder("[");
            items.forEach(i -> out.append(canonical(i)).append(','));
            return out.append(']').toString();
        }
        return String.valueOf(value);
    }

    /** Answer plus whether it replays an earlier request. */
    public record Accepted(SendEmailResponse response, boolean replayed) {
    }
}
