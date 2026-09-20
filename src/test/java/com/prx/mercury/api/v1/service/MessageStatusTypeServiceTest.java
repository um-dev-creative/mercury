package com.prx.mercury.api.v1.service;

import com.umdc.mercury.api.v1.service.MessageStatusTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class MessageStatusTypeServiceTest {

    private final MessageStatusTypeService messageStatusTypeService = new MessageStatusTypeService() {
    };

    @Test
    void testFindByName() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> messageStatusTypeService.findByName(null));
        assertEquals(HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(), exception.getMessage());
    }

}
