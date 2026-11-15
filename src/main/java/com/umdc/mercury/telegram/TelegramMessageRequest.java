package com.umdc.mercury.telegram;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Body of {@code POST /api/v1/telegram/messages}: what to say, not the text. The template is named either by {@code kind} + {@code language}
 * or, preferably, by {@code templateDefinedId} (the template's own language is then used); exactly one of the two.
 */
public record TelegramMessageRequest(
        @NotNull UUID userId,
        @Size(max = 50) String kind,
        String language,
        @Size(max = 30) Map<String, Object> params,
        Boolean disableNotification,
        UUID templateDefinedId) {

    /** Named by kind (the original form). */
    public TelegramMessageRequest(UUID userId, String kind, String language, Map<String, Object> params, Boolean disableNotification) {
        this(userId, kind, language, params, disableNotification, null);
    }
}
