package com.umdc.mercury.ingress;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/messages}: one message for any channel. {@code channel} is a code mercury has a provider for (see
 * {@code GET /api/v1/messages/channels}); {@code target} is the recipient address in that channel. What else a channel needs is told by its 400.
 */
public record MessageRequest(
        @NotBlank @Size(max = 40) String channel,
        @NotNull UUID userId,
        @NotBlank @Size(max = 320) String target,
        UUID templateDefinedId,
        @Size(max = 16) String language,
        @Size(max = 320) String from,
        @Size(max = 500) String subject,
        @Size(max = 20000) String body,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime sendDate,
        @Size(max = 50) Map<String, Object> params,
        @Size(max = 30) Map<String, Object> options,
        @Size(max = 2000) String mediaUrl,
        @Size(max = 20) String mediaType) {

    /** No target, subject, body or parameters: mercury logs requests and they carry personal data. */
    @Override
    public String toString() {
        return "MessageRequest{channel=" + channel + ", userId=" + userId + ", templateDefinedId=" + templateDefinedId + ", sendDate=" + sendDate
                + ", params=" + (params == null ? 0 : params.size()) + " keys}";
    }
}
