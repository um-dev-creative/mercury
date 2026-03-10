/*
 * @(#)$file.className.java.
 *
 * Copyright (c) Luis Antonio Mata Mata. All rights reserved.
 *
 * All rights to this product are owned by Luis Antonio Mata Mata and may only
 * be used under the terms of its associated license document. You may NOT
 * copy, modify, sublicense, or distribute this source file or portions of
 * it unless previously authorized in writing by Luis Antonio Mata Mata.
 * In any event, this notice and the above copyright must always be included
 * verbatim with this file.
 */
package com.prx.mercury.api.v1.controller;

import com.prx.mercury.api.v1.service.EmailService;
import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.api.v1.to.SendEmailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Mail controller that handles HTTP requests related to sending emails.
 * <p>
 * Provides an endpoint to send an email using the configured {@link EmailService}.
 * Registered as a Spring {@link RestController} with base path {@code api/v1/mail}.
 *
 * @version 1.0.0, 03-05-2022
 * @since 21
 */
@RestController
@RequestMapping("api/v1/mail")
public class MailController implements MailApi {

    private final EmailService emailService;

    /**
     * Constructor for MailController.
     *
     * @param emailService the email service implementation to be used for sending emails
     */
    public MailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Send an email.
     * This method handles POST requests to send an email. It consumes and produces JSON.
     * The request body is expected to be a {@link SendEmailRequest} containing email details.
     *
     * @param requestMail the request document containing the email details
     * @return a {@link ResponseEntity} with the result of the email sending operation
     */
    @Override
    public ResponseEntity<SendEmailResponse> send(@RequestBody SendEmailRequest requestMail) {
        return emailService.sendMail(Objects.requireNonNull(requestMail));
    }

}
