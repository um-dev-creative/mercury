package com.umdc.mercury.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateTO;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.entity.SeverityTypeEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateEntity;
import com.umdc.mercury.jpa.sql.entity.TemplateTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
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
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "templateType", source = "templateType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "application", source = "application.id")
    TemplateTO toTemplateTO(TemplateEntity templateEntity);

    /**
     * Builds a new, always-active {@link TemplateEntity} from a create request and its
     * already-resolved, already-validated references.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "location", source = "request.location")
    @Mapping(target = "fileFormat", source = "request.fileFormat")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    TemplateEntity toEntity(CreateTemplateRequest request, TemplateTypeEntity templateType,
                             ApplicationEntity application, SeverityTypeEntity severityType, LocalDateTime now);

    @Mapping(target = "templateTypeId", source = "templateType.id")
    @Mapping(target = "templateTypeName", source = "templateType.name")
    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "severityTypeId", source = "severityType.id")
    @Mapping(target = "severityTypeName", source = "severityType.name")
    TemplateDetailResponse toDetailResponse(TemplateEntity templateEntity);

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
