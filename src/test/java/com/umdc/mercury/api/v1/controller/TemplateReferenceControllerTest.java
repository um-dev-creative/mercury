package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.TemplateReferenceService;
import com.umdc.mercury.api.v1.to.ReferenceItemResponse;
import com.umdc.mercury.api.v1.to.TemplateDefinedItemResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateReferenceControllerTest {

    @Mock private TemplateReferenceService service;
    @InjectMocks private TemplateReferenceController controller;

    @Test
    void templateTypesReturnTheList() {
        List<ReferenceItemResponse> items = List.of(new ReferenceItemResponse(UUID.randomUUID(), "VERIFICATION", null));
        when(service.listTemplateTypes()).thenReturn(items);

        ResponseEntity<List<ReferenceItemResponse>> response = controller.templateTypes("t");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(items);
    }

    @Test
    void severityTypesReturnTheList() {
        List<ReferenceItemResponse> items = List.of(new ReferenceItemResponse(UUID.randomUUID(), "INFO", "Information"));
        when(service.listSeverityTypes()).thenReturn(items);

        assertThat(controller.severityTypes("t").getBody()).isEqualTo(items);
    }

    @Test
    void definedPassesTheApplicationAndTheSession() {
        UUID applicationId = UUID.randomUUID();
        List<TemplateDefinedItemResponse> items = List.of(
                new TemplateDefinedItemResponse(UUID.randomUUID(), UUID.randomUUID(), "Welcome", "welcome.ftl", "FTL", "WELCOME"));
        when(service.listDefined(applicationId, "t")).thenReturn(items);

        assertThat(controller.defined(applicationId, "t").getBody()).isEqualTo(items);
    }
}
