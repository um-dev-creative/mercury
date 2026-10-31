package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import com.umdc.mercury.api.v1.service.LoginClientAdminService;
import com.umdc.mercury.api.v1.to.CreateLoginClientRequest;
import com.umdc.mercury.api.v1.to.LoginClientResponse;
import com.umdc.mercury.api.v1.to.LoginClientSecretResponse;
import com.umdc.mercury.api.v1.to.RotateLoginClientPasswordRequest;
import com.umdc.mercury.api.v1.to.UpdateLoginClientRequest;
import com.umdc.mercury.security.SessionJwtServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/// Administration of the backend services allowed to log in to Mercury. The caller is identified by the verified `uid` of the session
/// token, never by decoding the payload by hand.
@RestController
@RequestMapping("/api/v1/login-clients")
public class LoginClientController implements LoginClientApi {

    private static final String INVALID_TOKEN_MESSAGE = "Invalid or missing session token.";

    private final LoginClientAdminService service;
    private final SessionJwtServiceImpl sessionJwtService;

    public LoginClientController(LoginClientAdminService service, SessionJwtServiceImpl sessionJwtService) {
        this.service = service;
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    public ResponseEntity<List<LoginClientResponse>> list(String sessionToken) {
        resolveVerifiedUserId(sessionToken);
        return ResponseEntity.ok(service.list(sessionToken));
    }

    @Override
    public ResponseEntity<LoginClientSecretResponse> create(CreateLoginClientRequest request, String sessionToken) {
        UUID actorId = resolveVerifiedUserId(sessionToken);
        LoginClientSecretResponse created = service.create(request, sessionToken, actorId);
        return ResponseEntity.created(URI.create("/api/v1/login-clients/" + created.client().id())).body(created);
    }

    @Override
    public ResponseEntity<LoginClientResponse> update(UUID id, String sessionToken, UpdateLoginClientRequest request) {
        UUID actorId = resolveVerifiedUserId(sessionToken);
        return ResponseEntity.ok(service.update(id, request, sessionToken, actorId));
    }

    @Override
    public ResponseEntity<LoginClientSecretResponse> rotatePassword(UUID id, String sessionToken, RotateLoginClientPasswordRequest request) {
        UUID actorId = resolveVerifiedUserId(sessionToken);
        return ResponseEntity.ok(service.rotatePassword(id, request, sessionToken, actorId));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id, String sessionToken) {
        UUID actorId = resolveVerifiedUserId(sessionToken);
        service.delete(id, sessionToken, actorId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveVerifiedUserId(String sessionToken) {
        return sessionJwtService.getVerifiedUid(sessionToken).orElseThrow(() -> new InvalidSessionTokenException(INVALID_TOKEN_MESSAGE));
    }
}
