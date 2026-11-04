package com.umdc.mercury.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeMarkerConfigTest {

    @TempDir
    Path dir;

    @Test
    void loadsTemplatesFromTheConfiguredDirectory() throws IOException {
        Files.writeString(dir.resolve("hello.ftl"), "hello");
        FreeMarkerConfig config = new FreeMarkerConfig();
        ReflectionTestUtils.setField(config, "templateLoaderPath", dir.toString());

        assertEquals("hello.ftl", config.getFreeMarkerConfiguration().getTemplate("hello.ftl").getName());
    }

    @Test
    void failsFastWhenTheTemplateDirectoryDoesNotExist() {
        FreeMarkerConfig config = new FreeMarkerConfig();
        ReflectionTestUtils.setField(config, "templateLoaderPath", dir.resolve("missing").toString());

        assertThrows(IOException.class, config::getFreeMarkerConfiguration);
    }

    @Test
    void exposesTheStorageOverTheSameFolder() throws IOException {
        Files.writeString(dir.resolve("shared.ftl"), "x");
        FreeMarkerConfig config = new FreeMarkerConfig();
        ReflectionTestUtils.setField(config, "templateLoaderPath", dir.toString());

        assertEquals("x", config.templateStorage().read("shared.ftl").orElseThrow().text());
    }

    private Configuration configuration() throws IOException {
        FreeMarkerConfig config = new FreeMarkerConfig();
        ReflectionTestUtils.setField(config, "templateLoaderPath", dir.toString());
        return config.getFreeMarkerConfiguration();
    }

    private String render(String source, Map<String, Object> model) throws IOException, TemplateException {
        Files.writeString(dir.resolve("t.ftl"), source);
        StringWriter out = new StringWriter();
        configuration().getTemplate("t.ftl").process(model, out);
        return out.toString();
    }

    @ParameterizedTest(name = "refuses {0}")
    @ValueSource(strings = {
            "<#assign ex=\"freemarker.template.utility.Execute\"?new()>${ex(\"id\")}",
            "<#assign oc=\"freemarker.template.utility.ObjectConstructor\"?new()>${oc(\"java.lang.ProcessBuilder\", \"id\")}",
            "<#assign cls=\"java.lang.Runtime\"?new()>"
    })
    void refusesToInstantiateClasses(String payload) {
        Exception error = assertThrows(Exception.class, () -> render(payload, new HashMap<>()));

        assertTrue(error.getMessage().toLowerCase().contains("not allowed")
                || error.getMessage().toLowerCase().contains("resolver"), error.getMessage());
    }

    @Test
    void refusesTheJavaApiOfTheModel() {
        Map<String, Object> model = Map.of("items", new java.util.ArrayList<>(java.util.List.of("a")));

        assertThrows(Exception.class, () -> render("${items?api.size()}", model));
    }

    @Test
    void stillRendersParametersAndDirectives() throws Exception {
        String out = render("<#if show>Hola ${name?html}</#if><#list xs as x>${x}</#list>",
                Map.of("show", true, "name", "<Ana>", "xs", java.util.List.of(1, 2)));

        assertEquals("Hola &lt;Ana&gt;12", out);
    }
}
