package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.security.LoginClientProperties;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.client.BackbonePublicClient;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service implementation for authentication-related operations.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SessionJwtServiceImpl sessionJwtService;
    private final BackbonePublicClient backbonePublicClient;
    private final LoginClientProperties loginClientProperties;

    /**
     * Constructor for AuthServiceImpl.
     *
     * @param sessionJwtService      the service for generating JWT tokens
     * @param backbonePublicClient   the client for backbone-rest's public endpoints
     * @param loginClientProperties  the registry of backend callers allowed to log in
     */
    public AuthServiceImpl(SessionJwtServiceImpl sessionJwtService, BackbonePublicClient backbonePublicClient,
                            LoginClientProperties loginClientProperties) {
        this.sessionJwtService = sessionJwtService;
        this.backbonePublicClient = backbonePublicClient;
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

    /**
     * Exchanges a backbone-issued {@code session-token-bkd} for a Mercury-signed session
     * token carrying the same {@code uid}.
     * <p>
     * {@code sessionTokenBkd} is caller-supplied and this endpoint is
     * {@code @SkipSessionValidation} (see {@code AuthAPi} in {@code security-oauth} — login
     * endpoints can't hold a session token before one is issued), so Mercury's own
     * {@code SessionJwtInterceptor} never validates it. Mercury and backbone-rest share the
     * same HS256 secret ({@code APP_TOKEN_SECRET}), so {@link SessionJwtServiceImpl} can and
     * must verify {@code sessionTokenBkd}'s signature itself before trusting its {@code uid}
     * claim — this method used to extract it via the unverified
     * {@code com.umdc.commons.util.JwtUtil.getUidFromToken}, which let any caller who could
     * satisfy the registered-login-client alias/password check mint a validly-signed Mercury
     * session token for an arbitrary {@code uid} of their choosing, impersonating any user
     * everywhere that token is later trusted (e.g. the Template Management API).
     * </p>
     *
     * @param authRequest     the registered login client's alias/password pair
     * @param sessionTokenBkd the backbone-issued token identifying the end user this session
     *                        is issued on behalf of
     * @return {@code 200} with the new session token, {@code 400} for a blank alias or a
     *         {@code sessionTokenBkd} that fails signature verification, or {@code 406} if
     *         token generation unexpectedly yields a blank token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest, String sessionTokenBkd) {
        if (authRequest.alias().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<UUID> userId = sessionJwtService.getVerifiedUid(sessionTokenBkd);
        if (userId.isEmpty()) {
            logger.warn("Rejected session-token-bkd for alias={}: signature verification failed", authRequest.alias());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        var parameters = new ConcurrentHashMap<String, String>();
        parameters.put("uid", userId.get().toString());

        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), parameters));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Validates the provided session token using the backbone client.
     *
     * @param sessionTokenBkd the session token to validate
     * @return true if the session token is valid, false otherwise
     */
    @Override
    public boolean validate(String sessionTokenBkd) {
        return backbonePublicClient.validate(sessionTokenBkd);
    }
}
