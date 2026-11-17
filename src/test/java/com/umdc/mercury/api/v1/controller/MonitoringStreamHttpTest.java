package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.MonitoringStreamService;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/** The wire format of the live feed, with the real emitter: what a browser's EventSource would read. */
@DisplayName("Live stream over HTTP")
class MonitoringStreamHttpTest {

    @Test
    @DisplayName("the response is text/event-stream with named 'summary' and 'health' events carrying JSON, and a push adds another pair")
    void wireFormat() throws Exception {
        var app = UUID.randomUUID();
        var monitoring = Mockito.mock(MonitoringService.class);
        var health = Mockito.mock(ServiceHealthService.class);
        var sessions = Mockito.mock(SessionJwtServiceImpl.class);
        when(sessions.getVerifiedUid("good")).thenReturn(Optional.of(UUID.randomUUID()));
        when(monitoring.summary(app, "1h")).thenReturn(new MonitoringTO.SummaryResponse("1h", LocalDateTime.of(2026, 11, 16, 12, 0),
                new MonitoringTO.Totals(7, 1, 2, 0.875), List.of(), List.of()));
        when(health.health()).thenReturn(new MonitoringTO.HealthResponse(LocalDateTime.of(2026, 11, 16, 12, 0), "OPERATIONAL", List.of()));
        var streams = new MonitoringStreamService(monitoring, health, 10, 5, 15);
        var mvc = MockMvcBuilders.standaloneSetup(new MonitoringController(monitoring, health, streams, sessions)).build();

        MvcResult result = mvc.perform(get("/api/v1/monitoring/applications/" + app + "/stream").param("range", "1h").header("session-token", "good")
                .accept("text/event-stream")).andExpect(request().asyncStarted()).andReturn();

        var response = result.getResponse();
        assertTrue(waitForContent(response, "event:health"), "the first pair arrives at once: " + response.getContentAsString());
        var first = response.getContentAsString();
        assertTrue(first.contains("event:summary"));
        assertTrue(first.contains("\"range\":\"1h\""));
        assertTrue(first.contains("\"sent\":7"));
        assertTrue(first.contains("data:{"), "the data of an event is JSON");
        assertEquals("text/event-stream", response.getContentType().split(";")[0]);

        streams.push();
        for (int i = 0; i < 100 && count(response.getContentAsString(), "event:summary") < 2; i++) {
            TimeUnit.MILLISECONDS.sleep(20);
        }
        assertEquals(2, count(response.getContentAsString(), "event:summary"));
        assertEquals(2, count(response.getContentAsString(), "event:health"));
        streams.stop();
    }

    private static boolean waitForContent(org.springframework.mock.web.MockHttpServletResponse response, String text) throws Exception {
        for (int i = 0; i < 100; i++) {
            if (response.getContentAsString().contains(text)) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        return false;
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
