package com.umdc.mercury.telegram;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("api/v1/telegram/webhook")
public class TelegramWebhookController implements TelegramWebhookApi {

    static final long MAX_BODY_BYTES = 1_048_576L;

    private final TelegramBotProperties properties;
    private final TelegramUpdateHandler handler;

    public TelegramWebhookController(TelegramBotProperties properties, TelegramUpdateHandler handler) {
        this.properties = properties;
        this.handler = handler;
    }

    @Override
    public ResponseEntity<Void> receive(@PathVariable("alias") String alias,
            @RequestHeader(name = SECRET_HEADER, required = false) String secret,
            @RequestBody Map<String, Object> update, HttpServletRequest request) {
        if (!properties.active() || properties.polling() || properties.application().isEmpty()
                || !properties.application().equals(alias)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!secretMatches(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        handler.handle(alias, update);
        return ResponseEntity.ok().build();
    }

    /** Constant-time comparison; an unset secret never matches. */
    private boolean secretMatches(String given) {
        final String expected = properties.webhookSecret();
        return !expected.isEmpty() && given != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
