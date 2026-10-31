package com.umdc.mercury.telegram;

import com.umdc.mercury.api.v1.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The webhook's statuses as a client sees them, through the application's exception handling. */
class TelegramWebhookStatusTest {

    private final TelegramBotProperties properties =
            new TelegramBotProperties(true, "token", "secret", null, List.of("es"), null, null, "zero", "webhook", null);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new TelegramWebhookController(properties, mock(TelegramUpdateHandler.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void wrongSecretIsUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/telegram/webhook/zero").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(TelegramWebhookApi.SECRET_HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherAliasIsNotFound() throws Exception {
        mvc.perform(post("/api/v1/telegram/webhook/other").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rightSecretIsAccepted() throws Exception {
        mvc.perform(post("/api/v1/telegram/webhook/zero").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(TelegramWebhookApi.SECRET_HEADER, "secret"))
                .andExpect(status().isOk());
    }
}
