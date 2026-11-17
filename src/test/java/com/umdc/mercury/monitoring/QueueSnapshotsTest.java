package com.umdc.mercury.monitoring;

import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueSnapshots")
class QueueSnapshotsTest {

    @Mock
    private TemplateDefinedRepository definitions;
    @Mock
    private EmailMessageNSRepository emailQueue;
    @Mock
    private MessageNSRepository channelQueue;

    @Test
    @DisplayName("an application without templates has empty queues and MongoDB is not read")
    void noTemplates() {
        var application = UUID.randomUUID();
        when(definitions.findIdsByApplicationId(application)).thenReturn(List.of());

        var snapshot = new QueueSnapshots(definitions, emailQueue, channelQueue, 10).of(application);

        for (ChannelType channel : ChannelType.values()) {
            assertEquals(0, snapshot.waiting(channel));
        }
        verifyNoInteractions(emailQueue, channelQueue);
    }

    @Test
    @DisplayName("the snapshot counts waiting messages, the old ones and the age of the oldest; undated ones never count as old")
    void snapshotQueries() {
        var limit = LocalDateTime.of(2026, 11, 16, 12, 0);
        var snapshot = new QueueSnapshots.QueueSnapshot(java.time.Instant.EPOCH, new java.util.EnumMap<>(java.util.Map.of(
                ChannelType.EMAIL, java.util.Arrays.asList(limit.minusMinutes(5), limit.minusMinutes(30), null, limit.plusMinutes(1)),
                ChannelType.SMS, List.<LocalDateTime>of(), ChannelType.TELEGRAM, List.<LocalDateTime>of(),
                ChannelType.WHATSAPP, List.<LocalDateTime>of(), ChannelType.PUSH, List.<LocalDateTime>of())));

        assertEquals(4, snapshot.waiting(ChannelType.EMAIL));
        assertEquals(2, snapshot.countOlderThan(ChannelType.EMAIL, limit));
        assertEquals(limit.minusMinutes(30), snapshot.oldestOlderThan(ChannelType.EMAIL, limit));
        assertNull(snapshot.oldestOlderThan(ChannelType.SMS, limit));
    }

    @Test
    @DisplayName("a zero or negative cache time never reuses the snapshot")
    void noCache() {
        var application = UUID.randomUUID();
        when(definitions.findIdsByApplicationId(application)).thenReturn(List.of(UUID.randomUUID()));
        when(emailQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of());
        when(channelQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of());
        var snapshots = new QueueSnapshots(definitions, emailQueue, channelQueue, -5);

        snapshots.of(application);
        snapshots.of(application);

        org.mockito.Mockito.verify(emailQueue, org.mockito.Mockito.times(2)).findByDeliveryStatus(DeliveryStatusType.OPENED);
    }

    @Test
    @DisplayName("all applications: every queued message counts, whatever its template, and no template is looked up")
    void everyApplication() {
        var known = UUID.randomUUID();
        when(emailQueue.findByDeliveryStatus(com.umdc.mercury.constant.DeliveryStatusType.OPENED)).thenReturn(List.of(
                new com.umdc.mercury.jpa.nosql.document.EmailMessageDocument("a", UUID.randomUUID(), known, UUID.randomUUID(), "f@e.co", null, null, "s", "b",
                        LocalDateTime.of(2026, 11, 16, 11, 0), java.util.Map.of(), com.umdc.mercury.constant.DeliveryStatusType.OPENED),
                new com.umdc.mercury.jpa.nosql.document.EmailMessageDocument("b", UUID.randomUUID(), null, UUID.randomUUID(), "f@e.co", null, null, "s", "b",
                        null, java.util.Map.of(), com.umdc.mercury.constant.DeliveryStatusType.OPENED)));
        when(channelQueue.findByDeliveryStatus(com.umdc.mercury.constant.DeliveryStatusType.OPENED)).thenReturn(List.of());

        var snapshot = new QueueSnapshots(definitions, emailQueue, channelQueue, 10).of(MonitoringScope.ALL);

        assertEquals(2, snapshot.waiting(ChannelType.EMAIL));
        org.mockito.Mockito.verify(definitions, org.mockito.Mockito.never()).findIdsByApplicationId(org.mockito.ArgumentMatchers.any());
    }
}
