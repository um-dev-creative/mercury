package com.prx.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.controller.AuthTokenController;
import com.umdc.mercury.security.LoginAttemptThrottle;
import com.umdc.mercury.security.LoginThrottleProperties;
import com.umdc.security.service.AuthService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthTokenController unit tests")
class AuthTokenControllerTest {

    private AuthService authService;
    private HttpServletRequest httpRequest;
    private AuthTokenController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.7");
        var properties = new LoginThrottleProperties();
        properties.setMaxAttempts(3);
        controller = new AuthTokenController(authService, new LoginAttemptThrottle(properties));
    }

    @Test
    @DisplayName("delegates to AuthService.token(AuthRequest) — no session-token-bkd involved")
    void accessToken_delegatesToAuthService() {
        AuthRequest request = new AuthRequest("directory-backend", "secret");
        when(authService.token(request)).thenReturn(ResponseEntity.ok(new AuthResponse("jwt")));

        ResponseEntity<AuthResponse> response = controller.accessToken(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new AuthResponse("jwt"));
    }

    @Test
    @DisplayName("propagates the service's rejection status unchanged")
    void accessToken_propagatesRejection() {
        AuthRequest request = new AuthRequest("x", "bad");
        when(authService.token(request)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        assertThat(controller.accessToken(request, httpRequest).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("after maxAttempts failures the alias is throttled with 429 + Retry-After, without reaching the service")
    void accessToken_throttlesAfterRepeatedFailures() {
        AuthRequest request = new AuthRequest("directory-backend", "wrong");
        when(authService.token(request)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        for (int i = 0; i < 3; i++) {
            assertThat(controller.accessToken(request, httpRequest).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        ResponseEntity<AuthResponse> blocked = controller.accessToken(request, httpRequest);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotNull();
        verify(authService, times(3)).token(request);
    }

    @Test
    @DisplayName("a blank-alias 400 is not counted as a failed login")
    void accessToken_badRequestNotCounted() {
        AuthRequest request = new AuthRequest(" ", "x");
        when(authService.token(request)).thenReturn(ResponseEntity.badRequest().build());

        for (int i = 0; i < 5; i++) {
            assertThat(controller.accessToken(request, httpRequest).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("a successful login clears the failure counter")
    void accessToken_successResetsCounter() {
        AuthRequest bad = new AuthRequest("directory-backend", "wrong");
        AuthRequest good = new AuthRequest("directory-backend", "right");
        when(authService.token(bad)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        when(authService.token(good)).thenReturn(ResponseEntity.ok(new AuthResponse("jwt")));

        controller.accessToken(bad, httpRequest);
        controller.accessToken(bad, httpRequest);
        controller.accessToken(good, httpRequest);
        controller.accessToken(bad, httpRequest);
        controller.accessToken(bad, httpRequest);

        assertThat(controller.accessToken(good, httpRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("throttling one address does not block the same alias from another address")
    void accessToken_throttleIsPerAddress() {
        AuthRequest request = new AuthRequest("directory-backend", "wrong");
        when(authService.token(request)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        for (int i = 0; i < 3; i++) {
            controller.accessToken(request, httpRequest);
        }
        HttpServletRequest other = mock(HttpServletRequest.class);
        when(other.getRemoteAddr()).thenReturn("10.9.9.9");

        assertThat(controller.accessToken(request, other).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authService, never()).token(new AuthRequest("never", "called"));
    }
}
