package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.exception.CampaignNotFoundException;
import com.prx.mercury.api.v1.to.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/v1/campaigns");
        request = mockRequest;
    }

    // ── Validation (400) ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleValidation – MethodArgumentNotValidException")
    class HandleValidation {

        @Test
        @DisplayName("returns 400 with aggregated field error messages")
        void handleValidation_returns400WithMessages() {
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("createCampaignRequest", "name", "must not be blank");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

            assertAll("validation error",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(400),
                    () -> assertThat(response.getBody().error()).isEqualTo("Bad Request"),
                    () -> assertThat(response.getBody().message()).contains("must not be blank"),
                    () -> assertThat(response.getBody().path()).isEqualTo("/api/v1/campaigns"),
                    () -> assertThat(response.getBody().timestamp()).isNotNull()
            );
        }

        @Test
        @DisplayName("aggregates multiple field errors into a single message")
        void handleValidation_aggregatesMultipleErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> errors = List.of(
                    new FieldError("req", "name", "must not be blank"),
                    new FieldError("req", "channelTypeCode", "must not be null")
            );
            when(bindingResult.getFieldErrors()).thenReturn(errors);

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message())
                    .contains("must not be blank")
                    .contains("must not be null");
        }
    }

    // ── Unreadable body (400) ─────────────────────────────────────────────────

    @Nested
    @DisplayName("handleNotReadable – HttpMessageNotReadableException")
    class HandleNotReadable {

        @Test
        @DisplayName("returns 400 for unreadable request body")
        void handleNotReadable_returns400() {
            HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
            when(ex.getMostSpecificCause()).thenReturn(new IllegalArgumentException("Campaign name is required"));

            ResponseEntity<ApiError> response = handler.handleNotReadable(ex, request);

            assertAll("not readable",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(400),
                    () -> assertThat(response.getBody().message()).contains("Campaign name is required")
            );
        }
    }

    // ── IllegalArgumentException (400) ───────────────────────────────────────

    @Nested
    @DisplayName("handleIllegalArgument – IllegalArgumentException")
    class HandleIllegalArgument {

        @Test
        @DisplayName("returns 400 for unknown template id")
        void handleIllegalArgument_templateNotFound() {
            IllegalArgumentException ex = new IllegalArgumentException("Template not found: abc-123");

            ResponseEntity<ApiError> response = handler.handleIllegalArgument(ex, request);

            assertAll("illegal argument",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(400),
                    () -> assertThat(response.getBody().message()).isEqualTo("Template not found: abc-123")
            );
        }

        @Test
        @DisplayName("returns 400 for missing recipients")
        void handleIllegalArgument_emptyRecipients() {
            IllegalArgumentException ex = new IllegalArgumentException("At least one recipient is required");

            ResponseEntity<ApiError> response = handler.handleIllegalArgument(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("At least one recipient is required");
        }
    }

    // ── IllegalStateException (422) ───────────────────────────────────────────

    @Nested
    @DisplayName("handleIllegalState – IllegalStateException")
    class HandleIllegalState {

        @Test
        @DisplayName("returns 422 for disabled channel type")
        void handleIllegalState_disabledChannel() {
            IllegalStateException ex = new IllegalStateException("Channel type is disabled: Email");

            ResponseEntity<ApiError> response = handler.handleIllegalState(ex, request);

            assertAll("illegal state",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(422),
                    () -> assertThat(response.getBody().error()).isEqualTo("Unprocessable Entity"),
                    () -> assertThat(response.getBody().message()).isEqualTo("Channel type is disabled: Email")
            );
        }
    }

    // ── CompletionException unwrapping ────────────────────────────────────────

    @Nested
    @DisplayName("handleCompletion – CompletionException unwrapping")
    class HandleCompletion {

        @Test
        @DisplayName("unwraps IllegalArgumentException cause and returns 400")
        void handleCompletion_illegalArgumentCause_returns400() {
            CompletionException ex = new CompletionException(
                    new IllegalArgumentException("Channel type not found: fax"));

            ResponseEntity<ApiError> response = handler.handleCompletion(ex, request);

            assertAll("completion + IAE",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(response.getBody().status()).isEqualTo(400),
                    () -> assertThat(response.getBody().message()).contains("Channel type not found")
            );
        }

        @Test
        @DisplayName("unwraps IllegalStateException cause and returns 422")
        void handleCompletion_illegalStateCause_returns422() {
            CompletionException ex = new CompletionException(
                    new IllegalStateException("Channel type is disabled: SMS"));

            ResponseEntity<ApiError> response = handler.handleCompletion(ex, request);

            assertAll("completion + ISE",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY),
                    () -> assertThat(response.getBody().status()).isEqualTo(422),
                    () -> assertThat(response.getBody().message()).contains("Channel type is disabled")
            );
        }

        @Test
        @DisplayName("returns 500 for unexpected cause type")
        void handleCompletion_unknownCause_returns500() {
            CompletableFuture<Void> future = CompletableFuture.failedFuture(new RuntimeException("Kafka down"));
            CompletionException ex;
            try {
                future.join();
                ex = null;
            } catch (CompletionException e) {
                ex = e;
            }

            ResponseEntity<ApiError> response = handler.handleCompletion(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().status()).isEqualTo(500);
        }
    }

    // ── Generic exception (500) ───────────────────────────────────────────────

    @Nested
    @DisplayName("handleGeneral – catch-all")
    class HandleGeneral {

        @Test
        @DisplayName("returns 500 for any unhandled exception")
        void handleGeneral_returns500() {
            Exception ex = new RuntimeException("Unexpected database failure");

            ResponseEntity<ApiError> response = handler.handleGeneral(ex, request);

            assertAll("general error",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(500),
                    () -> assertThat(response.getBody().error()).isEqualTo("Internal Server Error"),
                    () -> assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred"),
                    () -> assertThat(response.getBody().path()).isEqualTo("/api/v1/campaigns")
            );
        }

        @Test
        @DisplayName("returns 500 without exposing internal details in message")
        void handleGeneral_doesNotExposeInternalDetails() {
            Exception ex = new RuntimeException("SELECT * FROM secret_table — internal SQL");

            ResponseEntity<ApiError> response = handler.handleGeneral(ex, request);

            assertThat(response.getBody().message()).doesNotContain("secret_table");
        }
    }

    // ── CampaignNotFoundException (404) ───────────────────────────────────────

    @Nested
    @DisplayName("handleCampaignNotFound – CampaignNotFoundException")
    class HandleCampaignNotFound {

        @Test
        @DisplayName("returns 404 with campaign id in message")
        void handleCampaignNotFound_returns404() {
            String id = "fe5e185c-5525-4155-b361-ce6960525b16";
            CampaignNotFoundException ex = new CampaignNotFoundException("Campaign not found: " + id);

            ResponseEntity<ApiError> response = handler.handleCampaignNotFound(ex, request);

            assertAll("not found",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(404),
                    () -> assertThat(response.getBody().error()).isEqualTo("Not Found"),
                    () -> assertThat(response.getBody().message()).contains(id),
                    () -> assertThat(response.getBody().path()).isEqualTo("/api/v1/campaigns")
            );
        }
    }

    // ── MethodArgumentTypeMismatchException (400) ─────────────────────────────

    @Nested
    @DisplayName("handleTypeMismatch – MethodArgumentTypeMismatchException")
    class HandleTypeMismatch {

        @Test
        @DisplayName("returns 400 for invalid UUID path parameter")
        void handleTypeMismatch_returns400() {
            MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
            when(ex.getName()).thenReturn("id");
            when(ex.getValue()).thenReturn("not-a-uuid");

            ResponseEntity<ApiError> response = handler.handleTypeMismatch(ex, request);

            assertAll("type mismatch",
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().status()).isEqualTo(400),
                    () -> assertThat(response.getBody().message()).contains("not-a-uuid"),
                    () -> assertThat(response.getBody().message()).contains("id")
            );
        }
    }
}
