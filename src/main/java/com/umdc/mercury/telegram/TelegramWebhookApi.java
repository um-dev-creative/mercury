package com.umdc.mercury.telegram;

import com.umdc.security.annotation.SkipSessionValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/** Where Telegram posts updates in production (T-114). Not for callers: authenticated by the secret token header. */
@Tag(name = "telegram-webhook", description = "Telegram update webhook (Telegram only)")
@SkipSessionValidation("Called by Telegram, which has no session: authenticated by X-Telegram-Bot-Api-Secret-Token")
public interface TelegramWebhookApi {

    String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    @Operation(description = "Receives a Telegram update. Answers 200 at once; the secret header must match.", operationId = "telegramWebhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accepted."),
            @ApiResponse(responseCode = "401", description = "Wrong or missing secret token."),
            @ApiResponse(responseCode = "404", description = "Webhook mode is off or the alias is not the bot's application."),
            @ApiResponse(responseCode = "413", description = "Body over 1 MB.")
    })
    @PostMapping(path = "/{alias}", consumes = MediaType.APPLICATION_JSON_VALUE)
    default ResponseEntity<Void> receive(@PathVariable("alias") String alias,
            @RequestHeader(name = SECRET_HEADER, required = false) String secret,
            @RequestBody Map<String, Object> update, HttpServletRequest request) {
        return ResponseEntity.status(501).build();
    }
}
