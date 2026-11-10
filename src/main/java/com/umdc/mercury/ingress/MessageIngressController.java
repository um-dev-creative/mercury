package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.to.MailStatusResponse;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/messages")
public class MessageIngressController implements MessageIngressApi {

    private final MessageIngressService service;

    public MessageIngressController(MessageIngressService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody MessageRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        var accepted = service.accept(Objects.requireNonNull(request), caller, idempotencyKey);
        var answer = ResponseEntity.status(HttpStatus.ACCEPTED);
        if (accepted.replayed()) {
            answer.header("Idempotent-Replayed", "true");
        }
        return answer.body(accepted.response());
    }

    @Override
    public ResponseEntity<List<String>> channels() {
        return ResponseEntity.ok(service.channels());
    }

    @Override
    public ResponseEntity<MailStatusResponse> status(@PathVariable("id") UUID id,
            @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return ResponseEntity.ok(service.status(id, caller));
    }
}
