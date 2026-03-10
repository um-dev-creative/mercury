package com.prx.mercury.constant;

/**
 * Enumeration representing different types of communication channels.
 * Each channel type has an associated channel name identifier.
 */
public enum ChannelType {
    EMAIL("email", "Email"),
    SMS("sms", "SMS"),
    TELEGRAM("telegram", "Telegram"),
    WHATSAPP("whatsapp", "WhatsApp"),
    PUSH("push", "Push Notification");

    private final String code;
    private final String displayName;

    ChannelType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Obtiene el ChannelType a partir del código.
     *
     * @param code Código del canal (ej: 'email', 'sms')
     * @return ChannelType correspondiente
     * @throws IllegalArgumentException si el código no existe
     */
    public static ChannelType fromCode(String code) {
        for (ChannelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown channel type code: " + code);
    }
}
