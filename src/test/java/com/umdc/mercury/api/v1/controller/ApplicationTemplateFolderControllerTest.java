package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.ApplicationTemplateFolderService;
import com.umdc.mercury.api.v1.to.ApplicationFolderResponse;
import com.umdc.mercury.api.v1.to.AssignFolderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationTemplateFolderControllerTest {

    private final ApplicationTemplateFolderService service = mock(ApplicationTemplateFolderService.class);
    private final ApplicationTemplateFolderController controller = new ApplicationTemplateFolderController(service);
    private final UUID application = UUID.randomUUID();

    @Test
    void getReturnsTheFolder() {
        var folder = new ApplicationFolderResponse(application, "iris", true);
        when(service.get(application, "t")).thenReturn(folder);

        var response = controller.get(application, "t");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(folder);
    }

    @Test
    void putSetsTheFolder() {
        var folder = new ApplicationFolderResponse(application, "iris", true);
        when(service.assign(application, "iris", "t")).thenReturn(folder);

        assertThat(controller.assign(application, new AssignFolderRequest("iris"), "t").getBody()).isEqualTo(folder);
    }
}
