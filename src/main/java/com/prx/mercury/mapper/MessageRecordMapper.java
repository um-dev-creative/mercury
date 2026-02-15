package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.EmailContact;
import com.prx.mercury.api.v1.to.MessageRecordTO;
import com.prx.mercury.api.v1.to.MessageStatusTypeTO;
import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import com.prx.mercury.jpa.sql.entity.MessageRecordEntity;
import com.prx.mercury.jpa.sql.entity.MessageStatusTypeEntity;
import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {EmailMessageDocument.class, MessageRecordTO.class, MessageRecordEntity.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface MessageRecordMapper {

    @Mapping(target = "sender", source = "from")
    @Mapping(target = "to", expression = "java(listToString(messageRecordTO.to()))")
    @Mapping(target = "cc", expression = "java(listToString(messageRecordTO.cc()))")
    @Mapping(target = "templateDefined", expression = "java(toTemplateDefinedEntity(messageRecordTO))")
    @Mapping(target = "messageStatusType", expression = "java(toMessageStatusTypeEntity(messageRecordTO))")
    MessageRecordEntity toMessageRecordEntity(MessageRecordTO messageRecordTO);


    @Mapping(target = "from", source = "sender")
    @Mapping(target = "to", ignore = true)
    @Mapping(target = "cc", ignore = true)
    @Mapping(target = "templateDefinedId", source = "templateDefined.id")
    @Mapping(target = "messageStatusTypeId", source = "messageStatusType.id")
    MessageRecordTO toMessageRecordTO(MessageRecordEntity result);

    @Mapping(target = "id", source = "emailMessageDocument.messageId")
    @Mapping(target = "sender", source = "emailMessageDocument.from")
    @Mapping(target = "content", source = "emailMessageDocument.body")
    @Mapping(target = "subject", source = "emailMessageDocument.subject")
    @Mapping(target = "createdAt", source = "emailMessageDocument.sendDate")
    @Mapping(target = "updatedAt", source = "emailMessageDocument.sendDate")
    @Mapping(target = "to", expression = "java(listToString(emailMessageDocument.to()))")
    @Mapping(target = "cc", expression = "java(listToString(emailMessageDocument.cc()))")
    @Mapping(target = "templateDefined", expression = "java(toTemplateDefinedEntity(templateDefinedTO))")
    @Mapping(target = "messageStatusType", expression = "java(toMessageStatusTypeEntity(messageStatusTypeTO))")
    MessageRecordEntity toMessageRecordEntity(EmailMessageDocument emailMessageDocument,
                                              MessageStatusTypeTO messageStatusTypeTO,
                                              TemplateDefinedTO templateDefinedTO
    );

    @Mapping(target = "id", source = "emailMessageDocument.messageId")
    @Mapping(target = "from", source = "emailMessageDocument.from")
    @Mapping(target = "content", source = "emailMessageDocument.body")
    @Mapping(target = "subject", source = "emailMessageDocument.subject")
    @Mapping(target = "createdAt", source = "emailMessageDocument.sendDate")
    @Mapping(target = "updatedAt", source = "emailMessageDocument.sendDate")
    @Mapping(target = "to", source = "emailMessageDocument.to")
    @Mapping(target = "cc", source = "emailMessageDocument.cc")
    @Mapping(target = "templateDefinedId", source = "emailMessageDocument.templateDefinedId")
    @Mapping(target = "messageStatusTypeId", source = "messageStatusTypeTO.id")
    MessageRecordTO toMessageRecordTO(EmailMessageDocument emailMessageDocument,
                                              MessageStatusTypeTO messageStatusTypeTO
    );

    default String listToString(List<EmailContact> list) {
        final IntFunction<String[]> function = String[]::new;
        String sb ="";
        if (list != null) {
            sb = Arrays.toString(list.stream().filter(Objects::nonNull).map(EmailContact::email).toArray(function));
        }
        return sb;
    }

    default TemplateDefinedEntity toTemplateDefinedEntity(TemplateDefinedTO templateDefinedTO) {
        TemplateDefinedEntity templateDefinedEntity = new TemplateDefinedEntity();
        templateDefinedEntity.setId(templateDefinedTO.id());

        return templateDefinedEntity;
    }

    default TemplateDefinedEntity toTemplateDefinedEntity(MessageRecordTO messageRecordTO) {
        TemplateDefinedEntity templateDefinedEntity = new TemplateDefinedEntity();
        templateDefinedEntity.setId(messageRecordTO.templateDefinedId());

        return templateDefinedEntity;
    }

    default MessageStatusTypeEntity toMessageStatusTypeEntity(MessageRecordTO messageRecordTO) {
        MessageStatusTypeEntity messageStatusTypeEntity = new MessageStatusTypeEntity();
        messageStatusTypeEntity.setId(messageRecordTO.messageStatusTypeId());

        return messageStatusTypeEntity;
    }

    default MessageStatusTypeEntity toMessageStatusTypeEntity(MessageStatusTypeTO messageStatusTypeTO) {
        MessageStatusTypeEntity messageStatusTypeEntity = new MessageStatusTypeEntity();
        messageStatusTypeEntity.setId(messageStatusTypeTO.id());

        return messageStatusTypeEntity;
    }

}
