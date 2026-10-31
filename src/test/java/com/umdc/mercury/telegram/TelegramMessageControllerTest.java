package com.umdc.mercury.telegram;

import com.umdc.mercury.security.MachineTokenInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Telegram messages API")
class TelegramMessageControllerTest {

    private final TelegramMessageService service = mock(TelegramMessageService.class);
    private final UUID user = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TelegramMessageController(service)).build();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postBody(String body) {
        return post("/api/v1/telegram/messages").requestAttr(MachineTokenInterceptor.CALLER, "zerobound")
                .header("Idempotency-Key", "k1").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String body() {
        return "{\"userId\":\"" + user + "\",\"kind\":\"debt_due\",\"language\":\"es\",\"params\":{\"debtName\":\"Visa\"}}";
    }

    @Test
    @DisplayName("202 with the id; a replay adds Idempotent-Replayed")
    void accepted() throws Exception {
        when(service.accept(eq("zerobound"), any(), eq("k1")))
                .thenReturn(new TelegramMessageService.Result.Accepted(id, "QUEUED", false), new TelegramMessageService.Result.Accepted(id, "SENT", true));
        mvc.perform(postBody(body())).andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        mvc.perform(postBody(body())).andExpect(status().isAccepted()).andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    @DisplayName("409 carries the reason: TELEGRAM_NOT_LINKED or TELEGRAM_BLOCKED")
    void conflicts() throws Exception {
        when(service.accept(eq("zerobound"), any(), eq("k1")))
                .thenReturn(new TelegramMessageService.Result.NotLinked(), new TelegramMessageService.Result.Blocked());
        mvc.perform(postBody(body())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TELEGRAM_NOT_LINKED"));
        mvc.perform(postBody(body())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TELEGRAM_BLOCKED"));
    }

    @Test
    @DisplayName("a body without kind is a 400")
    void invalid() throws Exception {
        mvc.perform(postBody("{\"userId\":\"" + user + "\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET answers the state or 404")
    void status_() throws Exception {
        when(service.status(id, "zerobound")).thenReturn(Optional.of(new TelegramMessageStatusResponse(id, "SENT", null)));
        mvc.perform(get("/api/v1/telegram/messages/" + id).requestAttr(MachineTokenInterceptor.CALLER, "zerobound"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        mvc.perform(get("/api/v1/telegram/messages/" + UUID.randomUUID()).requestAttr(MachineTokenInterceptor.CALLER, "zerobound"))
                .andExpect(status().isNotFound());
    }
}
