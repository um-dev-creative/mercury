package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.security.LoginClientVerifier;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.service.AuthService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service implementation for authentication-related operations.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SessionJwtServiceImpl sessionJwtService;
    private final LoginClientVerifier loginClientVerifier;

    /**
     * Constructor for AuthServiceImpl.
     *
     * @param sessionJwtService   the service for generating JWT tokens
     * @param loginClientVerifier decides whether a backend caller is registered (database table, then the static registry)
     */
    public AuthServiceImpl(SessionJwtServiceImpl sessionJwtService, LoginClientVerifier loginClientVerifier) {
        this.sessionJwtService = sessionJwtService;
        this.loginClientVerifier = loginClientVerifier;
    }

    /**
     * Generates a session token based on the provided authentication request.
     * <p>
     * Used by other backend services (e.g. smartkin-backend, zerobound-rest) to obtain a
     * Mercury session-token before calling {@code /api/v1/mail} or
     * {@code /api/v1/verification-code}. The alias/password pair is checked by
     * {@link LoginClientVerifier} against the {@code mercury.login_clients} table and, for an alias
     * that is not there, the static registry — this is a registered-caller
     * credential check, not an end-user login.
     * </p>
     *
     * @param authRequest the authentication request containing the caller's alias and password
     * @return ResponseEntity containing the authentication response with the session token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest) {
        if (authRequest.alias().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!loginClientVerifier.isRegistered(authRequest.alias(), authRequest.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), new ConcurrentHashMap<>()));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }
}
