package com.umdc.mercury.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceHealthScheduler")
class ServiceHealthSchedulerTest {

    @FunctionalInterface
    private interface Check {
        ProbeResult run() throws Exception;
    }

    private record FakeProbe(String service, Duration interval, Check action, AtomicInteger calls) implements ServiceProbe {
        @Override
        public ProbeResult check() throws Exception {
            calls.incrementAndGet();
            return action.run();
        }
    }

    @Mock
    private ServiceHealthRegistry registry;

    private MovableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
    }

    private static FakeProbe probe(String name, long intervalSeconds, Check check) {
        return new FakeProbe(name, Duration.ofSeconds(intervalSeconds), check, new AtomicInteger());
    }

    @Test
    @DisplayName("every probe runs the first time and the result goes to the registry")
    void runsEveryProbeFirst() {
        var a = probe("A", 30, () -> ProbeResult.up(3));
        var b = probe("B", 30, () -> ProbeResult.failed(HealthCause.TIMEOUT, 5000));
        var scheduler = new ServiceHealthScheduler(List.of(a, b), registry, Duration.ofSeconds(5), clock);

        scheduler.runDueProbes();

        verify(registry).apply("A", ProbeResult.up(3));
        verify(registry).apply("B", ProbeResult.failed(HealthCause.TIMEOUT, 5000));
    }

    @Test
    @DisplayName("a probe is not run again before its own interval passes; a slower one waits longer than a faster one")
    void respectsIntervals() {
        var fast = probe("FAST", 30, () -> ProbeResult.up(1));
        var slow = probe("SLOW", 300, () -> ProbeResult.up(1));
        var scheduler = new ServiceHealthScheduler(List.of(fast, slow), registry, Duration.ofSeconds(5), clock);

        scheduler.runDueProbes();
        clock.advanceSeconds(15);
        scheduler.runDueProbes();
        assertEquals(1, fast.calls().get());

        clock.advanceSeconds(15);
        scheduler.runDueProbes();
        assertEquals(2, fast.calls().get());
        assertEquals(1, slow.calls().get());

        clock.advanceSeconds(300);
        scheduler.runDueProbes();
        assertEquals(2, slow.calls().get());
    }

    @Test
    @DisplayName("a probe that throws is a failed check, with no message passed on, and the others still run")
    void exceptionIsACheckError() {
        var broken = probe("BROKEN", 30, () -> {
            throw new IllegalStateException("password=secret host=db1");
        });
        var fine = probe("FINE", 30, () -> ProbeResult.up(2));
        var scheduler = new ServiceHealthScheduler(List.of(broken, fine), registry, Duration.ofSeconds(5), clock);

        scheduler.runDueProbes();

        verify(registry).apply("BROKEN", ProbeResult.failed(HealthCause.CHECK_ERROR, -1));
        verify(registry).apply("FINE", ProbeResult.up(2));
    }

    @Test
    @DisplayName("an interrupted probe is a failed check")
    void interruptedProbe() {
        var interrupted = probe("INT", 30, () -> {
            throw new InterruptedException();
        });
        var scheduler = new ServiceHealthScheduler(List.of(interrupted), registry, Duration.ofSeconds(5), clock);

        scheduler.runDueProbes();
        Thread.interrupted();

        verify(registry).apply("INT", ProbeResult.failed(HealthCause.CHECK_ERROR, -1));
    }

    @Test
    @DisplayName("a probe that hangs is cut off at the hard limit and reported as TIMEOUT, without holding back the others")
    void hangingProbeTimesOut() {
        var hanging = probe("HANG", 30, () -> {
            Thread.sleep(5000);
            return ProbeResult.up(1);
        });
        var fine = probe("FINE", 30, () -> ProbeResult.up(1));
        var scheduler = new ServiceHealthScheduler(List.of(hanging, fine), registry, Duration.ofMillis(200), clock);

        scheduler.runDueProbes();

        verify(registry).apply("HANG", ProbeResult.failed(HealthCause.TIMEOUT, -1));
        verify(registry).apply("FINE", ProbeResult.up(1));
        scheduler.stop();
    }

    @Test
    @DisplayName("tick runs the due probes and then writes the outages that were waiting for the database")
    void tickFlushes() {
        var a = probe("A", 30, () -> ProbeResult.up(1));
        var scheduler = new ServiceHealthScheduler(List.of(a), registry, Duration.ofSeconds(5), clock);

        scheduler.tick();

        verify(registry).apply("A", ProbeResult.up(1));
        verify(registry).flushPending();
    }

    @Test
    @DisplayName("nothing is applied when there are no probes; the production constructor builds")
    void noProbes() {
        var scheduler = new ServiceHealthScheduler(List.of(), registry, 5);

        scheduler.runDueProbes();

        verify(registry, never()).apply(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        scheduler.stop();
    }
}
