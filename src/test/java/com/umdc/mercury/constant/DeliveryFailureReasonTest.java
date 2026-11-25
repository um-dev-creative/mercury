package com.umdc.mercury.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DeliveryFailureReason")
class DeliveryFailureReasonTest {

    @Test
    @DisplayName("the codes are a stable contract: the OpenAPI enum lists exactly them")
    void matchesOpenApi() throws java.io.IOException {
        String yaml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/api/openapi.yaml"));
        for (var reason : DeliveryFailureReason.values()) {
            assertTrue(yaml.contains(reason.name()), reason + " is missing from openapi.yaml");
        }
        assertEquals(10, DeliveryFailureReason.values().length);
        assertEquals(DeliveryFailureReason.DEVICE_NOT_REGISTERED, DeliveryFailureReason.valueOf("DEVICE_NOT_REGISTERED"));
    }
}
