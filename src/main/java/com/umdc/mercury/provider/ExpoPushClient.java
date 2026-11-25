package com.umdc.mercury.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client of the Expo Push API ({@code POST /--/api/v2/push/send} and {@code POST /--/api/v2/push/getReceipts}, contrasted with
 * https://docs.expo.dev/push-notifications/sending-notifications/).
 *
 * <p>Contract it honours: a request body is a JSON array of at most {@value #MAX_MESSAGES} messages (the caller chunks) and a receipts request at
 * most {@value #MAX_RECEIPTS} ids; the answer is HTTP 200 with {@code data}, or a 4xx/5xx with a top-level {@code errors} array. Timeouts are set on
 * every request. A 429, a 5xx or a network error is retried up to {@code maxAttempts} times with a doubling backoff, and then reported as
 * {@link ExpoPushResult.Unavailable}; other 4xx are not retried. Message texts, tokens and the provider's error messages are never logged: only
 * status codes, counts and exception classes.</p>
 */
@Component
@EnableConfigurationProperties(ExpoPushProperties.class)
public class ExpoPushClient {

    /** Most messages Expo accepts in one send request. */
    public static final int MAX_MESSAGES = 100;
    /** Most receipt ids Expo accepts in one getReceipts request. */
    public static final int MAX_RECEIPTS = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(ExpoPushClient.class);
    private static final String SEND_PATH = "/--/api/v2/push/send";
    private static final String RECEIPTS_PATH = "/--/api/v2/push/getReceipts";
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int SERVER_ERROR = 500;
    private static final int OK = 200;

    private final ExpoPushProperties properties;
    private final RestClient client;

    public ExpoPushClient(ExpoPushProperties properties) {
        this.properties = properties;
        final var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
        factory.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Sends {@code messages} (at most {@value #MAX_MESSAGES}).
     *
     * @return {@link ExpoPushResult.Tickets} with one ticket per message, or the reason it could not be sent
     */
    public ExpoPushResult send(List<Map<String, Object>> messages) {
        if (messages.size() > MAX_MESSAGES) {
            throw new IllegalArgumentException("Expo accepts at most " + MAX_MESSAGES + " messages per request");
        }
        return post(SEND_PATH, messages, false);
    }

    /**
     * Reads the receipts of {@code ids} (at most {@value #MAX_RECEIPTS}).
     *
     * @return {@link ExpoPushResult.Receipts} (ids without a receipt are absent), or the reason it could not be read
     */
    public ExpoPushResult receipts(List<String> ids) {
        if (ids.size() > MAX_RECEIPTS) {
            throw new IllegalArgumentException("Expo accepts at most " + MAX_RECEIPTS + " receipt ids per request");
        }
        return post(RECEIPTS_PATH, Map.of("ids", ids), true);
    }

    private ExpoPushResult post(String path, Object body, boolean receipts) {
        long backoff = properties.retryBackoff().toMillis();
        String last = "no attempt";
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            final var outcome = attemptOnce(path, body, receipts);
            if (!(outcome instanceof ExpoPushResult.Unavailable unavailable)) {
                return outcome;
            }
            last = unavailable.reason();
            if (attempt < properties.maxAttempts()) {
                LOG.debug("Expo {} attempt {} failed ({}); retrying", path, attempt, last);
                if (!pause(backoff)) {
                    return new ExpoPushResult.Unavailable("interrupted");
                }
                backoff *= 2;
            }
        }
        LOG.warn("Expo {} unavailable after {} attempt(s): {}", path, properties.maxAttempts(), last);
        return new ExpoPushResult.Unavailable(last);
    }

    private static boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private ExpoPushResult attemptOnce(String path, Object body, boolean receipts) {
        try {
            return client.post()
                    .uri(properties.apiBaseUrl() + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(this::authorize)
                    .body(body)
                    .exchange((request, response) -> interpret(response.getStatusCode().value(), read(response), receipts));
        } catch (RuntimeException e) {
            // an HTTP library's message can name the URL or quote the body: the class only
            return new ExpoPushResult.Unavailable(e.getClass().getSimpleName());
        }
    }

    private void authorize(HttpHeaders headers) {
        if (!properties.accessToken().isEmpty()) {
            headers.setBearerAuth(properties.accessToken());
        }
    }

    private static Map<?, ?> read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        try {
            return response.bodyTo(new ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    static ExpoPushResult interpret(int status, Map<?, ?> json, boolean receipts) {
        final Map<?, ?> body = Objects.isNull(json) ? Map.of() : json;
        if (status == OK) {
            return receipts ? receiptsOf(body.get("data")) : ticketsOf(body.get("data"));
        }
        if (status == UNAUTHORIZED || status == FORBIDDEN) {
            return new ExpoPushResult.Unauthorized(status);
        }
        if (status == TOO_MANY_REQUESTS || status >= SERVER_ERROR) {
            return new ExpoPushResult.Unavailable("status " + status);
        }
        return new ExpoPushResult.Rejected(status, firstErrorCode(body));
    }

    private static ExpoPushResult ticketsOf(Object data) {
        if (!(data instanceof List<?> list)) {
            return new ExpoPushResult.Unavailable("malformed answer");
        }
        final var tickets = new ArrayList<ExpoPushResult.Ticket>(list.size());
        for (Object item : list) {
            tickets.add(ticket(item));
        }
        return new ExpoPushResult.Tickets(List.copyOf(tickets));
    }

    private static ExpoPushResult receiptsOf(Object data) {
        if (!(data instanceof Map<?, ?> map)) {
            return new ExpoPushResult.Unavailable("malformed answer");
        }
        final var byId = new LinkedHashMap<String, ExpoPushResult.Ticket>();
        map.forEach((id, receipt) -> byId.put(String.valueOf(id), ticket(receipt)));
        return new ExpoPushResult.Receipts(Map.copyOf(byId));
    }

    private static ExpoPushResult.Ticket ticket(Object item) {
        if (!(item instanceof Map<?, ?> map)) {
            return new ExpoPushResult.Ticket(false, null, "Malformed");
        }
        final boolean ok = "ok".equals(map.get("status"));
        final String id = map.get("id") instanceof String s ? s : null;
        final String error = !ok && map.get("details") instanceof Map<?, ?> details && details.get("error") instanceof String s ? s : null;
        return new ExpoPushResult.Ticket(ok, id, ok ? null : Objects.requireNonNullElse(error, "Unknown"));
    }

    private static String firstErrorCode(Map<?, ?> body) {
        if (body.get("errors") instanceof List<?> errors && !errors.isEmpty()
                && errors.getFirst() instanceof Map<?, ?> first && first.get("code") instanceof String code) {
            return code;
        }
        return "";
    }
}
