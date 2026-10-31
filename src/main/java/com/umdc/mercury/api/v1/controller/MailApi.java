package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.EmailService;
import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.api.v1.to.SendEmailResponse;
import com.umdc.mercury.security.MachineTokenInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Objects;
import java.util.UUID;

@Tag(name = "mail", description = "The mail API")
public interface MailApi {

    default EmailService getService() {
        return new EmailService() {
        };
    }

    /**
     * Accepts an email for delivery. Only a backend caller with a token Mercury minted for it
     * ({@code POST /api/v1/auth/token}) may call it. With an {@code Idempotency-Key} the same request is
     * accepted once: a repeat answers the first message with {@code Idempotent-Replayed: true}, and the same
     * key with another request is refused (422).
     *
     * @param requestMail    the request document containing the email details
     * @param idempotencyKey optional key chosen by the caller (scoped to the caller)
     * @param caller         login-client alias, set by the machine-token interceptor
     * @return the id to ask for the delivery state, and its state
     */
    @Operation(description = "Accept an email for delivery (backend callers only). Send an Idempotency-Key header to "
            + "make retries safe; the answer id is what GET /api/v1/mail/{id} takes.", operationId = "sendEmail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Accepted (or the replay of an earlier request with the same Idempotency-Key)."),
            @ApiResponse(responseCode = "400", description = "Invalid request or unknown template."),
            @ApiResponse(responseCode = "401", description = "Not a Mercury-minted backend token."),
            @ApiResponse(responseCode = "422", description = "The Idempotency-Key was already used with a different request.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<SendEmailResponse> send(
            @Valid @RequestBody SendEmailRequest requestMail,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return this.getService().sendMail(Objects.requireNonNull(requestMail), caller, idempotencyKey);
    }

    /**
     * Delivery state of a mail the caller submitted.
     *
     * @param id     the id {@link #send} answered
     * @param caller login-client alias, set by the machine-token interceptor
     * @return {@code OPENED}, {@code SENT} or {@code FAILED}; 404 for another caller's or an expired id
     */
    @Operation(description = "Delivery state of a mail this caller submitted.", operationId = "getMailStatus")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The state."),
            @ApiResponse(responseCode = "404", description = "Unknown id, another caller's mail, or past retention.")
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<MailStatusResponse> status(
            @PathVariable("id") UUID id,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return this.getService().mailStatus(id, caller);
    }
}
