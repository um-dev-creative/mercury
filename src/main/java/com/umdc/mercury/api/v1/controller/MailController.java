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
package com.umdc.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.service.EmailService;
import com.umdc.mercury.security.MachineTokenInterceptor;
import jakarta.validation.Valid;
import com.umdc.mercury.api.v1.to.MailStatusResponse;
import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.api.v1.to.SendEmailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

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

    @Override
    public ResponseEntity<SendEmailResponse> send(@Valid @RequestBody SendEmailRequest requestMail,
                                                  @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                                  @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return emailService.sendMail(Objects.requireNonNull(requestMail), caller, idempotencyKey);
    }

    @Override
    public ResponseEntity<MailStatusResponse> status(@PathVariable("id") UUID id,
                                                     @RequestAttribute(name = MachineTokenInterceptor.CALLER, required = false) String caller) {
        return emailService.mailStatus(id, caller);
    }

}
