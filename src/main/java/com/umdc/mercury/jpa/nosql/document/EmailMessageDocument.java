package com.umdc.mercury.jpa.nosql.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.constant.DeliveryStatusType;
import jakarta.persistence.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "messages")
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

    /** No recipients, subject, body or parameters: the processor logs the document and they are personal data. */
    @Override
    public String toString() {
        return "EmailMessage{id=" + id + ", messageId=" + messageId + ", templateDefinedId=" + templateDefinedId
                + ", userId=" + userId + ", recipients=" + (to == null ? 0 : to.size())
                + ", sendDate=" + sendDate + ", deliveryStatus=" + deliveryStatus + '}';
    }
}
