package com.umdc.mercury.client;

import com.umdc.mercury.client.to.BackboneApplication;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/// Backbone-rest's list of applications. Backbone reads the session of this resource only from `Authorization: Bearer`, so the
/// caller's own session goes there (the same call Iris makes): there is no outbound interceptor and Mercury's own credentials are
/// not sent. This is how Mercury knows the applications, since it must not read the `general` schema.
@RegisterReflectionForBinding(BackboneApplication.class)
@FeignClient(name = "backboneApplicationClient", url = "${umdc.backbone.base-url:https://api.umdc-qa.tst/backbone}")
public interface BackboneApplicationClient {

    @GetMapping("/api/v1/applications")
    List<BackboneApplication> applications(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearer);
}
