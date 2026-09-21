package com.umdc.mercury.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Wires the Telegram Bot API client.
 *
 * <p>The bean is only created when {@code umdc.telegram.bot-token} is set, so local dev and
 * environments without a token simply have no client — {@code TelegramMessageDispatcher}
 * then leaves queued messages untouched rather than failing them.</p>
 */
@Configuration
public class TelegramConfig {

    @Bean
    @ConditionalOnExpression("'${umdc.telegram.bot-token:}'.trim() != ''")
    public TelegramClient telegramClient(@Value("${umdc.telegram.bot-token}") String botToken) {
        return new OkHttpTelegramClient(botToken);
    }
}
