package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.exception.CampaignNotFoundException;
import com.prx.mercury.api.v1.to.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Global exception handler that maps exceptions thrown by controllers to structured
 * {@link ApiError} responses.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} – bean-validation failures → 400</li>
 *   <li>{@link MethodArgumentTypeMismatchException} – invalid path/query parameter type (e.g. non-UUID) → 400</li>
 *   <li>{@link HttpMessageNotReadableException} – malformed/unreadable request body → 400</li>
 *   <li>{@link IllegalArgumentException} – business-rule argument violations → 400</li>
 *   <li>{@link CompletionException} wrapping {@link IllegalArgumentException} → 400</li>
 *   <li>{@link CompletionException} wrapping {@link IllegalStateException} → 422</li>
 *   <li>{@link IllegalStateException} – disabled channel or other state violations → 422</li>
 *   <li>{@link CampaignNotFoundException} – campaign not found → 404</li>
 *   <li>All other {@link Exception} → 500</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler() {
        // default constructor required by PMD AtLeastOneConstructor rule
    }

    /**
     * Handles bean-validation failures triggered by {@code @Valid} on request bodies.
     * Aggregates all field-level error messages into a single comma-separated string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed for request '{}': {}", request.getRequestURI(), message);
        return ResponseEntity
                .badRequest()
                .body(buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    /**
     * Handles unreadable or malformed request bodies (e.g. invalid JSON or record
     * constructor throwing {@link IllegalArgumentException} during deserialization).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        String message = ex.getMostSpecificCause().getMessage();
        logger.warn("Unreadable request body for '{}': {}", request.getRequestURI(), message);
        return ResponseEntity
                .badRequest()
                .body(buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    /**
     * Handles type-mismatch errors for path/query parameters (e.g. a non-UUID value
     * supplied where a {@link java.util.UUID} is expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        logger.warn("Type mismatch for '{}': {}", request.getRequestURI(), message);
        return ResponseEntity
                .badRequest()
                .body(buildError(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    /**
     * Handles requests for campaigns that do not exist, returning {@code 404 Not Found}.
     */
    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<ApiError> handleCampaignNotFound(CampaignNotFoundException ex,
                                                           HttpServletRequest request) {
        logger.warn("Campaign not found for '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Delegates to the appropriate handler based on the underlying cause type.
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ApiError> handleCompletion(CompletionException ex,
                                                     HttpServletRequest request) {
        Throwable cause = ex.getCause();
        if (cause instanceof IllegalStateException ise) {
            return handleIllegalState(ise, request);
        }
        if (cause instanceof IllegalArgumentException iae) {
            return handleIllegalArgument(iae, request);
        }
        logger.error("Unexpected async failure for '{}': {}", request.getRequestURI(), cause.getMessage(), cause);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, cause.getMessage(), request.getRequestURI()));
    }

    /**
     * Handles argument violations (e.g. unknown template id, empty recipients).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        logger.warn("Bad request for '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(com.prx.mercury.api.v1.exception.ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(com.prx.mercury.api.v1.exception.ForbiddenException ex,
                                                    HttpServletRequest request) {
        logger.warn("Forbidden request for '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Handles state violations such as disabled channel types.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex,
                                                       HttpServletRequest request) {
        logger.warn("Unprocessable request for '{}': {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .unprocessableEntity()
                .body(buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Catch-all handler for any unhandled exception.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error for '{}': {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred", request.getRequestURI()));
    }

    private ApiError buildError(HttpStatus status, String message, String path) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
