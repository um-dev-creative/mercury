package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.TemplateTO;
import com.prx.mercury.jpa.sql.entity.ApplicationEntity;
import com.prx.mercury.jpa.sql.entity.TemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {TemplateTO.class, TemplateEntity.class, TemplateTypeMapper.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface TemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    @Mapping(target = "templateType", source = "templateType")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "application", expression = "java(getApplication(templateTO.application()))")
    TemplateEntity toSource(TemplateTO templateTO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "isActive", expression = "java(true)")
    @Mapping(target = "templateType", source = "templateType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "application", source = "application.id")
    TemplateTO toTemplateTO(TemplateEntity templateEntity);

    default TemplateEntity getTemplate(UUID templateId) {
        var templateEntity = new TemplateEntity();
        templateEntity.setId(templateId);
        return templateEntity;
    }

    default ApplicationEntity getApplication(UUID applicationId) {
        var applicationEntity = new ApplicationEntity();
        applicationEntity.setId(applicationId);
        return applicationEntity;
    }

}
