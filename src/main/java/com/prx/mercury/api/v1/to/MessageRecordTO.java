package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageRecordTO(
        UUID id,
        UUID templateDefinedId,
        String from,
        String content,
        String subject,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime updatedAt,
        UUID messageStatusTypeId,
        List<EmailContact> to,
        List<EmailContact> cc
) {

        @Override
        public String toString() {
                return "MessageRecordTO{" +
                        "id=" + id +
                        ", templateDefinedId=" + templateDefinedId +
                        ", from='" + from + '\'' +
                        ", content='" + content + '\'' +
                        ", subject='" + subject + '\'' +
                        ", createdAt=" + createdAt +
                        ", updatedAt=" + updatedAt +
                        ", messageStatusTypeId=" + messageStatusTypeId +
                        ", to=" + to +
                        ", cc=" + cc +
                        '}';
        }
}
