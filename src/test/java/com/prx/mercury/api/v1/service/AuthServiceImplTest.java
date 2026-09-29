package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.AuthServiceImpl;
import com.umdc.mercury.security.LoginClientProperties;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.client.BackbonePublicClient;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private final SessionJwtServiceImpl sessionJwtService = mock(SessionJwtServiceImpl.class);
    private final BackbonePublicClient backbonePublicClient = mock(BackbonePublicClient.class);
    private final LoginClientProperties loginClientProperties = registeredClient("validAlias", "validPassword");
    private final AuthServiceImpl authService =
            new AuthServiceImpl(sessionJwtService, backbonePublicClient, loginClientProperties);

    private static LoginClientProperties registeredClient(String alias, String password) {
        var client = new LoginClientProperties.LoginClient();
        client.setAlias(alias);
        client.setPassword(password);
        var properties = new LoginClientProperties();
        properties.setLoginClients(List.of(client));
        return properties;
    }

    @Test
    @DisplayName("token should return OK status with a registered alias/password")
    void tokenShouldReturnOkStatusWithValidAlias() {
        AuthRequest authRequest = new AuthRequest("validAlias", "validPassword");
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("validToken");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("validToken", response.getBody().token());
    }

    @Test
    @DisplayName("token should return BAD_REQUEST status with null alias")
    void tokenShouldReturnBadRequestStatusWithNullAlias() {
        AuthRequest authRequest = new AuthRequest("", null);

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("token should return BAD_REQUEST status with blank alias")
    void tokenShouldReturnBadRequestStatusWithBlankAlias() {
        AuthRequest authRequest = new AuthRequest("", "");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("token should return UNAUTHORIZED status for an alias not in the registry")
    void tokenShouldReturnUnauthorizedForUnknownAlias() {
        AuthRequest authRequest = new AuthRequest("someoneElse", "validPassword");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(sessionJwtService);
    }

    @Test
    @DisplayName("token should return UNAUTHORIZED status for a registered alias with the wrong password")
    void tokenShouldReturnUnauthorizedForWrongPassword() {
        AuthRequest authRequest = new AuthRequest("validAlias", "wrongPassword");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(sessionJwtService);
    }

    @Test
    @DisplayName("token should return UNAUTHORIZED status when no login clients are registered")
    void tokenShouldReturnUnauthorizedWhenRegistryEmpty() {
        var emptyProperties = new LoginClientProperties();
        var service = new AuthServiceImpl(sessionJwtService, backbonePublicClient, emptyProperties);
        AuthRequest authRequest = new AuthRequest("validAlias", "validPassword");

        ResponseEntity<AuthResponse> response = service.token(authRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("token should return NOT_ACCEPTABLE status with blank token")
    void tokenShouldReturnNotAcceptableStatusWithBlankToken() {
        AuthRequest authRequest = new AuthRequest("validAlias", "validPassword");
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("");

        ResponseEntity<AuthResponse> response = authService.token(authRequest);

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("validate should return true for valid token")
    void validateShouldReturnTrueForValidToken() {
        String sessionTokenBkd = "validToken";
        when(backbonePublicClient.validate(sessionTokenBkd)).thenReturn(true);

        boolean isValid = authService.validate(sessionTokenBkd);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("validate should return false for invalid token")
    void validateShouldReturnFalseForInvalidToken() {
        String sessionTokenBkd = "invalidToken";
        when(backbonePublicClient.validate(sessionTokenBkd)).thenReturn(false);

        boolean isValid = authService.validate(sessionTokenBkd);

        assertFalse(isValid);
    }

    /**
     * Covers {@link AuthServiceImpl#token(AuthRequest, String)} — the
     * {@code session-token-bkd}-exchange path. It used to trust the caller-supplied
     * {@code sessionTokenBkd}'s {@code uid} claim without verifying its signature (via the
     * unverified {@code com.umdc.commons.util.JwtUtil.getUidFromToken}), which let any
     * registered login client mint a validly-signed Mercury session token for an arbitrary
     * {@code uid} — full user impersonation. These tests pin the fixed behavior: the
     * {@code uid} embedded in the newly-minted token must come only from a
     * signature-verified claim.
     */
    @Nested
    @DisplayName("token(AuthRequest, sessionTokenBkd) — verified uid exchange")
    class TokenExchangeTests {

        @Test
        @DisplayName("returns OK and embeds the verified uid claim when sessionTokenBkd verifies")
        void tokenExchange_validSessionTokenBkd_embedsVerifiedUid() {
            AuthRequest authRequest = new AuthRequest("validAlias", "validPassword");
            UUID uid = UUID.randomUUID();
            when(sessionJwtService.getVerifiedUid("backbone-token")).thenReturn(Optional.of(uid));
            when(sessionJwtService.generateSessionToken(eq("validAlias"), argThat(params ->
                    uid.toString().equals(params.get("uid")))))
                    .thenReturn("mercuryToken");

            ResponseEntity<AuthResponse> response = authService.token(authRequest, "backbone-token");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("mercuryToken", response.getBody().token());
        }

        @Test
        @DisplayName("returns BAD_REQUEST and never mints a token when sessionTokenBkd fails signature verification")
        void tokenExchange_invalidSessionTokenBkd_rejectsWithoutMinting() {
            AuthRequest authRequest = new AuthRequest("validAlias", "validPassword");
            when(sessionJwtService.getVerifiedUid("forged-token")).thenReturn(Optional.empty());

            ResponseEntity<AuthResponse> response = authService.token(authRequest, "forged-token");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(sessionJwtService, never()).generateSessionToken(anyString(), anyMap());
        }

        @Test
        @DisplayName("returns BAD_REQUEST for a blank alias before even looking at sessionTokenBkd")
        void tokenExchange_blankAlias_returnsBadRequest() {
            AuthRequest authRequest = new AuthRequest("", "validPassword");

            ResponseEntity<AuthResponse> response = authService.token(authRequest, "backbone-token");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(sessionJwtService);
        }

        @Test
        @DisplayName("returns NOT_ACCEPTABLE when token generation unexpectedly yields a blank token")
        void tokenExchange_blankGeneratedToken_returnsNotAcceptable() {
            AuthRequest authRequest = new AuthRequest("validAlias", "validPassword");
            UUID uid = UUID.randomUUID();
            when(sessionJwtService.getVerifiedUid("backbone-token")).thenReturn(Optional.of(uid));
            when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("");

            ResponseEntity<AuthResponse> response = authService.token(authRequest, "backbone-token");

            assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
        }
    }
}
