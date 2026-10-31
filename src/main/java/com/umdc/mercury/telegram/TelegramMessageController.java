package com.umdc.mercury.telegram;

import com.umdc.mercury.security.MachineTokenInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

// The mappings, with their path variables, are on the *Api interface, which java:S6856 does not follow.
@SuppressWarnings("java:S6856")
@RestController
@RequestMapping("api/v1/telegram/messages")
public class TelegramMessageController implements TelegramMessageApi {

    private final TelegramMessageService service;

    public TelegramMessageController(TelegramMessageService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<Object> send(@Valid @RequestBody TelegramMessageRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return switch (service.accept(caller, request, idempotencyKey)) {
            case TelegramMessageService.Result.Accepted accepted -> {
                final var answer = ResponseEntity.status(HttpStatus.ACCEPTED);
                yield (accepted.replayed() ? answer.header("Idempotent-Replayed", "true") : answer)
                        .body(new TelegramMessageResponse(accepted.id(), accepted.status()));
            }
            case TelegramMessageService.Result.NotLinked notLinked ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "TELEGRAM_NOT_LINKED"));
            case TelegramMessageService.Result.Blocked blocked ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "TELEGRAM_BLOCKED"));
        };
    }

    @Override
    public ResponseEntity<TelegramMessageStatusResponse> status(@PathVariable("id") UUID id,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return service.status(id, caller).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
