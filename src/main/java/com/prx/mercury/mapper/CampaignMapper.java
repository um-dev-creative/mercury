package com.prx.mercury.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.mercury.api.v1.to.CampaignTO;
import com.prx.mercury.api.v1.to.ChannelTypeTO;
import com.prx.mercury.jpa.sql.entity.CampaignEntity;
import com.prx.mercury.jpa.sql.entity.ChannelTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = MapperAppConfig.class, uses = {ChannelTypeMapper.class})
public interface CampaignMapper {

    CampaignTO toCampaignTO(CampaignEntity entity);

    ChannelTypeTO toChannelTypeTO(ChannelTypeEntity entity);
}
