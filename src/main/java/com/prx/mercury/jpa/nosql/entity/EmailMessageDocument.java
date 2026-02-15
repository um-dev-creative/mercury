package com.prx.mercury.jpa.nosql.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.constant.DeliveryStatusType;
import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "email_message")
public record EmailMessageDocument(

        @Id
        String id,
        UUID messageId,
        UUID templateDefinedId,
        UUID userId,
        String from,
        List<EmailContact> to,
        List<EmailContact> cc,
        String subject,
        String body,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime sendDate,
        Map<String, Object> params,
        DeliveryStatusType deliveryStatus) {

    @Override
    public String toString() {
        return "MessageValueDTO{" +
                "id=" + id +
                ", templateDefinedId=" + templateDefinedId +
                ", userId=" + userId +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", cc='" + cc + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", sendDate='" + sendDate + '\'' +
                ", params='" + params + '\'' +
                ", deliveryStatus='" + deliveryStatus + '\'' +
                '}';
    }
}
