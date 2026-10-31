package com.umdc.mercury.telegram;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Body of {@code POST /api/v1/telegram/links}; {@code language} defaults to {@code es}. */
public record TelegramLinkRequest(@NotNull UUID userId, String language) { }
