package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.security.LoginClientProperties;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.service.AuthService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service implementation for authentication-related operations.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SessionJwtServiceImpl sessionJwtService;
    private final LoginClientProperties loginClientProperties;

    /**
     * Constructor for AuthServiceImpl.
     *
     * @param sessionJwtService      the service for generating JWT tokens
     * @param loginClientProperties  the registry of backend callers allowed to log in
     */
    public AuthServiceImpl(SessionJwtServiceImpl sessionJwtService, LoginClientProperties loginClientProperties) {
        this.sessionJwtService = sessionJwtService;
        this.loginClientProperties = loginClientProperties;
    }

    /**
     * Generates a session token based on the provided authentication request.
     * <p>
     * Used by other backend services (e.g. directory-backend) to obtain a
     * Mercury session-token before calling {@code /api/v1/mail} or
     * {@code /api/v1/verification-code}. The alias/password pair is checked
     * against {@link LoginClientProperties} — this is a static, registered-caller
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
        if (!isRegisteredLoginClient(authRequest)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), new ConcurrentHashMap<>()));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Checks the given alias/password against {@link LoginClientProperties}'
     * registered callers, using a constant-time comparison to avoid leaking
     * match progress through response timing.
     *
     * @param authRequest the incoming alias/password pair
     * @return {@code true} if it matches a registered login client
     */
    private boolean isRegisteredLoginClient(AuthRequest authRequest) {
        List<LoginClientProperties.LoginClient> clients = loginClientProperties.getLoginClients();
        return Objects.nonNull(clients) && clients.stream().anyMatch(client ->
                constantTimeEquals(client.getAlias(), authRequest.alias())
                        && constantTimeEquals(client.getPassword(), authRequest.password()));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (Objects.isNull(expected) || Objects.isNull(actual)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
