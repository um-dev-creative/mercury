package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.security.LoginClientVerifier;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// The service only decides the HTTP outcome; who is a registered login client is [LoginClientVerifier]'s job and has its own tests.
class AuthServiceImplTest {

    private final SessionJwtServiceImpl sessionJwtService = mock(SessionJwtServiceImpl.class);
    private final LoginClientVerifier verifier = mock(LoginClientVerifier.class);
    private final AuthServiceImpl authService = new AuthServiceImpl(sessionJwtService, verifier);

    @Test
    @DisplayName("token should return OK status with a registered alias/password")
    void tokenShouldReturnOkStatusWithValidAlias() {
        when(verifier.isRegistered("validAlias", "validPassword")).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("validToken");

        ResponseEntity<AuthResponse> response = authService.token(new AuthRequest("validAlias", "validPassword"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("validToken", response.getBody().token());
    }

    @Test
    @DisplayName("token should return BAD_REQUEST status with an empty alias")
    void tokenShouldReturnBadRequestStatusWithEmptyAlias() {
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest("", null)).getStatusCode());
        verifyNoInteractions(verifier, sessionJwtService);
    }

    @Test
    @DisplayName("token should return BAD_REQUEST status with a blank alias")
    void tokenShouldReturnBadRequestStatusWithBlankAlias() {
        assertEquals(HttpStatus.BAD_REQUEST, authService.token(new AuthRequest("  ", "")).getStatusCode());
        verifyNoInteractions(verifier, sessionJwtService);
    }

    @Test
    @DisplayName("token should return UNAUTHORIZED status when the verifier refuses the pair")
    void tokenShouldReturnUnauthorizedWhenNotRegistered() {
        when(verifier.isRegistered("someoneElse", "validPassword")).thenReturn(false);

        ResponseEntity<AuthResponse> response = authService.token(new AuthRequest("someoneElse", "validPassword"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(sessionJwtService);
    }

    @Test
    @DisplayName("token should return NOT_ACCEPTABLE status with blank token")
    void tokenShouldReturnNotAcceptableStatusWithBlankToken() {
        when(verifier.isRegistered("validAlias", "validPassword")).thenReturn(true);
        when(sessionJwtService.generateSessionToken(anyString(), anyMap())).thenReturn("");

        ResponseEntity<AuthResponse> response = authService.token(new AuthRequest("validAlias", "validPassword"));

        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
