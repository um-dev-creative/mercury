package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.EmailService;
import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.api.v1.to.SendEmailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class MailApiTest {

    @Test
    @DisplayName("Send email successfully")
    void sendEmailSuccessfully() {
        EmailService emailService = Mockito.mock(EmailService.class);
        SendEmailRequest request = new SendEmailRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "from@example.com",
                List.of(new EmailContact("to@example.com", "to", " To" )),
                List.of(new EmailContact("cc@example.com", "cc", " Cc" )),
                "body",
                "templateId",
                LocalDateTime.now(),
                Map.of());
        SendEmailResponse response = new SendEmailResponse(UUID.randomUUID(), "Delivered", "body");

        Mockito.when(emailService.sendMail(any(SendEmailRequest.class))).thenReturn(ResponseEntity.ok(response));

        MailApi mailApi = new MailApi() {
            @Override
            public EmailService getService() {
                return emailService;
            }
        };

        ResponseEntity<SendEmailResponse> result = mailApi.send(request);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals("Delivered", result.getBody().status());
    }

    @Test
    @DisplayName("Send email with null request")
    void sendEmailWithNullRequest() {
        MailApi mailApi = new MailApi() {
            @Override
            public EmailService getService() {
                return new EmailService() {
                };
            }
        };

        assertThrows(NullPointerException.class, () -> mailApi.send(null));
    }
}
