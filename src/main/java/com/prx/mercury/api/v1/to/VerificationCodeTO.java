package com.prx.mercury.api.v1.to;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.prx.commons.util.DateUtil;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationCodeTO(
        UUID id,
        UUID userId,
        UUID applicationId,
        String verificationCode,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime createdAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime modifiedAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime expiredAt,
        @JsonFormat(pattern = DateUtil.PATTERN_DATE_TIME)
        LocalDateTime verifiedAt,
        Boolean isVerified,
        Integer attempts,
        Integer maxAttempts,
        String createdBy,
        String updatedBy,
        UUID messageRecordId
) {

        @Override
        public String toString() {
                return "VerificationCodeTO{" +
                        "id=" + id +
                        ", userId=" + userId +
                        ", applicationId=" + applicationId +
                        ", verificationCode='" + verificationCode + '\'' +
                        ", createdAt=" + createdAt +
                        ", modifiedAt=" + modifiedAt +
                        ", expiredAt=" + expiredAt +
                        ", verifiedAt=" + verifiedAt +
                        ", isVerified=" + isVerified +
                        ", attempts=" + attempts +
                        ", maxAttempts=" + maxAttempts +
                        ", createdBy='" + createdBy + '\'' +
                        ", updatedBy='" + updatedBy + '\'' +
                        ", messageRecordId=" + messageRecordId +
                        '}';
        }
}
