package com.prx.mercury.api.v1.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateDefinedServiceTest {

    private final TemplateDefinedService templateDefinedService = new TemplateDefinedService() {
    };

    @Test
    void testFindById() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> templateDefinedService.find(null));
        assertEquals(org.springframework.http.HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(), exception.getMessage());
    }

}
