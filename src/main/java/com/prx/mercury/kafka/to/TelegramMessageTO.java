package com.prx.mercury.kafka.to;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Transfer Object para mensajes de Telegram enviados a través de Kafka.
 */
public record TelegramMessageTO(
        @JsonAlias("template_defined_id")
        @NotNull @NotBlank @NotEmpty
        UUID templateDefinedId,

        @JsonAlias("user_id")
        @NotNull
        UUID userId,

        @JsonAlias("chat_id")
        @NotNull
        Long chatId,

        @JsonAlias("message")
        @NotNull @NotBlank @NotEmpty
        String message,

        @JsonAlias("parse_mode")
        String parseMode, // "HTML", "Markdown", "MarkdownV2"

        @JsonAlias("send_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime sendDate,

        @JsonAlias("params")
        Map<String, Object> params,

        @JsonAlias("campaign_id")
        UUID campaignId,

        @JsonAlias("reply_to_message_id")
        Long replyToMessageId,

        @JsonAlias("disable_notification")
        Boolean disableNotification
) {
    // Compact constructor para valores por defecto
    public TelegramMessageTO {
        if (parseMode == null || parseMode.isBlank()) {
            parseMode = "HTML";
        }
        if (disableNotification == null) {
            disableNotification = false;
        }
    }

    @Override
    public String toString() {
        return "TelegramMessageTO{" +
                "templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", chatId=" + chatId +
                ", message='" + message +
                ", parseMode='" + parseMode +
                ", sendDate=" + sendDate +
                ", params=" + params +
                ", campaignId=" + campaignId +
                ", replyToMessageId=" + replyToMessageId +
                ", disableNotification=" + disableNotification +
                '}';
    }
}
