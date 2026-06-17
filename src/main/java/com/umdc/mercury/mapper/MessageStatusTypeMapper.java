package com.umdc.mercury.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.mercury.api.v1.to.MessageStatusTypeTO;
import com.umdc.mercury.jpa.sql.entity.MessageStatusTypeEntity;
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
