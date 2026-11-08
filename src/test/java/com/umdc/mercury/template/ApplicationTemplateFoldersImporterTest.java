package com.umdc.mercury.template;

import com.umdc.mercury.jpa.sql.repository.ApplicationTemplateFolderRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationTemplateFoldersImporterTest {

    private final ApplicationTemplateFolders folders = mock(ApplicationTemplateFolders.class);
    private final UUID iris = UUID.randomUUID();

    @Test
    void registersTheConfiguredFoldersAtStartup() {
        when(folders.importMissing(any())).thenReturn(1);

        new ApplicationTemplateFoldersImporter(folders, iris + "=iris").run(null);

        verify(folders).importMissing(Map.of(iris, "iris"));
    }

    @Test
    void doesNothingWithoutConfiguration() {
        new ApplicationTemplateFoldersImporter(folders, "").run(null);

        verify(folders, never()).importMissing(any());
    }

    @Test
    void aFailureOfTheRegistryDoesNotStopTheStart() {
        when(folders.importMissing(any())).thenThrow(new IllegalStateException("table missing"));

        assertThatCode(() -> new ApplicationTemplateFoldersImporter(folders, iris + "=iris").run(null)).doesNotThrowAnyException();
    }

    @Test
    void aMalformedConfigurationStopsTheStartWithAClearMessage() {
        assertThatThrownBy(() -> new ApplicationTemplateFoldersImporter(folders, "SAKN=smartkind").run(null))
                .hasMessageContaining("not an application id");
    }

    @Test
    void theImporterWorksWithARealRegistry() {
        var repository = mock(ApplicationTemplateFolderRepository.class);
        var real = new ApplicationTemplateFolders(repository, 60);

        new ApplicationTemplateFoldersImporter(real, iris + "=iris").run(null);

        verify(repository).saveAndFlush(any());
    }
}
