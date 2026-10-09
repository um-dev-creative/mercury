package com.umdc.mercury.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Backbone-rest's public session-validation endpoint, called without Mercury's own credentials (no
 * outbound interceptor, like {@code BackbonePublicClient}).
 * <p>
 * Why not {@code security-oauth}'s {@code BackbonePublicClient.validate}: it sends the token in a
 * {@code session-token} header, but backbone-rest's {@code GET /api/v1/session/validate} reads it from
 * {@code Authorization} (the raw JWT, no {@code Bearer} prefix) and answers {@code 400} when that header
 * is missing. Using it would make every Backbone-issued token look invalid. The header name is pinned by
 * {@code BackboneSessionClientContractTest}.
 * </p>
 * Answers {@code 200 true} (active), {@code 200 false} (wrong type or revoked) or {@code 401} (expired or
 * malformed, which surfaces as a Feign exception).
 */
@FeignClient(name = "backboneSessionClient", url = "${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}")
public interface BackboneSessionClient {

    @GetMapping("/api/v1/session/validate")
    Boolean validate(@RequestHeader(HttpHeaders.AUTHORIZATION) String sessionToken);
}
