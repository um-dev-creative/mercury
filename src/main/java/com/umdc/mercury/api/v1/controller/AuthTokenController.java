package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.security.MachineTokenExchange;
import com.umdc.security.to.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces security-oauth's {@code AuthApiController} (excluded in {@code MercuryApplication}). {@code POST /api/v1/auth/token} sits behind the Backbone
 * resource-server chain ({@code ManagedClientSecurityConfig}): by the time it runs the bearer token is active and its scopes are known, and
 * {@link MachineTokenExchange} turns that identity into Mercury's own M2M token.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthTokenController implements AuthTokenApi {

    private final MachineTokenExchange exchange;

    public AuthTokenController(MachineTokenExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public ResponseEntity<AuthResponse> accessToken(OAuth2AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(exchange.exchange(principal));
    }
}
