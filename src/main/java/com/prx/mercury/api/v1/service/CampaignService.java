// src/main/java/com/prx/mercury/api/v1/service/ChannelTypeService.java
package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignDetailResponse;
import com.prx.mercury.api.v1.to.CampaignProgressTO;
import com.prx.mercury.api.v1.to.CampaignTO;

import java.util.List;
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

    /**
     * Retrieves a campaign by its unique identifier.
     *
     * @param id the campaign UUID; must not be {@code null}.
     * @return the {@link CampaignDetailResponse} populated from the stored entity.
     * @throws com.prx.mercury.api.v1.exception.CampaignNotFoundException if no campaign with the given id exists.
     */
    CampaignDetailResponse getById(UUID id);

    List<CampaignDetailResponse> getByUserIdAndApplicationId(UUID userId, UUID applicationId);

}
