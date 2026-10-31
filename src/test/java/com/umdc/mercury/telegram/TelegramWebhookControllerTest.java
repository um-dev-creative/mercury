package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Telegram webhook")
class TelegramWebhookControllerTest {

    private static final String SECRET = "s3cr3t";
    private final TelegramUpdateHandler handler = mock(TelegramUpdateHandler.class);

    private MockMvc mvc(boolean enabled, String mode) {
        final var props = new TelegramBotProperties(enabled, "t", SECRET, "", null, null, null, "zerobound", mode, "https://x/hook");
        return MockMvcBuilders.standaloneSetup(new TelegramWebhookController(props, handler)).build();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String alias, String secret) {
        final var b = post("/api/v1/telegram/webhook/" + alias).contentType(MediaType.APPLICATION_JSON).content("{\"update_id\":1}");
        return secret == null ? b : b.header(TelegramWebhookApi.SECRET_HEADER, secret);
    }

    @Test
    @DisplayName("a correct secret hands the update to the handler and answers 200")
    void accepted() throws Exception {
        mvc(true, "webhook").perform(request("zerobound", SECRET)).andExpect(status().isOk());
        verify(handler).handle("zerobound", Map.of("update_id", 1));
    }

    @Test
    @DisplayName("a wrong or missing secret is a 401 and nothing is handled")
    void unauthorised() throws Exception {
        mvc(true, "webhook").perform(request("zerobound", "nope")).andExpect(status().isUnauthorized());
        mvc(true, "webhook").perform(request("zerobound", null)).andExpect(status().isUnauthorized());
        verify(handler, never()).handle(anyString(), any());
    }

    @Test
    @DisplayName("an unset secret never matches")
    void unsetSecret() throws Exception {
        final var props = new TelegramBotProperties(true, "t", "", "", null, null, null, "zerobound", "webhook", "");
        MockMvcBuilders.standaloneSetup(new TelegramWebhookController(props, handler)).build()
                .perform(request("zerobound", "")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("poll mode, a disabled channel or another alias answer 404")
    void notFound() throws Exception {
        mvc(true, "poll").perform(request("zerobound", SECRET)).andExpect(status().isNotFound());
        mvc(false, "webhook").perform(request("zerobound", SECRET)).andExpect(status().isNotFound());
        mvc(true, "webhook").perform(request("other", SECRET)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a body over 1 MB is a 413 and nothing is handled")
    void tooLarge() {
        final var props = new TelegramBotProperties(true, "t", SECRET, "", null, null, null, "zerobound", "webhook", "");
        final var controller = new TelegramWebhookController(props, handler);
        final var servlet = mock(jakarta.servlet.http.HttpServletRequest.class);
        org.mockito.Mockito.when(servlet.getContentLengthLong()).thenReturn(2_000_000L);
        final var e = org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.receive("zerobound", SECRET, Map.of(), servlet));
        org.junit.jupiter.api.Assertions.assertEquals(413, e.getStatusCode().value());
        verify(handler, never()).handle(anyString(), any());
    }
}
