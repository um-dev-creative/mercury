package com.prx.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.controller.AuthLogoutController;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.exception.CertificateSecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("AuthLogoutController unit tests")
class AuthLogoutControllerTest {

    private final SessionJwtServiceImpl sessionJwtService = mock(SessionJwtServiceImpl.class);
    private final AuthLogoutController controller = new AuthLogoutController(sessionJwtService);

    @Test
    @DisplayName("revokes the presented token and answers 204")
    void logout_revokes() throws CertificateSecurityException {
        var response = controller.logout("tok");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sessionJwtService).revokeToken("tok");
    }

    @Test
    @DisplayName("maps an untrusted token to InvalidSessionTokenException (401)")
    void logout_untrusted_unauthorized() throws CertificateSecurityException {
        doThrow(new CertificateSecurityException("nope")).when(sessionJwtService).revokeToken("bad");

        assertThatThrownBy(() -> controller.logout("bad")).isInstanceOf(InvalidSessionTokenException.class);
    }
}
