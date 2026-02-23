package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.ChannelTypeTO;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = MapperAppConfig.class)
public interface ChannelTypeMapper {

    ChannelTypeTO toChannelTypeTO(ChannelTypeEntity entity);

    ChannelTypeEntity toChannelTypeEntity(ChannelTypeTO to);
}
