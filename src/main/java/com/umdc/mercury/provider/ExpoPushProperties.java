package com.umdc.mercury.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;

/**
 * Expo Push settings bound at {@code umdc.mercury.push.expo.*}.
 *
 * <p>The channel is off by default: queued push messages then stay {@code OPENED} until it is switched on, like Telegram without a bot token.
 * {@code accessToken} is only needed when "enhanced push security" is enabled in the Expo project; it is Vault-owned, empty by default and
 * never reaches a log line or {@link #toString()}.</p>
 *
 * @param enabled             switches the delivery on
 * @param apiBaseUrl          Expo API base ({@code https://exp.host}); the paths {@code /--/api/v2/push/send} and {@code /--/api/v2/push/getReceipts} are appended
 * @param accessToken         optional bearer token (enhanced push security)
 * @param connectTimeout      TCP connect timeout
 * @param readTimeout         wait for the answer of each request
 * @param maxAttempts         HTTP attempts per request on 429, 5xx or a network error (1 to 5)
 * @param retryBackoff        wait before the second HTTP attempt; it doubles on each further attempt
 * @param androidChannelId    Android notification channel the notifications go to
 * @param maxDeliveryAttempts scheduler cycles a message may end in a transient failure before it is {@code FAILED}
 * @param receiptDelay        how long after the send the receipts are read (Expo advises about 15 minutes)
 */
@ConfigurationProperties(prefix = "umdc.mercury.push.expo")
public record ExpoPushProperties(
        boolean enabled,
        String apiBaseUrl,
        String accessToken,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryBackoff,
        String androidChannelId,
        int maxDeliveryAttempts,
        Duration receiptDelay) {

    static final int MAX_HTTP_ATTEMPTS = 5;

    @ConstructorBinding
    public ExpoPushProperties {
        apiBaseUrl = apiBaseUrl == null || apiBaseUrl.isBlank() ? "https://exp.host" : apiBaseUrl.strip().replaceAll("/+$", "");
        accessToken = accessToken == null ? "" : accessToken.strip();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
        maxAttempts = maxAttempts <= 0 ? 3 : Math.min(maxAttempts, MAX_HTTP_ATTEMPTS);
        retryBackoff = retryBackoff == null ? Duration.ofMillis(500) : retryBackoff;
        androidChannelId = androidChannelId == null || androidChannelId.isBlank() ? "default" : androidChannelId.strip();
        maxDeliveryAttempts = maxDeliveryAttempts <= 0 ? 5 : maxDeliveryAttempts;
        receiptDelay = receiptDelay == null ? Duration.ofMinutes(15) : receiptDelay;
    }

    /** Settings for tests and callers that only need a base URL. */
    public ExpoPushProperties(boolean enabled, String apiBaseUrl) {
        this(enabled, apiBaseUrl, null, null, null, 0, null, null, 0, null);
    }

    @Override
    public String toString() {
        return "ExpoPushProperties[enabled=" + enabled + ", apiBaseUrl=" + apiBaseUrl + ", accessToken=" + (accessToken.isEmpty() ? "" : "***")
                + ", maxAttempts=" + maxAttempts + ", maxDeliveryAttempts=" + maxDeliveryAttempts + "]";
    }
}
