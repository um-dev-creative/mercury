package com.umdc.mercury.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.mercury.api.v1.to.SendEmailRequest;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.kafka.to.EmailMessageTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {EmailMessageTO.class, SendEmailRequest.class, EmailMessageDocument.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface EmailMessageMapper {

    EmailMessageTO toEmailMessageTO(SendEmailRequest sendEmailRequest);

    SendEmailRequest toSendEmailRequest(EmailMessageTO emailMessageTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "messageId", ignore = true)
    @Mapping(target = "to", source = "to")
    @Mapping(target = "cc", source = "cc")
    @Mapping(target = "from", source = "from")
    @Mapping(target = "body", source = "body")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "params", source = "params")
    @Mapping(target = "sendDate", source = "sendDate")
    @Mapping(target = "templateDefinedId", source = "templateDefinedId")
    @Mapping(target = "deliveryStatus", expression = "java(com.umdc.mercury.constant.DeliveryStatusType.OPENED)")
    EmailMessageDocument toEmailMessageDocument(EmailMessageTO emailMessageTO);

    @Mapping(target = "to", source = "to")
    @Mapping(target = "cc", source = "cc")
    @Mapping(target = "from", source = "from")
    @Mapping(target = "body", source = "body")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "params", source = "params")
    @Mapping(target = "sendDate", source = "sendDate")
    @Mapping(target = "templateDefinedId", source = "templateDefinedId")
    EmailMessageTO toEmailMessageTO(EmailMessageDocument messageValueDTO);


}
