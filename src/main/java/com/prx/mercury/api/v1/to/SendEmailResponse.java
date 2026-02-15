package com.prx.mercury.api.v1.to;

import java.util.Objects;
import java.util.UUID;

/**
 * A record that represents the response of sending an email.
 *
 * @param id      The unique identifier of the email.
 * @param status  The status of the email sending operation.
 * @param message A message providing additional information about the email sending operation.
 */
public record SendEmailResponse(UUID id, String status, String message) {

    /**
     * Constructs a new SendEmailResponse.
     *
     * @param id      The unique identifier of the email. Must not be null.
     * @param status  The status of the email sending operation. Must not be null.
     * @param message A message providing additional information about the email sending operation. Must not be null.
     * @throws NullPointerException if any of the parameters are null.
     */
    public SendEmailResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * Returns a string representation of the SendEmailResponse.
     *
     * @return A string representation of the SendEmailResponse.
     */
    @Override
    public String toString() {
        return "SendEmailResponse{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
