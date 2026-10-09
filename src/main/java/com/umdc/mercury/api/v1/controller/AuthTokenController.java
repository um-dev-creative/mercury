package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.security.LoginAttemptThrottle;
import com.umdc.security.service.AuthService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces security-oauth's {@code AuthApiController} (excluded in {@code MercuryApplication}),
 * whose {@code POST /api/v1/auth/token} demanded a {@code session-token-bkd} header and whose
 * {@code POST /api/v1/auth/session-token} exchanged it for a token carrying a {@code uid}.
 * <p>
 * With no second credential left, the registered alias/password is the only barrier, so failed
 * attempts are throttled per alias + caller address ({@link LoginAttemptThrottle}) and audited. The
 * password is never logged.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthTokenController implements AuthTokenApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenController.class);

    private final AuthService authService;
    private final LoginAttemptThrottle throttle;

    public AuthTokenController(AuthService authService, LoginAttemptThrottle throttle) {
        this.authService = authService;
        this.throttle = throttle;
    }

    @Override
    public ResponseEntity<AuthResponse> accessToken(AuthRequest authRequest, HttpServletRequest request) {
        String alias = authRequest.alias();
        String address = request.getRemoteAddr();
        String key = LoginAttemptThrottle.key(alias, address);

        if (throttle.isBlocked(key)) {
            long retryAfter = Math.max(1, throttle.retryAfter(key).toSeconds());
            LOGGER.warn("Login throttled. alias={}, address={}", alias, address);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                    .build();
        }

        ResponseEntity<AuthResponse> response = authService.token(authRequest);
        if (response.getStatusCode().is2xxSuccessful()) {
            throttle.recordSuccess(key);
            LOGGER.info("Session token issued. alias={}, address={}", alias, address);
        } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throttle.recordFailure(key);
            LOGGER.warn("Login rejected. alias={}, address={}", alias, address);
        }
        return response;
    }
}
