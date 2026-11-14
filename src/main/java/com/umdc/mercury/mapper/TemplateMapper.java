package com.umdc.mercury.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateTO;
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
    @Mapping(target = "applicationId", source = "application")
    TemplateEntity toSource(TemplateTO templateTO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "templateType", source = "templateType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "application", source = "applicationId")
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
    @Mapping(target = "applicationId", source = "request.applicationId")
    @Mapping(target = "subject", source = "request.subject")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    TemplateEntity toEntity(CreateTemplateRequest request, TemplateTypeEntity templateType,
                             SeverityTypeEntity severityType, LocalDateTime now);

    @Mapping(target = "templateTypeId", source = "templateType.id")
    @Mapping(target = "templateTypeName", source = "templateType.name")
    @Mapping(target = "severityTypeId", source = "severityType.id")
    @Mapping(target = "severityTypeName", source = "severityType.name")
    TemplateDetailResponse toDetailResponse(TemplateEntity templateEntity);

    default TemplateEntity getTemplate(UUID templateId) {
        var templateEntity = new TemplateEntity();
        templateEntity.setId(templateId);
        return templateEntity;
    }


}
