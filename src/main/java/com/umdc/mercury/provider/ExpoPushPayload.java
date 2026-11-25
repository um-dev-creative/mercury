package com.umdc.mercury.provider;

import com.umdc.mercury.constant.DeliveryFailureReason;
import com.umdc.mercury.jpa.nosql.document.PushNotificationMessageDocument;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What mercury sends to Expo for one message and how it reads Expo's error codes (fields and codes as in
 * https://docs.expo.dev/push-notifications/sending-notifications/).
 */
final class ExpoPushPayload {

    /** Ticket and receipt error that means "slow down": transient, not a verdict on the message. */
    static final String RATE_EXCEEDED = "MessageRateExceeded";

    /** Expo tokens are {@code ExponentPushToken[...]} (older) or {@code ExpoPushToken[...]}. */
    private static final Pattern EXPO_TOKEN = Pattern.compile("^Expo(nent)?PushToken\\[[^\\]\\s]+]$");
    private static final Set<String> PRIORITIES = Set.of("default", "normal", "high");

    private ExpoPushPayload() {
        // utility class
    }

    /** Whether {@code token} has the shape of an Expo push token. */
    static boolean isExpoToken(String token) {
        return Objects.nonNull(token) && EXPO_TOKEN.matcher(token.strip()).matches();
    }

    /** Whether the message has text to show (Expo would accept a silent one, but a CHISPA notice without text is a bug). */
    static boolean hasText(PushNotificationMessageDocument message) {
        return Objects.nonNull(message.getBody()) && !message.getBody().isBlank();
    }

    /** The Expo message of {@code message}: {@code to, title, body, data, sound, badge, priority, channelId}. */
    static Map<String, Object> of(PushNotificationMessageDocument message, String androidChannelId) {
        final var body = new LinkedHashMap<String, Object>();
        body.put("to", message.getDeviceToken().strip());
        if (Objects.nonNull(message.getTitle()) && !message.getTitle().isBlank()) {
            body.put("title", message.getTitle());
        }
        body.put("body", message.getBody());
        if (Objects.nonNull(message.getData()) && !message.getData().isEmpty()) {
            body.put("data", message.getData());
        }
        body.put("sound", Objects.requireNonNullElse(message.getSound(), "default"));
        if (Objects.nonNull(message.getBadge())) {
            body.put("badge", message.getBadge());
        }
        body.put("priority", Objects.nonNull(message.getPriority()) && PRIORITIES.contains(message.getPriority()) ? message.getPriority() : "default");
        body.put("channelId", androidChannelId);
        return body;
    }

    /** The reason code of an Expo {@code details.error}. */
    static DeliveryFailureReason reasonOf(String expoError) {
        return switch (Objects.requireNonNullElse(expoError, "")) {
            case "DeviceNotRegistered" -> DeliveryFailureReason.DEVICE_NOT_REGISTERED;
            case "MessageTooBig" -> DeliveryFailureReason.MESSAGE_TOO_BIG;
            case RATE_EXCEEDED -> DeliveryFailureReason.MESSAGE_RATE_EXCEEDED;
            case "MismatchSenderId" -> DeliveryFailureReason.MISMATCH_SENDER_ID;
            case "InvalidCredentials" -> DeliveryFailureReason.INVALID_CREDENTIALS;
            default -> DeliveryFailureReason.PROVIDER_ERROR;
        };
    }
}
