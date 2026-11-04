package com.umdc.mercury.config;

import java.util.List;
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

    /**
     * Classes jjwt's public API ({@code Jwts}, {@code Jwks}, {@code Keys}) loads by name at class
     * initialisation ({@code Classes.newInstance("...$Supplier")}) and calls through its internal
     * bridges. GraalVM only keeps what it can see statically, so without these entries
     * {@code Jwts.<clinit>} fails with {@code NoSuchMethodException: DefaultJwtBuilder$Supplier.<init>()}
     * and every request that builds or parses a session token answers 500.
     */
    static final List<String> JJWT_LOADED_BY_NAME = List.of(
            "io.jsonwebtoken.impl.DefaultClaimsBuilder$Supplier",
            "io.jsonwebtoken.impl.DefaultJwtBuilder$Supplier",
            "io.jsonwebtoken.impl.DefaultJwtHeaderBuilder$Supplier",
            "io.jsonwebtoken.impl.DefaultJwtParserBuilder$Supplier",
            "io.jsonwebtoken.impl.io.StandardCompressionAlgorithms",
            "io.jsonwebtoken.impl.security.DefaultDynamicJwkBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultJwkParserBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultJwkSetBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultJwkSetParserBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultKeyOperationBuilder$Supplier",
            "io.jsonwebtoken.impl.security.DefaultKeyOperationPolicyBuilder$Supplier",
            "io.jsonwebtoken.impl.security.JwksBridge",
            "io.jsonwebtoken.impl.security.KeysBridge",
            "io.jsonwebtoken.impl.security.StandardCurves",
            "io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms",
            "io.jsonwebtoken.impl.security.StandardHashAlgorithms",
            "io.jsonwebtoken.impl.security.StandardKeyAlgorithms",
            "io.jsonwebtoken.impl.security.StandardKeyOperations",
            "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms");

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
            // jjwt (session tokens) instantiates its builders and algorithm registries by name, see
            // JJWT_LOADED_BY_NAME; it also finds its compression codecs through META-INF/services.
            JJWT_LOADED_BY_NAME.forEach(name -> hints.reflection().registerTypeIfPresent(classLoader, name, FULL_ACCESS));
            hints.resources().registerPattern("META-INF/services/io.jsonwebtoken.*");
        }
    }
}
