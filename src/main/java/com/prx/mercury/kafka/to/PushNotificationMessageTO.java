package com.prx.mercury.kafka.to;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record PushNotificationMessageTO(
        @JsonAlias("template_defined_id")
        @NotNull @NotBlank @NotEmpty
        UUID templateDefinedId,

        @JsonAlias("user_id")
        @NotNull
        UUID userId,

        @JsonAlias("device_token")
        @NotNull @NotBlank @NotEmpty
        String deviceToken,

        @JsonAlias("platform")
        @NotNull @NotBlank
        String platform, // 'android', 'ios', 'web'

        @JsonAlias("title")
        @NotNull @NotBlank @NotEmpty
        String title,

        @JsonAlias("body")
        @NotNull @NotBlank @NotEmpty
        String body,

        @JsonAlias("icon")
        String icon,

        @JsonAlias("image")
        String image,

        @JsonAlias("click_action")
        String clickAction,

        @JsonAlias("priority")
        String priority, // 'high', 'normal'

        @JsonAlias("badge")
        Integer badge,

        @JsonAlias("sound")
        String sound,

        @JsonAlias("send_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime sendDate,

        @JsonAlias("params")
        Map<String, Object> params,

        @JsonAlias("campaign_id")
        UUID campaignId
) {
    // Compact constructor para valores por defecto
    public PushNotificationMessageTO {
        if (priority == null || priority.isBlank()) {
            priority = "normal";
        }
    }

    @Override
    public String toString() {
        return "PushNotificationMessageTO{" +
                "templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", deviceToken='[HIDDEN]'" + // Ocultar token por seguridad
                ", platform='" + platform +
                ", title='" + title +
                ", body='" + body +
                ", icon='" + icon +
                ", image='" + image +
                ", clickAction='" + clickAction +
                ", priority='" + priority +
                ", badge=" + badge +
                ", sound='" + sound +
                ", sendDate=" + sendDate +
                ", params=" + params +
                ", campaignId=" + campaignId +
                '}';
    }
}
