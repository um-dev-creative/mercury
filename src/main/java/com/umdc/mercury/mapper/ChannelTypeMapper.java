package com.umdc.mercury.mapper;

import com.umdc.commons.services.config.mapper.MapperAppConfig;
import com.umdc.mercury.api.v1.to.ChannelTypeTO;
import com.umdc.mercury.jpa.sql.entity.ChannelTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = MapperAppConfig.class)
public interface ChannelTypeMapper {

    ChannelTypeTO toChannelTypeTO(ChannelTypeEntity entity);

    ChannelTypeEntity toChannelTypeEntity(ChannelTypeTO to);
}
