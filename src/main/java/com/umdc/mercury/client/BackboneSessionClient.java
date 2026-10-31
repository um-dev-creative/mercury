package com.umdc.mercury.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Backbone-rest's public session-validation endpoint, called without Mercury's own credentials (no
 * outbound interceptor, like {@code BackbonePublicClient}).
 * <p>
 * The token is sent in <strong>both</strong> headers so that the call works against every backbone-rest
 * version: the current one reads {@code session-token} (and keeps the raw {@code Authorization} only as a
 * deprecated fallback, because on its session endpoints {@code Authorization: Bearer} now carries an
 * application client); older ones read only the raw JWT in {@code Authorization} (no {@code Bearer}
 * prefix) and answer {@code 400} without it. Drop the legacy header once no environment runs an old
 * backbone-rest. Both header names are pinned by {@code BackboneSessionClientContractTest}.
 * </p>
 * Answers {@code 200 true} (active), {@code 200 false} (wrong type or revoked) or {@code 401} (expired or
 * malformed, which surfaces as a Feign exception).
 */
@FeignClient(name = "backboneSessionClient", url = "${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}")
public interface BackboneSessionClient {

    /** Header backbone-rest reads the session token from. */
    String SESSION_TOKEN_HEADER = "session-token";

    @GetMapping("/api/v1/session/validate")
    Boolean validate(@RequestHeader(SESSION_TOKEN_HEADER) String sessionToken,
                     @RequestHeader(HttpHeaders.AUTHORIZATION) String legacyAuthorization);

    /** Validates {@code sessionToken}, presenting it the way both current and older backbone-rest versions read it. */
    default Boolean validate(String sessionToken) {
        return validate(sessionToken, sessionToken);
    }
}
