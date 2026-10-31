package com.umdc.mercury.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/** Body of {@code POST /api/v1/telegram/messages}: what to say (template kind and parameters), not the text. */
public record TelegramMessageRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 50) String kind,
        String language,
        @Size(max = 30) Map<String, Object> params,
        Boolean disableNotification) { }
