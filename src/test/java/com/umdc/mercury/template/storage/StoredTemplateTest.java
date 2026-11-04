package com.umdc.mercury.template.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StoredTemplateTest {

    private static StoredTemplate template(String text) {
        return new StoredTemplate(new TemplateMetadata("a.ftl", "etag", Instant.EPOCH), text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void equalContentIsEqualWhateverTheArrayInstance() {
        assertThat(template("hola")).isEqualTo(template("hola")).hasSameHashCodeAs(template("hola"));
    }

    @Test
    void differentContentOrMetadataIsDifferent() {
        assertThat(template("hola")).isNotEqualTo(template("adiós")).isNotEqualTo("hola");
        StoredTemplate other = new StoredTemplate(new TemplateMetadata("b.ftl", "etag", Instant.EPOCH), "hola".getBytes(StandardCharsets.UTF_8));
        assertThat(template("hola")).isNotEqualTo(other);
    }

    @Test
    void toStringDoesNotPrintTheContent() {
        assertThat(template("secreto")).hasToString("StoredTemplate[TemplateMetadata[name=a.ftl, etag=etag, lastModified=1970-01-01T00:00:00Z], 7 bytes]");
    }

    @Test
    void textDecodesUtf8() {
        assertThat(template("¿qué?").text()).isEqualTo("¿qué?");
    }
}
