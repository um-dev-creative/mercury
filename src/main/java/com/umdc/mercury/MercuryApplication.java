package com.umdc.mercury;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.umdc.mercury.security.TrustStoreInitializer;
import com.umdc.security.config.SecurityConfig;
import com.umdc.security.controller.AuthApiController;

/**
 * MercuryApplication.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 03-05-2022
 * @since 11
 */
@EnableFeignClients(basePackages = "com.umdc.mercury.client")
@EnableScheduling
@EnableDiscoveryClient
// Hand-expanded @SpringBootApplication (= @SpringBootConfiguration + @EnableAutoConfiguration +
// @ComponentScan) instead of the shorthand: a class can carry only one *effective* @ComponentScan
// - stacking @SpringBootApplication(scanBasePackages=...) alongside a second, explicit
// @ComponentScan on the same class does NOT run both (verified empirically: the explicit one
// silently replaces @SpringBootApplication's, dropping every bean under com.umdc.mercury/
// com.umdc.commons.services). Expanding lets one @ComponentScan cover all three base packages
// plus the exclusion below, while keeping @SpringBootApplication's own default excludeFilters
// (TypeExcludeFilter for test slices, AutoConfigurationExcludeFilter to avoid double-registering
// autoconfig classes) so behavior otherwise matches the shorthand exactly.
//
// The added filter also excludes com.umdc.security.controller.AuthApiController: its
// POST /api/v1/auth/token requires a session-token-bkd header (validated remotely against
// backbone-rest) and it also maps POST /api/v1/auth/session-token (the removed bkd exchange).
// Mercury serves its own AuthTokenController at /api/v1/auth/token instead, with no bkd.
//
// It excludes com.umdc.security.config.SecurityConfig: the decommissioned
// Keycloak-style JWT resource-server chain (see SessionTokenSecurityConfig's and
// ManagedClientSecurityConfig's javadoc) from the closed-source security-oauth jar. It
// hard-requires spring.security.oauth2.resourceserver.jwt.jwk-set-uri via a bare @Value field
// with no default, so leaving it on the classpath forced that dead property to stay in
// application.yml just to keep the context from failing to start.
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "com.umdc.commons.services",
                "com.umdc.mercury",
                "com.umdc.security"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = {SecurityConfig.class, AuthApiController.class})
        }
)
public class MercuryApplication {

    public static void main(String[] args) {
        // eureka.client.tls.trust-store/key-store are sourced from the remote Config Server
        // (mercury-remote-supabase.yml et al.), and spring.config.import's imported property
        // source always gives that remote source priority over this file's own eureka.client.tls
        // values for any key both define - spring.cloud.config.override-none does NOT change
        // this, since it's bound from the remote source's own properties, never from
        // application.yml itself.
        // That remote value currently resolves to a ServletContext-relative path that can never
        // exist for a jar-deployed app, so eureka.client.tls stays effectively unusable from
        // Spring config alone; setupTLS() then skips building a custom SSLContext and Eureka's
        // RestClient transport falls back to the JVM's default one. Merging PRX Internal CA into
        // a COPY of the JDK's own cacerts - exactly what docker-entrypoint.sh used to do only for
        // Docker - makes that JVM default trust monitor.umdc-qa.tst too, for local/IDE runs and
        // Docker alike, without discarding the public CA bundle other TLS targets (MongoDB
        // Atlas, etc.) still need. javax.net.ssl.trustStore is a raw JSSE system property, never
        // a Spring property, so nothing above can shadow it.
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            TrustStoreInitializer.importLocalCertsIntoDefaultTrustStore();
        }
        SpringApplication.run(MercuryApplication.class, args);
    }
}
