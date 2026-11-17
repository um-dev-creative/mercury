package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The HTTP contract: how the query string reaches the service and what the errors look like. */
@DisplayName("MonitoringController over HTTP")
class MonitoringControllerHttpTest {

    private final MonitoringService service = Mockito.mock(MonitoringService.class);
    private final SessionJwtServiceImpl sessions = Mockito.mock(SessionJwtServiceImpl.class);
    private final UUID application = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(sessions.getVerifiedUid("good")).thenReturn(Optional.of(UUID.randomUUID()));
        when(sessions.getVerifiedUid("bad")).thenReturn(Optional.empty());
        mvc = MockMvcBuilders.standaloneSetup(new MonitoringController(service, sessions)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private String path(String endpoint) {
        return "/api/v1/monitoring/applications/" + application + "/" + endpoint;
    }

    @Test
    @DisplayName("the message filters, dates and paging of the query string reach the service; the page size defaults to 20")
    void messagesQueryString() throws Exception {
        var template = UUID.randomUUID();
        when(service.messages(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new MonitoringTO.MessagePage(2, 50, 7, List.of()));

        mvc.perform(get(path("messages")).header("session-token", "good").param("channel", "email").param("status", "FAILED").param("kind", "CAMPAIGN")
                        .param("templateDefinedId", template.toString()).param("from", "2026-11-01T00:00:00").param("to", "2026-11-02T00:00:00")
                        .param("page", "2").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7));

        verify(service).messages(application, "email", "FAILED", "CAMPAIGN", template, LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 2, 0, 0), 2, 50);

        mvc.perform(get(path("messages")).header("session-token", "good"))
                .andExpect(status().isOk());
        verify(service).messages(eq(application), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20));
    }

    @Test
    @DisplayName("the series query string reaches the service")
    void seriesQueryString() throws Exception {
        when(service.series(any(), any(), any(), any())).thenReturn(new MonitoringTO.SeriesResponse("1h", "minute", List.of()));

        mvc.perform(get(path("series")).header("session-token", "good").param("range", "1h").param("channel", "sms").param("kind", "CAMPAIGN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unit").value("minute"));

        verify(service).series(application, "1h", "sms", "CAMPAIGN");
    }

    @Test
    @DisplayName("a service refusal of a filter is a 400, and a bad date or application id is a 400 too")
    void invalidInputIsBadRequest() throws Exception {
        when(service.summary(any(), eq("3d"))).thenThrow(new IllegalArgumentException("Unknown range '3d': use 1h, 24h, 7d or 30d"));

        mvc.perform(get(path("summary")).header("session-token", "good").param("range", "3d")).andExpect(status().isBadRequest());
        mvc.perform(get(path("messages")).header("session-token", "good").param("from", "yesterday")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/monitoring/applications/not-a-uuid/summary").header("session-token", "good")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a missing or blank session-token header and an unverified one are all a 401")
    void tokenErrors() throws Exception {
        mvc.perform(get(path("summary"))).andExpect(status().isUnauthorized());
        mvc.perform(get(path("summary")).header("session-token", " ")).andExpect(status().isUnauthorized());
        mvc.perform(get(path("summary")).header("session-token", "bad")).andExpect(status().isUnauthorized());
        mvc.perform(get(path("failures")).header("session-token", "bad")).andExpect(status().isUnauthorized());
    }
}
