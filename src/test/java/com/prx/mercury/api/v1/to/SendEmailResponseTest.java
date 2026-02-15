package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailResponseTest {

    @Test
    @DisplayName("Create SendEmailResponse with valid data")
    void createSendEmailResponseWithValidData() {
        UUID id = UUID.randomUUID();
        SendEmailResponse response = new SendEmailResponse(id, "SUCCESS", "Email sent successfully");

        assertEquals(id, response.id());
        assertEquals("SUCCESS", response.status());
        assertEquals("Email sent successfully", response.message());
    }

    @Test
    @DisplayName("Fail to create SendEmailResponse with null id")
    void failToCreateSendEmailResponseWithNullId() {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
            new SendEmailResponse(null, "SUCCESS", "Email sent successfully")
        );

        assertEquals("id must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Fail to create SendEmailResponse with null status")
    void failToCreateSendEmailResponseWithNullStatus() {
        UUID id = UUID.randomUUID();
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
            new SendEmailResponse(id, null, "Email sent successfully")
        );

        assertEquals("status must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Fail to create SendEmailResponse with null message")
    void failToCreateSendEmailResponseWithNullMessage() {
        UUID id = UUID.randomUUID();
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
            new SendEmailResponse(id, "SUCCESS", null)
        );

        assertEquals("message must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("SendEmailResponse toString method")
    void sendEmailResponseToString() {
        UUID id = UUID.randomUUID();
        SendEmailResponse response = new SendEmailResponse(id, "SUCCESS", "Email sent successfully");

        String expected = "SendEmailResponse{id=" + id + ", status='SUCCESS', message='Email sent successfully'}";
        assertEquals(expected, response.toString());
    }
}
