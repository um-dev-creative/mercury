package com.umdc.mercury.telegram;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramMessageStatusResponse(UUID id, String status, LocalDateTime sentAt) { }
