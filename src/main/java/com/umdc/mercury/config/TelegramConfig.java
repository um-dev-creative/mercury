package com.umdc.mercury.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Wires the Telegram Bot API client.
 *
 * <p>Without {@code umdc.telegram.bot-token} the bean is null, so local dev and
 * environments without a token simply have no client — {@code TelegramMessageDispatcher}
 * then leaves queued messages untouched rather than failing them. It is decided here and not with a
 * condition because AOT evaluates conditions at build time, when the native image has no token.</p>
 */
@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(@Value("${umdc.telegram.bot-token:}") String botToken) {
        return botToken.isBlank() ? null : new OkHttpTelegramClient(botToken.strip());
    }
}
