package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
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

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationAuthorizationService authorization = mock(ApplicationAuthorizationService.class);
    private final TemplateApplicationServiceImpl service = new TemplateApplicationServiceImpl(applications, authorization, PERMISSION);

    private final ApplicationEntity sakn = application("SmartKind", "SAKN");
    private final ApplicationEntity iris = application("Iris", "iris");

    private static ApplicationEntity application(String name, String code) {
        var entity = mock(ApplicationEntity.class);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getName()).thenReturn(name);
        when(entity.getCodeName()).thenReturn(code);
        return entity;
    }

    @Test
    void aPlatformAdministratorGetsEveryApplicationByNameWithoutPerApplicationChecks() {
        when(applications.findAllByActiveTrue()).thenReturn(List.of(sakn, iris));
        when(authorization.isPlatformAdmin(TOKEN)).thenReturn(true);

        List<TemplateApplicationResponse> result = service.listManageable(TOKEN);

        assertEquals(List.of("Iris", "SmartKind"), result.stream().map(TemplateApplicationResponse::name).toList());
        assertEquals("iris", result.get(0).codeName());
        verify(authorization, never()).holds(any(), any(), any());
    }

    @Test
    void anyoneElseGetsOnlyTheApplicationsWhereTheyHoldThePermission() {
        when(applications.findAllByActiveTrue()).thenReturn(List.of(sakn, iris));
        when(authorization.isPlatformAdmin(TOKEN)).thenReturn(false);
        when(authorization.holds(TOKEN, sakn.getId(), PERMISSION)).thenReturn(true);
        when(authorization.holds(TOKEN, iris.getId(), PERMISSION)).thenReturn(false);

        var result = service.listManageable(TOKEN);

        assertEquals(List.of(sakn.getId()), result.stream().map(TemplateApplicationResponse::id).toList());
    }

    @Test
    void aCallerWithNoAccessGetsAnEmptyList() {
        when(applications.findAllByActiveTrue()).thenReturn(List.of(sakn));

        assertTrue(service.listManageable(TOKEN).isEmpty());
    }
}
