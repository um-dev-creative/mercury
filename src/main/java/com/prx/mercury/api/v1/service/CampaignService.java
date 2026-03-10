// src/main/java/com/prx/mercury/api/v1/service/ChannelTypeService.java
package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CampaignTO;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CampaignService {

    String PARSE_MODE_KEY = "parseMode";
    String SENDER_ID_KEY = "senderId";
    String MESSAGE_KEY = "message";
    String SUBJECT_KEY = "subject";
    String BODY_KEY = "body";
    String FROM_KEY = "from";

    CompletableFuture<CampaignProgressTO> createCampaign(CampaignTO request);

    CampaignProgressTO getProgress(UUID campaignId);

}
