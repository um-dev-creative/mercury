package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.TemplateDefinedTO;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public interface TemplateDefinedService {

    default TemplateDefinedTO find(UUID templateDefinedId) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }
}
