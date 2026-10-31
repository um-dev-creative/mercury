package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.to.CreateLoginClientRequest;
import com.umdc.mercury.api.v1.to.LoginClientResponse;
import com.umdc.mercury.api.v1.to.LoginClientSecretResponse;
import com.umdc.mercury.api.v1.to.RotateLoginClientPasswordRequest;
import com.umdc.mercury.api.v1.to.UpdateLoginClientRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

@Tag(name = "login-clients", description = "Backend services allowed to log in to Mercury (POST /api/v1/auth/token)")
public interface LoginClientApi {

    String CODE_401 = "401";
    String CODE_403 = "403";
    String MSG_UNAUTHORIZED = "Invalid or missing session token.";
    String MSG_FORBIDDEN = "Caller lacks the login-client-manage permission, or the administration is not configured.";
    String MSG_NOT_FOUND = "Login client not found.";

    @Operation(summary = "List the login clients",
            description = "Every registered backend service, without any password material. Needs the login-client-manage permission.",
            operationId = "listLoginClients")
    @ApiResponse(responseCode = "200", description = "The login clients, by alias.")
    @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)
    @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<LoginClientResponse>> list(@RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    @Operation(summary = "Register a login client",
            description = "Registers a backend service. When the body carries no password Mercury generates one and returns it ONCE; only its "
                    + "bcrypt hash is stored.",
            operationId = "createLoginClient")
    @ApiResponse(responseCode = "201", description = "Registered. `password` is present only when Mercury generated it.")
    @ApiResponse(responseCode = "400", description = "Invalid alias or password.")
    @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)
    @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN)
    @ApiResponse(responseCode = "409", description = "A login client with this alias already exists.")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LoginClientSecretResponse> create(@RequestBody @Valid CreateLoginClientRequest request,
                                                     @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);

    @Operation(summary = "Change a login client",
            description = "Updates the description and/or switches the service on or off. Fields left out stay as they were.",
            operationId = "updateLoginClient")
    @ApiResponse(responseCode = "200", description = "Updated.")
    @ApiResponse(responseCode = "400", description = "Invalid body.")
    @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)
    @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN)
    @ApiResponse(responseCode = "404", description = MSG_NOT_FOUND)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LoginClientResponse> update(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
                                               @RequestBody @Valid UpdateLoginClientRequest request);

    @Operation(summary = "Rotate the password of a login client",
            description = "Replaces the password and reactivates the service. Without a password in the body Mercury generates one and "
                    + "returns it ONCE. The service must be given the new password.",
            operationId = "rotateLoginClientPassword")
    @ApiResponse(responseCode = "200", description = "Rotated. `password` is present only when Mercury generated it.")
    @ApiResponse(responseCode = "400", description = "Invalid password.")
    @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)
    @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN)
    @ApiResponse(responseCode = "404", description = MSG_NOT_FOUND)
    @PutMapping(value = "/{id}/password", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LoginClientSecretResponse> rotatePassword(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken,
                                                             @RequestBody(required = false) @Valid RotateLoginClientPasswordRequest request);

    @Operation(summary = "Delete a login client",
            description = "Removes the row. An alias that is still in the static umdc.mercury.login-clients list can log in through it again; "
                    + "to switch a service off for good, update it with active=false instead.",
            operationId = "deleteLoginClient")
    @ApiResponse(responseCode = "204", description = "Deleted.")
    @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)
    @ApiResponse(responseCode = CODE_403, description = MSG_FORBIDDEN)
    @ApiResponse(responseCode = "404", description = MSG_NOT_FOUND)
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestHeader(SESSION_TOKEN_KEY) String sessionToken);
}
