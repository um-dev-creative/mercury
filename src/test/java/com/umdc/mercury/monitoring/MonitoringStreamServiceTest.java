package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.service.MonitoringService;
import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.monitoring.health.ServiceHealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT) // the first events are sent on another thread, which may not have run when a test ends
@DisplayName("MonitoringStreamService")
class MonitoringStreamServiceTest {

    /** An emitter that records the events sent to it (the real one buffers them until a response is attached). */
    private static final class RecordingEmitter extends SseEmitter {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<Object> data = new CopyOnWriteArrayList<>();
        volatile boolean failNext;
        volatile boolean completed;

        RecordingEmitter() {
            super(60_000L);
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            if (failNext) {
                throw new IOException("the viewer left");
            }
            var parts = builder.build();
            var name = new StringBuilder();
            for (var part : parts) {
                if (part.getData() instanceof String text && text.startsWith("event:")) {
                    name.append(text.substring(6).lines().findFirst().orElse("").strip());
                } else if (!(part.getData() instanceof String)) {
                    data.add(part.getData());
                }
            }
            events.add(name.toString());
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }

        boolean waitFor(int count) throws InterruptedException {
            for (int i = 0; i < 100 && events.size() < count; i++) {
                TimeUnit.MILLISECONDS.sleep(20);
            }
            return events.size() >= count;
        }
    }

    @Mock
    private MonitoringService monitoring;
    @Mock
    private ServiceHealthService health;

    private final List<RecordingEmitter> emitters = new ArrayList<>();

    private MonitoringStreamService service(int max, int perApp) {
        return new MonitoringStreamService(monitoring, health, max, perApp, 15) {
            @Override
            protected SseEmitter newEmitter(long timeout) {
                var emitter = new RecordingEmitter();
                emitters.add(emitter);
                return emitter;
            }
        };
    }

    private static MonitoringTO.SummaryResponse summary(String range) {
        return new MonitoringTO.SummaryResponse(range, LocalDateTime.now(), new MonitoringTO.Totals(1, 0, 0, 1.0), List.of(), List.of());
    }

    private static MonitoringTO.HealthResponse healthy() {
        return new MonitoringTO.HealthResponse(LocalDateTime.now(), "OPERATIONAL", List.of());
    }

    @Test
    @DisplayName("subscribing sends a summary and a health event right away")
    void firstEventsImmediately() throws Exception {
        var app = UUID.randomUUID();
        when(monitoring.summary(app, "1h")).thenReturn(summary("1h"));
        when(health.health()).thenReturn(healthy());
        var service = service(10, 5);

        service.subscribe(app, "1h");

        var emitter = emitters.getFirst();
        assertTrue(emitter.waitFor(2));
        assertEquals(List.of("summary", "health"), emitter.events.subList(0, 2));
        assertEquals(1, service.open());
        service.stop();
    }

    @Test
    @DisplayName("an unknown range is refused before a stream is opened")
    void unknownRange() {
        var service = service(10, 5);

        assertThrows(IllegalArgumentException.class, () -> service.subscribe(UUID.randomUUID(), "3d"));

        assertEquals(0, service.open());
        service.stop();
    }

    @Test
    @DisplayName("the server-wide and the per-application limits answer 429")
    void limits() {
        when(health.health()).thenReturn(healthy());
        when(monitoring.summary(any(), any())).thenReturn(summary("24h"));
        var service = service(3, 2);
        var app = UUID.randomUUID();
        service.subscribe(app, null);
        service.subscribe(app, null);

        var perApp = assertThrows(ResponseStatusException.class, () -> service.subscribe(app, null));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, perApp.getStatusCode());

        service.subscribe(UUID.randomUUID(), null);
        var total = assertThrows(ResponseStatusException.class, () -> service.subscribe(UUID.randomUUID(), null));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, total.getStatusCode());
        service.stop();
    }

    @Test
    @DisplayName("push sends the summary of each viewer's own application and range, built once per application and range, and one health event")
    void pushFansOut() throws Exception {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        when(monitoring.summary(a, "1h")).thenReturn(summary("1h"));
        when(monitoring.summary(b, "7d")).thenReturn(summary("7d"));
        when(health.health()).thenReturn(healthy());
        var service = service(10, 5);
        service.subscribe(a, "1h");
        service.subscribe(a, "1h");
        service.subscribe(b, "7d");
        for (var emitter : emitters) {
            assertTrue(emitter.waitFor(2));
        }
        org.mockito.Mockito.clearInvocations(monitoring, health);

        service.push();

        for (var emitter : emitters) {
            assertTrue(emitter.waitFor(4), "a second summary and health pair");
        }
        verify(monitoring, times(1)).summary(a, "1h");
        verify(monitoring, times(1)).summary(b, "7d");
        verify(health, times(1)).health();
        service.stop();
    }

    @Test
    @DisplayName("push with nobody listening does no work")
    void pushWithoutViewers() {
        var service = service(10, 5);

        service.push();

        verify(health, never()).health();
        verify(monitoring, never()).summary(any(), any());
        service.stop();
    }

    @Test
    @DisplayName("a viewer that left is dropped and its stream completed; the others keep receiving")
    void failedSendDropsTheViewer() throws Exception {
        var app = UUID.randomUUID();
        when(monitoring.summary(eq(app), any())).thenReturn(summary("24h"));
        when(health.health()).thenReturn(healthy());
        var service = service(10, 5);
        service.subscribe(app, null);
        service.subscribe(app, null);
        assertTrue(emitters.get(0).waitFor(2));
        assertTrue(emitters.get(1).waitFor(2));

        emitters.get(0).failNext = true;
        service.push();

        assertTrue(emitters.get(1).waitFor(4));
        for (int i = 0; i < 100 && service.open() > 1; i++) {
            TimeUnit.MILLISECONDS.sleep(20);
        }
        assertEquals(1, service.open());
        assertTrue(emitters.get(0).completed);
        service.stop();
    }

    @Test
    @DisplayName("a summary that cannot be built is reported as an error event instead of ending the stream")
    void summaryFailureKeepsTheStream() throws Exception {
        var app = UUID.randomUUID();
        when(monitoring.summary(eq(app), any())).thenThrow(new IllegalStateException("db down"));
        when(health.health()).thenReturn(healthy());
        var service = service(10, 5);

        service.subscribe(app, null);

        var emitter = emitters.getFirst();
        assertTrue(emitter.waitFor(2));
        assertEquals(java.util.Map.of("error", "SUMMARY_UNAVAILABLE"), emitter.data.getFirst());
        assertEquals(1, service.open());
        service.stop();
    }

    @Test
    @DisplayName("stopping completes every stream and forgets them; the real emitter factory builds a real SseEmitter")
    void stops() throws Exception {
        org.mockito.Mockito.lenient().when(health.health()).thenReturn(healthy());
        org.mockito.Mockito.lenient().when(monitoring.summary(any(), any())).thenReturn(summary("24h"));
        var service = service(10, 5);
        service.subscribe(UUID.randomUUID(), null);

        service.stop();

        assertTrue(emitters.getFirst().completed);
        assertEquals(0, service.open());
        var real = new MonitoringStreamService(monitoring, health, 1, 1, 1);
        assertEquals(60_000L, real.newEmitter(60_000L).getTimeout());
        real.stop();
    }
}
