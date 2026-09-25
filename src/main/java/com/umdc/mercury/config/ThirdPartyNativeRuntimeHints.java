package com.umdc.mercury.config;

import java.util.UUID;

import com.umdc.security.config.SecurityConfig;
import com.umdc.security.properties.AuthProperties;
import com.umdc.security.properties.ClientProperties;
import com.umdc.security.properties.ManagementAuthenticatorProperties;
import com.umdc.security.properties.SecurityProperties;
import com.umdc.security.properties.StoreProperties;
import com.umdc.security.util.KeystoreUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.cloud.vault.config.VaultProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Native-image reflection/proxy gaps found empirically in third-party code this project depends
 * on but doesn't control (Hibernate, com.umdc.commons-services, com.umdc.security-oauth — none of
 * which ship their own GraalVM reachability metadata). Each entry names exactly the failure
 * GraalVM reported. See docs/architecture/graalvm-native-image.md.
 */
@Configuration
@ImportRuntimeHints(ThirdPartyNativeRuntimeHints.Hints.class)
public class ThirdPartyNativeRuntimeHints {

    private static final MemberCategory[] FULL_ACCESS = {
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS
    };

    static class Hints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Hibernate's multi-ID batch loader (MultiIdEntityLoaderArrayParam) reflectively
            // allocates a UUID[] for every UUID-keyed entity here.
            hints.reflection().registerType(UUID[].class);
            // com.umdc.commons-services' "requestBodyInterceptor" bean autowires a request-scoped
            // HttpServletRequest into a singleton, which Spring resolves via a JDK dynamic proxy.
            hints.proxies().registerJdkProxy(HttpServletRequest.class);
            // com.umdc.security-oauth's SecurityConfig field-injects SecurityProperties (and
            // nested Auth/Client/Store/ManagementAuthenticator properties) and KeystoreUtil reads
            // StoreProperties reflectively via a compiled (source-unavailable) jar with no native
            // hints of its own — field injection silently produced nulls under AOT
            // (NullPointerException in KeystoreUtil.getKeyStore, not a loud
            // MissingReflectionRegistrationError) until these were registered explicitly.
            hints.reflection().registerType(SecurityConfig.class, FULL_ACCESS);
            hints.reflection().registerType(SecurityProperties.class, FULL_ACCESS);
            hints.reflection().registerType(AuthProperties.class, FULL_ACCESS);
            hints.reflection().registerType(ClientProperties.class, FULL_ACCESS);
            hints.reflection().registerType(StoreProperties.class, FULL_ACCESS);
            hints.reflection().registerType(ManagementAuthenticatorProperties.class, FULL_ACCESS);
            hints.reflection().registerType(KeystoreUtil.class, FULL_ACCESS);
            // spring-cloud-vault-config ships no GraalVM reachability metadata of its own either,
            // so VaultProperties' reflective no-arg-constructor binding failed with
            // "NoSuchMethodException: VaultProperties.<init>()" despite the constructor existing.
            hints.reflection().registerType(VaultProperties.class, FULL_ACCESS);
        }
    }
}
