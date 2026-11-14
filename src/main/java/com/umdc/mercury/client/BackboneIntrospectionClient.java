package com.umdc.mercury.client;

import com.umdc.mercury.client.to.TokenIntrospectionRequest;
import com.umdc.mercury.client.to.TokenIntrospectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for backbone-rest's public M2M introspection endpoint. Same endpoint as the shared library's
 * {@code BackbonePublicClient.introspect}, but answering with {@link TokenIntrospectionResponse}, which carries the application the
 * managed client belongs to. Deliberately has no outbound-auth {@code configuration}: the endpoint is public.
 */
@FeignClient(name = "backboneIntrospectionClient", url = "${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}")
public interface BackboneIntrospectionClient {

    @PostMapping("/api/v1/managed-clients/introspect")
    TokenIntrospectionResponse introspect(@RequestBody TokenIntrospectionRequest request);

}
