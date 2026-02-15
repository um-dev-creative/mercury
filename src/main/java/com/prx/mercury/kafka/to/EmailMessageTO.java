package com.prx.mercury.kafka.to;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.mercury.api.v1.to.EmailContact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EmailMessageTO (
        @JsonAlias("template_defined_id")
        @NotNull @NotBlank @NotEmpty UUID templateDefinedId,
        @JsonAlias("user_id")
        @NotNull UUID userId,
        @JsonAlias("from")
        @NotNull @NotBlank @NotEmpty String from,
        @JsonAlias("to")
        @NotNull List<EmailContact> to,
        @JsonAlias("cc")
        @NotNull List<EmailContact> cc,
        @JsonAlias("subject")
        @NotNull @NotBlank @NotEmpty String subject,
        @JsonAlias("body")
        @NotNull @NotBlank @NotEmpty String body,
        @JsonAlias("send_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime sendDate,
        @JsonAlias("params")
        Map<String, Object> params
){

        @Override
        public String toString() {
                return "EmailMessageTO{" +
                        "templateDefinedId=" + templateDefinedId +
                        ", userId=" + userId +
                        ", from='" + from + '\'' +
                        ", to=" + to +
                        ", cc=" + cc +
                        ", subject='" + subject + '\'' +
                        ", body='" + body + '\'' +
                        ", sendDate=" + sendDate +
                        ", params=" + params +
                        '}';
        }
}
