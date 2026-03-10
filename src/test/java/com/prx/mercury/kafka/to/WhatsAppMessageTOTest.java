package com.prx.mercury.kafka.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppMessageTOTest {

    @Test
    @DisplayName("WhatsAppMessageTO stores template metadata and params")
    void createsWhatsAppMessageTO() {
        var component = new WhatsAppMessageTO.WhatsAppTemplateComponent(
                "body",
                null,
                0,
                List.of(new WhatsAppMessageTO.WhatsAppParameter(
                        "text",
                        "Hello",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
        );

        WhatsAppMessageTO messageTO = new WhatsAppMessageTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "+525512345678",
                "template",
                "{\"name\":\"promo\"}",
                "order_confirmation",
                "es_MX",
                List.of(component),
                null,
                null,
                LocalDateTime.now(),
                Map.of("discount", 20),
                UUID.randomUUID()
        );

        assertEquals("template", messageTO.messageType());
        assertEquals("order_confirmation", messageTO.templateName());
        assertEquals("es_MX", messageTO.templateLanguage());
        assertEquals(1, messageTO.templateComponents().size());
        assertEquals("body", messageTO.templateComponents().get(0).type());
    }

    @Test
    @DisplayName("Nested WhatsApp records support equality and field access")
    void whatsappNestedRecords() {
        var media = new WhatsAppMessageTO.WhatsAppMedia("link", "id", "caption", "file.pdf");
        var parameter = new WhatsAppMessageTO.WhatsAppParameter(
                "media", null, null, null, null, null, null, media
        );
        var component = new WhatsAppMessageTO.WhatsAppTemplateComponent("header", null, 1, List.of(parameter));

        assertEquals("media", parameter.type());
        assertEquals("file.pdf", parameter.media().filename());
        assertEquals(1, component.index());
        assertEquals(parameter, component.parameters().get(0));
    }
}
