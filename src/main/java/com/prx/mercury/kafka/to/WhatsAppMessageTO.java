package com.prx.mercury.kafka.to;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transfer Object para mensajes de WhatsApp enviados a través de Kafka.
 */
public record WhatsAppMessageTO(
        @JsonAlias("template_defined_id")
        @NotNull @NotBlank @NotEmpty
        UUID templateDefinedId,

        @JsonAlias("user_id")
        @NotNull
        UUID userId,

        @JsonAlias("phone_number")
        @NotNull @NotBlank @NotEmpty
        @Pattern(regexp = "^\\\\+[1-9]\\\\d{1,14}$", message = "Phone number must be in E.164 format")
        String phoneNumber,

        @JsonAlias("message_type")
        @NotNull @NotBlank
        String messageType, // 'text', 'template', 'media', 'interactive'

        @JsonAlias("content")
        @NotNull @NotBlank @NotEmpty
        String content,

        @JsonAlias("template_name")
        String templateName,

        @JsonAlias("template_language")
        String templateLanguage,

        @JsonAlias("template_components")
        List<WhatsAppTemplateComponent> templateComponents,

        @JsonAlias("media_url")
        String mediaUrl,

        @JsonAlias("media_type")
        String mediaType, // 'image', 'video', 'audio', 'document'

        @JsonAlias("send_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime sendDate,

        @JsonAlias("params")
        Map<String, Object> params,

        @JsonAlias("campaign_id")
        UUID campaignId
) {
    @Override
    public String toString() {
        return "WhatsAppMessageTO{" +
                "templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", phoneNumber='" + phoneNumber +
                ", messageType='" + messageType +
                ", content='" + content +
                ", templateName='" + templateName +
                ", templateLanguage='" + templateLanguage +
                ", templateComponents=" + templateComponents +
                ", mediaUrl='" + mediaUrl +
                ", mediaType='" + mediaType +
                ", sendDate=" + sendDate +
                ", params=" + params +
                ", campaignId=" + campaignId +
                '}';
    }

    /**
     * Record para componentes de plantilla de WhatsApp
     */
    public record WhatsAppTemplateComponent(
            String type,        // 'header', 'body', 'footer', 'button'
            String subType,     // 'quick_reply', 'url', etc.
            Integer index,      // Posición del componente
            List<WhatsAppParameter> parameters
    ) {
    }

    /**
     * Record para parámetros de plantilla de WhatsApp
     */
    public record WhatsAppParameter(
            String type,  // 'text', 'currency', 'date_time', 'image', 'video', 'document'
            String text,
            String currency,
            String currencyCode,
            Long amount,
            String dateTime,
            String fallbackValue,
            WhatsAppMedia media
    ) {
    }

    /**
     * Record para archivos multimedia en plantillas de WhatsApp
     */
    public record WhatsAppMedia(
            String link,
            String id,
            String caption,
            String filename
    ) {
    }
}
