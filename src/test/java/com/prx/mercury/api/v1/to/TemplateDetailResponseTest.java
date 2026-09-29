package com.prx.mercury.api.v1.to;

import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateDetailResponseTest {

    @Test
    @DisplayName("record accessors expose every constructed field")
    void accessorsExposeValues() {
        UUID id = UUID.randomUUID();
        UUID templateTypeId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID severityTypeId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        var response = new TemplateDetailResponse(
                id, "desc", "loc", "HTML", templateTypeId, "Marketing",
                applicationId, severityTypeId, "High", true, now, now);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.description()).isEqualTo("desc");
        assertThat(response.location()).isEqualTo("loc");
        assertThat(response.fileFormat()).isEqualTo("HTML");
        assertThat(response.templateTypeId()).isEqualTo(templateTypeId);
        assertThat(response.templateTypeName()).isEqualTo("Marketing");
        assertThat(response.applicationId()).isEqualTo(applicationId);
        assertThat(response.severityTypeId()).isEqualTo(severityTypeId);
        assertThat(response.severityTypeName()).isEqualTo("High");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("equals/hashCode follow record semantics")
    void equalsAndHashCode() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        var r1 = new TemplateDetailResponse(id, "d", "l", "HTML", null, null, null, null, null, false, now, now);
        var r2 = new TemplateDetailResponse(id, "d", "l", "HTML", null, null, null, null, null, false, now, now);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }
}
