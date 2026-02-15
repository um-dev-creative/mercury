package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.EmailService;
import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.api.v1.to.SendEmailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Objects;

@Tag(name = "mail", description = "The mail API")
public interface MailApi {

    default EmailService getService() {
        return new EmailService() {
        };
    }

    /**
     * Send an email.
     * This method handles POST requests to send an email. It consumes and produces JSON.
     * The request body is expected to be a {@link SendEmailRequest} containing email details.
     * The method returns a {@link ResponseEntity} with the result of the email sending operation.
     *
     * @param requestMail the request entity containing the email details
     * @return a ResponseEntity with the result of the email sending operation
     */
    @Operation(description = "Send a email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "To send a email.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<SendEmailResponse> send(@RequestBody SendEmailRequest requestMail) {
        return this.getService().sendMail(Objects.requireNonNull(requestMail));
    }
}
