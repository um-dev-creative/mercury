package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.TemplateTypeTO;
import com.prx.mercury.jpa.sql.entity.TemplateTypeEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {TemplateTypeTO.class, TemplateTypeEntity.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface TemplateTypeMapper {

    @InheritInverseConfiguration
    TemplateTypeEntity toSource(TemplateTypeTO templateTypeTO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "isActive", source = "active")
    TemplateTypeTO toTemplateTypeTO(TemplateTypeEntity templateTypeEntity);

}
