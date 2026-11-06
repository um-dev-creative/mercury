package com.umdc.mercury.template;

import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationTemplateFoldersTest {

    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final UUID iris = UUID.randomUUID();
    private final UUID smartkin = UUID.randomUUID();

    private ApplicationEntity application(UUID id, String codeName) throws Exception {
        ApplicationEntity entity = new ApplicationEntity();
        entity.setId(id);
        var field = ApplicationEntity.class.getDeclaredField("codeName");
        field.setAccessible(true);
        field.set(entity, codeName);
        return entity;
    }

    @Test
    void theFolderIsTheLowerCasedCodeNameOfTheApplication() throws Exception {
        when(repository.findById(iris)).thenReturn(Optional.of(application(iris, " Iris ")));
        var folders = new ApplicationTemplateFolders(repository, "");

        assertThat(folders.folderOf(iris)).contains("iris");
        assertThat(folders.nameOf(iris, "iris_welcome.ftl")).isEqualTo("iris/iris_welcome.ftl");
    }

    @Test
    void aConfiguredFolderWinsOverTheCodeName() throws Exception {
        when(repository.findById(smartkin)).thenReturn(Optional.of(application(smartkin, "SMARTKIN")));
        var folders = new ApplicationTemplateFolders(repository, smartkin + "=smartkind, " + UUID.randomUUID() + "=other");

        assertThat(folders.nameOf(smartkin, "sk_a.ftl")).isEqualTo("smartkind/sk_a.ftl");
        verify(repository, times(0)).findById(smartkin);
    }

    @Test
    void anApplicationWithoutAUsableFolderKeepsTheBareFileName() throws Exception {
        UUID noCode = UUID.randomUUID();
        UUID badCode = UUID.randomUUID();
        when(repository.findById(noCode)).thenReturn(Optional.of(application(noCode, null)));
        when(repository.findById(badCode)).thenReturn(Optional.of(application(badCode, "N/A")));
        var folders = new ApplicationTemplateFolders(repository, "");

        assertThat(folders.nameOf(noCode, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(badCode, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(UUID.randomUUID(), "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(null, "a.ftl")).isEqualTo("a.ftl");
    }

    @Test
    void aNameThatAlreadyHasAFolderIsLeftAlone() {
        var folders = new ApplicationTemplateFolders(repository, "");

        assertThat(folders.nameOf(iris, "zerobound/a.ftl")).isEqualTo("zerobound/a.ftl");
        assertThat(folders.nameOf(iris, null)).isNull();
    }

    @Test
    void theFolderIsLookedUpOnlyOnce() throws Exception {
        when(repository.findById(iris)).thenReturn(Optional.of(application(iris, "iris")));
        var folders = new ApplicationTemplateFolders(repository, "");

        folders.folderOf(iris);
        folders.folderOf(iris);

        verify(repository, times(1)).findById(iris);
    }

    @Test
    void aMissingFolderIsNotCachedSoALaterCodeIsPickedUp() throws Exception {
        when(repository.findById(iris)).thenReturn(Optional.empty(), Optional.of(application(iris, "iris")));
        var folders = new ApplicationTemplateFolders(repository, "");

        assertThat(folders.folderOf(iris)).isEmpty();
        assertThat(folders.folderOf(iris)).contains("iris");
    }

    @Test
    void aBadConfigurationStopsTheStartupWithAClearMessage() {
        assertThatThrownBy(() -> new ApplicationTemplateFolders(repository, "no-equals"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("<application id or code_name>=<folder>");
        assertThatThrownBy(() -> new ApplicationTemplateFolders(repository, iris + "=Bad Folder"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not a valid folder");
        assertThatThrownBy(() -> new ApplicationTemplateFolders(repository, "=iris"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing application id or code_name");
    }

    @Test
    void emptyConfigurationIsFine() {
        assertThat(ApplicationTemplateFolders.parse(null)).isEmpty();
        assertThat(ApplicationTemplateFolders.parse("  ")).isEmpty();
    }

    @Test
    void aFolderConfiguredForTheCodeNameAppliesToEveryApplicationWithThatCode() throws Exception {
        UUID zerobound = UUID.randomUUID();
        UUID smartKin = UUID.randomUUID();
        UUID luckyLeaf = UUID.randomUUID();
        UUID luckyLeafRest = UUID.randomUUID();
        when(repository.findById(zerobound)).thenReturn(Optional.of(application(zerobound, "zeronbou")));
        when(repository.findById(smartKin)).thenReturn(Optional.of(application(smartKin, "SAKN")));
        when(repository.findById(luckyLeaf)).thenReturn(Optional.of(application(luckyLeaf, "lucky_le")));
        when(repository.findById(luckyLeafRest)).thenReturn(Optional.of(application(luckyLeafRest, "lucky_le")));
        var folders = new ApplicationTemplateFolders(repository, "SAKN=smartkind, Zeronbou=zerobound");

        assertThat(folders.nameOf(zerobound, "zb_a.ftl")).isEqualTo("zerobound/zb_a.ftl");
        assertThat(folders.nameOf(smartKin, "sk_a.ftl")).isEqualTo("smartkind/sk_a.ftl");
        assertThat(folders.nameOf(luckyLeaf, "a.ftl")).isEqualTo("lucky_le/a.ftl");
        assertThat(folders.nameOf(luckyLeafRest, "a.ftl")).isEqualTo("lucky_le/a.ftl");
    }

    @Test
    void anIdEntryWinsOverACodeEntry() throws Exception {
        when(repository.findById(iris)).thenReturn(Optional.of(application(iris, "iris")));
        var folders = new ApplicationTemplateFolders(repository, "iris=by-code, " + iris + "=by-id");

        assertThat(folders.folderOf(iris)).contains("by-id");
    }
}
