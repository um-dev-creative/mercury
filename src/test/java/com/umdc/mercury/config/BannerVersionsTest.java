/*
 *  @(#)BannerVersionsTest.java
 *
 *  Copyright (c) Luis Antonio Mata Mata. All rights reserved.
 *
 *   All rights to this product are owned by Luis Antonio Mata Mata and may only
 *  be used under the terms of its associated license document. You may NOT
 *  copy, modify, sublicense, or distribute this source file or portions of
 *  it unless previously authorized in writing by Luis Antonio Mata Mata.
 *  In any event, this notice and the above copyright must always be included
 *  verbatim with this file.
 */
package com.umdc.mercury.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** The versions in the startup banner come from a Maven-filtered file: an unfiltered {@code @...@} would print as-is. */
class BannerVersionsTest {

    @Test
    void theFilteredFileHasRealVersions() throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("build-versions.properties")) {
            assertTrue(in != null, "build-versions.properties must be on the classpath");
            props.load(in);
        }
        for (String key : new String[] {"umdc.build.spring-cloud-version", "umdc.build.umdc-libs-version"}) {
            String value = props.getProperty(key);
            assertTrue(value != null && !value.isBlank(), key + " is empty");
            assertFalse(value.contains("@"), key + " was not filtered: " + value);
        }
    }

    @Test
    void theBannerPrintsBothVersionsAndTheApplicationLoadsTheFile() throws IOException {
        String banner = Files.readString(Path.of("src/main/resources/banner.txt"));
        assertTrue(banner.contains("${umdc.build.spring-cloud-version}"));
        assertTrue(banner.contains("${umdc.build.umdc-libs-version}"));
        assertTrue(Files.readString(Path.of("src/main/resources/application.yml")).contains("classpath:build-versions.properties"));
    }
}
