package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.to.MailStatusResponse;
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

import java.util.List;
import java.util.UUID;

/** The channel-agnostic, asynchronous ingress (backend callers only: a token Mercury minted for a registered login client). */
@Tag(name = "messages", description = "One asynchronous ingress for every channel (backend callers only)")
public interface MessageIngressApi {

    @Operation(description = "Accepts a message for any channel mercury has a provider for and answers at once; the delivery is asynchronous. "
            + "Send an Idempotency-Key so a retry is safe.", operationId = "sendMessage")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Queued (OPENED), or the replay of an earlier request with the same Idempotency-Key (Idempotent-Replayed: true)."),
            @ApiResponse(responseCode = "400", description = "Unknown or disabled channel, invalid target, a missing field the channel needs, or an unknown template."),
            @ApiResponse(responseCode = "401", description = "Not a Mercury-minted backend token."),
            @ApiResponse(responseCode = "422", description = "The Idempotency-Key was already used with a different request.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<MessageResponse> send(@Valid @RequestBody MessageRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }

    @Operation(description = "The channel codes this mercury can deliver through now.", operationId = "listMessageChannels")
    @ApiResponse(responseCode = "200", description = "The codes, sorted.")
    @GetMapping(path = "/channels", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<List<String>> channels() {
        return ResponseEntity.status(501).build();
    }

    @Operation(description = "Delivery state of a message this caller submitted: OPENED, SENT or FAILED.", operationId = "getMessageStatus")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "The state."),
            @ApiResponse(responseCode = "404", description = "Unknown id, another caller's message, or past retention.")})
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<MailStatusResponse> status(@PathVariable("id") UUID id,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }
}
