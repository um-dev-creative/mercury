package com.umdc.mercury.api.v1.to;

import com.umdc.mercury.constant.DeliveryFailureReason;
import com.umdc.mercury.constant.DeliveryStatusType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("MailStatusResponse")
class MailStatusResponseTest {

    @Test
    @DisplayName("the e-mail constructor leaves the failure reason empty; the push one carries it")
    void failureReason() {
        var id = UUID.randomUUID();
        var now = Instant.now();

        assertNull(new MailStatusResponse(id, DeliveryStatusType.SENT, now, now).failureReason());
        assertEquals(DeliveryFailureReason.DEVICE_NOT_REGISTERED,
                new MailStatusResponse(id, DeliveryStatusType.FAILED, now, now, DeliveryFailureReason.DEVICE_NOT_REGISTERED).failureReason());
    }
}
