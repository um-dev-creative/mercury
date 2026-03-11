package com.prx.mercury.api.v1.exception;

import java.util.UUID;

/**
 * Thrown when a campaign with the requested {@link UUID} does not exist in the database.
 *
 * <p>Mapped to HTTP {@code 404 Not Found} by the global exception handler.</p>
 */
public class CampaignNotFoundException extends RuntimeException {

    /**
     * Constructs a {@code CampaignNotFoundException} with a detail message.
     *
     * @param message human-readable description of the missing resource.
     */
    public CampaignNotFoundException(String message) {
        super(message);
    }
}
