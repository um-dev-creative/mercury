package com.umdc.mercury.telegram;

import java.util.UUID;

public record TelegramMessageResponse(UUID id, String status) { }
