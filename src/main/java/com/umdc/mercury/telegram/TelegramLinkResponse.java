package com.umdc.mercury.telegram;

import java.time.LocalDateTime;

public record TelegramLinkResponse(String deepLink, LocalDateTime expiresAt) { }
