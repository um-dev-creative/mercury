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
 * ({@code SessionApi#validateSessionToken}): path, and the token in the {@code Authorization} header.
 * A mismatch here is invisible to mock-based tests yet makes every Backbone token look invalid.
 */
@DisplayName("BackboneSessionClient wire contract")
class BackboneSessionClientContractTest {

    @Test
    @DisplayName("calls GET /api/v1/session/validate with the raw token in the Authorization header")
    void validateContract() throws NoSuchMethodException {
        Method validate = BackboneSessionClient.class.getMethod("validate", String.class);

        assertThat(validate.getAnnotation(GetMapping.class).value()).containsExactly("/api/v1/session/validate");
        RequestHeader header = validate.getParameters()[0].getAnnotation(RequestHeader.class);
        assertThat(header.value()).isEqualTo(HttpHeaders.AUTHORIZATION);
    }
}
