package com.umdc.mercury.monitoring.alerts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertEngine")
class AlertEngineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AlertRuleRepository rules;
    @Mock
    private AlertEventRepository events;
    @Mock
    private AlertEvaluator evaluator;
    @Mock
    private AlertNotifier notifier;
    @Mock
    private PlatformTransactionManager transactions;
    private AlertEngine engine;
    private AlertRuleEntity rule;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        engine = new AlertEngine(rules, events, evaluator, notifier, transactions, CLOCK);
        rule = new AlertRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setApplicationId(UUID.randomUUID());
        rule.setType("FAILURE_RATE");
        org.mockito.Mockito.lenient().when(rules.findByEnabledTrue()).thenReturn(List.of(rule));
    }

    private static AlertEvaluator.Reading reading(boolean breached, double value) {
        return new AlertEvaluator.Reading(breached, value, "detail " + value);
    }

    private AlertEventEntity firing() {
        var alert = new AlertEventEntity();
        alert.setId(UUID.randomUUID());
        alert.setRuleId(rule.getId());
        alert.setStatus("FIRING");
        alert.setValue(BigDecimal.ONE);
        alert.setStartedAt(LocalDateTime.of(2026, 11, 17, 7, 0));
        return alert;
    }

    @Test
    @DisplayName("a breached rule with nothing firing raises an alert and notifies once")
    void raises() {
        when(evaluator.read(rule, LocalDateTime.of(2026, 11, 17, 8, 0))).thenReturn(reading(true, 42.456));
        when(events.findFirstByRuleIdAndStatus(rule.getId(), "FIRING")).thenReturn(Optional.empty());

        assertEquals(1, engine.evaluateAll());

        var alert = ArgumentCaptor.forClass(AlertEventEntity.class);
        verify(events).saveAndFlush(alert.capture());
        assertEquals("FIRING", alert.getValue().getStatus());
        assertEquals(new BigDecimal("42.46"), alert.getValue().getValue());
        assertEquals(rule.getApplicationId(), alert.getValue().getApplicationId());
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), alert.getValue().getStartedAt());
        assertNull(alert.getValue().getResolvedAt());
        verify(notifier).notifyFiring(rule, alert.getValue());
    }

    @Test
    @DisplayName("a rule that stays breached keeps its one alert, updated, and notifies nobody again")
    void keepsFiring() {
        var open = firing();
        when(evaluator.read(any(), any())).thenReturn(reading(true, 50));
        when(events.findFirstByRuleIdAndStatus(rule.getId(), "FIRING")).thenReturn(Optional.of(open));

        assertEquals(0, engine.evaluateAll());

        verify(events).save(open);
        assertEquals(new BigDecimal("50.00"), open.getValue());
        assertEquals("FIRING", open.getStatus());
        verifyNoInteractions(notifier);
        verify(events, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("when the rule stops being breached its alert is resolved")
    void resolves() {
        var open = firing();
        when(evaluator.read(any(), any())).thenReturn(reading(false, 1));
        when(events.findFirstByRuleIdAndStatus(rule.getId(), "FIRING")).thenReturn(Optional.of(open));

        engine.evaluateAll();

        assertEquals("RESOLVED", open.getStatus());
        assertEquals(LocalDateTime.of(2026, 11, 17, 8, 0), open.getResolvedAt());
        verify(events).save(open);
    }

    @Test
    @DisplayName("a rule that is fine and has nothing firing does nothing")
    void quiet() {
        when(evaluator.read(any(), any())).thenReturn(reading(false, 0));
        when(events.findFirstByRuleIdAndStatus(rule.getId(), "FIRING")).thenReturn(Optional.empty());

        assertEquals(0, engine.evaluateAll());

        verify(events, never()).save(any());
        verify(events, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("another instance raising the same alert first (unique index) means no second alert and no second e-mail")
    void raceIsHarmless() {
        when(evaluator.read(any(), any())).thenReturn(reading(true, 30));
        when(events.findFirstByRuleIdAndStatus(rule.getId(), "FIRING")).thenReturn(Optional.empty());
        when(events.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertEquals(0, engine.evaluateAll());

        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("a rule that cannot be evaluated does not stop the others")
    void oneFailureDoesNotStopTheRest() {
        var other = new AlertRuleEntity();
        other.setId(UUID.randomUUID());
        other.setApplicationId(UUID.randomUUID());
        other.setType("QUEUE_STUCK");
        when(rules.findByEnabledTrue()).thenReturn(List.of(rule, other));
        when(evaluator.read(rule, LocalDateTime.of(2026, 11, 17, 8, 0))).thenThrow(new IllegalStateException("source down"));
        when(evaluator.read(other, LocalDateTime.of(2026, 11, 17, 8, 0))).thenReturn(reading(true, 9));
        when(events.findFirstByRuleIdAndStatus(other.getId(), "FIRING")).thenReturn(Optional.empty());

        assertEquals(1, engine.evaluateAll());
    }

    @Test
    @DisplayName("purges the resolved alerts older than the cutoff")
    void purges() {
        var cutoff = LocalDateTime.of(2026, 8, 1, 0, 0);
        when(events.deleteResolvedBefore(cutoff)).thenReturn(4);

        assertEquals(4, engine.purgeResolvedBefore(cutoff));
    }

    @Test
    @DisplayName("the production constructor reads the system clock")
    void productionConstructor() {
        var production = new AlertEngine(rules, events, evaluator, notifier, transactions);
        when(evaluator.read(any(), any())).thenReturn(reading(false, 0));
        when(events.findFirstByRuleIdAndStatus(rule.getId(), "FIRING")).thenReturn(Optional.empty());

        assertEquals(0, production.evaluateAll());
    }
}
