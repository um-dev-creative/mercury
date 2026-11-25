package com.umdc.mercury.constant;

/**
 * Why a message ended in {@link DeliveryStatusType#FAILED}. It is a stable, machine-readable code that
 * {@code GET /api/v1/messages/{id}} returns as {@code failureReason}, so the caller can act on it (for example drop a
 * device token) without parsing text. It never carries provider messages, which may quote a recipient address.
 */
public enum DeliveryFailureReason {

    /** Push: the provider says the device token is dead (app uninstalled, token rotated). The caller must stop using that token. */
    DEVICE_NOT_REGISTERED,
    /** Push: the target is not a well-formed Expo push token; nothing was sent. The caller should drop it. */
    INVALID_TOKEN,
    /** The message cannot be delivered as built (for example it has no text). */
    INVALID_MESSAGE,
    /** Push: the payload exceeds the provider limit (4096 bytes at Expo). */
    MESSAGE_TOO_BIG,
    /** Push: the provider kept refusing because too many messages went to one device. */
    MESSAGE_RATE_EXCEEDED,
    /** Push: the FCM sender id of the project does not match the credentials. */
    MISMATCH_SENDER_ID,
    /** Push: the push credentials of the project are invalid or revoked. */
    INVALID_CREDENTIALS,
    /** The provider rejected the request as malformed or for a reason that retrying does not fix. */
    PROVIDER_REJECTED,
    /** The provider stayed unreachable or answered with a server error until the delivery attempts ran out. */
    PROVIDER_UNAVAILABLE,
    /** The provider reported an error mercury does not know. */
    PROVIDER_ERROR
}
