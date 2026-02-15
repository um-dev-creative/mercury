package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A record that represents a request to send an email.
 *
 * @param templateDefinedId The ID of the email template defined.
 * @param userId            The ID of the template usage.
 * @param from              The sender's email address.
 * @param to                The list of recipient email addresses.
 * @param cc                The list of CC email addresses.
 * @param subject           The subject of the email.
 * @param body              The body content of the email.
 * @param sendDate          The date the email was sent.
 * @param params            Additional parameters for the email.
 */
public record SendEmailRequest (
        @NotNull UUID templateDefinedId,
        @NotNull UUID userId,
        @NotNull @NotBlank @NotEmpty String from,
        @NotNull List<EmailContact> to,
        @NotNull List<EmailContact> cc,
        @NotNull @NotBlank @NotEmpty String subject,
        @NotNull @NotBlank @NotEmpty String body,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime sendDate,
        Map<String, Object> params
){

    @Override
    public String toString() {
        return "SendEmailRequest{" +
                "templateId='" + templateDefinedId + '\'' +
                ", userId='" + userId + '\'' +
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
