package com.umdc.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/// A backend service allowed to log in to Mercury. The password (or its hash) is never part of this response.
///
/// @param id          the login client id
/// @param alias       the name of the calling service
/// @param description free text about the service
/// @param active      whether the service may log in
/// @param createdAt   when it was registered
/// @param updatedAt   when it last changed
public record LoginClientResponse(
        UUID id,
        String alias,
        String description,
        boolean active,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt) {
}
