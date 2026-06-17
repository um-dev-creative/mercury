package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.api.v1.to.SendEmailResponse;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * EmailService.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 05-05-2022
 * @since 11
 */
public interface EmailService {

    /**
     * Send an email.
     *
     * @param mail the email request containing the email details
     * @return a ResponseEntity containing the email response and HTTP status
     */
    default ResponseEntity<SendEmailResponse> sendMail(SendEmailRequest mail) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default EmailMessageDocument sendEmail(EmailMessageDocument emailMessageDocument, TemplateDefinedTO templateDefinedTO) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default void updateEmailStatus(EmailMessageDocument emailMessageDocument) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default List<EmailMessageDocument> findByDeliveryStatus(DeliveryStatusType deliveryStatus) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default void delete(EmailMessageDocument emailMessageDocument) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }
}
