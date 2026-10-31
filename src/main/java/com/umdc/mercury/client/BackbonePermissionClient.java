package com.umdc.mercury.client;

import com.umdc.mercury.client.interceptor.BackendFeignClientInterceptor;
import com.umdc.mercury.client.to.PermissionCheckRequest;
import com.umdc.mercury.client.to.PermissionCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for backbone-rest's IAM permission-check endpoint, called on Mercury's
 * own behalf (outbound M2M Bearer attached by {@link BackendFeignClientInterceptor},
 * same as {@link BackboneClient}).
 * <p>
 * This is the authoritative, per-application answer to "can this caller manage this
 * application" — backed by the existing {@code general.application_role_user} ACL in
 * backbone-rest, as opposed to trusting a locally-cached or token-embedded claim.
 * </p>
 */
@FeignClient(name = "backbonePermissionClient", url = "${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}", configuration = BackendFeignClientInterceptor.class)
public interface BackbonePermissionClient {

    @PostMapping("/api/v1/iam/permissions/check")
    PermissionCheckResponse check(@RequestBody PermissionCheckRequest request);

}
