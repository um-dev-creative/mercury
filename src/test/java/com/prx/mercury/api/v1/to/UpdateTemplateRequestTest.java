package com.prx.mercury.api.v1.to;

import com.umdc.mercury.api.v1.to.UpdateTemplateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateTemplateRequest bean validation")
class UpdateTemplateRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    @Test
    @DisplayName("an all-null request (no-op update) has no violations")
    void allNull_noViolations() {
        var request = new UpdateTemplateRequest(null, null, null, null, null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("a fully populated request has no violations")
    void fullyPopulated_noViolations() {
        var request = new UpdateTemplateRequest("desc", "loc", "HTML", UUID.randomUUID(), UUID.randomUUID());
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("rejects location over the max length")
    void oversizedLocation_isRejected() {
        var request = new UpdateTemplateRequest(null, "x".repeat(501), null, null, null);
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("location"));
    }

    @Test
    @DisplayName("rejects fileFormat over the max length")
    void oversizedFileFormat_isRejected() {
        var request = new UpdateTemplateRequest(null, null, "x".repeat(51), null, null);
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("fileFormat"));
    }

    @Test
    @DisplayName("record accessors expose the constructed values")
    void accessorsExposeValues() {
        UUID templateTypeId = UUID.randomUUID();
        UUID severityTypeId = UUID.randomUUID();
        var request = new UpdateTemplateRequest("desc", "loc", "HTML", templateTypeId, severityTypeId);

        assertThat(request.description()).isEqualTo("desc");
        assertThat(request.location()).isEqualTo("loc");
        assertThat(request.fileFormat()).isEqualTo("HTML");
        assertThat(request.templateTypeId()).isEqualTo(templateTypeId);
        assertThat(request.severityTypeId()).isEqualTo(severityTypeId);
    }
}
