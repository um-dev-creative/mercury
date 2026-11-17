package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import com.umdc.mercury.constant.ChannelType;
import com.umdc.mercury.monitoring.SendEventEntity;
import com.umdc.mercury.monitoring.SendEventRepository;
import com.umdc.mercury.monitoring.SendEventSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** The message table and its export: the same filters, newest first, recipients as stored (masked). Every search is scoped to one application. */
final class MessageSearch {

    static final int MAX_PAGE_SIZE = 100;

    private final SendEventRepository events;
    private final int exportMaxRows;

    MessageSearch(SendEventRepository events, int exportMaxRows) {
        this.events = events;
        this.exportMaxRows = Math.max(1, exportMaxRows);
    }

    MonitoringTO.MessagePage messages(UUID applicationId, String channel, String status, String kind, UUID templateDefinedId, LocalDateTime from,
                                      LocalDateTime to, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "occurredAt"));
        var result = events.findAll(specification(applicationId, channel, status, kind, templateDefinedId, from, to), pageable);
        return new MonitoringTO.MessagePage(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getContent().stream().map(MessageSearch::toItem).toList());
    }

    MonitoringTO.MessageExport export(UUID applicationId, String channel, String status, String kind, UUID templateDefinedId, LocalDateTime from,
                                      LocalDateTime to) {
        var result = events.findAll(specification(applicationId, channel, status, kind, templateDefinedId, from, to),
                PageRequest.of(0, exportMaxRows, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new MonitoringTO.MessageExport(result.getContent().stream().map(MessageSearch::toItem).toList(), result.getTotalElements());
    }

    private static org.springframework.data.jpa.domain.Specification<SendEventEntity> specification(UUID applicationId, String channel, String status,
                                                                                                    String kind, UUID templateDefinedId,
                                                                                                    LocalDateTime from, LocalDateTime to) {
        var channelName = channel == null || channel.isBlank() ? null : ChannelType.fromCode(channel.strip()).name();
        var statusName = status == null || status.isBlank() ? null : requireStatus(status);
        var kindName = kind == null || kind.isBlank() ? null : MonitoringServiceImpl.requireKind(kind);
        return SendEventSpecifications.search(applicationId, channelName, statusName, kindName, templateDefinedId, from, to);
    }

    private static MonitoringTO.MessageItem toItem(SendEventEntity e) {
        return new MonitoringTO.MessageItem(e.getId(), e.getOccurredAt(), e.getChannel(), e.getKind(), e.getStatus(), e.getMessageId(),
                e.getCampaignId(), e.getTemplateDefinedId(), e.getRecipientMasked(), e.getErrorCause());
    }

    private static String requireStatus(String status) {
        var value = status.strip().toUpperCase(Locale.ROOT);
        if (!"SENT".equals(value) && !"FAILED".equals(value)) {
            throw new IllegalArgumentException("Unknown status '" + status + "': use SENT or FAILED");
        }
        return value;
    }
}
