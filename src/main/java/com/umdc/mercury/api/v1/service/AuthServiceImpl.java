package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.client.BackboneClient;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.service.AuthService;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.umdc.commons.util.JwtUtil.getUidFromToken;
import static org.apache.commons.lang3.BooleanUtils.FALSE;

/**
 * Service implementation for authentication-related operations.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SessionJwtServiceImpl sessionJwtService;
    private final BackboneClient backboneClient;

    /**
     * Constructor for AuthServiceImpl.
     *
     * @param sessionJwtService the service for generating JWT tokens
     * @param backboneClient the client for interacting with the backbone service
     */
    public AuthServiceImpl(SessionJwtServiceImpl sessionJwtService, BackboneClient backboneClient) {
        this.sessionJwtService = sessionJwtService;
        this.backboneClient = backboneClient;
    }

    /**
     * Generates a session token based on the provided authentication request.
     *
     * @param authRequest the authentication request containing user alias
     * @return ResponseEntity containing the authentication response with the session token
     */
    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest) {
        if (authRequest.alias().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        var authResponse = new AuthResponse(sessionJwtService.generateSessionToken(authRequest.alias(), new ConcurrentHashMap<>()));
        if (authResponse.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
        return ResponseEntity.ok(authResponse);
    }

    @Override
    public ResponseEntity<AuthResponse> token(AuthRequest authRequest, String sessionTokenBkd) {
        var parameters = new ConcurrentHashMap<String, String>();
        if (authRequest.alias().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        UUID userId = getUidFromToken(sessionTokenBkd);
        try {
            parameters.put("uid", userId.toString());
        } catch (FeignException.NotFound e) {
            logger.info("Token is not validated {}:{}", userId, authRequest.alias());
            parameters.put("vcCompleted", FALSE);
        }
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
        return backboneClient.validate(sessionTokenBkd);
    }
}
