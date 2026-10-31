package com.umdc.mercury.client;

import com.umdc.mercury.client.interceptor.BackendFeignClientInterceptor;
import com.umdc.security.to.AuthRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for backbone-rest endpoints called on Mercury's own behalf
 * (outbound Bearer attached by {@link BackendFeignClientInterceptor}).
 * <p>
 * Public backbone endpoints (session validate, M2M introspect) live in
 * {@link BackbonePublicClient} instead — see its Javadoc for why.
 * </p>
 */
@FeignClient(name = "backboneClient", url = "${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}", configuration = BackendFeignClientInterceptor.class)
public interface BackboneClient {

    @PostMapping("/api/v1/session")
    String token(@RequestBody AuthRequest authRequest);

}
