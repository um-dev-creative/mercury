package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.ChannelTemplateRenderer;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.api.v1.to.TemplateTO;
import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChannelTemplateRenderer")
class ChannelTemplateRendererTest {

    @TempDir
    Path templateDir;

    private ChannelTemplateRenderer renderer;

    @BeforeEach
    void setUp() throws IOException {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
        configuration.setDirectoryForTemplateLoading(templateDir.toFile());
        renderer = new ChannelTemplateRenderer(configuration, com.umdc.mercury.template.TemplateFolderFixtures.noFolders());
    }

    private TemplateDefinedTO templateAt(String location) {
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "desc", location, "TXT",
                null, UUID.randomUUID(), null, null, true);
        return new TemplateDefinedTO(UUID.randomUUID(), template, null, null, null, null, null, true, null);
    }

    @Test
    @DisplayName("renders the template substituting the campaign params")
    void rendersTemplateWithParams() throws IOException {
        Files.writeString(templateDir.resolve("welcome.ftl"), "Hola ${name}, tu código es ${code}.");

        String result = renderer.render(templateAt("welcome.ftl"),
                Map.of("name", "Ana", "code", "1234"), "texto crudo");

        assertThat(result).isEqualTo("Hola Ana, tu código es 1234.");
    }

    @Test
    @DisplayName("falls back to the raw body when the template is null")
    void fallsBackWhenTemplateIsNull() {
        assertThat(renderer.render(null, Map.of("name", "Ana"), "texto crudo"))
                .isEqualTo("texto crudo");
    }

    @Test
    @DisplayName("falls back to the raw body when the template location is blank")
    void fallsBackWhenLocationIsBlank() {
        assertThat(renderer.render(templateAt("   "), Map.of(), "texto crudo"))
                .isEqualTo("texto crudo");
    }

    @Test
    @DisplayName("falls back to the raw body when the template file does not exist")
    void fallsBackWhenTemplateFileMissing() {
        assertThat(renderer.render(templateAt("no-existe.ftl"), Map.of(), "texto crudo"))
                .isEqualTo("texto crudo");
    }

    @Test
    @DisplayName("falls back to the raw body when a param referenced by the template is missing")
    void fallsBackWhenParamMissing() throws IOException {
        Files.writeString(templateDir.resolve("strict.ftl"), "Hola ${missingParam}!");

        assertThat(renderer.render(templateAt("strict.ftl"), Map.of(), "texto crudo"))
                .isEqualTo("texto crudo");
    }

    @Test
    @DisplayName("treats null params as an empty model instead of failing")
    void nullParamsRenderAsEmptyModel() throws IOException {
        Files.writeString(templateDir.resolve("static.ftl"), "Mensaje sin variables.");

        assertThat(renderer.render(templateAt("static.ftl"), null, "texto crudo"))
                .isEqualTo("Mensaje sin variables.");
    }

    @Test
    @DisplayName("looks the template up in the folder of its application")
    void rendersFromTheApplicationFolder() throws IOException {
        UUID application = UUID.randomUUID();
        Files.createDirectories(templateDir.resolve("iris"));
        Files.writeString(templateDir.resolve("iris/welcome.ftl"), "Hola ${name} desde iris");
        var folders = com.umdc.mercury.template.TemplateFolderFixtures.folders(application, "Iris");
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_30);
        configuration.setDirectoryForTemplateLoading(templateDir.toFile());
        var inFolder = new ChannelTemplateRenderer(configuration, folders);
        TemplateTO template = new TemplateTO(UUID.randomUUID(), "desc", "welcome.ftl", "TXT", null, application, null, null, true);

        String result = inFolder.render(new TemplateDefinedTO(UUID.randomUUID(), template, null, null, null, null, null, true, null),
                Map.of("name", "Ana"), "crudo");

        assertThat(result).isEqualTo("Hola Ana desde iris");
    }
}
