package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;
import com.umdc.mercury.client.to.BackboneApplication;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateApplicationServiceImplTest {

    private static final String TOKEN = "session";
    private static final String PERMISSION = "TEMPLATE_MANAGE";

    private final ApplicationDirectory applications = mock(ApplicationDirectory.class);
    private final ApplicationAuthorizationService authorization = mock(ApplicationAuthorizationService.class);
    private final TemplateApplicationServiceImpl service = new TemplateApplicationServiceImpl(applications, authorization, PERMISSION);

    private final BackboneApplication sakn = application("SmartKind", "SAKN");
    private final BackboneApplication iris = application("Iris", "iris");

    private static BackboneApplication application(String name, String code) {
        return new BackboneApplication(UUID.randomUUID(), name, code, true);
    }

    @Test
    void aPlatformAdministratorGetsEveryApplicationByNameWithoutPerApplicationChecks() {
        when(applications.active(TOKEN)).thenReturn(List.of(sakn, iris));
        when(authorization.isPlatformAdmin(TOKEN)).thenReturn(true);

        List<TemplateApplicationResponse> result = service.listManageable(TOKEN);

        assertEquals(List.of("Iris", "SmartKind"), result.stream().map(TemplateApplicationResponse::name).toList());
        assertEquals("iris", result.get(0).codeName());
        verify(authorization, never()).holds(any(), any(), any());
    }

    @Test
    void anyoneElseGetsOnlyTheApplicationsWhereTheyHoldThePermission() {
        when(applications.active(TOKEN)).thenReturn(List.of(sakn, iris));
        when(authorization.isPlatformAdmin(TOKEN)).thenReturn(false);
        when(authorization.holds(TOKEN, sakn.id(), PERMISSION)).thenReturn(true);
        when(authorization.holds(TOKEN, iris.id(), PERMISSION)).thenReturn(false);

        var result = service.listManageable(TOKEN);

        assertEquals(List.of(sakn.id()), result.stream().map(TemplateApplicationResponse::id).toList());
    }

    @Test
    void aCallerWithNoAccessGetsAnEmptyList() {
        when(applications.active(TOKEN)).thenReturn(List.of(sakn));

        assertTrue(service.listManageable(TOKEN).isEmpty());
    }
}
