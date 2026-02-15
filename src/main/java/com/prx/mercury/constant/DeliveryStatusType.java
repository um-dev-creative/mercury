package com.prx.mercury.constant;

/**
 * Enum for the status of the delivery of a message.
 */
public enum DeliveryStatusType {
    OPENED,
    PENDING,
    IN_PROGRESS,
    SENT,
    DELIVERED,
    REJECTED,
    CANCELED,
    ABORTED,
    FAILED;

    @Override
    public String toString() {
        return this.name();
    }
}
