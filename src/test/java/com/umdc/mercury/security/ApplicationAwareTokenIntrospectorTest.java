package com.umdc.mercury.security;

import com.umdc.mercury.client.BackboneIntrospectionClient;
import com.umdc.mercury.client.to.TokenIntrospectionRequest;
import com.umdc.mercury.client.to.TokenIntrospectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Introspecting a Backbone M2M token")
class ApplicationAwareTokenIntrospectorTest {

    private BackboneIntrospectionClient backbone;
    private ApplicationAwareTokenIntrospector introspector;

    @BeforeEach
    void setUp() {
        backbone = mock(BackboneIntrospectionClient.class);
        introspector = new ApplicationAwareTokenIntrospector(backbone);
    }

    @Test
    @DisplayName("an active token keeps the client, scopes, name and the application Backbone ties it to")
    void activeToken() {
        when(backbone.introspect(any())).thenReturn(new TokenIntrospectionResponse(true, "client-1", "zerobound-rest",
                List.of("mercury:message:read", "mercury:message:write"), "backbone-rest", 2_000L, 1_000L, "jti-1", "81398159-1785-46a5-15ae-f491fd1dc509"));

        var principal = introspector.introspect("backbone-token");

        assertThat(principal.getName()).isEqualTo("client-1");
        assertThat((String) principal.getAttribute(ApplicationAwareTokenIntrospector.APPLICATION_ID_CLAIM)).isEqualTo("81398159-1785-46a5-15ae-f491fd1dc509");
        assertThat((String) principal.getAttribute("client_name")).isEqualTo("zerobound-rest");
        assertThat((String) principal.getAttribute("jti")).isEqualTo("jti-1");
        assertThat((Instant) principal.getAttribute("exp")).isEqualTo(Instant.ofEpochSecond(2_000));
        assertThat((Instant) principal.getAttribute("iat")).isEqualTo(Instant.ofEpochSecond(1_000));
        assertThat(principal.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("SCOPE_mercury:message:read", "SCOPE_mercury:message:write");
    }

    @Test
    @DisplayName("an older backbone-rest that returns no application leaves the claim out")
    void noApplication() {
        when(backbone.introspect(any())).thenReturn(new TokenIntrospectionResponse(true, "client-1", null, null, null, null, null, null, null));

        var principal = introspector.introspect("t");

        assertThat((Object) principal.getAttribute(ApplicationAwareTokenIntrospector.APPLICATION_ID_CLAIM)).isNull();
        assertThat(principal.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("an inactive or empty answer is a bad token")
    void inactive() {
        when(backbone.introspect(any())).thenReturn(new TokenIntrospectionResponse(false, null, null, null, null, null, null, null, null), (TokenIntrospectionResponse) null);

        assertThatThrownBy(() -> introspector.introspect("t")).isInstanceOf(BadOpaqueTokenException.class);
        assertThatThrownBy(() -> introspector.introspect("t")).isInstanceOf(BadOpaqueTokenException.class);
    }

    @Test
    @DisplayName("an unreachable Backbone fails closed")
    void unreachable() {
        when(backbone.introspect(any())).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> introspector.introspect("t")).isInstanceOf(OAuth2IntrospectionException.class);
    }

    @Test
    @DisplayName("the request never prints the token")
    void requestHidesToken() {
        assertThat(new TokenIntrospectionRequest("secret-token").toString()).doesNotContain("secret-token");
    }
}
