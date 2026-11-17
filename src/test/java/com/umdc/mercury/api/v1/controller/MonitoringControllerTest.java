package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringController")
class MonitoringControllerTest {

    private static final String TOKEN = "good-token";
    private static final String BAD = "bad-token";

    @Mock
    private MonitoringService service;
    @Mock
    private com.umdc.mercury.monitoring.health.ServiceHealthService healthService;
    @Mock
    private com.umdc.mercury.monitoring.MonitoringStreamService streamService;
    @Mock
    private com.umdc.mercury.monitoring.MessageRetryService retryService;
    @Mock
    private com.umdc.mercury.monitoring.alerts.AlertRuleService alertService;
    @Mock
    private SessionJwtServiceImpl sessionJwtService;
    @InjectMocks
    private MonitoringController controller;

    private final UUID application = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(sessionJwtService.getVerifiedUid(TOKEN)).thenReturn(Optional.of(UUID.randomUUID()));
        lenient().when(sessionJwtService.getVerifiedUid(BAD)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("each endpoint answers 200 with what the service returns when the session token is verified")
    void delegates() {
        var summary = new MonitoringTO.SummaryResponse("24h", LocalDateTime.now(), new MonitoringTO.Totals(1, 0, 0, 1.0), List.of(), List.of());
        var series = new MonitoringTO.SeriesResponse("24h", "hour", List.of());
        var failures = new MonitoringTO.FailuresResponse("24h", 15, List.of(), List.of());
        var codes = new MonitoringTO.VerificationCodeStats("24h", 1, 1, 0, 0, 0, 1.0);
        var page = new MonitoringTO.MessagePage(0, 20, 0, List.of());
        when(service.summary(application, "24h")).thenReturn(summary);
        when(service.series(application, "24h", "email", "CAMPAIGN")).thenReturn(series);
        when(service.failures(application, "24h")).thenReturn(failures);
        when(service.verificationCodes(application, "24h")).thenReturn(codes);
        when(service.messages(application, "email", "FAILED", "CAMPAIGN", null, null, null, 0, 20)).thenReturn(page);

        assertSame(summary, controller.summary(application, "24h", TOKEN).getBody());
        assertSame(series, controller.series(application, new MonitoringTO.SeriesQuery("24h", "email", "CAMPAIGN"), TOKEN).getBody());
        assertSame(failures, controller.failures(application, "24h", TOKEN).getBody());
        assertSame(codes, controller.verificationCodes(application, "24h", TOKEN).getBody());
        var response = controller.messages(application, new MonitoringTO.MessageQuery("email", "FAILED", "CAMPAIGN", null, null, null, 0, 20), TOKEN);
        assertSame(page, response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("health, outages and the stream answer with what their services return when the token is verified")
    void delegatesHealthOutagesStream() {
        var health = new MonitoringTO.HealthResponse(LocalDateTime.now(), "OPERATIONAL", List.of());
        var outages = new MonitoringTO.OutagesResponse("24h", List.of());
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        when(healthService.health()).thenReturn(health);
        when(healthService.outages("24h")).thenReturn(outages);
        when(streamService.subscribe(application, "1h")).thenReturn(emitter);

        assertSame(health, controller.health(TOKEN).getBody());
        assertSame(outages, controller.outages("24h", TOKEN).getBody());
        assertSame(emitter, controller.stream(application, "1h", TOKEN));
    }

    @Test
    @DisplayName("a session token that does not verify is refused on every endpoint and the service is never called")
    void refusesUnverifiedToken() {
        assertThrows(InvalidSessionTokenException.class, () -> controller.summary(application, null, null));
        assertThrows(InvalidSessionTokenException.class, () -> controller.summary(application, null, "  "));
        assertThrows(InvalidSessionTokenException.class, () -> controller.summary(application, null, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.series(application, new MonitoringTO.SeriesQuery(null, null, null), BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.failures(application, null, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.verificationCodes(application, null, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.messages(application, new MonitoringTO.MessageQuery(null, null, null, null, null, null, 0, 20), BAD));

        assertThrows(InvalidSessionTokenException.class, () -> controller.health(BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.outages(null, null));
        assertThrows(InvalidSessionTokenException.class, () -> controller.stream(application, null, BAD));

        verifyNoInteractions(service, healthService, streamService);
    }

    private static MonitoringTO.MessageItem item(String recipient) {
        return new MonitoringTO.MessageItem(UUID.randomUUID(), LocalDateTime.of(2026, 11, 17, 8, 0), "EMAIL", "CAMPAIGN", "FAILED", UUID.randomUUID(), null, null,
                recipient, "SEND_FAILED");
    }

    @Test
    @DisplayName("export: CSV by default, JSON on request, with the file name, the total and whether it was cut")
    void export() {
        var exported = new MonitoringTO.MessageExport(List.of(item("j***@e***.com")), 3);
        when(service.export(application, null, null, null, null, null, null)).thenReturn(exported);

        var csv = controller.exportMessages(application, new MonitoringTO.ExportQuery(null, null, null, null, null, null, null), TOKEN);

        assertEquals(200, csv.getStatusCode().value());
        assertEquals("text/csv;charset=UTF-8", csv.getHeaders().getContentType().toString());
        assertEquals("3", csv.getHeaders().getFirst("X-Total-Count"));
        assertEquals("true", csv.getHeaders().getFirst("X-Export-Truncated"));
        assertEquals("no-store", csv.getHeaders().getCacheControl());
        org.junit.jupiter.api.Assertions.assertTrue(csv.getHeaders().getFirst("Content-Disposition").matches("attachment; filename=\"mercury-messages-\\d{8}-\\d{6}\\.csv\""));
        org.junit.jupiter.api.Assertions.assertTrue(new String((byte[]) csv.getBody(), java.nio.charset.StandardCharsets.UTF_8).contains("j***@e***.com"));

        var json = controller.exportMessages(application, new MonitoringTO.ExportQuery(" JSON ", null, null, null, null, null, null), TOKEN);
        assertEquals("application/json", json.getHeaders().getContentType().toString());
        assertSame(exported.items(), json.getBody());
        org.junit.jupiter.api.Assertions.assertTrue(json.getHeaders().getFirst("Content-Disposition").endsWith(".json\""));
    }

    @Test
    @DisplayName("export: an unknown format is a bad request and nothing is read")
    void exportUnknownFormat() {
        assertThrows(IllegalArgumentException.class, () -> controller.exportMessages(application, new MonitoringTO.ExportQuery("xml", null, null, null, null, null, null), TOKEN));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("retry: 202 when queued, 404 when there is no failed e-mail, 409 when it cannot be retried")
    void retry() {
        var message = UUID.randomUUID();
        when(retryService.retry(org.mockito.ArgumentMatchers.eq(application), org.mockito.ArgumentMatchers.eq(message), org.mockito.ArgumentMatchers.any()))
                .thenReturn(com.umdc.mercury.monitoring.MessageRetryService.Result.QUEUED, com.umdc.mercury.monitoring.MessageRetryService.Result.NOT_FOUND,
                        com.umdc.mercury.monitoring.MessageRetryService.Result.NOT_RETRYABLE);

        assertEquals(202, controller.retryMessage(application, message, TOKEN).getStatusCode().value());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.retryMessage(application, message, TOKEN)).getStatusCode().value());
        assertEquals(409, assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.retryMessage(application, message, TOKEN)).getStatusCode().value());
    }

    @Test
    @DisplayName("alerts: rules and alerts delegate to the service; an unknown rule is a 404")
    void alerts() {
        var rule = new MonitoringTO.AlertRule(UUID.randomUUID(), "FAILURE_RATE", 20, 15, 10, null, List.of(), true, null, null);
        var request = new MonitoringTO.AlertRuleRequest("FAILURE_RATE", 20.0, 15, 10, null, null, true);
        var rules = new MonitoringTO.AlertRulesResponse(List.of(rule));
        var alerts = new MonitoringTO.AlertsResponse("24h", 0, List.of());
        when(alertService.list(application)).thenReturn(rules);
        when(alertService.create(application, request)).thenReturn(rule);
        when(alertService.update(application, rule.id(), request)).thenReturn(rule).thenReturn(null);
        when(alertService.delete(application, rule.id())).thenReturn(true).thenReturn(false);
        when(alertService.alerts(application, "24h")).thenReturn(alerts);

        assertSame(rules, controller.alertRules(application, TOKEN).getBody());
        var created = controller.createAlertRule(application, request, TOKEN);
        assertEquals(201, created.getStatusCode().value());
        assertSame(rule, created.getBody());
        assertSame(rule, controller.updateAlertRule(application, rule.id(), request, TOKEN).getBody());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.updateAlertRule(application, rule.id(), request, TOKEN)).getStatusCode().value());
        assertEquals(204, controller.deleteAlertRule(application, rule.id(), TOKEN).getStatusCode().value());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.deleteAlertRule(application, rule.id(), TOKEN)).getStatusCode().value());
        assertSame(alerts, controller.alerts(application, "24h", TOKEN).getBody());
    }

    @Test
    @DisplayName("the new endpoints also refuse a missing or forged session token")
    void newEndpointsNeedAVerifiedUser() {
        var message = UUID.randomUUID();
        var query = new MonitoringTO.ExportQuery(null, null, null, null, null, null, null);
        var request = new MonitoringTO.AlertRuleRequest("FAILURE_RATE", 20.0, null, null, null, null, null);
        assertThrows(InvalidSessionTokenException.class, () -> controller.exportMessages(application, query, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.retryMessage(application, message, null));
        assertThrows(InvalidSessionTokenException.class, () -> controller.alertRules(application, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.createAlertRule(application, request, " "));
        assertThrows(InvalidSessionTokenException.class, () -> controller.updateAlertRule(application, message, request, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.deleteAlertRule(application, message, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.alerts(application, "1h", BAD));
        verifyNoInteractions(service, retryService, alertService);
    }
}
