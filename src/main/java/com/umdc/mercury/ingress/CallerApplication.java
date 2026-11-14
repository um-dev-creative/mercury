package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.exception.ForbiddenException;

import java.util.Objects;
import java.util.UUID;

/**
 * The application a backend caller acts for. Its Mercury M2M token carries the application Backbone ties the client to, and the endpoint hands it over as the
 * {@code CALLER} attribute (a UUID as text): a caller works on its own application only.
 */
final class CallerApplication {

    private CallerApplication() {
    }

    /** The caller's application; {@code null} only when there is no caller (never in production: the interceptor always sets it). */
    static UUID of(String caller) {
        if (Objects.isNull(caller) || caller.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(caller);
        } catch (IllegalArgumentException _) {
            throw new ForbiddenException("The caller is not tied to an application");
        }
    }

    /** A caller naming another application is refused, and one naming none gets its own. */
    static MessageRequest apply(MessageRequest request, UUID application) {
        if (Objects.isNull(application)) {
            return request;
        }
        if (Objects.nonNull(request.applicationId())) {
            assertOwn(application, request.applicationId());
            return request;
        }
        return new MessageRequest(request.channel(), request.userId(), request.target(), request.templateDefinedId(), request.template(), application,
                request.language(), request.from(), request.subject(), request.body(), request.sendDate(), request.params(), request.options(),
                request.mediaUrl(), request.mediaType());
    }

    /** @throws ForbiddenException the caller has an application and {@code requested} is another one */
    static void assertOwn(UUID application, UUID requested) {
        if (Objects.nonNull(application) && !application.equals(requested)) {
            throw new ForbiddenException("The caller may only act for its own application");
        }
    }
}
