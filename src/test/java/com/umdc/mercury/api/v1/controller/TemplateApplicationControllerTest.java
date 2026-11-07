package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.TemplateApplicationService;
import com.umdc.mercury.api.v1.to.TemplateApplicationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateApplicationControllerTest {

    @Test
    void passesTheSessionTokenAndReturnsTheApplications() {
        var service = mock(TemplateApplicationService.class);
        var items = List.of(new TemplateApplicationResponse(UUID.randomUUID(), "Iris", "iris"));
        when(service.listManageable("t")).thenReturn(items);

        assertEquals(items, new TemplateApplicationController(service).manageable("t").getBody());
    }
}
