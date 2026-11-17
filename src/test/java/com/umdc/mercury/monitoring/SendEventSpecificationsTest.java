package com.umdc.mercury.monitoring;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SendEventSpecifications")
@SuppressWarnings({"unchecked", "rawtypes"})
class SendEventSpecificationsTest {

    private final Root<SendEventEntity> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Predicate predicate = mock(Predicate.class);
    private final Path path = mock(Path.class);

    private void stubs() {
        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(Path.class), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.lessThan(any(Path.class), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
    }

    @Test
    @DisplayName("the application is always applied and the absent filters are left out")
    void onlyApplication() {
        stubs();
        var app = UUID.randomUUID();

        var result = SendEventSpecifications.search(app, null, null, null, null, null, null).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(root).get("applicationId");
        verify(root, never()).get("channel");
        verify(root, never()).get("status");
        verify(root, never()).get("kind");
        verify(root, never()).get("templateDefinedId");
        verify(root, never()).get("occurredAt");
    }

    @Test
    @DisplayName("every filter given is added to the predicate")
    void everyFilter() {
        stubs();
        var from = LocalDateTime.of(2026, 1, 1, 0, 0);

        SendEventSpecifications.search(UUID.randomUUID(), "EMAIL", "FAILED", "CAMPAIGN", UUID.randomUUID(), from, from.plusDays(1)).toPredicate(root, query, cb);

        verify(root).get("channel");
        verify(root).get("status");
        verify(root).get("kind");
        verify(root).get("templateDefinedId");
        verify(root, org.mockito.Mockito.times(2)).get("occurredAt");
        verify(cb).greaterThanOrEqualTo(any(Path.class), eq(from));
        verify(cb).lessThan(any(Path.class), eq(from.plusDays(1)));
    }
}
