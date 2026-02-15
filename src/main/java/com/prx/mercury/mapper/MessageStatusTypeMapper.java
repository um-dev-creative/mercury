package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.MessageStatusTypeTO;
import com.prx.mercury.jpa.sql.entity.MessageStatusTypeEntity;
import org.mapstruct.Mapper;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {MessageStatusTypeTO.class, MessageStatusTypeEntity.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface MessageStatusTypeMapper {

    MessageStatusTypeEntity toMessageStatusTypeEntity(MessageStatusTypeTO messageStatusTypeTO);

    MessageStatusTypeTO toMessageStatusTypeTO(MessageStatusTypeEntity messageStatusTypeEntity);
}
