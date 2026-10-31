package com.umdc.mercury.api.v1.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerResponseStatusTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void keepsTheChosenStatusAndReason() {
        when(request.getRequestURI()).thenReturn("/api/v1/telegram/links");

        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram channel unavailable"), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Telegram channel unavailable", response.getBody().message());
    }

    @Test
    void usesTheStatusPhraseWithoutAReason() {
        when(request.getRequestURI()).thenReturn("/api/v1/telegram/webhook/zero");

        var response = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.UNAUTHORIZED), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().message());
    }
}
