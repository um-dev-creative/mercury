package com.umdc.mercury.config;

import com.umdc.security.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.cloud.vault.config.VaultProperties;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartyNativeRuntimeHintsTest {

    @Test
    void registersTheTypesNativeImageNeedsFromThirdPartyCode() {
        RuntimeHints hints = new RuntimeHints();

        new ThirdPartyNativeRuntimeHints.Hints().registerHints(hints, getClass().getClassLoader());

        assertTrue(RuntimeHintsPredicates.reflection().onType(UUID[].class).test(hints));
        assertTrue(RuntimeHintsPredicates.reflection().onType(SecurityProperties.class).test(hints));
        assertTrue(RuntimeHintsPredicates.reflection().onType(VaultProperties.class).test(hints));
    }

    @Test
    void configurationClassCanBeCreated() {
        assertNotNull(new ThirdPartyNativeRuntimeHints());
    }
}
