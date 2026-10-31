package com.umdc.mercury.telegram;

import com.umdc.mercury.security.MachineTokenInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// The mappings, with their path variables, are on the *Api interface, which java:S6856 does not follow.
@SuppressWarnings("java:S6856")
@RestController
@RequestMapping("api/v1/telegram/links")
public class TelegramLinkController implements TelegramLinkApi {

    private final TelegramLinkService service;

    public TelegramLinkController(TelegramLinkService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<TelegramLinkResponse> create(@Valid @RequestBody TelegramLinkRequest request,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        final var created = service.createLink(caller, request.userId(), request.language());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TelegramLinkResponse(created.deepLink(), created.expiresAt()));
    }

    @Override
    public ResponseEntity<TelegramLinkStatusResponse> get(@PathVariable("userId") UUID userId,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        final var status = service.status(caller, userId);
        return ResponseEntity.ok(new TelegramLinkStatusResponse(status.status(), status.linkedAt(), status.language()));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable("userId") UUID userId,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        service.unlink(caller, userId);
        return ResponseEntity.noContent().build();
    }
}
