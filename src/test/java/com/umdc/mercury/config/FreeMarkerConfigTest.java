package com.umdc.mercury.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
