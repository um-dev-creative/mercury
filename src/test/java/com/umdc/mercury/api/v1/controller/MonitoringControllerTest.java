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
    @DisplayName("a session token that does not verify is refused on every endpoint and the service is never called")
    void refusesUnverifiedToken() {
        assertThrows(InvalidSessionTokenException.class, () -> controller.summary(application, null, null));
        assertThrows(InvalidSessionTokenException.class, () -> controller.summary(application, null, "  "));
        assertThrows(InvalidSessionTokenException.class, () -> controller.summary(application, null, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.series(application, new MonitoringTO.SeriesQuery(null, null, null), BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.failures(application, null, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.verificationCodes(application, null, BAD));
        assertThrows(InvalidSessionTokenException.class, () -> controller.messages(application, new MonitoringTO.MessageQuery(null, null, null, null, null, null, 0, 20), BAD));

        verifyNoInteractions(service);
    }
}
