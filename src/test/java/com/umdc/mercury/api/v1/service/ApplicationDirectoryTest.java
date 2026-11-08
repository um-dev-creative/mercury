package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.client.BackboneApplicationClient;
import com.umdc.mercury.client.to.BackboneApplication;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApplicationDirectoryTest {

    private final BackboneApplicationClient client = mock(BackboneApplicationClient.class);
    private final ApplicationDirectory directory = new ApplicationDirectory(client);

    private final BackboneApplication iris = new BackboneApplication(UUID.randomUUID(), "Iris", "iris", true);
    private final BackboneApplication off = new BackboneApplication(UUID.randomUUID(), "Old", "OLD", false);
    private final BackboneApplication unknown = new BackboneApplication(UUID.randomUUID(), "New", null, null);

    @Test
    void theSessionGoesToBackboneAsBearer() {
        when(client.applications("Bearer session")).thenReturn(List.of(iris));

        assertEquals(List.of(iris), directory.all(" session "));
    }

    @Test
    void onlyEnabledApplicationsAreActive() {
        when(client.applications("Bearer s")).thenReturn(List.of(iris, off, unknown));

        assertEquals(List.of(iris), directory.active("s"));
        assertTrue(iris.isActive());
        assertFalse(unknown.isActive());
    }

    @Test
    void aCodeNameIsMatchedIgnoringCaseAndApplicationsWithoutOneAreSkipped() {
        when(client.applications("Bearer s")).thenReturn(List.of(iris, off, unknown));

        assertEquals(List.of(off), directory.byCodeName("s", "old"));
        assertTrue(directory.byCodeName("s", "none").isEmpty());
    }

    @Test
    void anEmptyOrNullAnswerIsNoApplications() {
        when(client.applications("Bearer s")).thenReturn(null);
        assertTrue(directory.all("s").isEmpty());

        when(client.applications("Bearer s")).thenReturn(Arrays.asList(iris, null));
        assertEquals(List.of(iris), directory.all("s"));
    }

    @Test
    void withoutSessionNothingIsAskedAndTheCallerIsRefused() {
        for (String token : new String[] {null, " "}) {
            assertThrows(ForbiddenException.class, () -> directory.all(token));
        }
        verifyNoInteractions(client);
    }

    @Test
    void whenBackboneRefusesOrFailsTheCallerIsRefused() {
        when(client.applications("Bearer s")).thenThrow(new IllegalStateException("401"));

        assertThrows(ForbiddenException.class, () -> directory.active("s"));
    }
}
