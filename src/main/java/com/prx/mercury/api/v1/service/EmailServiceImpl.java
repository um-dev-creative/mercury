package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.api.v1.to.TemplateTO;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import com.prx.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.IntFunction;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED;

/**
 * Service implementation for sending emails using JavaMailSender and FreeMarker templates.
 * <p>
 * Implements {@link EmailService}. The class is registered as a Spring {@link Service}.
 * It processes templates via FreeMarker and sends emails using the configured {@link JavaMailSender}.
 * Sent messages are persisted to the NoSQL repository when applicable.
 *
 * @version 1.0.0, 03-05-2022
 * @since 11
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final Configuration freemarkerConfig;
    private final EmailMessageNSRepository emailMessageNSRepository;
    private final JavaMailSender mailSender;

    /**
     * Constructs an EmailServiceImpl with required dependencies.
     *
     * @param freemarkerConfig         The FreeMarker configuration for processing email templates
     * @param emailMessageNSRepository The repository for email message document persistence
     * @param mailSender               The JavaMailSender used to send emails
     */
    public EmailServiceImpl(@Qualifier("getFreeMarkerConfiguration") Configuration freemarkerConfig, EmailMessageNSRepository emailMessageNSRepository, JavaMailSender mailSender
    ) {
        this.freemarkerConfig = freemarkerConfig;
        this.emailMessageNSRepository = emailMessageNSRepository;
        this.mailSender = mailSender;
    }

    /**
     * Retrieves email messages by their delivery status.
     *
     * @param deliveryStatus The delivery status to filter by (OPENED or SENT)
     * @return A list of email message documents with the specified delivery status
     * @throws IllegalArgumentException if delivery status is null
     */
    @Override
    public List<EmailMessageDocument> findByDeliveryStatus(DeliveryStatusType deliveryStatus) {
        if (Objects.isNull(deliveryStatus)) {
            throw new IllegalArgumentException("Delivery status is required");
        }
        if (DeliveryStatusType.OPENED.equals(deliveryStatus) || DeliveryStatusType.SENT.equals(deliveryStatus)) {
            return emailMessageNSRepository.findByDeliveryStatus(deliveryStatus);
        }
        // PENDING - get the rest of the delivery status from the database and return the list
        return Collections.emptyList();
    }

    /**
     * Updates the status of an email message in the database.
     *
     * @param emailMessageDocument The email message document to update
     * @throws IllegalArgumentException if email message document is null
     */
    @Override
    public void updateEmailStatus(EmailMessageDocument emailMessageDocument) {
        if (Objects.isNull(emailMessageDocument)) {
            throw new IllegalArgumentException("Email message document is required");
        }
        emailMessageNSRepository.save(emailMessageDocument);
    }

    /**
     * Deletes an email message document from the database.
     *
     * @param emailMessageDocument The email message document to delete
     */
    @Override
    public void delete(EmailMessageDocument emailMessageDocument) {
        emailMessageNSRepository.delete(emailMessageDocument);
    }

    /**
     * Sends an email based on the provided email message document and template definition.
     * Updates the delivery status based on the result of the sending operation.
     *
     * @param emailMessageDocument The email message document containing email details
     * @param templateDefinedTO    The template definition to use for the email content
     * @return An updated email message document with the new delivery status
     * @throws IllegalArgumentException if template definition is null
     */
    @Override
    public EmailMessageDocument sendEmail(EmailMessageDocument emailMessageDocument, TemplateDefinedTO templateDefinedTO) {
        if (Objects.isNull(templateDefinedTO)) {
            var error = new IllegalArgumentException("Template defined not found");
            logger.error(error.getMessage(), error);
            throw error;
        }
        var result = process(templateDefinedTO.template(), emailMessageDocument.subject(), emailMessageDocument.from(),
                emailMessageDocument.to(), emailMessageDocument.cc(), emailMessageDocument.params());
        return new EmailMessageDocument(
                emailMessageDocument.id(),
                emailMessageDocument.messageId(),
                emailMessageDocument.templateDefinedId(),
                emailMessageDocument.userId(),
                emailMessageDocument.from(),
                emailMessageDocument.to(),
                emailMessageDocument.cc(),
                emailMessageDocument.subject(),
                emailMessageDocument.body(),
                emailMessageDocument.sendDate(),
                emailMessageDocument.params(),
                result ? DeliveryStatusType.SENT : DeliveryStatusType.FAILED);
    }

    /**
     * Processes an email template and sends the email using the configured mail sender.
     *
     * @param templateTO The template object containing the template location
     * @param subject    The email subject
     * @param from       The sender's email address
     * @param to         List of recipient email contacts
     * @param cc         List of CC email contacts
     * @param params     Parameters to be applied to the template
     * @return true if the email was successfully sent, false otherwise
     */
    private boolean process(TemplateTO templateTO, String subject, String from, List<EmailContact> to, List<EmailContact> cc, Map<String, Object> params) {
        final IntFunction<String[]> function = String[]::new;
        final var mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
        boolean isProcessed = false;

        try {
            var body = FreeMarkerTemplateUtils.processTemplateIntoString(
                    freemarkerConfig.getTemplate(templateTO.location()), params);
            final var mimeMessageHelper = new MimeMessageHelper(mimeMessage, MULTIPART_MODE_MIXED_RELATED, UTF_8.name());
            mimeMessageHelper.setText(body, true);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setFrom(from);
            mimeMessageHelper.setTo(to.stream().filter(Objects::nonNull).map(EmailContact::email).toArray(function));
            if (!cc.isEmpty()) {
                var ccList = cc.stream().filter(Objects::nonNull).map(EmailContact::email).toArray(function);
                mimeMessageHelper.setCc(ccList);
            }

            mailSender.send(mimeMessage);
            isProcessed = true;
        } catch (IOException | TemplateException | MessagingException ex) {
            logger.error("Error sending email: {}", ex.getMessage());
        }
        return isProcessed;
    }
}
