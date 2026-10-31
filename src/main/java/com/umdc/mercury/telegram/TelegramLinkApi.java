package com.umdc.mercury.telegram;

import com.umdc.mercury.security.MachineTokenInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/** Telegram links of a backend caller's users (T-113). Only mercury-minted M2M tokens; the caller is the application. */
@Tag(name = "telegram", description = "Telegram links (backend callers only)")
public interface TelegramLinkApi {

    @Operation(description = "A deep link t.me/<bot>?start=<token> (10 minutes, single use). Earlier tokens of the user stop working.",
            operationId = "createTelegramLink")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The link and when it expires."),
            @ApiResponse(responseCode = "400", description = "Invalid user or unsupported language."),
            @ApiResponse(responseCode = "401", description = "Not a Mercury-minted backend token."),
            @ApiResponse(responseCode = "503", description = "The Telegram channel is off or the bot is unreachable.")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<TelegramLinkResponse> create(@Valid @RequestBody TelegramLinkRequest request,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }

    @Operation(description = "State of the user's link: NONE, PENDING, ACTIVE or BLOCKED. Never the chat id.", operationId = "getTelegramLink")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "The state."),
            @ApiResponse(responseCode = "401", description = "Not a Mercury-minted backend token.")})
    @GetMapping(path = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<TelegramLinkStatusResponse> get(@PathVariable("userId") UUID userId,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }

    @Operation(description = "Removes the link and pending tokens of the user. Idempotent.", operationId = "deleteTelegramLink")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Removed (or there was nothing)."),
            @ApiResponse(responseCode = "401", description = "Not a Mercury-minted backend token.")})
    @DeleteMapping("/{userId}")
    default ResponseEntity<Void> delete(@PathVariable("userId") UUID userId,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.status(501).build();
    }
}
