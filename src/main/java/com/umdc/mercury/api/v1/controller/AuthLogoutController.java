package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.exception.CertificateSecurityException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Not {@code @SkipSessionValidation}: {@code SessionJwtInterceptor} verifies the token before this
 * runs, and {@link SessionJwtServiceImpl#revokeToken(String)} verifies it again before revoking.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthLogoutController implements AuthLogoutApi {

    private final SessionJwtServiceImpl sessionJwtService;

    public AuthLogoutController(SessionJwtServiceImpl sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    public ResponseEntity<Void> logout(String sessionToken) {
        try {
            sessionJwtService.revokeToken(sessionToken);
        } catch (CertificateSecurityException e) {
            throw new InvalidSessionTokenException("Invalid or missing session token.", e);
        }
        return ResponseEntity.noContent().build();
    }
}
