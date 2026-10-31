package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.api.v1.to.SendEmailResponse;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("EmailServiceImpl: backend intake and delivery log")
class EmailServiceImplMailTest {

    MailTrackingService tracking = mock(MailTrackingService.class);
    EmailMessageNSRepository messages = mock(EmailMessageNSRepository.class);
    EmailServiceImpl service = new EmailServiceImpl(new freemarker.template.Configuration(
            freemarker.template.Configuration.VERSION_2_3_30), messages, mock(JavaMailSender.class), tracking);

    private SendEmailRequest request() {
        return new SendEmailRequest(UUID.randomUUID(), UUID.randomUUID(), "f@x.test",
                List.of(new EmailContact("a@x.test", "A", "a")), List.of(), "s", "b", LocalDateTime.now(), Map.of());
    }

    @Test
    @DisplayName("a first request answers 200 with no replay header")
    void firstRequest() {
        var body = new SendEmailResponse(UUID.randomUUID(), "OPENED", "Queued");
        when(tracking.accept(any(), any(), any())).thenReturn(new MailTrackingService.Accepted(body, false));

        var answer = service.sendMail(request(), "zerobound", "k");

        assertThat(answer.getStatusCode().value()).isEqualTo(200);
        assertThat(answer.getBody()).isEqualTo(body);
        assertThat(answer.getHeaders().containsHeader("Idempotent-Replayed")).isFalse();
    }

    @Test
    @DisplayName("a replay is marked Idempotent-Replayed")
    void replay() {
        var body = new SendEmailResponse(UUID.randomUUID(), "SENT", "Already accepted");
        when(tracking.accept(any(), any(), any())).thenReturn(new MailTrackingService.Accepted(body, true));

        assertThat(service.sendMail(request(), "zerobound", "k").getHeaders().getFirst("Idempotent-Replayed"))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("the status endpoint answers what the log holds")
    void status() {
        var id = UUID.randomUUID();
        var state = new MailStatusResponse(id, DeliveryStatusType.SENT, Instant.now(), Instant.now());
        when(tracking.status(id, "zerobound")).thenReturn(state);

        assertThat(service.mailStatus(id, "zerobound").getBody()).isEqualTo(state);
    }

    @Test
    @DisplayName("the scheduler's status update is copied to the delivery log")
    void updateStatusFeedsTheLog() {
        var messageId = UUID.randomUUID();
        var doc = new EmailMessageDocument("m1", messageId, UUID.randomUUID(), UUID.randomUUID(), "f", List.of(),
                List.of(), "s", "b", LocalDateTime.now(), Map.of(), DeliveryStatusType.FAILED);

        service.updateEmailStatus(doc);

        verify(messages).save(doc);
        verify(tracking).recordStatus(messageId, DeliveryStatusType.FAILED);
    }

    @Test
    @DisplayName("requests and documents do not print recipients, subject, body or parameters")
    void logsCarryNoPersonalData() {
        var req = new SendEmailRequest(UUID.randomUUID(), UUID.randomUUID(), "f@x.test",
                List.of(new EmailContact("secret@x.test", "Secret Name", "sn")), List.of(), "secret subject",
                "secret body", LocalDateTime.now(), Map.of("amount", "secret-param"));
        var doc = new EmailMessageDocument("m1", UUID.randomUUID(), req.templateDefinedId(), req.userId(), "f@x.test",
                req.to(), req.cc(), "secret subject", "secret body", LocalDateTime.now(), req.params(),
                DeliveryStatusType.OPENED);

        for (String printed : new String[]{req.toString(), doc.toString()}) {
            assertThat(printed).doesNotContain("secret").doesNotContain("x.test");
        }
        assertThat(new SendEmailRequest(null, null, "f", null, null, "s", "b", null, null).toString())
                .contains("recipients=0");
    }
}
