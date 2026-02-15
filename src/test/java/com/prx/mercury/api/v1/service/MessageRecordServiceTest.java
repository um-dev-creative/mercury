package com.prx.mercury.api.v1.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class MessageRecordServiceTest {

    private final MessageRecordService messageRecordService = new MessageRecordService() {
    };

    @Test
    void testCreate() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> messageRecordService.create(null));
        assertEquals(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(), exception.getMessage());
    }
}
