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
    private final com.umdc.mercury.monitoring.health.ServiceHealthService health = Mockito.mock(com.umdc.mercury.monitoring.health.ServiceHealthService.class);
    private final com.umdc.mercury.monitoring.MonitoringStreamService streams = Mockito.mock(com.umdc.mercury.monitoring.MonitoringStreamService.class);
    private final com.umdc.mercury.monitoring.MessageRetryService retry = Mockito.mock(com.umdc.mercury.monitoring.MessageRetryService.class);
    private final com.umdc.mercury.monitoring.alerts.AlertRuleService alerts = Mockito.mock(com.umdc.mercury.monitoring.alerts.AlertRuleService.class);
    private final SessionJwtServiceImpl sessions = Mockito.mock(SessionJwtServiceImpl.class);
    private final UUID application = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(sessions.getVerifiedUid("good")).thenReturn(Optional.of(UUID.randomUUID()));
        when(sessions.getVerifiedUid("bad")).thenReturn(Optional.empty());
        mvc = MockMvcBuilders.standaloneSetup(new MonitoringController(service, health, streams, retry, alerts, sessions)).setControllerAdvice(new GlobalExceptionHandler()).build();
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

    @Test
    @DisplayName("health and outages answer JSON; an unknown range is a 400")
    void healthAndOutages() throws Exception {
        when(health.health()).thenReturn(new MonitoringTO.HealthResponse(LocalDateTime.of(2026, 11, 16, 12, 0), "DEGRADED",
                List.of(new MonitoringTO.ServiceHealth("SMTP", "DEGRADED", null, "QUOTA_EXCEEDED", "The sending limit of the account was exceeded", null, null))));
        when(health.outages("7d")).thenReturn(new MonitoringTO.OutagesResponse("7d", List.of()));
        when(health.outages("9d")).thenThrow(new IllegalArgumentException("Unknown range"));

        mvc.perform(get("/api/v1/monitoring/health").header("session-token", "good")).andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").value("DEGRADED")).andExpect(jsonPath("$.services[0].cause").value("QUOTA_EXCEEDED"));
        mvc.perform(get("/api/v1/monitoring/health/outages").header("session-token", "good").param("range", "7d")).andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("7d"));
        mvc.perform(get("/api/v1/monitoring/health/outages").header("session-token", "good").param("range", "9d")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/monitoring/health")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the stream starts an asynchronous response; too many streams is a 429 and a bad range a 400")
    void stream() throws Exception {
        when(streams.subscribe(application, "1h")).thenReturn(new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());
        when(streams.subscribe(application, "9d")).thenThrow(new IllegalArgumentException("Unknown range"));
        when(streams.subscribe(application, "30d")).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "full"));

        mvc.perform(get(path("stream")).header("session-token", "good").param("range", "1h").accept("text/event-stream"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted());
        mvc.perform(get(path("stream")).header("session-token", "good").param("range", "9d")).andExpect(status().isBadRequest());
        mvc.perform(get(path("stream")).header("session-token", "good").param("range", "30d")).andExpect(status().isTooManyRequests());
        mvc.perform(get(path("stream"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("export: the filters reach the service and the answer is a CSV attachment with the total and the truncation flag")
    void exportOverHttp() throws Exception {
        var item = new MonitoringTO.MessageItem(UUID.randomUUID(), LocalDateTime.of(2026, 11, 17, 8, 0), "EMAIL", "CAMPAIGN", "FAILED", null, null, null,
                "=cmd@e***.com", "SEND_FAILED");
        when(service.export(any(), any(), any(), any(), any(), any(), any())).thenReturn(new MonitoringTO.MessageExport(List.of(item), 1));

        var result = mvc.perform(get(path("messages/export")).header("session-token", "good").param("status", "FAILED").param("from", "2026-11-17T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Export-Truncated", "false"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Total-Count", "1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("text/csv;charset=UTF-8"))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(result.startsWith("occurredAt,channel"));
        org.junit.jupiter.api.Assertions.assertTrue(result.contains("'=cmd@e***.com"), "a formula is neutralised");
        verify(service).export(application, null, "FAILED", null, null, LocalDateTime.of(2026, 11, 17, 0, 0), null);
    }

    @Test
    @DisplayName("export as JSON, an unknown format (400) and no session (401)")
    void exportJsonAndErrors() throws Exception {
        when(service.export(any(), any(), any(), any(), any(), any(), any())).thenReturn(new MonitoringTO.MessageExport(List.of(), 0));

        mvc.perform(get(path("messages/export")).header("session-token", "good").param("format", "json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get(path("messages/export")).header("session-token", "good").param("format", "xml")).andExpect(status().isBadRequest());
        mvc.perform(get(path("messages/export"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("retry: 202, 404 and 409 map from the service result; a forged token is 401")
    void retryOverHttp() throws Exception {
        var message = UUID.randomUUID();
        when(retry.retry(any(), any(), any())).thenReturn(com.umdc.mercury.monitoring.MessageRetryService.Result.QUEUED,
                com.umdc.mercury.monitoring.MessageRetryService.Result.NOT_FOUND, com.umdc.mercury.monitoring.MessageRetryService.Result.NOT_RETRYABLE);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("messages/" + message + "/retry")).header("session-token", "good"))
                .andExpect(status().isAccepted());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("messages/" + message + "/retry")).header("session-token", "good"))
                .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("messages/" + message + "/retry")).header("session-token", "good"))
                .andExpect(status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("messages/" + message + "/retry")).header("session-token", "bad"))
                .andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("messages/not-a-uuid/retry")).header("session-token", "good"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("alert rules over HTTP: create (201), an invalid rule (400), update, delete and an unknown rule (404)")
    void alertRulesOverHttp() throws Exception {
        var rule = new MonitoringTO.AlertRule(UUID.randomUUID(), "FAILURE_RATE", 20, 15, 10, null, List.of(), true, null, null);
        when(alerts.create(any(), any())).thenReturn(rule).thenThrow(new IllegalArgumentException("threshold is required for FAILURE_RATE"));
        when(alerts.update(any(), any(), any())).thenReturn(null);
        when(alerts.delete(any(), any())).thenReturn(false);
        when(alerts.list(any())).thenReturn(new MonitoringTO.AlertRulesResponse(List.of(rule)));
        when(alerts.alerts(any(), any())).thenReturn(new MonitoringTO.AlertsResponse("24h", 1, List.of()));
        var json = org.springframework.http.MediaType.APPLICATION_JSON;
        var body = "{\"type\":\"FAILURE_RATE\",\"threshold\":20,\"windowMinutes\":15}";

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("alerts/rules")).header("session-token", "good").contentType(json).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("FAILURE_RATE"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path("alerts/rules")).header("session-token", "good").contentType(json).content("{\"type\":\"FAILURE_RATE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path("alerts/rules/" + rule.id())).header("session-token", "good").contentType(json).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path("alerts/rules/" + rule.id())).header("session-token", "good"))
                .andExpect(status().isNotFound());
        mvc.perform(get(path("alerts/rules")).header("session-token", "good")).andExpect(status().isOk()).andExpect(jsonPath("$.rules[0].threshold").value(20.0));
        mvc.perform(get(path("alerts")).header("session-token", "good").param("range", "7d")).andExpect(status().isOk()).andExpect(jsonPath("$.firing").value(1));
        mvc.perform(get(path("alerts"))).andExpect(status().isUnauthorized());
    }
}
