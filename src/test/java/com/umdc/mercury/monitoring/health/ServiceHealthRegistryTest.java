package com.umdc.mercury.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceHealthRegistry")
class ServiceHealthRegistryTest {

    @Mock
    private ServiceOutageRepository outages;
    @Mock
    private PlatformTransactionManager transactionManager;

    private MovableClock clock;
    private ServiceHealthRegistry registry;

    private static ServiceProbe probe(String name) {
        return new ServiceProbe() {
            @Override
            public String service() {
                return name;
            }

            @Override
            public Duration interval() {
                return Duration.ofSeconds(30);
            }

            @Override
            public ProbeResult check() {
                return ProbeResult.up(1);
            }
        };
    }

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        registry = new ServiceHealthRegistry(List.of(probe("SMTP"), probe("KAFKA")), outages, transactionManager, 10, clock);
    }

    private ServiceHealthRegistry.ServiceState state(String service) {
        return registry.snapshot().stream().filter(s -> s.service().equals(service)).findFirst().orElseThrow();
    }

    private List<ServiceOutageEntity> saved(int times) {
        var captor = ArgumentCaptor.forClass(ServiceOutageEntity.class);
        verify(outages, times(times)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("every probed service starts UNKNOWN and the snapshot is alphabetical")
    void startsUnknown() {
        var snapshot = registry.snapshot();

        assertEquals(List.of("KAFKA", "SMTP"), snapshot.stream().map(ServiceHealthRegistry.ServiceState::service).toList());
        assertTrue(snapshot.stream().allMatch(s -> s.status() == ServiceStatus.UNKNOWN && s.checkedAt() == null && s.latencyMs() == -1));
    }

    @Test
    @DisplayName("a clean check marks the service UP with its latency and opens no outage")
    void upOpensNothing() {
        registry.apply("SMTP", ProbeResult.up(42));

        assertEquals(ServiceStatus.UP, state("SMTP").status());
        assertEquals(42, state("SMTP").latencyMs());
        verify(outages, never()).save(any());
    }

    @Test
    @DisplayName("a failure opens an outage that is written, and the recovery closes it with the same id")
    void outageLifecycle() {
        registry.apply("SMTP", ProbeResult.failed(HealthCause.CONNECTION_FAILED, 5000));
        var opened = saved(1).getFirst();
        assertEquals("SMTP", opened.getService());
        assertEquals("DOWN", opened.getStatus());
        assertEquals("CONNECTION_FAILED", opened.getCauseCode());
        assertEquals("The service cannot be reached", opened.getCauseText());
        assertNull(opened.getEndedAt());
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 0), opened.getStartedAt());

        clock.advanceSeconds(90);
        registry.apply("SMTP", ProbeResult.up(20));

        var all = saved(2);
        var closed = all.get(1);
        assertEquals(opened.getId(), closed.getId());
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 1, 30), closed.getEndedAt());
        assertEquals(ServiceStatus.UP, state("SMTP").status());
    }

    @Test
    @DisplayName("the same failure checked again does not open another outage, and keeps the original start")
    void sameFailureIsOneOutage() {
        registry.apply("SMTP", ProbeResult.failed(HealthCause.TIMEOUT, 5000));
        var since = state("SMTP").since();
        clock.advanceSeconds(30);

        registry.apply("SMTP", ProbeResult.failed(HealthCause.TIMEOUT, 5000));

        saved(1);
        assertEquals(since, state("SMTP").since());
        assertTrue(state("SMTP").checkedAt().isAfter(since));
    }

    @Test
    @DisplayName("when the cause changes the first outage ends and a new one begins")
    void causeChangeSplitsTheOutage() {
        registry.apply("SMTP", ProbeResult.failed(HealthCause.TIMEOUT, 5000));
        clock.advanceSeconds(60);

        registry.apply("SMTP", ProbeResult.failed(HealthCause.QUOTA_EXCEEDED, 10));

        var writes = saved(3);
        assertEquals("DOWN", writes.get(0).getStatus());
        assertEquals("DOWN", writes.get(1).getStatus());
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 1), writes.get(1).getEndedAt());
        assertEquals("DEGRADED", writes.get(2).getStatus());
        assertNull(writes.get(2).getEndedAt());
    }

    @Test
    @DisplayName("NOT_CONFIGURED and UNKNOWN are not outages: they close an open one")
    void notConfiguredClosesOutage() {
        registry.apply("KAFKA", ProbeResult.failed(HealthCause.CONNECTION_FAILED, 1));
        registry.apply("KAFKA", ProbeResult.notConfigured(HealthCause.LISTENERS_OFF));

        assertEquals(ServiceStatus.NOT_CONFIGURED, state("KAFKA").status());
        assertEquals(2, saved(2).size());
    }

    @Test
    @DisplayName("a failure seen in real traffic marks the service and a clean check right after does not clear it")
    void trafficFailureHoldsAgainstCleanCheck() {
        registry.reportFailure("SMTP", HealthCause.QUOTA_EXCEEDED);
        assertEquals(ServiceStatus.DEGRADED, state("SMTP").status());
        assertEquals(HealthCause.QUOTA_EXCEEDED, state("SMTP").cause());

        clock.advanceSeconds(5 * 60);
        registry.apply("SMTP", ProbeResult.up(30));
        assertEquals(ServiceStatus.DEGRADED, state("SMTP").status(), "held for 10 minutes");

        clock.advanceSeconds(6 * 60);
        registry.apply("SMTP", ProbeResult.up(30));
        assertEquals(ServiceStatus.UP, state("SMTP").status(), "the hold is over");
    }

    @Test
    @DisplayName("a send that works clears a traffic failure at once; with nothing failing it does nothing")
    void successClearsTrafficFailure() {
        registry.reportSuccess("SMTP");
        verify(outages, never()).save(any());

        registry.reportFailure("SMTP", HealthCause.AUTH_REFUSED);
        assertEquals(ServiceStatus.DOWN, state("SMTP").status());
        registry.reportSuccess("SMTP");

        assertEquals(ServiceStatus.UP, state("SMTP").status());
        assertNull(state("SMTP").cause());
        saved(2);
    }

    @Test
    @DisplayName("a cause that is not an outage by default is reported as DEGRADED when it comes from traffic")
    void trafficFailureAlwaysAnOutage() {
        registry.reportFailure("SMTP", HealthCause.NOT_CONFIGURED);

        assertEquals(ServiceStatus.DEGRADED, state("SMTP").status());
    }

    @Test
    @DisplayName("an outage that cannot be written stays in memory and is written when the database is back")
    void pendingWritesAreRetried() {
        doThrow(new IllegalStateException("db down")).when(outages).save(any());

        registry.apply("SMTP", ProbeResult.failed(HealthCause.CONNECTION_FAILED, 1));

        assertEquals(1, registry.inMemoryOutages().size());
        org.mockito.Mockito.reset(outages);

        registry.flushPending();

        var written = saved(1).getFirst();
        assertEquals("SMTP", written.getService());
        registry.flushPending();
        verify(outages, times(1)).save(any());
    }

    @Test
    @DisplayName("the in-memory outages are the open ones; a closed one still waiting for the database is included too")
    void inMemoryOutages() {
        doThrow(new IllegalStateException("db down")).when(outages).save(any());
        registry.apply("SMTP", ProbeResult.failed(HealthCause.CONNECTION_FAILED, 1));
        registry.apply("KAFKA", ProbeResult.failed(HealthCause.TIMEOUT, 1));
        clock.advanceSeconds(30);
        registry.apply("KAFKA", ProbeResult.up(1));

        var all = registry.inMemoryOutages();

        assertEquals(2, all.size());
        assertEquals(1, all.stream().filter(o -> o.getEndedAt() == null).count());
    }

    @Test
    @DisplayName("on start the outages still open in the database are restored, so a recovery closes the same row")
    void restoresOpenOutages() {
        var row = new ServiceOutageEntity();
        row.setId(UUID.randomUUID());
        row.setService("SMTP");
        row.setStatus("DOWN");
        row.setCauseCode("CONNECTION_FAILED");
        row.setCauseText("The service cannot be reached");
        row.setStartedAt(LocalDateTime.of(2026, 11, 16, 11, 0));
        when(outages.findByEndedAtIsNull()).thenReturn(List.of(row));

        registry.loadOpenOutages();
        clock.advanceSeconds(10);
        registry.apply("SMTP", ProbeResult.up(5));

        var closed = saved(1).getFirst();
        assertEquals(row.getId(), closed.getId());
        assertEquals(LocalDateTime.of(2026, 11, 16, 11, 0), closed.getStartedAt());
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 0, 10), closed.getEndedAt());
    }

    @Test
    @DisplayName("a database that cannot be read at start is logged and does not stop Mercury")
    void restoreFailureIsSwallowed() {
        when(outages.findByEndedAtIsNull()).thenThrow(new IllegalStateException("db down"));

        registry.loadOpenOutages();

        verify(outages, atLeastOnce()).findByEndedAtIsNull();
        assertTrue(registry.inMemoryOutages().isEmpty());
    }

    @Test
    @DisplayName("the production constructor works with the system clock")
    void productionConstructor() {
        var real = new ServiceHealthRegistry(List.of(probe("SMTP")), outages, transactionManager, 10);

        real.reportFailure("SMTP", HealthCause.TIMEOUT);

        assertEquals(ServiceStatus.DOWN, real.snapshot().getFirst().status());
    }
}
