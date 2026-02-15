package com.prx.mercury.constant;

import com.prx.commons.constants.httpstatus.type.MessageType;

/**
 * Standardized Mercury message codes and statuses used with {@link MessageType}.
 */
public enum MercuryMessage implements MessageType {
    /**
     * Returned when a requested operation is not implemented (HTTP 501).
     */
    METHOD_NOT_IMPLEMENTED(501, "Method not implemented");

    /**
     * Numeric status code.
     */
    private final int code;
    /**
     * Human-readable status text.
     */
    private final String status;

    /**
     * Creates a message with the given status code and text.
     *
     * @param code numeric status code
     * @param status human-readable status text
     */
    MercuryMessage(int code, String status) {
        this.code = code;
        this.status = status;
    }

    /**
     * Returns the numeric status code.
     *
     * @return status code
     */
    @Override
    public int getCode() {
        return code;
    }

    /**
     * Returns the numeric status code as a string.
     *
     * @return status code as a string
     */
    @Override
    public String getCodeToString() {
        return String.valueOf(this.getCode());
    }

    /**
     * Returns the human-readable status text.
     *
     * @return status text
     */
    @Override
    public String getStatus() {
        return status;
    }
}
