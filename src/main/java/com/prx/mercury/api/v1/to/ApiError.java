package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Structured error response returned by the global exception handler.
 *
 * @param timestamp ISO-formatted timestamp when the error occurred.
 * @param status    HTTP status code.
 * @param error     Short description of the HTTP error (e.g. {@code Bad Request}).
 * @param message   Detailed error message suitable for API consumers.
 * @param path      Request URI that triggered the error.
 */
public record ApiError(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
