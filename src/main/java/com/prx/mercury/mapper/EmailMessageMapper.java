package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import com.prx.mercury.kafka.to.EmailMessageTO;
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
    @Mapping(target = "to", source = "to")
    @Mapping(target = "cc", source = "cc")
    @Mapping(target = "from", source = "from")
    @Mapping(target = "body", source = "body")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "params", source = "params")
    @Mapping(target = "sendDate", source = "sendDate")
    @Mapping(target = "templateDefinedId", source = "templateDefinedId")
    @Mapping(target = "deliveryStatus", expression = "java(com.prx.mercury.constant.DeliveryStatusType.OPENED)")
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
