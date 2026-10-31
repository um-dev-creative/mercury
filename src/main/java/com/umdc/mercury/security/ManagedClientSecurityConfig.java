package com.umdc.mercury.security;

import com.umdc.security.introspection.BackboneOpaqueTokenIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security chain for the endpoints M2M applications call using a backbone-issued
 * opaque token (validated via {@link BackboneOpaqueTokenIntrospector}).
 * <p>
 * Deliberately scoped to only {@code /api/v1/campaigns/**},
 * {@code /api/v1/channel-types/**} and {@code /api/v1/templates/**} — the controllers
 * M2M clients actually consume today — rather than a blanket {@code /api/v1/**}.
 * {@code /api/v1/templates/**} additionally requires the end user's {@code session-token}
 * ({@code SessionJwtInterceptor}); the Bearer here identifies the calling service. Registered at
 * {@link Ordered#HIGHEST_PRECEDENCE} so Spring Security resolves these
 * paths here first; everything else under {@code /api/v1/**} (including
 * Mercury's own {@code /api/v1/auth/**} login endpoints) falls to
 * {@link SessionTokenSecurityConfig}, one order lower — see its Javadoc for why
 * that, and not {@code security-oauth}'s own {@code SecurityConfig}, is now the
 * effective catch-all for this app.
 * </p>
 * <p>
 * Which scope grants read vs. write is a plain, hardcoded resource-server declaration —
 * standard OAuth2: the resource server (Mercury) always owns "which scope unlocks which of
 * my own endpoints." It does not own, store, or duplicate "which client has which scope" —
 * that stays exclusively Backbone's (backbone-rest's managed-client registry), fetched at
 * request time by {@link BackboneOpaqueTokenIntrospector} (in the shared {@code security-oauth}
 * library, not here) via its introspection call.
 * </p>
 */
@Configuration
public class ManagedClientSecurityConfig {

    private static final String CAMPAIGNS = "/api/v1/campaigns/**";
    private static final String CHANNEL_TYPES = "/api/v1/channel-types/**";
    private static final String TEMPLATES = "/api/v1/templates/**";
    private static final String SCOPE_READ = "SCOPE_mercury:message:read";
    private static final String SCOPE_WRITE = "SCOPE_mercury:message:write";

    private final BackboneOpaqueTokenIntrospector introspector;

    public ManagedClientSecurityConfig(BackboneOpaqueTokenIntrospector introspector) {
        this.introspector = introspector;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain managedClientSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(CAMPAIGNS, CHANNEL_TYPES, TEMPLATES)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, CAMPAIGNS, CHANNEL_TYPES, TEMPLATES).hasAuthority(SCOPE_READ)
                        .anyRequest().hasAuthority(SCOPE_WRITE))
                .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(opaque -> opaque.introspector(introspector)));
        return http.build();
    }
}
