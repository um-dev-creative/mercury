// src/main/java/com/prx/mercury/api/v1/service/ChannelTypeService.java
package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.ChannelTypeTO;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

public interface ChannelTypeService {

    default List<ChannelTypeTO> findAll() {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default List<ChannelTypeTO> findEnabled() {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default ChannelTypeTO findByCode(String code) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default ChannelTypeTO findById(UUID id) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default ChannelTypeTO create(ChannelTypeTO channelTypeTO) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default ChannelTypeTO update(UUID id, ChannelTypeTO channelTypeTO) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }

    default void toggleEnabled(UUID id, boolean enabled) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }
}
