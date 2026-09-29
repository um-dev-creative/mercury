package com.prx.mercury.api.v1.to;

import com.umdc.mercury.api.v1.to.CreateTemplateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateTemplateRequest bean validation")
class CreateTemplateRequestTest {

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

    private CreateTemplateRequest valid() {
        return new CreateTemplateRequest(
                "Welcome email", "s3://templates/welcome.html", "HTML",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    @DisplayName("no violations for a fully populated request")
    void validRequest_noViolations() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("rejects blank description")
    void blankDescription_isRejected() {
        var request = new CreateTemplateRequest(
                " ", "loc", "HTML", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Set<ConstraintViolation<CreateTemplateRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    @DisplayName("rejects null description")
    void nullDescription_isRejected() {
        var request = new CreateTemplateRequest(
                null, "loc", "HTML", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    @DisplayName("rejects location over the max length")
    void oversizedLocation_isRejected() {
        var request = new CreateTemplateRequest(
                "desc", "x".repeat(501), "HTML", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("location"));
    }

    @Test
    @DisplayName("rejects fileFormat over the max length")
    void oversizedFileFormat_isRejected() {
        var request = new CreateTemplateRequest(
                "desc", "loc", "x".repeat(51), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("fileFormat"));
    }

    @Test
    @DisplayName("rejects null templateTypeId")
    void nullTemplateTypeId_isRejected() {
        var request = new CreateTemplateRequest(
                "desc", "loc", "HTML", null, UUID.randomUUID(), UUID.randomUUID());
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("templateTypeId"));
    }

    @Test
    @DisplayName("rejects null applicationId")
    void nullApplicationId_isRejected() {
        var request = new CreateTemplateRequest(
                "desc", "loc", "HTML", UUID.randomUUID(), null, UUID.randomUUID());
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("applicationId"));
    }

    @Test
    @DisplayName("rejects null severityTypeId")
    void nullSeverityTypeId_isRejected() {
        var request = new CreateTemplateRequest(
                "desc", "loc", "HTML", UUID.randomUUID(), UUID.randomUUID(), null);
        assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("severityTypeId"));
    }

    @Test
    @DisplayName("record accessors expose the constructed values")
    void accessorsExposeValues() {
        UUID templateTypeId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID severityTypeId = UUID.randomUUID();
        var request = new CreateTemplateRequest("desc", "loc", "HTML", templateTypeId, applicationId, severityTypeId);

        assertThat(request.description()).isEqualTo("desc");
        assertThat(request.location()).isEqualTo("loc");
        assertThat(request.fileFormat()).isEqualTo("HTML");
        assertThat(request.templateTypeId()).isEqualTo(templateTypeId);
        assertThat(request.applicationId()).isEqualTo(applicationId);
        assertThat(request.severityTypeId()).isEqualTo(severityTypeId);
    }
}
