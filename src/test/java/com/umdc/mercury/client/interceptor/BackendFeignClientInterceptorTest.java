package com.umdc.mercury.client.interceptor;

import com.umdc.commons.exception.StandardException;
import com.umdc.security.properties.ClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pins the contract with backbone-rest's {@code POST /api/v1/managed-clients/token}: JSON body, camelCase answer. */
class BackendFeignClientInterceptorTest {

    private static ClientProperties client() {
        ClientProperties properties = new ClientProperties();
        properties.setClientId("cid");
        properties.setClientSecret("sec");
        properties.setScope("legacy-keycloak-scope");
        return properties;
    }

    @Test
    void tokenRequestIsJsonWithClientCredentialsAndScopes() {
        var entity = BackendFeignClientInterceptor.tokenRequest(client(), "a:read, b:write  c:manage");

        assertEquals(MediaType.APPLICATION_JSON, entity.getHeaders().getContentType());
        assertEquals("cid", entity.getBody().clientId());
        assertEquals("sec", entity.getBody().clientSecret());
        assertEquals(List.of("a:read", "b:write", "c:manage"), entity.getBody().scopes());
    }

    @Test
    void theLegacyClientScopeIsNotSent() {
        assertEquals(List.of("mercury:message:read"),
                BackendFeignClientInterceptor.tokenRequest(client(), "mercury:message:read").getBody().scopes());
    }

    @Test
    void blankOrMissingScopeSendsNoScopes() {
        assertEquals(List.of(), BackendFeignClientInterceptor.tokenRequest(client(), null).getBody().scopes());
        assertEquals(List.of(), BackendFeignClientInterceptor.tokenRequest(client(), "  ").getBody().scopes());
    }

    @Test
    void accessTokenIsReadFromTheAnswer() {
        assertEquals("tkn", BackendFeignClientInterceptor.accessToken(new BackendFeignClientInterceptor.ManagedClientToken("tkn")));
    }

    @Test
    void anAnswerWithoutTokenIsRefused() {
        assertThrows(StandardException.class, () -> BackendFeignClientInterceptor.accessToken(null));
        assertThrows(StandardException.class, () -> BackendFeignClientInterceptor.accessToken(new BackendFeignClientInterceptor.ManagedClientToken(null)));
        assertThrows(StandardException.class, () -> BackendFeignClientInterceptor.accessToken(new BackendFeignClientInterceptor.ManagedClientToken(" ")));
    }

    @Test
    void tokenUrlIsAFixedPathUnderTheBaseUrl() {
        assertEquals("https://h/backbone/api/v1/managed-clients/token", BackendFeignClientInterceptor.tokenUrl("https://h/backbone"));
        assertEquals("https://h/backbone/api/v1/managed-clients/token", BackendFeignClientInterceptor.tokenUrl("https://h/backbone//"));
    }
}
