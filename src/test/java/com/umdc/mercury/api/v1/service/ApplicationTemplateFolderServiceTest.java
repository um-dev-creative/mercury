package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.to.ApplicationFolderResponse;
import com.umdc.mercury.template.ApplicationTemplateFolders;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationTemplateFolderServiceTest {

    private final ApplicationTemplateFolders folders = mock(ApplicationTemplateFolders.class);
    private final ApplicationAuthorizationService authorization = mock(ApplicationAuthorizationService.class);
    private final ApplicationTemplateFolderService service = new ApplicationTemplateFolderService(folders, authorization, "MANAGE", "READ");
    private final UUID application = UUID.randomUUID();

    @Test
    void getNeedsTheReadPermissionAndReportsTheFolder() {
        when(folders.folderOf(application)).thenReturn(Optional.of("iris"));

        ApplicationFolderResponse response = service.get(application, "t");

        verify(authorization).assertPermission("t", application, "READ");
        assertThat(response).isEqualTo(new ApplicationFolderResponse(application, "iris", true));
    }

    @Test
    void getReportsAnApplicationWithoutAFolder() {
        when(folders.folderOf(application)).thenReturn(Optional.empty());

        assertThat(service.get(application, "t")).isEqualTo(new ApplicationFolderResponse(application, null, false));
    }

    @Test
    void assignNeedsTheManagePermissionAndReturnsTheFolder() {
        when(folders.assign(application, "iris")).thenReturn("iris");

        ApplicationFolderResponse response = service.assign(application, "iris", "t");

        verify(authorization).assertPermission("t", application, "MANAGE");
        assertThat(response).isEqualTo(new ApplicationFolderResponse(application, "iris", true));
    }

    @Test
    void withoutPermissionNothingIsRead() {
        doThrow(new ForbiddenException("no")).when(authorization).assertPermission(any(), any(), any());

        assertThatThrownBy(() -> service.get(application, "t")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.assign(application, "iris", "t")).isInstanceOf(ForbiddenException.class);
        verify(folders, never()).assign(any(), any());
    }
}
