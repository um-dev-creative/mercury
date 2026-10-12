package com.umdc.mercury.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the wire contract with backbone-rest's {@code GET /api/v1/session/validate}
 * ({@code SessionApi#validateSessionToken}): path, the token in the {@code session-token} header (what the
 * current backbone-rest reads) and, for older versions, the same raw token in {@code Authorization}.
 * A mismatch here is invisible to mock-based tests yet makes every Backbone token look invalid.
 */
@DisplayName("BackboneSessionClient wire contract")
class BackboneSessionClientContractTest {

    @Test
    @DisplayName("calls GET /api/v1/session/validate with the token in session-token and, for older versions, in Authorization")
    void validateContract() throws NoSuchMethodException {
        Method validate = BackboneSessionClient.class.getMethod("validate", String.class, String.class);

        assertThat(validate.getAnnotation(GetMapping.class).value()).containsExactly("/api/v1/session/validate");
        assertThat(validate.getParameters()[0].getAnnotation(RequestHeader.class).value()).isEqualTo("session-token");
        assertThat(validate.getParameters()[1].getAnnotation(RequestHeader.class).value()).isEqualTo(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("the single-argument call presents the same raw token in both headers")
    void sameTokenInBothHeaders() {
        String[] sent = new String[2];
        BackboneSessionClient client = (sessionToken, legacyAuthorization) -> {
            sent[0] = sessionToken;
            sent[1] = legacyAuthorization;
            return Boolean.TRUE;
        };

        assertThat(client.validate("raw.jwt.token")).isTrue();
        assertThat(sent).containsExactly("raw.jwt.token", "raw.jwt.token");
    }
}
