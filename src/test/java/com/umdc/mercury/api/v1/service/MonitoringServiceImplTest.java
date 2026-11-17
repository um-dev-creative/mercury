package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.jpa.nosql.document.SmsMessageDocument;
import com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository;
import com.umdc.mercury.jpa.nosql.repository.MessageNSRepository;
import com.umdc.mercury.jpa.sql.repository.TemplateDefinedRepository;
import com.umdc.mercury.jpa.sql.repository.VerificationCodeRepository;
import com.umdc.mercury.monitoring.QueueSnapshots;
import com.umdc.mercury.monitoring.SendEventEntity;
import com.umdc.mercury.monitoring.SendEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringServiceImpl")
class MonitoringServiceImplTest {

    private static final Instant START = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);

    /** A clock the test can move forward. */
    private static final class MovableClock extends Clock {
        private Instant instant = START;

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private record Outcome(String channel, String kind, String status, long total) implements SendEventRepository.OutcomeCount {
        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public String getKind() {
            return kind;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }

    private record Cause(String cause, String channel, long total) implements SendEventRepository.CauseCount {
        @Override
        public String getCause() {
            return cause;
        }

        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }

    private record Bucket(LocalDateTime bucket, String status, long total) implements SendEventRepository.BucketCount {
        @Override
        public LocalDateTime getBucket() {
            return bucket;
        }

        @Override
        public String getStatus() {
            return status;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }

    @Mock
    private SendEventRepository events;
    @Mock
    private VerificationCodeRepository codes;
    @Mock
    private TemplateDefinedRepository definitions;
    @Mock
    private EmailMessageNSRepository emailQueue;
    @Mock
    private MessageNSRepository channelQueue;

    private final UUID application = UUID.randomUUID();
    private final UUID template = UUID.randomUUID();
    private MovableClock clock;
    private MonitoringServiceImpl service;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        service = new MonitoringServiceImpl(events, codes, new QueueSnapshots(definitions, emailQueue, channelQueue, 10, clock), 15, clock);
        lenient().when(definitions.findIdsByApplicationId(application)).thenReturn(List.of(template));
        lenient().when(emailQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of());
        lenient().when(channelQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of());
    }

    private EmailMessageDocument email(UUID templateId, LocalDateTime sendDate) {
        return new EmailMessageDocument("id", UUID.randomUUID(), templateId, UUID.randomUUID(), "f@e.co", null, null, "s", "b", sendDate, Map.of(),
                DeliveryStatusType.OPENED);
    }

    private SmsMessageDocument sms(UUID templateId, LocalDateTime sendDate) {
        var document = new SmsMessageDocument();
        document.setChannelType(ChannelType.SMS);
        document.setTemplateDefinedId(templateId);
        document.setSendDate(sendDate);
        return document;
    }

    @Test
    @DisplayName("summary adds the outcomes per channel and kind, and the live queues, with the success rates")
    void summaryAggregates() {
        when(events.countOutcomes(eq(application), any())).thenReturn(List.of(
                new Outcome("EMAIL", "CAMPAIGN", "SENT", 8), new Outcome("EMAIL", "CAMPAIGN", "FAILED", 2),
                new Outcome("TELEGRAM", "TRANSACTIONAL", "SENT", 5), new Outcome("EMAIL", "VERIFICATION_CODE", "SENT", 3),
                new Outcome("GHOST", "TRANSACTIONAL", "SENT", 100)));
        when(emailQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(email(template, NOW), email(template, NOW)));
        when(channelQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(sms(template, NOW)));

        var summary = service.summary(application, "24h");

        assertEquals("24h", summary.range());
        assertEquals(NOW, summary.generatedAt());
        assertEquals(16, summary.totals().sent(), "the unknown channel does not count");
        assertEquals(2, summary.totals().failed());
        assertEquals(3, summary.totals().queued());
        assertEquals(16.0 / 18.0, summary.totals().successRate());
        assertEquals(List.of("EMAIL", "SMS", "TELEGRAM", "WHATSAPP", "PUSH"), summary.channels().stream().map(MonitoringTO.ChannelSummary::channel).toList());
        var email = summary.channels().getFirst();
        assertEquals(11, email.sent());
        assertEquals(2, email.failed());
        assertEquals(2, email.queued());
        assertTrue(email.providerConnected());
        var sms = summary.channels().get(1);
        assertEquals(1, sms.queued());
        assertFalse(sms.providerConnected(), "SMS has no provider: its messages only queue");
        assertNull(sms.successRate(), "nothing finished, no rate invented");
        assertEquals(List.of("TRANSACTIONAL", "CAMPAIGN", "VERIFICATION_CODE"), summary.kinds().stream().map(MonitoringTO.KindSummary::kind).toList());
        assertEquals(8, summary.kinds().get(1).sent());
        assertEquals(2, summary.kinds().get(1).failed());
        assertEquals(0.8, summary.kinds().get(1).successRate());
    }

    @Test
    @DisplayName("with no data every count is zero and no rate is invented, and the queues are not read without templates")
    void summaryEmpty() {
        when(definitions.findIdsByApplicationId(application)).thenReturn(List.of());
        when(events.countOutcomes(eq(application), any())).thenReturn(List.of());

        var summary = service.summary(application, null);

        assertEquals("24h", summary.range());
        assertEquals(0, summary.totals().sent());
        assertNull(summary.totals().successRate());
        assertTrue(summary.channels().stream().allMatch(c -> c.sent() == 0 && c.queued() == 0 && c.successRate() == null));
        verifyNoInteractions(emailQueue, channelQueue);
    }

    @Test
    @DisplayName("only the queued messages of the application's own templates are counted")
    void queuesAreScopedToTheApplication() {
        when(events.countOutcomes(eq(application), any())).thenReturn(List.of());
        var foreign = UUID.randomUUID();
        var noChannel = sms(template, NOW);
        noChannel.setChannelType(null);
        when(emailQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(email(template, NOW), email(foreign, NOW)));
        when(channelQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(sms(foreign, NOW), noChannel));

        var summary = service.summary(application, "1h");

        assertEquals(1, summary.totals().queued());
    }

    @Test
    @DisplayName("the queues are read once for a few seconds and again after the cache expires")
    void queuesAreCached() {
        when(events.countOutcomes(eq(application), any())).thenReturn(List.of());

        service.summary(application, "24h");
        clock.advanceSeconds(5);
        service.summary(application, "24h");
        verify(emailQueue, times(1)).findByDeliveryStatus(DeliveryStatusType.OPENED);

        clock.advanceSeconds(6);
        service.summary(application, "24h");
        verify(emailQueue, times(2)).findByDeliveryStatus(DeliveryStatusType.OPENED);
    }

    @Test
    @DisplayName("series returns every bucket of the range, empty ones as zeros, and passes the channel and kind to the query")
    void seriesFillsBuckets() {
        var bucket = LocalDateTime.of(2026, 11, 16, 11, 30);
        when(events.countByBucket(eq(application), eq(NOW.minusHours(1)), eq("minute"), eq("EMAIL"), eq("VERIFICATION_CODE")))
                .thenReturn(List.of(new Bucket(bucket, "SENT", 4), new Bucket(bucket, "FAILED", 1)));

        var series = service.series(application, "1h", "email", "verification_code");

        assertEquals("minute", series.unit());
        assertEquals(61, series.points().size(), "11:00 to 12:00 inclusive, a point per minute");
        var point = series.points().stream().filter(p -> p.bucket().equals(bucket)).findFirst().orElseThrow();
        assertEquals(4, point.sent());
        assertEquals(1, point.failed());
        assertEquals(0, series.points().getFirst().sent());
        assertEquals(NOW.minusHours(1), series.points().getFirst().bucket());
    }

    @Test
    @DisplayName("without a channel or kind the query gets nulls; 24h draws per hour and 30d per day")
    void seriesOptionalFilters() {
        when(events.countByBucket(eq(application), any(), any(), isNull(), isNull())).thenReturn(List.of());

        assertEquals(25, service.series(application, "24h", null, " ").points().size());
        assertEquals(31, service.series(application, "30d", "", null).points().size());
        assertEquals("day", service.series(application, "7d", null, null).unit());
    }

    @Test
    @DisplayName("series refuses an unknown channel, kind or range")
    void seriesValidates() {
        assertThrows(IllegalArgumentException.class, () -> service.series(application, "24h", "fax", null));
        assertThrows(IllegalArgumentException.class, () -> service.series(application, "24h", null, "SPAM"));
        assertThrows(IllegalArgumentException.class, () -> service.series(application, "2h", null, null));
    }

    @Test
    @DisplayName("failures lists the top causes and the queues older than the threshold, with the oldest age")
    void failuresCausesAndStuck() {
        when(events.topCauses(eq(application), any(), any())).thenReturn(List.of(new Cause("SEND_FAILED", "EMAIL", 7), new Cause("DELIVERY_FAILED", "TELEGRAM", 2)));
        when(emailQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(
                email(template, NOW.minusMinutes(20)), email(template, NOW.minusMinutes(40)), email(template, NOW.minusMinutes(5)), email(template, null)));
        when(channelQueue.findByDeliveryStatus(DeliveryStatusType.OPENED)).thenReturn(List.of(sms(template, NOW.minusHours(3))));

        var failures = service.failures(application, "7d");

        assertEquals("7d", failures.range());
        assertEquals(15, failures.stuckAfterMinutes());
        assertEquals(List.of("SEND_FAILED", "DELIVERY_FAILED"), failures.causes().stream().map(MonitoringTO.FailureCause::cause).toList());
        assertEquals(2, failures.stuck().size());
        var email = failures.stuck().getFirst();
        assertEquals("EMAIL", email.channel());
        assertEquals(2, email.waiting(), "the 5-minute-old and the undated ones are not stuck");
        assertEquals(40, email.oldestMinutes());
        assertTrue(email.providerConnected());
        var sms = failures.stuck().get(1);
        assertEquals("SMS", sms.channel());
        assertEquals(180, sms.oldestMinutes());
        assertFalse(sms.providerConnected());
    }

    @Test
    @DisplayName("nothing stuck and no failures give empty lists")
    void failuresEmpty() {
        when(events.topCauses(eq(application), any(), any())).thenReturn(List.of());

        var failures = service.failures(application, null);

        assertTrue(failures.causes().isEmpty());
        assertTrue(failures.stuck().isEmpty());
    }

    @Test
    @DisplayName("verification codes: generated, confirmed, expired, pending, failed sends and the confirmation rate")
    void verificationCodes() {
        var since = NOW.minusHours(24);
        when(codes.countByApplicationIdAndCreatedAtGreaterThanEqual(application, since)).thenReturn(10L);
        when(codes.countByApplicationIdAndCreatedAtGreaterThanEqualAndIsVerifiedTrue(application, since)).thenReturn(6L);
        when(codes.countByApplicationIdAndCreatedAtGreaterThanEqualAndIsVerifiedFalseAndExpiresAtBefore(application, since, NOW)).thenReturn(3L);
        when(events.countOutcomes(application, since)).thenReturn(List.of(new Outcome("EMAIL", "VERIFICATION_CODE", "FAILED", 2),
                new Outcome("EMAIL", "VERIFICATION_CODE", "SENT", 9), new Outcome("EMAIL", "CAMPAIGN", "FAILED", 50)));

        var stats = service.verificationCodes(application, "24h");

        assertEquals(10, stats.generated());
        assertEquals(6, stats.confirmed());
        assertEquals(3, stats.expired());
        assertEquals(1, stats.pending());
        assertEquals(2, stats.failedSends(), "only the failed verification codes");
        assertEquals(0.6, stats.confirmationRate());
    }

    @Test
    @DisplayName("with no codes the confirmation rate is null and pending never goes negative")
    void verificationCodesNone() {
        when(codes.countByApplicationIdAndCreatedAtGreaterThanEqual(eq(application), any())).thenReturn(0L);
        when(codes.countByApplicationIdAndCreatedAtGreaterThanEqualAndIsVerifiedTrue(eq(application), any())).thenReturn(1L);
        when(codes.countByApplicationIdAndCreatedAtGreaterThanEqualAndIsVerifiedFalseAndExpiresAtBefore(eq(application), any(), any())).thenReturn(1L);
        when(events.countOutcomes(eq(application), any())).thenReturn(List.of());

        var stats = service.verificationCodes(application, "1h");

        assertNull(stats.confirmationRate());
        assertEquals(0, stats.pending());
    }

    private SendEventEntity entity(String status) {
        var event = new SendEventEntity();
        event.setId(UUID.randomUUID());
        event.setOccurredAt(NOW);
        event.setChannel("EMAIL");
        event.setKind("CAMPAIGN");
        event.setStatus(status);
        event.setRecipientMasked("j***@e***.com");
        event.setErrorCause("SEND_FAILED");
        return event;
    }

    @SuppressWarnings("unchecked")
    private Pageable searchedPage(String channel, String status, String kind, int page, int size) {
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call -> new PageImpl<>(List.of(entity("FAILED")), call.getArgument(1), 41));
        service.messages(application, channel, status, kind, null, null, null, page, size);
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(events).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("messages returns the page with the masked recipient as stored")
    void messagesMapsItems() {
        var stored = entity("FAILED");
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call -> new PageImpl<>(List.of(stored), call.getArgument(1), 41));

        var page = service.messages(application, "email", "failed", "campaign", UUID.randomUUID(), NOW.minusDays(1), NOW, 1, 20);

        assertEquals(1, page.page());
        assertEquals(20, page.size());
        assertEquals(41, page.total());
        var item = page.items().getFirst();
        assertEquals(stored.getId(), item.id());
        assertEquals("j***@e***.com", item.recipient());
        assertEquals("SEND_FAILED", item.errorCause());
        assertEquals("FAILED", item.status());
    }

    @Test
    @DisplayName("the page size is clamped to 1..100 and the page to 0 or more, newest first")
    void messagesClampsPaging() {
        var pageable = searchedPage(null, null, null, -3, 1000);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals("occurredAt: DESC", pageable.getSort().toString());
    }

    @Test
    @DisplayName("a page size of zero becomes one")
    void messagesMinimumSize() {
        assertEquals(1, searchedPage(" ", " ", " ", 0, 0).getPageSize());
    }

    @Test
    @DisplayName("messages refuses an unknown channel, status or kind")
    void messagesValidates() {
        assertThrows(IllegalArgumentException.class, () -> service.messages(application, "fax", null, null, null, null, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.messages(application, null, "DONE", null, null, null, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.messages(application, null, null, "SPAM", null, null, null, 0, 20));
    }

    @Test
    @DisplayName("the production constructor reads the system clock")
    void productionConstructor() {
        var production = new MonitoringServiceImpl(events, codes, new QueueSnapshots(definitions, emailQueue, channelQueue, 10), 15, 10_000);
        when(events.countOutcomes(eq(application), any())).thenReturn(List.of());

        var generated = production.summary(application, "1h").generatedAt();

        assertTrue(generated.isAfter(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
    }

    @Test
    @DisplayName("export returns the newest rows up to the configured maximum and the total that matched")
    @SuppressWarnings("unchecked")
    void exportCapsTheRows() {
        var capped = new MonitoringServiceImpl(events, codes, new QueueSnapshots(definitions, emailQueue, channelQueue, 10, clock), 15, clock, 500);
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call -> new PageImpl<>(List.of(entity("FAILED")), call.getArgument(1), 900));

        var export = capped.export(application, "email", "failed", "campaign", null, NOW.minusDays(1), NOW);

        assertEquals(1, export.items().size());
        assertEquals(900, export.total());
        org.junit.jupiter.api.Assertions.assertTrue(export.truncated());
        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(events).findAll(any(Specification.class), captor.capture());
        assertEquals(500, captor.getValue().getPageSize());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("occurredAt").getDirection());
        assertEquals("j***@e***.com", export.items().getFirst().recipient());
    }

    @Test
    @DisplayName("export of everything that fits is not truncated; a bad channel, status or kind is rejected")
    @SuppressWarnings("unchecked")
    void exportValidates() {
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(call -> new PageImpl<>(List.of(entity("SENT")), call.getArgument(1), 1));

        org.junit.jupiter.api.Assertions.assertFalse(service.export(application, null, null, null, null, null, null).truncated());
        assertThrows(IllegalArgumentException.class, () -> service.export(application, "fax", null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.export(application, null, "weird", null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.export(application, null, null, "weird", null, null, null));
    }
}
