package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.MailNotFoundException;
import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.document.MailTrackingDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MailTrackingNSRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Mail intake: queue once, log the delivery")
class MailTrackingServiceTest {

    EmailMessageNSRepository messages = mock(EmailMessageNSRepository.class);
    MailTrackingNSRepository tracking = mock(MailTrackingNSRepository.class);
    TemplateDefinedService templates = mock(TemplateDefinedService.class);
    MailTrackingService service;
    final UUID template = UUID.randomUUID();
    final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MailTrackingService(messages, tracking, templates);
    }

    private SendEmailRequest request(String subject, Map<String, Object> params) {
        return new SendEmailRequest(template, user, "no-reply@zerobound.test",
                List.of(new EmailContact("ana@example.com", "Ana", "ana")), List.of(), subject, "b",
                LocalDateTime.of(2026, 10, 17, 8, 0), params);
    }

    @Test
    @DisplayName("a new mail is claimed, queued as OPENED and answered with its id")
    void acceptsAndQueues() {
        var accepted = service.accept(request("s", Map.of("x", 1)), "zerobound", null);

        assertThat(accepted.replayed()).isFalse();
        assertThat(accepted.response().status()).isEqualTo("OPENED");
        var queued = ArgumentCaptor.forClass(EmailMessageDocument.class);
        verify(messages).save(queued.capture());
        assertThat(queued.getValue().messageId()).isEqualTo(accepted.response().id());
        assertThat(queued.getValue().deliveryStatus()).isEqualTo(DeliveryStatusType.OPENED);
        var claim = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking).insert(claim.capture());
        assertThat(claim.getValue().id()).isEqualTo(accepted.response().id().toString());   // no key: the id is the claim
    }

    @Test
    @DisplayName("an unknown template is refused before anything is stored")
    void unknownTemplate() {
        when(templates.find(template)).thenThrow(new IllegalArgumentException("Template defined not found"));

        assertThatThrownBy(() -> service.accept(request("s", Map.of()), "zerobound", "k"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(messages, tracking);
    }

    @Test
    @DisplayName("the same key and request is accepted once: the repeat answers the first message")
    void replayAnswersTheFirstMessage() {
        var first = request("s", Map.of("a", 1, "b", 2));
        var claim = new MailTrackingDocument("zerobound:k1", UUID.randomUUID(), "zerobound",
                MailTrackingService.hash(first), DeliveryStatusType.SENT, null, null);
        doThrow(new DuplicateKeyException("dup")).when(tracking).insert(any(MailTrackingDocument.class));
        when(tracking.findById("zerobound:k1")).thenReturn(Optional.of(claim));

        // same content, params in another order
        var params = new LinkedHashMap<String, Object>();
        params.put("b", 2);
        params.put("a", 1);
        var again = service.accept(request("s", params), "zerobound", " k1 ");

        assertThat(again.replayed()).isTrue();
        assertThat(again.response().id()).isEqualTo(claim.messageId());
        assertThat(again.response().status()).isEqualTo("SENT");
        verify(messages, never()).save(any());
    }

    @Test
    @DisplayName("the same key with another request is refused")
    void reusedKeyWithAnotherRequest() {
        var claim = new MailTrackingDocument("zerobound:k1", UUID.randomUUID(), "zerobound",
                MailTrackingService.hash(request("s", Map.of())), DeliveryStatusType.OPENED, null, null);
        doThrow(new DuplicateKeyException("dup")).when(tracking).insert(any(MailTrackingDocument.class));
        when(tracking.findById("zerobound:k1")).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.accept(request("other subject", Map.of()), "zerobound", "k1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different request");
    }

    @Test
    @DisplayName("a duplicate whose first claim vanished asks the caller to retry")
    void claimDisappears() {
        doThrow(new DuplicateKeyException("dup")).when(tracking).insert(any(MailTrackingDocument.class));
        when(tracking.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(request("s", Map.of()), "zerobound", "k1"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("retry");
    }

    @Test
    @DisplayName("if queuing fails the claim is released so the same key can be retried")
    void claimReleasedWhenQueuingFails() {
        when(messages.save(any())).thenThrow(new IllegalStateException("mongo down"));

        assertThatThrownBy(() -> service.accept(request("s", Map.of()), "zerobound", "k1"))
                .hasMessage("mongo down");
        verify(tracking).deleteById("zerobound:k1");
    }

    @Test
    @DisplayName("the key is scoped to the caller: two callers with the same key do not collide")
    void keyIsScopedToTheCaller() {
        service.accept(request("s", Map.of()), "zerobound", "k");
        service.accept(request("s", Map.of()), "directory", "k");

        var claims = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking, times(2)).insert(claims.capture());
        assertThat(claims.getAllValues()).extracting(MailTrackingDocument::id)
                .containsExactly("zerobound:k", "directory:k");
    }

    @Test
    @DisplayName("status is visible to the caller that sent the mail only")
    void statusIsScoped() {
        var id = UUID.randomUUID();
        var doc = new MailTrackingDocument("x", id, "zerobound", "h", DeliveryStatusType.FAILED, null, null);
        when(tracking.findByMessageId(id)).thenReturn(Optional.of(doc));

        assertThat(service.status(id, "zerobound").status()).isEqualTo(DeliveryStatusType.FAILED);
        assertThatThrownBy(() -> service.status(id, "directory")).isInstanceOf(MailNotFoundException.class);
        assertThatThrownBy(() -> service.status(UUID.randomUUID(), "zerobound")).isInstanceOf(MailNotFoundException.class);
    }

    @Test
    @DisplayName("the scheduler's state change reaches the log; mail not sent through the API is ignored")
    void recordsStatus() {
        var id = UUID.randomUUID();
        var doc = new MailTrackingDocument("x", id, "zerobound", "h", DeliveryStatusType.OPENED, null, null);
        when(tracking.findByMessageId(id)).thenReturn(Optional.of(doc));

        service.recordStatus(id, DeliveryStatusType.SENT);
        service.recordStatus(UUID.randomUUID(), DeliveryStatusType.SENT);
        service.recordStatus(null, DeliveryStatusType.SENT);
        service.recordStatus(id, null);

        var saved = ArgumentCaptor.forClass(MailTrackingDocument.class);
        verify(tracking, times(1)).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(DeliveryStatusType.SENT);
    }

    @Test
    @DisplayName("nested lists and a null params map hash deterministically")
    void hashHandlesNestedValues() {
        var a = request("s", Map.of("l", List.of(Map.of("k", 1, "j", 2))));
        var b = request("s", Map.of("l", List.of(Map.of("j", 2, "k", 1))));

        assertThat(MailTrackingService.hash(a)).isEqualTo(MailTrackingService.hash(b));
        assertThat(MailTrackingService.hash(request("s", null))).isNotEqualTo(MailTrackingService.hash(a));
    }
}
