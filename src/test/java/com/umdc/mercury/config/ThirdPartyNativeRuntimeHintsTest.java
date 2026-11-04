package com.umdc.mercury.config;

import com.umdc.security.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
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

    @Test
    void registersEveryJjwtClassLoadedByName() throws ClassNotFoundException {
        RuntimeHints hints = new RuntimeHints();

        new ThirdPartyNativeRuntimeHints.Hints().registerHints(hints, getClass().getClassLoader());

        for (String name : ThirdPartyNativeRuntimeHints.JJWT_LOADED_BY_NAME) {
            // the class must exist in the jjwt version on the classpath, or the hint would silently register nothing
            Class<?> type = Class.forName(name, false, getClass().getClassLoader());
            assertTrue(RuntimeHintsPredicates.reflection().onType(type).withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                    .test(hints), name);
        }
        assertTrue(RuntimeHintsPredicates.resource().forResource("META-INF/services/io.jsonwebtoken.CompressionCodec").test(hints));
    }

    @Test
    void jwtsInitialisesOnThePlainJvm() {
        assertNotNull(io.jsonwebtoken.Jwts.builder());
    }
}
