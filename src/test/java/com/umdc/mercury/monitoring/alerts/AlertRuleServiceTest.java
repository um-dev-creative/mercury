package com.umdc.mercury.monitoring.alerts;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleService")
class AlertRuleServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T08:00:00Z"), ZoneOffset.UTC);
    private final UUID application = UUID.randomUUID();

    @Mock
    private AlertRuleRepository rules;
    @Mock
    private AlertEventRepository events;
    private AlertRuleService service;

    @BeforeEach
    void setUp() {
        service = new AlertRuleService(rules, events, CLOCK, 5);
    }

    private static MonitoringTO.AlertRuleRequest request(String type, Double threshold, Integer window, Integer volume, String serviceName, List<String> emails, Boolean enabled) {
        return new MonitoringTO.AlertRuleRequest(type, threshold, window, volume, serviceName, emails, enabled);
    }

    private AlertRuleEntity saved() {
        var captor = ArgumentCaptor.forClass(AlertRuleEntity.class);
        verify(rules).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("creates a failure-rate rule with its defaults: window 15, enabled, no e-mails")
    void createsWithDefaults() {
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));

        var rule = service.create(application, request("failure_rate", 20.0, null, 10, null, null, null));

        var entity = saved();
        assertEquals(application, entity.getApplicationId());
        assertEquals("FAILURE_RATE", entity.getType());
        assertEquals(15, entity.getWindowMinutes());
        assertEquals(10, entity.getMinVolume());
        assertTrue(entity.isEnabled());
        assertNull(entity.getNotifyEmails());
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), entity.getCreatedAt());
        assertEquals(20.0, rule.threshold());
        assertEquals(List.of(), rule.notifyEmails());
    }

    @Test
    @DisplayName("a service-down rule ignores the threshold, names a known service in upper case and keeps the e-mails de-duplicated")
    void serviceDownRule() {
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));

        var rule = service.create(application, request("SERVICE_DOWN", null, 5, 99, " smtp ", List.of("a@x.com", " a@x.com ", "b@x.com", " "), false));

        assertEquals("SMTP", rule.service());
        assertEquals(1.0, rule.threshold());
        assertEquals(0, rule.minVolume(), "minVolume only applies to FAILURE_RATE");
        assertEquals(List.of("a@x.com", "b@x.com"), rule.notifyEmails());
        assertFalse(rule.enabled());
    }

    @Test
    @DisplayName("a queue-stuck rule needs a message count")
    void queueStuckRule() {
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));

        assertEquals(50.0, service.create(application, request("QUEUE_STUCK", 50.0, 30, null, null, null, true)).threshold());
        assertThrows(IllegalArgumentException.class, () -> service.create(application, request("QUEUE_STUCK", null, 30, null, null, null, true)));
    }

    @Test
    @DisplayName("invalid rules are rejected before anything is stored")
    void rejectsInvalid() {
        for (var bad : List.of(
                request(null, 10.0, 5, 0, null, null, null),
                request(" ", 10.0, 5, 0, null, null, null),
                request("NOPE", 10.0, 5, 0, null, null, null),
                request("FAILURE_RATE", null, 5, 0, null, null, null),
                request("FAILURE_RATE", 0.0, 5, 0, null, null, null),
                request("FAILURE_RATE", 100.5, 5, 0, null, null, null),
                request("FAILURE_RATE", Double.NaN, 5, 0, null, null, null),
                request("FAILURE_RATE", Double.POSITIVE_INFINITY, 5, 0, null, null, null),
                request("QUEUE_STUCK", 1_000_001.0, 5, 0, null, null, null),
                request("FAILURE_RATE", 10.0, 0, 0, null, null, null),
                request("FAILURE_RATE", 10.0, 1441, 0, null, null, null),
                request("FAILURE_RATE", 10.0, 5, -1, null, null, null),
                request("FAILURE_RATE", 10.0, 5, 1_000_001, null, null, null),
                request("FAILURE_RATE", 10.0, 5, 0, "SMTP", null, null),
                request("SERVICE_DOWN", null, 5, 0, "REDIS", null, null),
                request("FAILURE_RATE", 10.0, 5, 0, null, List.of("a@x.com", "b@x.com", "c@x.com", "d@x.com", "e@x.com", "f@x.com"), null),
                request("FAILURE_RATE", 10.0, 5, 0, null, List.of("not-an-address"), null),
                request("FAILURE_RATE", 10.0, 5, 0, null, List.of("a@x.com,b@x.com"), null))) {
            assertThrows(IllegalArgumentException.class, () -> service.create(application, bad), String.valueOf(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> service.create(application, null));
        verify(rules, never()).save(any());
    }

    @Test
    @DisplayName("an application can have at most 20 rules")
    void limitsTheRules() {
        when(rules.countByApplicationId(application)).thenReturn(20L);

        assertThrows(IllegalArgumentException.class, () -> service.create(application, request("FAILURE_RATE", 10.0, 5, 0, null, null, null)));
    }

    @Test
    @DisplayName("the e-mail limit follows the configuration but never exceeds five")
    void emailLimit() {
        var strict = new AlertRuleService(rules, events, CLOCK, 1);
        var generous = new AlertRuleService(rules, events, CLOCK, 50);

        assertThrows(IllegalArgumentException.class, () -> strict.create(application, request("FAILURE_RATE", 10.0, 5, 0, null, List.of("a@x.com", "b@x.com"), null)));
        assertThrows(IllegalArgumentException.class, () -> generous.create(application,
                request("FAILURE_RATE", 10.0, 5, 0, null, List.of("a@x.com", "b@x.com", "c@x.com", "d@x.com", "e@x.com", "f@x.com"), null)));
    }

    @Test
    @DisplayName("lists the rules of the application, oldest first")
    void lists() {
        var rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setType("FAILURE_RATE");
        rule.setThreshold(new BigDecimal("12.50"));
        rule.setNotifyEmails("a@x.com, b@x.com");
        when(rules.findByApplicationIdOrderByCreatedAtAsc(application)).thenReturn(List.of(rule));

        var listed = service.list(application).rules().getFirst();

        assertEquals(12.5, listed.threshold());
        assertEquals(List.of("a@x.com", "b@x.com"), listed.notifyEmails());
    }

    @Test
    @DisplayName("updating changes the rule of the application; a rule of another application, or an unknown one, is null")
    void updates() {
        var rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setApplicationId(application);
        rule.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(rules.findById(rule.getId())).thenReturn(Optional.of(rule));
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));

        var updated = service.update(application, rule.getId(), request("QUEUE_STUCK", 5.0, 10, null, null, List.of("a@x.com"), false));

        assertEquals("QUEUE_STUCK", updated.type());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), updated.createdAt());
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), updated.updatedAt());
        assertNull(service.update(UUID.randomUUID(), rule.getId(), request("QUEUE_STUCK", 5.0, 10, null, null, null, null)));
    }

    @Test
    @DisplayName("deleting removes the rule of the application only")
    void deletes() {
        var rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setApplicationId(application);
        when(rules.findById(rule.getId())).thenReturn(Optional.of(rule));

        assertFalse(service.delete(UUID.randomUUID(), rule.getId()));
        verify(rules, never()).delete(any());
        assertTrue(service.delete(application, rule.getId()));
        verify(rules).delete(rule);
        assertFalse(service.delete(application, UUID.randomUUID()));
    }

    @Test
    @DisplayName("alerts returns what fires now and what started in the range, with the count that fires")
    void alerts() {
        var event = new AlertEventEntity();
        event.setId(UUID.randomUUID());
        event.setStatus("FIRING");
        event.setValue(new BigDecimal("33.30"));
        event.setDetail("detail");
        when(events.recent(any(), any(), any())).thenReturn(List.of(event));
        when(events.countByApplicationIdAndStatus(application, "FIRING")).thenReturn(1L);

        var response = service.alerts(application, "7d");

        assertEquals("7d", response.range());
        assertEquals(1, response.firing());
        assertEquals(33.3, response.alerts().getFirst().value());
        var since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(events).recent(any(), since.capture(), any());
        assertEquals(LocalDateTime.of(2026, 11, 10, 8, 0), since.getValue());
    }

    @Test
    @DisplayName("the production constructor reads the system clock")
    void productionConstructor() {
        var production = new AlertRuleService(rules, events, 5);
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));

        var rule = production.create(application, request("FAILURE_RATE", 10.0, 5, 0, null, null, null));

        assertTrue(rule.createdAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
    }

    @Test
    @DisplayName("no stored e-mails is an empty list")
    void splitEmails() {
        assertEquals(List.of(), AlertRuleService.splitEmails(null));
        assertEquals(List.of(), AlertRuleService.splitEmails(" "));
        assertEquals(List.of("a@x.com"), AlertRuleService.splitEmails("a@x.com,,"));
    }

    @Test
    @DisplayName("the services a SERVICE_DOWN rule may name are exactly the names the health probes report")
    void serviceNamesMatchTheProbes() {
        assertEquals(java.util.Set.of("POSTGRESQL", "MONGODB", "KAFKA", "SMTP", "BACKBONE", "TELEGRAM"), AlertRuleValidator.SERVICES);
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));

        assertEquals("MONGODB", service.create(application, request("SERVICE_DOWN", null, 5, 0, "mongodb", null, null)).service());
        assertEquals("POSTGRESQL", service.create(application, request("SERVICE_DOWN", null, 5, 0, "postgresql", null, null)).service());
    }
}
