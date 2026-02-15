package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.EmailServiceImpl;
import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.api.v1.to.SendEmailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MailControllerTest {

    @Test
    @DisplayName("Send email successfully")
    void sendEmailSuccessfully() {
        EmailServiceImpl emailService = Mockito.mock(EmailServiceImpl.class);
        SendEmailRequest request = new SendEmailRequest(
                "templateName",
                "from@example.com",
                List.of(new EmailContact("to@example.com", "to", " To" )),
                List.of(new EmailContact("cc@example.com", "cc", " Cc" )),
                "body",
                "templateId",
                LocalDateTime.now(),
                Map.of());
        SendEmailResponse response = new SendEmailResponse(UUID.randomUUID(), "Delivered", "body");

        when(emailService.sendMail(any(SendEmailRequest.class))).thenReturn(ResponseEntity.ok(response));

        MailController mailController = new MailController(emailService);

        ResponseEntity<SendEmailResponse> result = mailController.send(request);

        assertTrue(result.getStatusCode().is2xxSuccessful());
        assertEquals("Delivered", Objects.requireNonNull(result.getBody()).status());
    }

    @Test
    @DisplayName("Send email with null request")
    void sendEmailWithNullRequest() {
        EmailServiceImpl emailService = Mockito.mock(EmailServiceImpl.class);
        MailController mailController = new MailController(emailService);

        assertThrows(NullPointerException.class, () -> mailController.send(null));
    }

    @Test
    @DisplayName("Send email with invalid email address")
    void sendEmailWithInvalidEmailAddress() {
        EmailServiceImpl emailService = Mockito.mock(EmailServiceImpl.class);
        SendEmailRequest request = new SendEmailRequest(
                "templateName",
                "invalid-email",
                List.of(new EmailContact("to@example.com", "to", " To" )),
                Collections.emptyList(),
                "subject",
                "body",
                LocalDateTime.now(),
                Map.of());

        when(emailService.sendMail(any(SendEmailRequest.class))).thenThrow(new IllegalArgumentException("Invalid email address"));

        MailController mailController = new MailController(emailService);

        assertThrows(IllegalArgumentException.class, () -> mailController.send(request));
    }

    @Test
    @DisplayName("Send email with empty subject")
    void sendEmailWithEmptySubject() {
        EmailServiceImpl emailService = Mockito.mock(EmailServiceImpl.class);
        SendEmailRequest request = new SendEmailRequest(
                "templateName",
                "from@example.com",
                List.of(new EmailContact("to@example.com", "to", " To" )),
                List.of(new EmailContact("cc@example.com", "cc", " Cc" )),
                "",
                "templateId",
                LocalDateTime.now(),
                Map.of());

        when(emailService.sendMail(any(SendEmailRequest.class))).thenThrow(new IllegalArgumentException("Subject cannot be empty"));

        MailController mailController = new MailController(emailService);

        assertThrows(IllegalArgumentException.class, () -> mailController.send(request));
    }
}
