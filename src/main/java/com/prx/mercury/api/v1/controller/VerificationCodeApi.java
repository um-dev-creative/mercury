package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.VerificationCodeService;
import com.prx.mercury.api.v1.to.VerificationCodeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "verification-code", description = "The verification code API")
public interface VerificationCodeApi {

    default VerificationCodeService getService() {
        return new VerificationCodeService() {
        };
    }

    /**
     * Send verification code to user.
     *
     * @param verificationCodeRequest the request document containing the verification code details
     * @return a ResponseEntity with the result of the verification code sending operation
     */
    @Operation(description = "Send verification code to user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification code sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid phone number"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Void> sendVerificationCode(@RequestBody @Valid VerificationCodeRequest verificationCodeRequest) {
        return this.getService().confirmCode(verificationCodeRequest);
    }

    /**
     * Get the is_verified status of the latest verification code for a user.
     *
     * @param userId the user ID
     * @return ResponseEntity<Boolean> is_verified status or null if not found
     */
    @Operation(description = "Get the is_verified status of the latest verification code for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No verification code found for user"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(path = "/latest-status")
    ResponseEntity<Boolean> getLatestIsVerifiedStatus(@RequestParam("userId") String userId);

}
