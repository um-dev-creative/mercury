package com.umdc.mercury.monitoring.health;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    private static MonitoringTO.OutageQuery query(String range, LocalDateTime from, LocalDateTime to, String q, String service, String status,
                                                  Integer page, Integer size) {
        return new MonitoringTO.OutageQuery(range, from, to, q, service, status, page, size);
    }

    private void database(List<ServiceOutageEntity> rows, long total) {
        when(outages.search(any(), any(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(rows, call.getArgument(5, Pageable.class), total));
    }

    @Test
    @DisplayName("outages are newest first with their duration; an ongoing one counts up to now and has no end; the page says the window and total")
    void outagesFromTheDatabase() {
        var ended = outage("SMTP", NOW.minusHours(5), NOW.minusHours(4));
        var ongoing = outage("KAFKA", NOW.minusMinutes(10), null);
        database(List.of(ended, ongoing), 2);
        when(registry.inMemoryOutages()).thenReturn(List.of());

        var response = service.outages("24h");

        assertEquals("24h", response.range());
        assertEquals(NOW.minusHours(24), response.from());
        assertEquals(NOW, response.to());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.total());
        assertEquals(1, response.totalPages());
        assertEquals(List.of("KAFKA", "SMTP"), response.outages().stream().map(MonitoringTO.Outage::service).toList());
        var first = response.outages().getFirst();
        assertTrue(first.ongoing());
        assertNull(first.endedAt());
        assertEquals(600, first.durationSeconds());
        var second = response.outages().get(1);
        assertFalse(second.ongoing());
        assertEquals(3600, second.durationSeconds());
        assertEquals("TIMEOUT", second.cause());
        verify(outages).search(NOW.minusHours(24), NOW, "", "", "%", PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("the database gets the window, the filters (trimmed, upper case), an escaped LIKE pattern and the page asked for")
    void searchArguments() {
        database(List.of(), 41);
        when(registry.inMemoryOutages()).thenReturn(List.of());
        var from = LocalDateTime.of(2026, 10, 1, 0, 0);
        var to = LocalDateTime.of(2026, 10, 15, 0, 0);

        var response = service.outages(query(null, from, to, "  50%_Time!out ", " smtp ", "down", 2, 500));

        assertEquals("custom", response.range());
        assertEquals(from, response.from());
        assertEquals(to, response.to());
        assertEquals(2, response.page());
        assertEquals(100, response.size(), "at most 100 per page");
        assertEquals(41, response.total());
        verify(outages).search(from, to, "SMTP", "DOWN", "%50!%!_time!!out%", PageRequest.of(2, 100));
    }

    @Test
    @DisplayName("a window with only 'from' ends now; with only 'to' it starts the range before it")
    void halfWindows() {
        database(List.of(), 0);
        when(registry.inMemoryOutages()).thenReturn(List.of());

        var onlyFrom = service.outages(query(null, NOW.minusDays(3), null, null, null, null, null, null));
        assertEquals(NOW, onlyFrom.to());
        assertEquals(NOW.minusDays(3), onlyFrom.from());

        var onlyTo = service.outages(query("7d", null, NOW.minusDays(1), null, null, null, null, null));
        assertEquals(NOW.minusDays(8), onlyTo.from());
        assertEquals("custom", onlyTo.range());
    }

    @Test
    @DisplayName("bad windows, statuses, searches and services are refused")
    void refusesBadQueries() {
        assertThrows(IllegalArgumentException.class, () -> service.outages("3d"));
        assertThrows(IllegalArgumentException.class, () -> service.outages(query(null, NOW, NOW, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.outages(query(null, NOW, NOW.minusHours(1), null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.outages(query(null, null, null, null, null, "UP", null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.outages(query(null, null, null, "x".repeat(101), null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.outages(query(null, null, null, null, "S".repeat(31), null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.outages(query(null, null, null, null, null, "DEGRADEDXXX", null, null)));
    }

    @Test
    @DisplayName("an in-memory copy replaces its row; one the database does not have goes on the first page and counts in the total")
    void outagesMergeWithMemory() {
        var persisted = outage("SMTP", NOW.minusHours(2), null);
        var memoryCopy = outage("SMTP", NOW.minusHours(2), NOW.minusHours(1));
        memoryCopy.setId(persisted.getId());
        var onlyMemory = outage("POSTGRESQL", NOW.minusMinutes(30), NOW.minusMinutes(5));
        var old = outage("MONGODB", NOW.minusDays(5), NOW.minusDays(4));
        database(List.of(persisted), 1);
        when(outages.existsById(persisted.getId())).thenReturn(true);
        when(outages.existsById(onlyMemory.getId())).thenReturn(false);
        when(registry.inMemoryOutages()).thenReturn(List.of(memoryCopy, onlyMemory, old));

        var response = service.outages("24h");

        assertEquals(2, response.outages().size(), "the old one is outside the window");
        assertEquals(2, response.total());
        assertEquals(List.of("POSTGRESQL", "SMTP"), response.outages().stream().map(MonitoringTO.Outage::service).toList());
        assertFalse(response.outages().get(1).ongoing(), "the in-memory copy ended");

        var second = service.outages(query("24h", null, null, null, null, null, 1, 20));
        assertTrue(second.outages().stream().noneMatch(o -> o.service().equals("POSTGRESQL")), "only the first page carries them");
    }

    @Test
    @DisplayName("the in-memory outages go through the same filters: window, service, status and text in service, cause or sentence")
    void memoryFilters() {
        var smtp = outage("SMTP", NOW.minusMinutes(20), null);
        var kafka = outage("KAFKA", NOW.minusMinutes(20), null);
        kafka.setStatus("DEGRADED");
        kafka.setCauseCode("SLOW");
        kafka.setCauseText("Slow answers");
        var later = outage("MONGODB", NOW.plusHours(1), null);
        when(outages.search(any(), any(), anyString(), anyString(), anyString(), any(Pageable.class))).thenThrow(new IllegalStateException("db down"));
        when(registry.inMemoryOutages()).thenReturn(List.of(smtp, kafka, later));

        assertEquals(List.of("SMTP"), services(service.outages(query(null, null, null, null, "smtp", null, null, null))));
        assertEquals(List.of("KAFKA"), services(service.outages(query(null, null, null, null, null, "degraded", null, null))));
        assertEquals(List.of("KAFKA"), services(service.outages(query(null, null, null, "SLOW ANS", null, null, null, null))));
        assertEquals(List.of("KAFKA"), services(service.outages(query(null, null, null, "kaf", null, null, null, null))));
        assertEquals(List.of("SMTP"), services(service.outages(query(null, null, null, "did not answer", null, null, null, null))));
        assertEquals(List.of(), services(service.outages(query(null, null, null, "nothing like this", null, null, null, null))));
        assertEquals(2, service.outages("24h").total(), "the one that starts after the window is out");
    }

    private static List<String> services(MonitoringTO.OutagesResponse response) {
        return response.outages().stream().map(MonitoringTO.Outage::service).sorted().toList();
    }

    @Test
    @DisplayName("when the history cannot be read the answer is what is in memory, paged, not an error")
    void outagesWithoutDatabase() {
        when(outages.search(any(), any(), anyString(), anyString(), anyString(), any(Pageable.class))).thenThrow(new IllegalStateException("db down"));
        var three = List.of(outage("POSTGRESQL", NOW.minusMinutes(3), null), outage("SMTP", NOW.minusMinutes(2), null), outage("KAFKA", NOW.minusMinutes(1), null));
        when(registry.inMemoryOutages()).thenReturn(three);

        var first = service.outages(query(null, null, null, null, null, null, 0, 2));
        assertEquals("24h", first.range());
        assertEquals(3, first.total());
        assertEquals(List.of("KAFKA", "SMTP"), first.outages().stream().map(MonitoringTO.Outage::service).toList());
        var second = service.outages(query(null, null, null, null, null, null, 1, 2));
        assertEquals(List.of("POSTGRESQL"), second.outages().stream().map(MonitoringTO.Outage::service).toList());
        assertEquals(List.of(), service.outages(query(null, null, null, null, null, null, 9, 2)).outages());
    }

    @Test
    @DisplayName("a transaction that gives nothing back is an empty page")
    void emptyTransaction() {
        var none = org.mockito.Mockito.mock(PlatformTransactionManager.class);
        var quiet = new ServiceHealthService(registry, outages, none, 90, new MovableClock()) {
        };
        when(none.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(outages.search(any(), any(), anyString(), anyString(), anyString(), any(Pageable.class))).thenReturn(null);
        when(registry.inMemoryOutages()).thenReturn(List.of());

        var response = quiet.outages("1h");

        assertEquals(0, response.total());
        assertTrue(response.outages().isEmpty());
    }

    @Test
    @DisplayName("the query defaults: first page of 20; a negative page is the first; the list-only response has every outage on one page")
    void defaults() {
        var q = new MonitoringTO.OutageQuery(null, null, null, null, null, null, -3, 0);
        assertEquals(0, q.page());
        assertEquals(20, q.size());
        var all = new MonitoringTO.OutagesResponse("24h", List.of());
        assertEquals(0, all.total());
        assertEquals(0, all.totalPages());
        assertEquals(0, new MonitoringTO.OutagesResponse("24h", null, null, 0, 0, 5, List.of()).totalPages());
        assertEquals(3, new MonitoringTO.OutagesResponse("24h", null, null, 0, 2, 5, List.of()).totalPages());
        assertEquals("%", ServiceHealthService.likePattern(""));
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
