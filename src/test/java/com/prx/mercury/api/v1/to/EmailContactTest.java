package com.prx.mercury.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailContactTest {

    @Test
    @DisplayName("Create EmailContact with valid data")
    void createEmailContactWithValidData() {
        EmailContact emailContact = new EmailContact("test@example.com", "Test Name", "Test Alias");

        assertEquals("test@example.com", emailContact.email());
        assertEquals("Test Name", emailContact.name());
        assertEquals("Test Alias", emailContact.alias());
    }

    @Test
    @DisplayName("Fail to create EmailContact with null email")
    void failToCreateEmailContactWithNullEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new EmailContact(null, "Test Name", "Test Alias")
        );

        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Fail to create EmailContact with empty email")
    void failToCreateEmailContactWithEmptyEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new EmailContact("", "Test Name", "Test Alias")
        );

        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Fail to create EmailContact with blank email")
    void failToCreateEmailContactWithBlankEmail() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new EmailContact("   ", "Test Name", "Test Alias")
        );

        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Create EmailContact with null alias")
    void createEmailContactWithNullAlias() {
        EmailContact emailContact = new EmailContact("test@example.com", "Test Name", null);

        assertEquals("test@example.com", emailContact.email());
        assertEquals("Test Name", emailContact.name());
        assertNull(emailContact.alias());
    }

    @Test
    @DisplayName("Create EmailContact with empty alias")
    void createEmailContactWithEmptyAlias() {
        EmailContact emailContact = new EmailContact("test@example.com", "Test Name", "");

        assertEquals("test@example.com", emailContact.email());
        assertEquals("Test Name", emailContact.name());
        assertEquals("", emailContact.alias());
    }

    @Test
    @DisplayName("Create EmailContact with blank alias")
    void createEmailContactWithBlankAlias() {
        EmailContact emailContact = new EmailContact("test@example.com", "Test Name", "   ");

        assertEquals("test@example.com", emailContact.email());
        assertEquals("Test Name", emailContact.name());
        assertEquals("   ", emailContact.alias());
    }

    @Test
    @DisplayName("EmailContact toString method")
    void emailContactToString() {
        EmailContact emailContact = new EmailContact("test@example.com", "Test Name", "Test Alias");

        String expected = "EmailContact{email='test@example.com', name='Test Name', alias='Test Alias'}";
        assertEquals(expected, emailContact.toString());
    }
}
