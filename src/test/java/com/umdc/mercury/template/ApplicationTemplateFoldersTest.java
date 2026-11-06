package com.umdc.mercury.template;

import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.umdc.mercury.template.TemplateFolderFixtures.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationTemplateFoldersTest {

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final UUID iris = UUID.randomUUID();

    @Test
    void theFolderIsTheLowerCasedCodeNameOfTheApplication() {
        when(repository.findById(iris)).thenReturn(Optional.of(application(iris, " Iris ")));
        var folders = new ApplicationTemplateFolders(repository);

        assertThat(folders.folderOf(iris)).contains("iris");
        assertThat(folders.nameOf(iris, "iris_welcome.ftl")).isEqualTo("iris/iris_welcome.ftl");
    }

    @Test
    void everyApplicationUsesItsOwnCodeNameWithoutAnyMapping() {
        UUID smartKin = UUID.randomUUID();
        UUID zerobound = UUID.randomUUID();
        UUID luckyLeaf = UUID.randomUUID();
        UUID luckyLeafRest = UUID.randomUUID();
        when(repository.findById(smartKin)).thenReturn(Optional.of(application(smartKin, "SAKN")));
        when(repository.findById(zerobound)).thenReturn(Optional.of(application(zerobound, "zeronbou")));
        when(repository.findById(luckyLeaf)).thenReturn(Optional.of(application(luckyLeaf, "lucky_le")));
        when(repository.findById(luckyLeafRest)).thenReturn(Optional.of(application(luckyLeafRest, "lucky_le")));
        var folders = new ApplicationTemplateFolders(repository);

        assertThat(folders.nameOf(smartKin, "sk_a.ftl")).isEqualTo("sakn/sk_a.ftl");
        assertThat(folders.nameOf(zerobound, "zb_a.ftl")).isEqualTo("zeronbou/zb_a.ftl");
        assertThat(folders.nameOf(luckyLeaf, "a.ftl")).isEqualTo("lucky_le/a.ftl");
        assertThat(folders.nameOf(luckyLeafRest, "a.ftl")).isEqualTo("lucky_le/a.ftl");
    }

    @Test
    void anApplicationWithoutAUsableCodeNameKeepsTheBareFileName() {
        UUID noCode = UUID.randomUUID();
        UUID badCode = UUID.randomUUID();
        when(repository.findById(noCode)).thenReturn(Optional.of(application(noCode, null)));
        when(repository.findById(badCode)).thenReturn(Optional.of(application(badCode, "N/A")));
        var folders = new ApplicationTemplateFolders(repository);

        assertThat(folders.nameOf(noCode, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(badCode, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(UUID.randomUUID(), "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(null, "a.ftl")).isEqualTo("a.ftl");
    }

    @Test
    void aNameThatAlreadyHasAFolderIsLeftAlone() {
        var folders = new ApplicationTemplateFolders(repository);

        assertThat(folders.nameOf(iris, "zerobound/a.ftl")).isEqualTo("zerobound/a.ftl");
        assertThat(folders.nameOf(iris, null)).isNull();
    }

    @Test
    void theFolderIsLookedUpOnlyOnce() {
        when(repository.findById(iris)).thenReturn(Optional.of(application(iris, "iris")));
        var folders = new ApplicationTemplateFolders(repository);

        folders.folderOf(iris);
        folders.folderOf(iris);

        verify(repository, times(1)).findById(iris);
    }

    @Test
    void aMissingFolderIsNotCachedSoALaterCodeIsPickedUp() {
        when(repository.findById(iris)).thenReturn(Optional.empty(), Optional.of(application(iris, "iris")));
        var folders = new ApplicationTemplateFolders(repository);

        assertThat(folders.folderOf(iris)).isEmpty();
        assertThat(folders.folderOf(iris)).contains("iris");
    }
}
