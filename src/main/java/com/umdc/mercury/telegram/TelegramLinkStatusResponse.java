package com.umdc.mercury.telegram;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/** State of a link; the chat id is deliberately absent. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramLinkStatusResponse(TelegramLinkService.Status status, LocalDateTime linkedAt, String language) { }
