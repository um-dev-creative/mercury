package com.umdc.mercury.telegram;

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

import java.util.UUID;

/** Telegram messages of a backend caller's users (T-115). Only mercury-minted M2M tokens. */
@Tag(name = "telegram", description = "Telegram messages (backend callers only)")
public interface TelegramMessageApi {

    @Operation(description = "Queues a message built from a template kind and parameters for a linked user. Send an "
            + "Idempotency-Key so a retry is safe.", operationId = "sendTelegramMessage")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted, or the replay of an earlier request with the same key (Idempotent-Replayed: true)."),
            @ApiResponse(responseCode = "400", description = "Invalid request, unknown template or language."),
            @ApiResponse(responseCode = "401", description = "Not a Mercury-minted backend token."),
            @ApiResponse(responseCode = "409", description = "TELEGRAM_NOT_LINKED or TELEGRAM_BLOCKED."),
            @ApiResponse(responseCode = "422", description = "The Idempotency-Key was already used with a different request.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Object> send(@Valid @RequestBody TelegramMessageRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }

    @Operation(description = "Delivery state of a message this caller submitted.", operationId = "getTelegramMessage")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "QUEUED, SENT, FAILED or BLOCKED."),
            @ApiResponse(responseCode = "404", description = "Unknown id or another caller's message.")})
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<TelegramMessageStatusResponse> status(@PathVariable("id") UUID id,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }
}
