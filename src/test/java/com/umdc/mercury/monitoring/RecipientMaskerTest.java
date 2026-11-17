package com.umdc.mercury.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("RecipientMasker")
class RecipientMaskerTest {

    @Test
    @DisplayName("an e-mail keeps the first letter of the user and of the host and the domain suffix")
    void masksEmail() {
        assertEquals("j***@e***.com", RecipientMasker.mask("john.doe@example.com"));
        assertEquals("a***@m***.uk", RecipientMasker.mask("ann@mail.co.uk"));
        assertEquals("j***@e***", RecipientMasker.mask("john@example"));
    }

    @Test
    @DisplayName("a phone number, chat id or token keeps only its last three characters")
    void masksOthers() {
        assertEquals("***567", RecipientMasker.mask("+573001234567"));
        assertEquals("***345", RecipientMasker.mask("12345"));
        assertEquals("***", RecipientMasker.mask("12"));
        assertEquals("***", RecipientMasker.mask("abc"));
    }

    @Test
    @DisplayName("a value that only looks like an e-mail is treated as plain text")
    void edgeCases() {
        assertEquals("***com", RecipientMasker.mask("@x.com"));
        assertEquals("***om@", RecipientMasker.mask("x.com@"));
    }

    @Test
    @DisplayName("blank or null gives null, and the original never survives")
    void blankAndNoLeak() {
        assertNull(RecipientMasker.mask(null));
        assertNull(RecipientMasker.mask("  "));
        assertFalse(RecipientMasker.mask("secret.person@corp.example.org").contains("secret"));
    }
}
