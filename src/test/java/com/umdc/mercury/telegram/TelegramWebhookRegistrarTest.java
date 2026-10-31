package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Telegram webhook registration")
class TelegramWebhookRegistrarTest {

    private final TelegramBotApi api = mock(TelegramBotApi.class);

    private TelegramWebhookRegistrar registrar(boolean enabled, String mode, String url) {
        return new TelegramWebhookRegistrar(new TelegramBotProperties(enabled, "t", "sec", "", null, null, null, "zerobound", mode, url), api);
    }

    @Test
    @DisplayName("webhook mode registers the URL with the secret, and a failure only warns")
    void registers() {
        when(api.setWebhook("https://x/hook", "sec")).thenReturn(new BotResult.Ok(true), new BotResult.Rejected(400, "bad url"));
        registrar(true, "webhook", "https://x/hook").register();
        registrar(true, "webhook", "https://x/hook").register();
        verify(api, org.mockito.Mockito.times(2)).setWebhook("https://x/hook", "sec");
    }

    @Test
    @DisplayName("poll mode, a disabled channel or an empty URL register nothing")
    void skips() {
        registrar(true, "poll", "https://x/hook").register();
        registrar(false, "webhook", "https://x/hook").register();
        registrar(true, "webhook", "").register();
        verify(api, never()).setWebhook(anyString(), anyString());
    }
}
