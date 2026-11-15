package com.umdc.mercury.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("umdc.mail.recipients.* as Spring binds it from the environment variables")
class MailRecipientPropertiesBindingTest {

    private static MailRecipientProperties bind(Map<String, Object> values) {
        return new Binder(new MapConfigurationPropertySource(values)).bind("umdc.mail.recipients", MailRecipientProperties.class).get();
    }

    @Test
    @DisplayName("a comma separated MAIL_DEFAULT_RECIPIENTS becomes the list and the flag turns the request off")
    void commaSeparatedList() {
        var props = bind(Map.of("umdc.mail.recipients.defaults", "qa1@x.test, qa2@x.test", "umdc.mail.recipients.honor-request", "false"));

        assertEquals(List.of("qa1@x.test", "qa2@x.test"), props.defaults());
        assertFalse(props.honorRequest());
    }

    @Test
    @DisplayName("nothing set (empty values as application.yml defaults them) keeps today's behaviour")
    void emptyKeepsTheRequest() {
        var props = bind(Map.of("umdc.mail.recipients.defaults", "", "umdc.mail.recipients.honor-request", "true"));

        assertTrue(props.defaults().isEmpty());
        assertTrue(props.honorRequest());
    }

    @Test
    @DisplayName("the request ignored with an empty list refuses to bind, so Mercury does not start")
    void overrideWithoutListFailsTheBinding() {
        assertThrows(BindException.class, () -> bind(Map.of("umdc.mail.recipients.defaults", "", "umdc.mail.recipients.honor-request", "false")));
    }
}
