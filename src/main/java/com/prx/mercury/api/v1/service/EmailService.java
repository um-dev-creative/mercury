package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.api.v1.to.SendEmailResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}
