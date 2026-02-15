package com.prx.mercury.processor;

                import com.prx.mercury.api.v1.service.*;
                import com.prx.mercury.api.v1.to.MessageRecordTO;
                import com.prx.mercury.api.v1.to.TemplateDefinedTO;
                import com.prx.mercury.api.v1.to.VerificationCodeTO;
                import com.prx.mercury.constant.DeliveryStatusType;
                import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
                import com.prx.mercury.mapper.MessageRecordMapper;
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

                    private final EmailService emailService;
                    private final MessageRecordService messageRecordService;
                    private final TemplateDefinedService templateDefinedService;
                    private final MessageRecordMapper messageRecordMapper;
                    private final MessageStatusTypeService messageStatusTypeService;
                    private final VerificationCodeService verificationCodeService;

                    /**
                     * ID of the verification code template, injected from application properties.
                     */
                    @Value("${prx.verification.code.template.id}")
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
                                            MessageStatusTypeService messageStatusTypeService, VerificationCodeService verificationCodeService) {
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
                                    // Load the template defined for each message
                                    var templateDefinedTO = templateDefinedService.find(emailMessageDocument.templateDefinedId());
                                    logger.debug("Sending email: {}", emailMessageDocument);
                                    // Send the email and update the status
                                    emailService.updateEmailStatus(emailService.sendEmail(emailMessageDocument, templateDefinedTO));
                                    logger.debug("Email sent: {}", emailMessageDocument);
                                });
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
                                    // Get message status type
                                    var messageStatusTypeTO = messageStatusTypeService.findByName(emailMessageDocument.deliveryStatus().name());
                                    var templateDefinedTO = templateDefinedService.find(emailMessageDocument.templateDefinedId());
                                    // Create the message record
                                    logger.debug("Creating message record: {}", emailMessageDocument);
                                    var messageRecordTO = messageRecordService.create(messageRecordMapper.toMessageRecordTO(emailMessageDocument, messageStatusTypeTO));
                                    logger.debug("Message record created: {}", messageRecordTO);

                                    // If a message type equal to verification code, create the verification code record
                                    if (templateDefinedTO.template().templateType().id().equals(verificationCodeId)) {
                                        // Create the verification code record
                                        logger.debug("Creating verification code: {}", emailMessageDocument);
                                        verificationCodeService.create(getVerificationCodeTO(emailMessageDocument, templateDefinedTO, messageRecordTO));
                                        logger.debug("Verification code created: {}", emailMessageDocument);
                                    }

                                    // Delete the message from mongo db
                                    emailService.delete(emailMessageDocument);
                                    logger.debug("Message processed saved and updated: {}", emailMessageDocument);
                                });
                    }

                    /**
                     * Creates a VerificationCodeTO object from email message data.
                     *
                     * @param emailMessageDocument The email message document containing verification code information
                     * @param templateDefinedTO The template definition used for the email
                     * @param messageRecordTO The created message record
                     * @return A new VerificationCodeTO object with data from the email message
                     */
                    private VerificationCodeTO getVerificationCodeTO(EmailMessageDocument emailMessageDocument,
                                                                     TemplateDefinedTO templateDefinedTO, MessageRecordTO messageRecordTO) {
                        return new VerificationCodeTO(null,
                                emailMessageDocument.userId(),
                                templateDefinedTO.applicationId(),
                                emailMessageDocument.params().get("vc").toString(),
                                emailMessageDocument.sendDate(),
                                emailMessageDocument.sendDate(),
                                emailMessageDocument.sendDate().plusDays(2),
                                null,
                                false,
                                0,
                                3,
                                "",
                                "",
                                messageRecordTO.id());
                    }
                }
