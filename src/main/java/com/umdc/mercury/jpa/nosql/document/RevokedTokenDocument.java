package com.umdc.mercury.jpa.nosql.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A revoked session-token, keyed by its {@code jti}. {@code expiresAt} mirrors the token's
 * own {@code exp}: MongoDB's TTL monitor deletes the row at that instant, since after it the
 * token is rejected by the expiration check anyway.
 */
@Document(collection = "revoked_tokens")
public record RevokedTokenDocument(
        @Id
        String jti,
        @Indexed(expireAfter = "0s")
        Instant expiresAt,
        Instant revokedAt) {
}
