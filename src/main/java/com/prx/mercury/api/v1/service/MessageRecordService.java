package com.prx.mercury.api.v1.service;

import com.prx.mercury.api.v1.to.MessageRecordTO;
import org.springframework.http.HttpStatus;

/**
 * Service interface for handling message records.
 */
public interface MessageRecordService {

    /**
     * Creates a new message record.
     *
     * @param messageRecordTO the message record transfer object
     * @return the created message record transfer object
     * @throws UnsupportedOperationException if the method is not implemented
     */
    default MessageRecordTO create(MessageRecordTO messageRecordTO) {
        throw new UnsupportedOperationException(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase());
    }
}
