package com.umdc.mercury.ingress;

import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.constant.DeliveryStatusType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageIngressControllerTest {

    private final MessageIngressService service = mock(MessageIngressService.class);
    private final MessageIngressController controller = new MessageIngressController(service);
    private final MessageRequest request = new MessageRequest("sms", UUID.randomUUID(), "+34600112233", null, null, null, null, null, null, "hola", null, Map.of(), null, null, null);

    @Test
    void anAcceptedMessageAnswers202WithItsIdAndNoReplayHeader() {
        var response = new MessageResponse(UUID.randomUUID(), "OPENED", "Queued");
        when(service.accept(request, "zerobound", "k")).thenReturn(new MessageIngressService.Accepted(response, false));

        var answer = controller.send(request, "k", "zerobound");

        assertEquals(HttpStatus.ACCEPTED, answer.getStatusCode());
        assertEquals(response, answer.getBody());
        assertNull(answer.getHeaders().getFirst("Idempotent-Replayed"));
    }

    @Test
    void aReplayIsMarkedSoTheCallerKnowsNothingNewWasQueued() {
        var response = new MessageResponse(UUID.randomUUID(), "SENT", "Already accepted");
        when(service.accept(request, "zerobound", "k")).thenReturn(new MessageIngressService.Accepted(response, true));

        var answer = controller.send(request, "k", "zerobound");

        assertEquals("true", answer.getHeaders().getFirst("Idempotent-Replayed"));
    }

    @Test
    void aNullBodyIsRejected() {
        assertThrows(NullPointerException.class, () -> controller.send(null, null, "zerobound"));
    }

    @Test
    void theChannelsAndTheStatusComeFromTheService() {
        var id = UUID.randomUUID();
        var status = new MailStatusResponse(id, DeliveryStatusType.SENT, Instant.now(), Instant.now());
        when(service.channels()).thenReturn(List.of("email", "sms"));
        when(service.status(id, "zerobound")).thenReturn(status);

        assertEquals(List.of("email", "sms"), controller.channels().getBody());
        assertEquals(status, controller.status(id, "zerobound").getBody());
    }

    @Test
    void theTemplatesOfAnApplicationComeFromTheService() {
        var app = UUID.randomUUID();
        var entries = List.of(new TemplateNameResolver.Entry("zb_verification", List.of("en", "es")));
        when(service.templates(app, app.toString())).thenReturn(entries);

        assertEquals(entries, controller.templates(app, app.toString()).getBody());
    }

    @Test
    void theApiInterfaceAnswers501UntilAControllerImplementsIt() {
        MessageIngressApi bare = new MessageIngressApi() {
        };

        assertEquals(501, bare.send(request, null, null).getStatusCode().value());
        assertEquals(501, bare.channels().getStatusCode().value());
        assertEquals(501, bare.status(UUID.randomUUID(), null).getStatusCode().value());
        assertEquals(501, bare.templates(UUID.randomUUID(), null).getStatusCode().value());
    }
}
