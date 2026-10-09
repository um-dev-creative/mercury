package com.umdc.mercury.security;

import com.umdc.security.introspection.BackboneOpaqueTokenIntrospector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the REAL {@link ManagedClientSecurityConfig} and {@link SessionTokenSecurityConfig}
 * filter chains (the project has no {@code @SpringBootTest}, so until now their wiring — which paths
 * need a Bearer, which scope unlocks which verb — was only reviewed by eye). Probe controllers stand
 * in for the real ones: only the chains are under test, not the controllers.
 */
@WebMvcTest(controllers = SecurityFilterChainsTest.Probe.class, properties = {
        // application.yml is on the classpath and leaves these to the runtime environment
        "SPRING_BOOT_PROFILE_ACTIVE=test",
        "spring.cloud.config.enabled=false",
        "spring.cloud.vault.enabled=false"})
@Import({ManagedClientSecurityConfig.class, SessionTokenSecurityConfig.class, SecurityFilterChainsTest.Probe.class})
@DisplayName("Security filter chains")
class SecurityFilterChainsTest {

    /** Minimal configuration so the slice does not bootstrap MercuryApplication (Feign, Vault, Kafka...). */
    @SpringBootConfiguration
    static class TestApp {
    }

    @RestController
    static class Probe {
        @GetMapping({"/api/v1/templates", "/api/v1/campaigns/x", "/api/v1/channel-types", "/api/v1/mail"})
        String read() {
            return "ok";
        }

        @PostMapping({"/api/v1/templates", "/api/v1/campaigns", "/api/v1/channel-types", "/api/v1/auth/token",
                "/api/v1/auth/logout"})
        String write() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackboneOpaqueTokenIntrospector introspector;

    private static DefaultOAuth2AuthenticatedPrincipal principal(String... scopes) {
        return new DefaultOAuth2AuthenticatedPrincipal("svc", Map.of("client_id", "svc"),
                java.util.Arrays.stream(scopes)
                        .<org.springframework.security.core.GrantedAuthority>map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                        .toList());
    }

    @BeforeEach
    void setUp() {
        // doX().when(...) form: when(mock.m()) would invoke the already-throwing default stub
        doThrow(new BadOpaqueTokenException("inactive")).when(introspector).introspect(anyString());
        doReturn(principal("mercury:message:read")).when(introspector).introspect("read-token");
        doReturn(principal("mercury:message:write")).when(introspector).introspect("write-token");
    }

    @Test
    @DisplayName("templates, campaigns and channel-types reject a request with no Bearer (401)")
    void bearerChain_noBearer_unauthorized() throws Exception {
        for (String path : List.of("/api/v1/templates", "/api/v1/campaigns/x", "/api/v1/channel-types")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("an inactive / unknown Bearer is rejected (401)")
    void bearerChain_inactiveBearer_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/templates").header("Authorization", "Bearer revoked"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the read scope unlocks GET on every managed path")
    void bearerChain_readScope_allowsGet() throws Exception {
        for (String path : List.of("/api/v1/templates", "/api/v1/campaigns/x", "/api/v1/channel-types")) {
            mockMvc.perform(get(path).header("Authorization", "Bearer read-token")).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("the read scope does NOT unlock a write (403)")
    void bearerChain_readScope_forbidsPost() throws Exception {
        for (String path : List.of("/api/v1/templates", "/api/v1/campaigns", "/api/v1/channel-types")) {
            mockMvc.perform(post(path).header("Authorization", "Bearer read-token"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("the write scope unlocks POST on every managed path")
    void bearerChain_writeScope_allowsPost() throws Exception {
        for (String path : List.of("/api/v1/templates", "/api/v1/campaigns", "/api/v1/channel-types")) {
            mockMvc.perform(post(path).header("Authorization", "Bearer write-token")).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("the write scope does not unlock a GET (reads need the read scope)")
    void bearerChain_writeScope_forbidsGet() throws Exception {
        mockMvc.perform(get("/api/v1/templates").header("Authorization", "Bearer write-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/auth/token, /auth/logout and the M2M mail path are NOT behind the Bearer chain "
            + "(their own credentials — alias/password, session-token — are checked elsewhere)")
    void sessionTokenChain_isPermitAllAtFilterLevel() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/mail")).andExpect(status().isOk());
    }
}
