package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.MonitoringStreamService;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final SessionJwtServiceImpl sessionJwtService;

    public MonitoringController(MonitoringService monitoringService, ServiceHealthService healthService, MonitoringStreamService streamService,
                                SessionJwtServiceImpl sessionJwtService) {
        this.monitoringService = monitoringService;
        this.healthService = healthService;
        this.streamService = streamService;
        this.sessionJwtService = sessionJwtService;
    }

    private void requireVerifiedUser(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE);
        }
        sessionJwtService.getVerifiedUid(sessionToken).orElseThrow(() -> new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE));
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
}
