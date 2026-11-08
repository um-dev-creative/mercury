package com.umdc.mercury.template;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTemplateFoldersTest {

    private final UUID iris = UUID.randomUUID();
    private final UUID smartkin = UUID.randomUUID();

    @Test
    void theFolderIsTheOneConfiguredForTheApplicationId() {
        var folders = new ApplicationTemplateFolders(iris + "=iris, " + smartkin + "=smartkind");

        assertThat(folders.folderOf(iris)).contains("iris");
        assertThat(folders.nameOf(iris, "iris_welcome.ftl")).isEqualTo("iris/iris_welcome.ftl");
        assertThat(folders.nameOf(smartkin, "sk_a.ftl")).isEqualTo("smartkind/sk_a.ftl");
    }

    @Test
    void anApplicationWithoutAnEntryKeepsTheBareFileName() {
        var folders = new ApplicationTemplateFolders(iris + "=iris");
        UUID other = UUID.randomUUID();

        assertThat(folders.folderOf(other)).isEmpty();
        assertThat(folders.nameOf(other, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(other, "a.ftl")).isEqualTo("a.ftl");
        assertThat(folders.nameOf(null, "a.ftl")).isEqualTo("a.ftl");
    }

    @Test
    void aLocationThatAlreadyHasAFolderIsLeftAlone() {
        var folders = new ApplicationTemplateFolders(iris + "=iris");

        assertThat(folders.nameOf(iris, "other/a.ftl")).isEqualTo("other/a.ftl");
        assertThat(folders.nameOf(iris, null)).isNull();
    }

    @Test
    void noConfigurationMeansNoFolders() {
        assertThat(new ApplicationTemplateFolders("").folderOf(iris)).isEmpty();
        assertThat(new ApplicationTemplateFolders(null).folderOf(iris)).isEmpty();
        assertThat(new ApplicationTemplateFolders("   ").folderOf(iris)).isEmpty();
    }

    @Test
    void anEntryThatIsNotAnApplicationIdIsRefusedWithAHint() {
        assertThatThrownBy(() -> new ApplicationTemplateFolders("SAKN=smartkind"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not an application id")
                .hasMessageContaining("does not read the application code");
    }

    @Test
    void aMalformedEntryOrFolderIsRefused() {
        assertThatThrownBy(() -> new ApplicationTemplateFolders(iris.toString())).hasMessageContaining("expected <application id>=<folder>");
        assertThatThrownBy(() -> new ApplicationTemplateFolders(iris + "=Not Valid")).hasMessageContaining("not a valid folder name");
    }

    @Test
    void theDefaultOfApplicationYmlParses() throws Exception {
        String line = java.nio.file.Files.readAllLines(java.nio.file.Path.of("src/main/resources/application.yml")).stream()
                .filter(l -> l.contains("application-folders:")).findFirst().orElseThrow();
        String value = line.substring(line.indexOf("TEMPLATE_APPLICATION_FOLDERS:") + "TEMPLATE_APPLICATION_FOLDERS:".length(), line.lastIndexOf('}'));
        var folders = new ApplicationTemplateFolders(value);

        assertThat(folders.folderOf(UUID.fromString("8249aa97-23fa-47cd-9c6d-4685f0c03cc3"))).contains("iris");
        assertThat(folders.folderOf(UUID.fromString("e50b2e01-1a40-43a3-aa55-90bc9b207fa3"))).contains("smartkind");
        assertThat(folders.folderOf(UUID.fromString("c913b241-866e-4f1d-9ee4-f241b00d6475"))).contains("zerobound");
    }
}
