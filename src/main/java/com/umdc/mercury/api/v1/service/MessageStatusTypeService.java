package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MessageStatusTypeTO;
import org.springframework.http.HttpStatus;

public interface MessageStatusTypeService {

    default MessageStatusTypeTO findByName(String messageStatusTypeName) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }
}
