package com.umdc.mercury.api.v1.controller;

import com.umdc.security.annotation.SkipSessionValidation;
import com.umdc.security.to.AuthRequest;
import com.umdc.security.to.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "auth", description = "Session token issuance for registered backend callers")
@SkipSessionValidation("Login endpoint — the caller has no session-token yet; authenticated by its registered alias/password")
public interface AuthTokenApi {

    @Operation(
            summary = "Generate a session token (M2M-only, no uid)",
            description = "Checks the body's alias/password against Mercury's registered login-client registry "
                    + "(umdc.mercury.login-clients) and mints a Mercury-signed session-token carrying no uid claim. "
                    + "It only authenticates the calling backend service, so it is only useful against pure M2M "
                    + "endpoints (/api/v1/mail, /api/v1/verification-code). User-scoped endpoints take the end "
                    + "user's backbone-rest session token directly in the session-token header instead.",
            operationId = "accessToken")
    @ApiResponse(responseCode = "200", description = "Successfully generated session token.")
    @ApiResponse(responseCode = "400", description = "Blank alias.")
    @ApiResponse(responseCode = "401", description = "Alias/password not in the registered login-client registry.")
    @ApiResponse(responseCode = "429", description = "Too many failed attempts for this alias and address; see Retry-After.")
    @ApiResponse(responseCode = "406", description = "Token generation unexpectedly produced a blank token.")
    @PostMapping("/token")
    ResponseEntity<AuthResponse> accessToken(@RequestBody AuthRequest authRequest,
                                             @Parameter(hidden = true) HttpServletRequest request);
}
