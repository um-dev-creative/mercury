package com.prx.mercury.kafka.to;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record SmsMessageTO(
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

        @JsonAlias("message")
        @NotNull @NotBlank @NotEmpty
        String message,

        @JsonAlias("sender_id")
        String senderId,

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
        return "SmsMessageTO{" +
                "templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", phoneNumber='" + phoneNumber +
                ", message='" + message +
                ", senderId='" + senderId +
                ", sendDate=" + sendDate +
                ", params=" + params +
                ", campaignId=" + campaignId +
                '}';
    }
}
