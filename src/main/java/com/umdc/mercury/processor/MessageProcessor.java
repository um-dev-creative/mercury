package com.umdc.mercury.processor;

                import com.umdc.mercury.api.v1.service.*;
                import com.umdc.mercury.api.v1.to.MessageRecordTO;
                import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
                import com.umdc.mercury.api.v1.to.VerificationCodeTO;
                import com.umdc.mercury.constant.ChannelType;
                import com.umdc.mercury.constant.DeliveryStatusType;
                import com.umdc.mercury.monitoring.SendEventService;
                import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
                import com.umdc.mercury.mapper.MessageRecordMapper;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                import org.springframework.beans.factory.annotation.Value;
                import org.springframework.stereotype.Component;
                import org.springframework.transaction.annotation.Transactional;

                import java.util.UUID;

                /**
                 * Processor component responsible for handling email message operations.
                 * This class manages email sending workflows and status updates for messages.
                 */
                @Component
                public class MessageProcessor {

                    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
                    /** What a verification code lived for before the sender could ask for less, and still the ceiling. */
                    public static final java.time.Duration MAX_CODE_LIFETIME = java.time.Duration.ofDays(2);
    private static final String VERIFICATION_CODE_TYPE = "VERIFICATION_CODE";
    private static final int SMTP_FAILURE_MAX = 600;

                    private final EmailService emailService;
                    private final MessageRecordService messageRecordService;
                    private final TemplateDefinedService templateDefinedService;
                    private final MessageRecordMapper messageRecordMapper;
                    private final MessageStatusTypeService messageStatusTypeService;
                    private final VerificationCodeService verificationCodeService;
    private final CampaignMetricsService campaignMetricsService;
    private final SendEventService sendEventService;
    private final org.springframework.transaction.support.TransactionTemplate perMessage;

                    /**
                     * ID of the verification code template, injected from application properties.
                     */
                    @Value("${umdc.verification.code.template.id}")
                    private UUID verificationCodeId;

                    /**
                     * Constructs a MessageProcessor with all required dependencies.
                     *
                     * @param emailService The service handling email operations
                     * @param messageRecordService The service managing message records
                     * @param templateDefinedService The service for email templates
                     * @param messageRecordMapper The mapper for converting between message entities and TOs
                     * @param messageStatusTypeService The service for message status types
                     * @param verificationCodeService The service for verification code operations
                     */
                    public MessageProcessor(EmailService emailService, MessageRecordService messageRecordService,
                                            TemplateDefinedService templateDefinedService, MessageRecordMapper messageRecordMapper,
                                            MessageStatusTypeService messageStatusTypeService, VerificationCodeService verificationCodeService,
                                            CampaignMetricsService campaignMetricsService,
                                            org.springframework.transaction.PlatformTransactionManager transactionManager,
                                            SendEventService sendEventService) {
                        // each message is recorded in its own transaction: one that the database refuses must not undo or block the others
                        this.perMessage = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
                        this.perMessage.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        this.campaignMetricsService = campaignMetricsService;
                        this.sendEventService = sendEventService;
                        this.emailService = emailService;
                        this.messageRecordService = messageRecordService;
                        this.templateDefinedService = templateDefinedService;
                        this.messageRecordMapper = messageRecordMapper;
                        this.messageStatusTypeService = messageStatusTypeService;
                        this.verificationCodeService = verificationCodeService;
                    }

                    /**
                     * Processes email messages that are in OPENED status.
                     * For each email message:
                     * 1. Loads the template definition
                     * 2. Sends the email
                     * 3. Updates the email delivery status
                     *
                     * This method is designed to be called by a scheduler.
                     */
                    @Transactional
                    public void processMessage() {
                        // Load the messages with the OPENED status from mongo db
                        emailService.findByDeliveryStatus(DeliveryStatusType.OPENED)
                                .forEach(emailMessageDocument -> {
                                    if (emailMessageDocument.templateDefinedId() == null) {
                                        logger.error("Skipping OPENED message {}: it has no template definition", emailMessageDocument.id());
                                        return; // one malformed document must not stop the others
                                    }
                                    // Load the template defined for each message
                                    var templateDefinedTO = templateDefinedService.find(emailMessageDocument.templateDefinedId());
                                    logger.debug("Sending email: {}", emailMessageDocument);
                                    // Send the email and update the status
                                    com.umdc.mercury.jpa.nosql.document.EmailMessageDocument sent;
                                    try {
                                        sent = emailService.sendEmail(emailMessageDocument, templateDefinedTO);
                                    } catch (org.springframework.mail.MailException e) {
                                        // the SMTP server timed out / refused the connection: keep the message OPENED for the next run and go on with the others
                                        logger.warn("SMTP failure sending message {}, left OPENED to retry: {}", emailMessageDocument.messageId(), describeSmtpFailure(e));
                                        return;
                                    }
                                    emailService.updateEmailStatus(sent);
                                    // an e-mail of a campaign advances its counters once, when it reaches SENT or FAILED (no-op outside a campaign)
                                    if (sent != null) {
                                        campaignMetricsService.recordOutcome(sent.campaignId(), sent.deliveryStatus());
                                        recordSendEvent(sent, templateDefinedTO);
                                    }
                                    logger.debug("Email sent: {}", emailMessageDocument);
                                    // A verification code must be confirmable as soon as the e-mail leaves, not at the next (5 min) reconciliation
                                    if (sent != null && sent.deliveryStatus() == DeliveryStatusType.SENT && isVerificationCode(templateDefinedTO)) {
                                        reconcileIsolated(sent, templateDefinedTO);
                                    }
                                });
                    }

                    /** Keeps the outcome for the monitoring views (best effort: it never affects the delivery). */
                    private void recordSendEvent(com.umdc.mercury.jpa.nosql.document.EmailMessageDocument sent, TemplateDefinedTO templateDefinedTO) {
                        var first = sent.to() == null || sent.to().isEmpty() || sent.to().getFirst() == null ? null : sent.to().getFirst().email();
                        sendEventService.record(ChannelType.EMAIL, SendEventService.kindOf(isVerificationCode(templateDefinedTO), sent.campaignId()),
                                sent.deliveryStatus(), sent.messageId(), sent.campaignId(), sent.templateDefinedId(), first, SendEventService.CAUSE_SEND_FAILED);
                    }

                    /**
                     * Updates the status of SENT messages and creates the necessary records.
                     * For each message that has been sent:
                     * 1. Retrieves message status type and template information
                     * 2. Creates a message record in the persistent store
                     * 3. If the message is a verification code message, create verification code record
                     * 4. Delete the document from MongoDB after processing
                     * <p>
                     * This method is designed to be called by a scheduler.
                     */
                    @Transactional
                    public void updateMessageStatus() {
                        // Save the message record
                        emailService.findByDeliveryStatus(DeliveryStatusType.SENT)
                                .forEach(emailMessageDocument -> {
                                    logger.debug("Saving message processed: {}", emailMessageDocument);
                                    var templateDefinedTO = findTemplateOrNull(emailMessageDocument);
                                    if (templateDefinedTO == null) {
                                        return; // one orphan document must not stop the reconciliation of the others
                                    }
                                    reconcileIsolated(emailMessageDocument, templateDefinedTO);
                                });
                    }

                    /**
                     * {@link #reconcileSent} in its own transaction, and a failure (a database constraint, a lock) is logged with the message id and
                     * leaves the document {@code SENT} in MongoDB, to be retried by the next reconciliation, without touching the other messages.
                     * The insert is flushed inside, so the error shows here and not later at the commit of the whole run.
                     */
                    private void reconcileIsolated(EmailMessageDocument document, TemplateDefinedTO templateDefinedTO) {
                        try {
                            perMessage.executeWithoutResult(status -> reconcileSent(document, templateDefinedTO));
                        } catch (RuntimeException e) {
                            logger.error("Could not record message {} (it stays SENT in MongoDB and is retried): {}", document.messageId(), rootMessage(e));
                        }
                    }

                    private static String rootMessage(Throwable throwable) {
                        Throwable root = throwable;
                        while (root.getCause() != null && root.getCause() != root) {
                            root = root.getCause();
                        }
                        return root.getClass().getSimpleName() + ": " + root.getMessage();
                    }

                    /** Records a SENT message in PostgreSQL, stores its verification code when it is one, and removes the MongoDB document. */
                    private void reconcileSent(EmailMessageDocument emailMessageDocument, TemplateDefinedTO templateDefinedTO) {
                        // Get message status type
                        var messageStatusTypeTO = messageStatusTypeService.findByName(emailMessageDocument.deliveryStatus().name());
                        // Create the message record
                        logger.debug("Creating message record: {}", emailMessageDocument);
                        var messageRecordTO = messageRecordService.create(messageRecordMapper.toMessageRecordTO(emailMessageDocument, messageStatusTypeTO));
                        logger.debug("Message record created: {}", messageRecordTO);

                        // If a message type equal to verification code, create the verification code record
                        if (isVerificationCode(templateDefinedTO)) {
                            createVerificationCode(emailMessageDocument, templateDefinedTO, messageRecordTO);
                        } else {
                            logger.debug("Message {} is not a verification code (template type {}), no code stored", emailMessageDocument.messageId(),
                                    templateDefinedTO.template().templateType());
                        }

                        // Delete the message from mongo db
                        emailService.delete(emailMessageDocument);
                        logger.debug("Message processed saved and updated: {}", emailMessageDocument);
                    }

                    /**
                     * Explains an SMTP failure: the exception type, every cause down to the root (the SMTP server's own reply such as
                     * {@code 535 5.7.8 Username and Password not accepted} lives there, not in the {@code MailSendException}), and a hint
                     * for the replies Gmail sends most. Never contains the credentials (the SMTP reply does not echo them).
                     */
                    static String describeSmtpFailure(Throwable failure) {
                        var text = new StringBuilder(failure.getClass().getSimpleName());
                        var seen = new java.util.ArrayList<Throwable>();
                        for (Throwable t = failure; t != null && !seen.contains(t) && seen.size() < 8; t = t.getCause()) {
                            seen.add(t);
                            if (t.getMessage() != null && !t.getMessage().isBlank()) {
                                text.append(" | ").append(t == failure ? "" : t.getClass().getSimpleName() + ": ")
                                        .append(t.getMessage().replaceAll("\\s+", " ").trim());
                            } else if (t != failure) {
                                text.append(" | ").append(t.getClass().getSimpleName());
                            }
                        }
                        var full = text.length() > SMTP_FAILURE_MAX ? text.substring(0, SMTP_FAILURE_MAX) + "..." : text.toString();
                        var hint = smtpHint(text.toString());
                        return hint == null ? full : full + " -> " + hint;
                    }

                    private static String smtpHint(String detail) {
                        if (detail.contains("535") || detail.contains("AuthenticationFailedException")) {
                            return "the SMTP login was refused: use an app password (Google account with 2-step verification) or create a new one";
                        }
                        if (detail.contains("5.4.5") || detail.contains("quota")) {
                            return "the daily sending limit of the account was exceeded: wait about 24 h or send fewer messages";
                        }
                        if (detail.contains("421") || detail.contains("454") || detail.contains("4.7.0")) {
                            return "the server is rate limiting this account: retry later and lower the send rate";
                        }
                        if (detail.contains("534") || detail.contains("5.7.9")) {
                            return "the provider blocked the sign-in as suspicious: confirm it from the account's security alert";
                        }
                        if (detail.contains("timed out") || detail.contains("ConnectException") || detail.contains("UnknownHostException")) {
                            return "the SMTP server is unreachable from here: check MAIL_HOST/MAIL_PORT and the network";
                        }
                        return null;
                    }

                    /**
                     * Whether the template is a verification code one: its type is the configured id OR is named {@code VERIFICATION_CODE}. The
                     * name is accepted too so that a deployment whose {@code PRX_VERIFICATION_CODE_TEMPLATE_ID} does not match the type id
                     * in the database still stores the codes (before, the message was recorded, the document deleted and no code kept, silently).
                     */
                    boolean isVerificationCode(TemplateDefinedTO templateDefinedTO) {
                        var type = templateDefinedTO.template().templateType();
                        if (type == null) {
                            return false;
                        }
                        boolean sameId = type.id() != null && type.id().equals(verificationCodeId);
                        boolean sameName = type.name() != null
                                && VERIFICATION_CODE_TYPE.equals(type.name().trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_'));
                        return sameId || sameName;
                    }

                    private void createVerificationCode(EmailMessageDocument document, TemplateDefinedTO templateDefinedTO, MessageRecordTO messageRecordTO) {
                        Object code = document.params() == null ? null : document.params().get("vc");
                        if (code == null || code.toString().isBlank()) {
                            logger.error("Message {} uses a verification code template but carries no 'vc' parameter: no code stored", document.messageId());
                            return;
                        }
                        verificationCodeService.create(getVerificationCodeTO(document, templateDefinedTO, messageRecordTO));
                        logger.info("Verification code stored for user {} (message {})", document.userId(), document.messageId());
                    }

                    /**
                     * The template definition of a SENT document, or {@code null} (logged) when it no longer exists, so a single document
                     * that points to a missing definition is skipped instead of aborting the whole scheduled run on every cycle.
                     */
                    private com.umdc.mercury.api.v1.to.TemplateDefinedTO findTemplateOrNull(EmailMessageDocument document) {
                        if (document.templateDefinedId() == null) {
                            logger.error("Skipping SENT message {}: it has no template definition", document.id());
                            return null;
                        }
                        try {
                            return templateDefinedService.find(document.templateDefinedId());
                        } catch (IllegalArgumentException e) {
                            logger.error("Skipping SENT message {}: its template definition {} was not found", document.id(), document.templateDefinedId());
                            return null;
                        }
                    }

                    /**
                     * Creates a VerificationCodeTO object from email message data.
                     *
                     * @param emailMessageDocument The email message document containing verification code information
                     * @param templateDefinedTO The template definition used for the email
                     * @param messageRecordTO The created message record
                     * @return A new VerificationCodeTO object with data from the email message
                     */
                    /**
                     * How long the code lives: the {@code expiresInMinutes} the sender printed in the e-mail, so what the user is told
                     * is what is enforced, never longer than {@link #MAX_CODE_LIFETIME} (which is also the default when it is absent,
                     * zero, negative or not a number).
                     */
                    public static java.time.Duration expiryOf(java.util.Map<String, ?> params) {
                        Object minutes = params == null ? null : params.get("expiresInMinutes");
                        if (minutes != null) {
                            try {
                                long value = Long.parseLong(minutes.toString().trim());
                                if (value > 0) {
                                    return java.time.Duration.ofMinutes(value).compareTo(MAX_CODE_LIFETIME) < 0
                                            ? java.time.Duration.ofMinutes(value) : MAX_CODE_LIFETIME;
                                }
                            } catch (NumberFormatException _) {
                                // not a number: the default applies
                            }
                        }
                        return MAX_CODE_LIFETIME;
                    }

                    private VerificationCodeTO getVerificationCodeTO(EmailMessageDocument emailMessageDocument,
                                                                     TemplateDefinedTO templateDefinedTO, MessageRecordTO messageRecordTO) {
                        var sentAt = emailMessageDocument.sendDate() != null ? emailMessageDocument.sendDate() : java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
                        return new VerificationCodeTO(null,
                                emailMessageDocument.userId(),
                                templateDefinedTO.applicationId(),
                                emailMessageDocument.params().get("vc").toString(),
                                sentAt,
                                sentAt,
                                sentAt.plus(expiryOf(emailMessageDocument.params())),
                                null,
                                false,
                                0,
                                3,
                                "",
                                "",
                                messageRecordTO.id());
                    }
                }
