package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.MessageExportFormatter;
import com.umdc.mercury.monitoring.MessageRetryService;
import com.umdc.mercury.monitoring.MonitoringStreamService;
import com.umdc.mercury.monitoring.alerts.AlertRuleService;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Thin controller of the monitoring dashboard. Like {@link CampaignController}, it verifies the {@code session-token} itself (the M2M chain
 * does not) before answering: a missing or forged token is a 401, never data.
 */
@RestController
public class MonitoringController implements MonitoringApi {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or missing session token.";

    private final MonitoringService monitoringService;
    private final ServiceHealthService healthService;
    private final MonitoringStreamService streamService;
    private final MessageRetryService retryService;
    private final AlertRuleService alertService;
    private final SessionJwtServiceImpl sessionJwtService;

    public MonitoringController(MonitoringService monitoringService, ServiceHealthService healthService, MonitoringStreamService streamService,
                                MessageRetryService retryService, AlertRuleService alertService, SessionJwtServiceImpl sessionJwtService) {
        this.monitoringService = monitoringService;
        this.healthService = healthService;
        this.streamService = streamService;
        this.retryService = retryService;
        this.alertService = alertService;
        this.sessionJwtService = sessionJwtService;
    }

    private UUID requireVerifiedUser(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE);
        }
        return sessionJwtService.getVerifiedUid(sessionToken).orElseThrow(() -> new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE));
    }

    @Override
    public ResponseEntity<MonitoringTO.SummaryResponse> summary(UUID applicationId, String range, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(monitoringService.summary(applicationId, range));
    }

    @Override
    public ResponseEntity<MonitoringTO.SeriesResponse> series(UUID applicationId, MonitoringTO.SeriesQuery query, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(monitoringService.series(applicationId, query.range(), query.channel(), query.kind()));
    }

    @Override
    public ResponseEntity<MonitoringTO.FailuresResponse> failures(UUID applicationId, String range, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(monitoringService.failures(applicationId, range));
    }

    @Override
    public ResponseEntity<MonitoringTO.VerificationCodeStats> verificationCodes(UUID applicationId, String range, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(monitoringService.verificationCodes(applicationId, range));
    }

    @Override
    public ResponseEntity<MonitoringTO.MessagePage> messages(UUID applicationId, MonitoringTO.MessageQuery query, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(monitoringService.messages(applicationId, query.channel(), query.status(), query.kind(), query.templateDefinedId(),
                query.from(), query.to(), query.page(), query.size()));
    }

    @Override
    public ResponseEntity<MonitoringTO.HealthResponse> health(String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(healthService.health());
    }

    @Override
    public ResponseEntity<MonitoringTO.OutagesResponse> outages(String range, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(healthService.outages(range));
    }

    @Override
    public SseEmitter stream(UUID applicationId, String range, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return streamService.subscribe(applicationId, range);
    }

    @Override
    public ResponseEntity<Object> exportMessages(UUID applicationId, MonitoringTO.ExportQuery query, String sessionToken) {
        requireVerifiedUser(sessionToken);
        var format = query.format() == null || query.format().isBlank() ? "csv" : query.format().strip().toLowerCase(Locale.ROOT);
        if (!"csv".equals(format) && !"json".equals(format)) {
            throw new IllegalArgumentException("Unknown format '" + query.format() + "': use csv or json");
        }
        var export = monitoringService.export(applicationId, query.channel(), query.status(), query.kind(), query.templateDefinedId(), query.from(), query.to());
        var name = "mercury-messages-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now(ZoneOffset.UTC)) + "." + format;
        var response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .header("X-Total-Count", String.valueOf(export.total()))
                .header("X-Export-Truncated", String.valueOf(export.truncated()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store");
        return "json".equals(format)
                ? response.contentType(MediaType.APPLICATION_JSON).body(export.items())
                : response.contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(MessageExportFormatter.csv(export.items()));
    }

    @Override
    public ResponseEntity<Void> retryMessage(UUID applicationId, UUID messageId, String sessionToken) {
        var user = requireVerifiedUser(sessionToken);
        return switch (retryService.retry(applicationId, messageId, user)) {
            case QUEUED -> ResponseEntity.accepted().build();
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No failed e-mail with that id in the application.");
            case NOT_RETRYABLE -> throw new ResponseStatusException(HttpStatus.CONFLICT, "The message cannot be retried: it is already queued, sent, or not an e-mail.");
        };
    }

    @Override
    public ResponseEntity<MonitoringTO.AlertRulesResponse> alertRules(UUID applicationId, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(alertService.list(applicationId));
    }

    @Override
    public ResponseEntity<MonitoringTO.AlertRule> createAlertRule(UUID applicationId, MonitoringTO.AlertRuleRequest request, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.create(applicationId, request));
    }

    @Override
    public ResponseEntity<MonitoringTO.AlertRule> updateAlertRule(UUID applicationId, UUID ruleId, MonitoringTO.AlertRuleRequest request, String sessionToken) {
        requireVerifiedUser(sessionToken);
        var updated = alertService.update(applicationId, ruleId, request);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such alert rule in the application.");
        }
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<Void> deleteAlertRule(UUID applicationId, UUID ruleId, String sessionToken) {
        requireVerifiedUser(sessionToken);
        if (!alertService.delete(applicationId, ruleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such alert rule in the application.");
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MonitoringTO.AlertsResponse> alerts(UUID applicationId, String range, String sessionToken) {
        requireVerifiedUser(sessionToken);
        return ResponseEntity.ok(alertService.alerts(applicationId, range));
    }
}
