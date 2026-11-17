package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.security.annotation.SkipSessionValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import static com.umdc.security.constant.ConstantApp.SESSION_TOKEN_KEY;

/**
 * REST API contract of the monitoring dashboard: what was sent, what failed and what waits, per application.
 *
 * <p>All endpoints are rooted at {@code /api/v1/monitoring/applications/{applicationId}}. The caller is a service (the Iris BFF) with the
 * Bearer of its managed client ({@code mercury:message:read}) plus the {@code session-token} of the end user; the application scope is
 * the one the service asks for, and every query is filtered by it. Recipients are always masked.</p>
 */
@Tag(name = "monitoring", description = "Monitoring dashboard API")
@SkipSessionValidation("M2M-only endpoint — gated by ManagedClientSecurityConfig's backbone opaque-token chain instead")
public interface MonitoringApi {

    String CODE_200 = "200";
    String CODE_400 = "400";
    String CODE_401 = "401";
    String MSG_UNAUTHORIZED = "Invalid or missing token.";
    String RANGE_HELP = "Range: 1h, 24h (default), 7d or 30d.";

    @Operation(summary = "Totals per channel and kind", description = "Sent, failed and queued, with the success rate (null when nothing finished). "
            + RANGE_HELP, operationId = "getMonitoringSummary")
    @ApiResponses({@ApiResponse(responseCode = CODE_200, description = "The summary."),
            @ApiResponse(responseCode = CODE_400, description = "Unknown range."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)})
    @GetMapping(value = "/api/v1/monitoring/applications/{applicationId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MonitoringTO.SummaryResponse> summary(@PathVariable UUID applicationId, @RequestParam(required = false) String range,
                                                         @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken);

    @Operation(summary = "Sent and failed over time", description = "One point per minute (1h), hour (24h) or day (7d, 30d); empty buckets are zeros. "
            + "Optional channel (email, sms, telegram, whatsapp, push) and kind (TRANSACTIONAL, CAMPAIGN, VERIFICATION_CODE).",
            operationId = "getMonitoringSeries")
    @ApiResponses({@ApiResponse(responseCode = CODE_200, description = "The series."),
            @ApiResponse(responseCode = CODE_400, description = "Unknown range, channel or kind."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)})
    @GetMapping(value = "/api/v1/monitoring/applications/{applicationId}/series", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MonitoringTO.SeriesResponse> series(@PathVariable UUID applicationId, @ModelAttribute MonitoringTO.SeriesQuery query,
                                                       @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken);

    @Operation(summary = "Failure causes and stuck queues", description = "The most frequent causes of failed messages and the queues that hold "
            + "messages longer than the threshold. " + RANGE_HELP, operationId = "getMonitoringFailures")
    @ApiResponses({@ApiResponse(responseCode = CODE_200, description = "The failures."),
            @ApiResponse(responseCode = CODE_400, description = "Unknown range."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)})
    @GetMapping(value = "/api/v1/monitoring/applications/{applicationId}/failures", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MonitoringTO.FailuresResponse> failures(@PathVariable UUID applicationId, @RequestParam(required = false) String range,
                                                           @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken);

    @Operation(summary = "Verification codes", description = "Generated, confirmed, expired and pending codes and the confirmation rate. " + RANGE_HELP,
            operationId = "getMonitoringVerificationCodes")
    @ApiResponses({@ApiResponse(responseCode = CODE_200, description = "The code statistics."),
            @ApiResponse(responseCode = CODE_400, description = "Unknown range."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)})
    @GetMapping(value = "/api/v1/monitoring/applications/{applicationId}/verification-codes", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MonitoringTO.VerificationCodeStats> verificationCodes(@PathVariable UUID applicationId, @RequestParam(required = false) String range,
                                                                         @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken);

    @Operation(summary = "Message table", description = "Delivery outcomes, newest first, recipients masked, at most 100 per page. Filters: channel, "
            + "status (SENT, FAILED), kind, templateDefinedId, from and to (ISO local date-time, UTC).", operationId = "getMonitoringMessages")
    @ApiResponses({@ApiResponse(responseCode = CODE_200, description = "One page of outcomes."),
            @ApiResponse(responseCode = CODE_400, description = "An invalid filter."),
            @ApiResponse(responseCode = CODE_401, description = MSG_UNAUTHORIZED)})
    @GetMapping(value = "/api/v1/monitoring/applications/{applicationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MonitoringTO.MessagePage> messages(@PathVariable UUID applicationId, @ModelAttribute MonitoringTO.MessageQuery query,
                                                      @RequestHeader(value = SESSION_TOKEN_KEY, required = false) String sessionToken);
}
