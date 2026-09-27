package com.umdc.mercury.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security chain for all of Mercury's own {@code /api/v1/**} endpoints that
 * are NOT already claimed by {@link ManagedClientSecurityConfig} — login
 * ({@code /api/v1/auth/**}) and the backend-to-backend utility endpoints other
 * UMDC services (e.g. directory-backend) call to trigger a communication
 * ({@code /api/v1/mail/**}, {@code /api/v1/verification-code/**}), plus any
 * future controller added under {@code /api/v1/**}.
 * <p>
 * Intentionally {@code permitAll()} at the Spring Security filter level —
 * actual authentication is {@link SessionJwtInterceptor}'s job, at the MVC
 * dispatch layer, checking Mercury's own self-issued {@code session-token}
 * header (see {@link SessionJwtServiceImpl}). That check is independent of any
 * external identity provider, and already covers all of {@code /api/v1/**}
 * (see {@code SessionJwtWebConfigurer}).
 * </p>
 * <p>
 * The matcher is deliberately the full {@code /api/v1/**}, not just the three
 * paths named above: {@link ManagedClientSecurityConfig} is registered at
 * {@link Ordered#HIGHEST_PRECEDENCE} (one lower order value than this chain's),
 * so {@code /api/v1/campaigns/**} and {@code /api/v1/channel-types/**} are
 * still resolved by it first — Spring Security dispatches each request to the
 * first chain (by order) whose {@code securityMatcher} applies, never both.
 * Scoping this chain broadly means a controller added under {@code /api/v1/**}
 * tomorrow is safe by default, instead of silently falling through to
 * {@code security-oauth}'s own {@code SecurityConfig} (human OAuth2 JWT + role,
 * resolved via an external Keycloak-style IdP's JWKS) — decommissioned, so that
 * chain can no longer authenticate anything: every request into it 500s trying
 * to fetch a JWKS from a host that no longer serves it, regardless of any
 * bearer token the caller presents.
 * </p>
 */
@Configuration
public class SessionTokenSecurityConfig {

    private static final String API_V1 = "/api/v1/**";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain sessionTokenSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(API_V1)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
