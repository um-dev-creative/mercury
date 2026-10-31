package com.umdc.mercury.telegram;

import com.umdc.mercury.security.MachineTokenInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Telegram links API")
class TelegramLinkControllerTest {

    private final TelegramLinkService service = mock(TelegramLinkService.class);
    private final UUID user = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TelegramLinkController(service)).build();
    }

    @Test
    @DisplayName("POST creates the link for the caller's application")
    void create() throws Exception {
        when(service.createLink("zerobound", user, "es"))
                .thenReturn(new TelegramLinkService.Created("https://t.me/b?start=abc", LocalDateTime.parse("2026-11-02T10:10:00")));
        mvc.perform(post("/api/v1/telegram/links").requestAttr(MachineTokenInterceptor.CALLER, "zerobound")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + user + "\",\"language\":\"es\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deepLink").value("https://t.me/b?start=abc"));
    }

    @Test
    @DisplayName("POST without a user is a 400")
    void createInvalid() throws Exception {
        mvc.perform(post("/api/v1/telegram/links").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET answers the state and never a chat id")
    void getStatus() throws Exception {
        when(service.status("zerobound", user)).thenReturn(new TelegramLinkService.LinkStatus(
                TelegramLinkService.Status.ACTIVE, LocalDateTime.parse("2026-11-02T10:00:00"), "es"));
        mvc.perform(get("/api/v1/telegram/links/" + user).requestAttr(MachineTokenInterceptor.CALLER, "zerobound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.language").value("es"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("chat"))));
    }

    @Test
    @DisplayName("GET of NONE omits the null fields")
    void getNone() throws Exception {
        when(service.status("zerobound", user)).thenReturn(new TelegramLinkService.LinkStatus(TelegramLinkService.Status.NONE, null, null));
        mvc.perform(get("/api/v1/telegram/links/" + user).requestAttr(MachineTokenInterceptor.CALLER, "zerobound"))
                .andExpect(content().json("{\"status\":\"NONE\"}", true));
    }

    @Test
    @DisplayName("DELETE is a 204 and calls unlink")
    void deleteLink() throws Exception {
        mvc.perform(delete("/api/v1/telegram/links/" + user).requestAttr(MachineTokenInterceptor.CALLER, "zerobound"))
                .andExpect(status().isNoContent());
        verify(service).unlink("zerobound", user);
    }
}
