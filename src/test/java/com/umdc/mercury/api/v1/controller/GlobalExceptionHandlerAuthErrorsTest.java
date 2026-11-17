package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.api.v1.exception.InvalidSessionTokenException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerAuthErrorsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void forbiddenAnswers403() {
        when(request.getRequestURI()).thenReturn("/api/v1/templates");

        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleForbidden(new ForbiddenException("denied"), request).getStatusCode());
    }

    @Test
    void invalidSessionTokenAnswers401() {
        when(request.getRequestURI()).thenReturn("/api/v1/campaigns");

        assertEquals(HttpStatus.UNAUTHORIZED,
                handler.handleInvalidSessionToken(new InvalidSessionTokenException("expired"), request).getStatusCode());
    }

    @Test
    void errorBodyIsJsonWhateverTheRequestAccepts() {
        when(request.getRequestURI()).thenReturn("/api/v1/monitoring/applications/x/stream");

        var response = handler.handleInvalidSessionToken(new InvalidSessionTokenException("expired"), request);

        assertEquals(org.springframework.http.MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(org.springframework.http.MediaType.APPLICATION_JSON,
                handler.handleForbidden(new ForbiddenException("denied"), request).getHeaders().getContentType());
        assertEquals(org.springframework.http.MediaType.APPLICATION_JSON,
                handler.handleGeneral(new RuntimeException("x"), request).getHeaders().getContentType());
    }
}
