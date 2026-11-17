package com.umdc.mercury.monitoring.health;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceHealthService")
class ServiceHealthServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Mock
    private ServiceHealthRegistry registry;
    @Mock
    private ServiceOutageRepository outages;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ServiceHealthService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new ServiceHealthService(registry, outages, transactionManager, 90, new MovableClock());
    }

    private static ServiceHealthRegistry.ServiceState state(String name, ServiceStatus status, HealthCause cause, long latency) {
        var at = Instant.parse("2026-11-16T11:00:00Z");
        return new ServiceHealthRegistry.ServiceState(name, status, latency, cause, at, at.plusSeconds(60));
    }

    private static ServiceOutageEntity outage(String service, LocalDateTime start, LocalDateTime end) {
        var entity = new ServiceOutageEntity();
        entity.setId(UUID.randomUUID());
        entity.setService(service);
        entity.setStatus("DOWN");
        entity.setCauseCode("TIMEOUT");
        entity.setCauseText("The service did not answer in time");
        entity.setStartedAt(start);
        entity.setEndedAt(end);
        return entity;
    }

    @Test
    @DisplayName("health shows each service with its latency, cause code and fixed sentence; no latency is null")
    void healthViews() {
        when(registry.snapshot()).thenReturn(List.of(state("KAFKA", ServiceStatus.NOT_CONFIGURED, HealthCause.LISTENERS_OFF, -1),
                state("SMTP", ServiceStatus.UP, null, 120)));

        var health = service.health();

        assertEquals(NOW, health.generatedAt());
        var kafka = health.services().getFirst();
        assertNull(kafka.latencyMs());
        assertEquals("LISTENERS_OFF", kafka.cause());
        assertEquals("Kafka listeners are switched off in this environment", kafka.causeText());
        var smtp = health.services().get(1);
        assertEquals(120L, smtp.latencyMs());
        assertNull(smtp.cause());
        assertNull(smtp.causeText());
        assertEquals(LocalDateTime.of(2026, 11, 16, 11, 0), smtp.since());
        assertEquals(LocalDateTime.of(2026, 11, 16, 11, 1), smtp.lastCheckedAt());
    }

    @Test
    @DisplayName("the overall state: any DOWN is OUTAGE, else any DEGRADED, nothing checked is UNKNOWN, otherwise OPERATIONAL")
    void overall() {
        when(registry.snapshot()).thenReturn(List.of(state("A", ServiceStatus.UP, null, 1), state("B", ServiceStatus.DOWN, HealthCause.TIMEOUT, 1),
                state("C", ServiceStatus.DEGRADED, HealthCause.SLOW, 1)));
        assertEquals("OUTAGE", service.health().overall());

        when(registry.snapshot()).thenReturn(List.of(state("A", ServiceStatus.UP, null, 1), state("C", ServiceStatus.DEGRADED, HealthCause.SLOW, 1)));
        assertEquals("DEGRADED", service.health().overall());

        when(registry.snapshot()).thenReturn(List.of(state("A", ServiceStatus.UNKNOWN, null, -1), state("K", ServiceStatus.NOT_CONFIGURED, HealthCause.NOT_CONFIGURED, -1)));
        assertEquals("UNKNOWN", service.health().overall());

        when(registry.snapshot()).thenReturn(List.of(state("K", ServiceStatus.NOT_CONFIGURED, HealthCause.NOT_CONFIGURED, -1)));
        assertEquals("UNKNOWN", service.health().overall(), "nothing to check");

        when(registry.snapshot()).thenReturn(List.of(state("A", ServiceStatus.UP, null, 1), state("K", ServiceStatus.NOT_CONFIGURED, HealthCause.NOT_CONFIGURED, -1)));
        assertEquals("OPERATIONAL", service.health().overall());

        when(registry.snapshot()).thenReturn(List.of());
        assertEquals("UNKNOWN", service.health().overall());
    }

    @Test
    @DisplayName("outages are newest first with their duration; an ongoing one counts up to now and has no end")
    void outagesFromTheDatabase() {
        var ended = outage("SMTP", NOW.minusHours(5), NOW.minusHours(4));
        var ongoing = outage("KAFKA", NOW.minusMinutes(10), null);
        when(outages.findOverlapping(NOW.minusHours(24))).thenReturn(List.of(ended, ongoing));
        when(registry.inMemoryOutages()).thenReturn(List.of());

        var response = service.outages("24h");

        assertEquals("24h", response.range());
        assertEquals(List.of("KAFKA", "SMTP"), response.outages().stream().map(MonitoringTO.Outage::service).toList());
        var first = response.outages().getFirst();
        assertTrue(first.ongoing());
        assertNull(first.endedAt());
        assertEquals(600, first.durationSeconds());
        var second = response.outages().get(1);
        assertFalse(second.ongoing());
        assertEquals(3600, second.durationSeconds());
        assertEquals("TIMEOUT", second.cause());
    }

    @Test
    @DisplayName("an outage only in memory (PostgreSQL was down) is added; one already in the database is replaced by the newer in-memory copy")
    void outagesMergeWithMemory() {
        var persisted = outage("SMTP", NOW.minusHours(2), null);
        var memoryCopy = outage("SMTP", NOW.minusHours(2), NOW.minusHours(1));
        memoryCopy.setId(persisted.getId());
        var onlyMemory = outage("POSTGRESQL", NOW.minusMinutes(30), NOW.minusMinutes(5));
        var old = outage("MONGODB", NOW.minusDays(5), NOW.minusDays(4));
        when(outages.findOverlapping(NOW.minusHours(24))).thenReturn(List.of(persisted));
        when(registry.inMemoryOutages()).thenReturn(List.of(memoryCopy, onlyMemory, old));

        var response = service.outages("24h");

        assertEquals(2, response.outages().size(), "the old one is outside the range");
        assertEquals(List.of("POSTGRESQL", "SMTP"), response.outages().stream().map(MonitoringTO.Outage::service).toList());
        assertFalse(response.outages().get(1).ongoing(), "the in-memory copy ended");
    }

    @Test
    @DisplayName("when the history cannot be read the answer is what is in memory, not an error")
    void outagesWithoutDatabase() {
        when(outages.findOverlapping(any())).thenThrow(new IllegalStateException("db down"));
        when(registry.inMemoryOutages()).thenReturn(List.of(outage("POSTGRESQL", NOW.minusMinutes(3), null)));

        var response = service.outages(null);

        assertEquals("24h", response.range());
        assertEquals(1, response.outages().size());
        assertTrue(response.outages().getFirst().ongoing());
    }

    @Test
    @DisplayName("an unknown range is refused")
    void unknownRange() {
        assertThrows(IllegalArgumentException.class, () -> service.outages("3d"));
    }

    @Test
    @DisplayName("purgeExpired deletes the outages that ended before the retention period and reports how many")
    void purges() {
        when(outages.deleteEndedBefore(any())).thenReturn(4);

        assertEquals(4, service.purgeExpired());

        var captor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outages).deleteEndedBefore(captor.capture());
        assertEquals(NOW.minusDays(90), captor.getValue());
    }

    @Test
    @DisplayName("the production constructor works with the system clock")
    void productionConstructor() {
        var real = new ServiceHealthService(registry, outages, transactionManager, 90, (com.umdc.mercury.monitoring.MonitoringStorage) null);
        when(registry.snapshot()).thenReturn(List.of());

        assertTrue(real.health().generatedAt().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1)));
    }

    @Test
    @DisplayName("the health answer carries what the storage check says (missing tables, failed recordings), or says all is fine without a check")
    void healthCarriesTheStorageStatus() {
        var storage = org.mockito.Mockito.mock(com.umdc.mercury.monitoring.MonitoringStorage.class);
        var missing = new MonitoringTO.Storage(false, List.of("send_events"), List.of("V2039__send_events.sql"), 3);
        when(storage.status()).thenReturn(missing);
        when(registry.snapshot()).thenReturn(List.of());

        var checked = new ServiceHealthService(registry, outages, transactionManager, 90, java.time.Clock.systemUTC(), storage);

        assertEquals(missing, checked.health().storage());
        assertTrue(new ServiceHealthService(registry, outages, transactionManager, 90, java.time.Clock.systemUTC()).health().storage().ready());
    }
}
