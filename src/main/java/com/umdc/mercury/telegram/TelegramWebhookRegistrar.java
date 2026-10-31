package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** In webhook mode, tells Telegram where to post updates and with which secret (T-114). */
@Component
public class TelegramWebhookRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramWebhookRegistrar.class);

    private final TelegramBotProperties properties;
    private final TelegramBotApi api;

    public TelegramWebhookRegistrar(TelegramBotProperties properties, TelegramBotApi api) {
        this.properties = properties;
        this.api = api;
    }

    @EventListener(ApplicationReadyEvent.class)
    void register() {
        if (!properties.active() || properties.polling() || properties.webhookUrl().isEmpty()) {
            return;
        }
        if (api.setWebhook(properties.webhookUrl(), properties.webhookSecret()) instanceof BotResult.Ok) {
            LOG.info("Telegram webhook registered");
        } else {
            LOG.warn("Telegram setWebhook failed: check the URL (HTTPS) and the token");
        }
    }
}
