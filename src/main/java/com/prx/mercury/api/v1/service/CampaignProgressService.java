package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.CampaignProgressTO;

import java.util.UUID;

public interface CampaignProgressService {

    CampaignProgressTO getProgress(UUID campaignId);

}

