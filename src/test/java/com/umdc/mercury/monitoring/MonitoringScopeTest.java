package com.umdc.mercury.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MonitoringScope")
class MonitoringScopeTest {

    @Test
    @DisplayName("the nil UUID, and no id at all, mean every application; any other id narrows the view")
    void scope() {
        var application = UUID.randomUUID();

        assertTrue(MonitoringScope.isAll(MonitoringScope.ALL));
        assertTrue(MonitoringScope.isAll(null));
        assertFalse(MonitoringScope.isAll(application));
        assertNull(MonitoringScope.filter(MonitoringScope.ALL));
        assertNull(MonitoringScope.filter(null));
        assertEquals(application, MonitoringScope.filter(application));
        assertEquals("00000000-0000-0000-0000-000000000000", MonitoringScope.ALL.toString());
    }
}
