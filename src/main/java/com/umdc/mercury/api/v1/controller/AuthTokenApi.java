package com.umdc.mercury.api.v1.controller;

import com.umdc.security.annotation.SkipSessionValidation;
import com.umdc.security.to.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "auth", description = "Mercury M2M token for backend applications")
@SkipSessionValidation("Token endpoint — the caller has no Mercury token yet; authenticated by its Backbone M2M bearer token")
public interface AuthTokenApi {

    @Operation(
            summary = "Exchange a Backbone M2M token for a Mercury M2M token",
            description = "The application sends its Backbone managed-client token as `Authorization: Bearer <token>` (no body, no password). "
                    + "Mercury introspects it against Backbone and, if it is active and holds a `mercury:message:read` or `mercury:message:write` scope, "
                    + "issues a Mercury token (sent afterwards in the `session-token` header) that carries the client, the application Backbone ties it to "
                    + "and its Mercury scopes. That token lets the application manage its own templates (create, edit, delete) and send messages; "
                    + "it can never reach another application.",
            operationId = "accessToken")
    @ApiResponse(responseCode = "200", description = "Mercury M2M token issued.")
    @ApiResponse(responseCode = "401", description = "Missing, inactive, expired or revoked Backbone token.")
    @ApiResponse(responseCode = "403", description = "The client holds no Mercury scope, or Backbone does not tie it to an application.")
    @PostMapping("/token")
    ResponseEntity<AuthResponse> accessToken(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal);
}
