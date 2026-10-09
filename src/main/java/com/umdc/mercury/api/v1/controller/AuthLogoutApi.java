package com.umdc.mercury.api.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

@Tag(name = "auth", description = "Session token issuance and revocation")
public interface AuthLogoutApi {

    @Operation(
            summary = "Revoke the presented session token",
            description = "Adds the token's jti to Mercury's denylist until the token's own expiry, so it is "
                    + "rejected from now on by every Mercury endpoint. It does not log the user out of "
                    + "backbone-rest: do that there as well to invalidate the token everywhere.",
            operationId = "logout")
    @ApiResponse(responseCode = "204", description = "Token revoked.")
    @ApiResponse(responseCode = "401", description = "Missing, expired, tampered, already revoked or jti-less token.")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken);
}
