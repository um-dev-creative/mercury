package com.umdc.mercury.security;

import com.umdc.mercury.jpa.nosql.document.RevokedTokenDocument;
import com.umdc.mercury.jpa.nosql.repository.RevokedTokenNSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Denylist of revoked session-token {@code jti}s, backed by MongoDB (TTL-purged at each
 * token's own {@code exp}, see {@link RevokedTokenDocument}).
 */
@Service
public class TokenRevocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenNSRepository repository;

    public TokenRevocationService(RevokedTokenNSRepository repository) {
        this.repository = repository;
    }

    /**
     * Revokes the token identified by {@code jti} until {@code expiresAt} (the token's own
     * {@code exp}; after that it is rejected as expired and the row is purged).
     */
    public void revoke(String jti, Instant expiresAt) {
        if (Objects.isNull(jti) || jti.isBlank() || Objects.isNull(expiresAt)) {
            throw new IllegalArgumentException("jti and expiresAt are required to revoke a token");
        }
        repository.save(new RevokedTokenDocument(jti, expiresAt, Instant.now()));
        LOGGER.info("Session token revoked, jti={}", jti);
    }

    /**
     * @return {@code true} if {@code jti} is on the denylist. A token without a {@code jti}
     * cannot be revoked individually and is reported as not revoked.
     */
    public boolean isRevoked(String jti) {
        return Objects.nonNull(jti) && !jti.isBlank() && repository.existsById(jti);
    }
}
