package com.prx.mercury.api.v1.to;

import com.umdc.mercury.api.v1.to.TemplateDetailResponse;
import com.umdc.mercury.api.v1.to.TemplateSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateSearchResponseTest {

    @Test
    @DisplayName("record accessors expose every constructed field")
    void accessorsExposeValues() {
        LocalDateTime now = LocalDateTime.now();
        var item = new TemplateDetailResponse(
                UUID.randomUUID(), "d", "l", "HTML", null, null, null, null, null, true, now, now);
        var response = new TemplateSearchResponse(List.of(item), 0, 20, 1L, 1);

        assertThat(response.items()).containsExactly(item);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("supports an empty result set")
    void emptyResults() {
        var response = new TemplateSearchResponse(List.of(), 0, 20, 0L, 0);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
