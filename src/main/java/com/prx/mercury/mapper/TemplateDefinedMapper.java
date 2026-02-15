package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.SendEmailRequest;
import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.FrequencyTypeEntity;
import com.prx.mercury.jpa.sql.entity.TemplateEntity;
import com.prx.mercury.jpa.sql.entity.TemplateDefinedEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {TemplateDefinedEntity.class, SendEmailRequest.class, TemplateMapper.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface TemplateDefinedMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expiredAt", ignore = true)
    @Mapping(target = "isActive", expression ="java(true)")
    @Mapping(target = "userId", source = "sendEmailRequest.userId")
    @Mapping(target = "frequencyType", source = "frequencyTypeId")
    @Mapping(target = "template", expression ="java(getTemplate(sendEmailRequest))")
    @Mapping(target = "application", expression ="java(getApplication(applicationId))")
    @Mapping(target = "createdAt", expression ="java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression ="java(java.time.LocalDateTime.now())")
    TemplateDefinedEntity toSource(SendEmailRequest sendEmailRequest, UUID applicationId,
                                 UUID messageStatusTypeId, UUID frequencyTypeId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expiredAt", ignore = true)
    @Mapping(target = "application", ignore = true)
    @Mapping(target = "userId", source = "mail.userId")
    @Mapping(target = "isActive", expression = "java(true)")
    @Mapping(target = "template", expression = "java(getTemplate(mail))")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "frequencyType", expression = "java(getFrequencyType(frequencyTypeId))")
    TemplateDefinedEntity toTemplateUsageEntity(SendEmailRequest mail, UUID messageStatusTypeId, UUID frequencyTypeId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "template", source = "template")
    @Mapping(target = "expiredAt", source = "expiredAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "frequencyTypeId", source = "frequencyType.id")
    TemplateDefinedTO toTemplateDefinedTO(TemplateDefinedEntity templateDefinedEntity);

    default FrequencyTypeEntity getFrequencyType(UUID frequencyTypeId) {
        FrequencyTypeEntity frequencyTypeEntity = new FrequencyTypeEntity();
        frequencyTypeEntity.setId(frequencyTypeId);
        return frequencyTypeEntity;
    }

    default TemplateEntity getTemplate(SendEmailRequest sendEmailRequest) {
        var templateEntity = new TemplateEntity();
        templateEntity.setId(sendEmailRequest.templateDefinedId());
        return templateEntity;
    }

    default ApplicationEntity getApplication(UUID applicationId) {
        var applicationEntity = new ApplicationEntity();
        applicationEntity.setId(applicationId);
        return applicationEntity;
    }
}
