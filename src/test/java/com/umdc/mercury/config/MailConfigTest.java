package com.umdc.mercury.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailConfigTest {

    private final MailConfig config = new MailConfig();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(config, "port", 587);
        ReflectionTestUtils.setField(config, "host", "smtp.test");
        ReflectionTestUtils.setField(config, "username", "user");
        ReflectionTestUtils.setField(config, "password", "secret");
        ReflectionTestUtils.setField(config, "protocol", "smtp");
        ReflectionTestUtils.setField(config, "timeout", 5000);
        ReflectionTestUtils.setField(config, "starttls", " false ");
        ReflectionTestUtils.setField(config, "auth", "");
    }

    @Test
    void senderUsesTheConfiguredServer() {
        var sender = (JavaMailSenderImpl) config.getJavaMailSender();

        assertEquals("smtp.test", sender.getHost());
        assertEquals(587, sender.getPort());
        assertEquals("smtp", sender.getProtocol());
        assertEquals("user", sender.getUsername());
        assertEquals("secret", sender.getPassword());
        assertEquals("smtp.test", sender.getJavaMailProperties().get("mail.smtp.ssl.trust"));
        assertEquals(5000, sender.getJavaMailProperties().get("mail.smtp.timeout"));
    }

    @Test
    void gettersExposeTheConfiguredValues() {
        assertEquals(587, config.getPort());
        assertEquals("smtp.test", config.getHost());
        assertEquals("user", config.getUsername());
        assertEquals("secret", config.getPassword());
        assertEquals("smtp", config.getProtocol());
        assertEquals(5000, config.getTimeout());
    }

    @Test
    void flagsAreTrimmedAndBlankFallsBackToTrue() {
        assertFalse(config.isStarttls());
        assertTrue(config.isAuth());
    }

    @Test
    void missingFlagFallsBackToTrue() {
        ReflectionTestUtils.setField(config, "starttls", null);

        assertTrue(config.isStarttls());
    }
}
