package com.umdc.mercury.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Security chain for the requests that carry a Backbone-issued opaque M2M token (validated via {@link ApplicationAwareTokenIntrospector}).
 * <ul>
 *   <li>{@code POST /api/v1/auth/token}: the one door an application uses to get Mercury's own M2M token. Needs a Mercury scope (read or write).</li>
 *   <li>{@code /api/v1/campaigns/**} and {@code /api/v1/channel-types/**}: the service Bearer, as before.</li>
 *   <li>{@code /api/v1/templates/**} <b>only when the request has an {@code Authorization} header</b>: a human of Iris (Bearer of the calling service
 *       plus the user's {@code session-token}), as before. Without that header the request is an application's M2M call and is authenticated by
 *       {@code TemplateCallerInterceptor} with the Mercury M2M token in {@code session-token}.</li>
 * </ul>
 * Registered at {@link Ordered#HIGHEST_PRECEDENCE} so Spring Security resolves these first; everything else under {@code /api/v1/**} falls to
 * {@link SessionTokenSecurityConfig}, one order lower.
 * <p>
 * Which scope grants read vs. write is a plain, hardcoded resource-server declaration; "which client has which scope" and "which application a client
 * belongs to" stay exclusively Backbone's, fetched at request time by the introspector (in the shared {@code security-oauth} library).
 * </p>
 */
@Configuration
public class ManagedClientSecurityConfig {

    private static final String AUTH_TOKEN = "/api/v1/auth/token";
    private static final String CAMPAIGNS = "/api/v1/campaigns/**";
    private static final String CHANNEL_TYPES = "/api/v1/channel-types/**";
    private static final String TEMPLATES = "/api/v1/templates/**";
    private static final String SCOPE_READ = "SCOPE_mercury:message:read";
    private static final String SCOPE_WRITE = "SCOPE_mercury:message:write";

    private final ApplicationAwareTokenIntrospector introspector;

    public ManagedClientSecurityConfig(ApplicationAwareTokenIntrospector introspector) {
        this.introspector = introspector;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain managedClientSecurityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        RequestMatcher bearerTemplates = new AndRequestMatcher(paths.matcher(TEMPLATES), new RequestHeaderRequestMatcher(HttpHeaders.AUTHORIZATION));
        http.securityMatcher(new OrRequestMatcher(paths.matcher(HttpMethod.POST, AUTH_TOKEN), paths.matcher(CAMPAIGNS), paths.matcher(CHANNEL_TYPES), bearerTemplates))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, AUTH_TOKEN).hasAnyAuthority(SCOPE_READ, SCOPE_WRITE)
                        .requestMatchers(HttpMethod.GET, CAMPAIGNS, CHANNEL_TYPES, TEMPLATES).hasAuthority(SCOPE_READ)
                        .anyRequest().hasAuthority(SCOPE_WRITE))
                .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(opaque -> opaque.introspector(introspector)));
        return http.build();
    }
}
