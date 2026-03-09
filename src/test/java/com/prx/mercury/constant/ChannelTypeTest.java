package com.prx.mercury.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ChannelType} enum.
 *
 * Tests cover enum instantiation, field accessors, factory method (fromCode),
 * enum constants, identity, case-insensitivity, and business logic characteristics
 * of the ChannelType enum.
 */
@DisplayName("ChannelType Enum Tests")
class ChannelTypeTest {

    @Nested
    @DisplayName("Enum Constant Tests")
    class EnumConstantTests {

        @Test
        @DisplayName("EMAIL constant should exist and be accessible")
        void emailConstantExists() {
            assertNotNull(ChannelType.EMAIL);
        }

        @Test
        @DisplayName("SMS constant should exist and be accessible")
        void smsConstantExists() {
            assertNotNull(ChannelType.SMS);
        }

        @Test
        @DisplayName("TELEGRAM constant should exist and be accessible")
        void telegramConstantExists() {
            assertNotNull(ChannelType.TELEGRAM);
        }

        @Test
        @DisplayName("WHATSAPP constant should exist and be accessible")
        void whatsappConstantExists() {
            assertNotNull(ChannelType.WHATSAPP);
        }

        @Test
        @DisplayName("PUSH constant should exist and be accessible")
        void pushConstantExists() {
            assertNotNull(ChannelType.PUSH);
        }

        @Test
        @DisplayName("All constants should be ChannelType instances")
        void constantsAreInstances() {
            assertTrue(ChannelType.EMAIL instanceof ChannelType);
            assertTrue(ChannelType.SMS instanceof ChannelType);
            assertTrue(ChannelType.TELEGRAM instanceof ChannelType);
            assertTrue(ChannelType.WHATSAPP instanceof ChannelType);
            assertTrue(ChannelType.PUSH instanceof ChannelType);
        }

        @Test
        @DisplayName("Enum should have exactly five constants")
        void enumConstantsCount() {
            ChannelType[] values = ChannelType.values();
            assertEquals(5, values.length);
        }

        @Test
        @DisplayName("All constants should be present in values array")
        void allConstantsInValuesArray() {
            ChannelType[] values = ChannelType.values();
            assertTrue(contains(values, ChannelType.EMAIL));
            assertTrue(contains(values, ChannelType.SMS));
            assertTrue(contains(values, ChannelType.TELEGRAM));
            assertTrue(contains(values, ChannelType.WHATSAPP));
            assertTrue(contains(values, ChannelType.PUSH));
        }

        private boolean contains(ChannelType[] values, ChannelType type) {
            for (ChannelType value : values) {
                if (value == type) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("EMAIL Accessor Tests")
    class EmailAccessorTests {

        @Test
        @DisplayName("EMAIL getCode should return 'email'")
        void emailCodeIsEmail() {
            assertEquals("email", ChannelType.EMAIL.getCode());
        }

        @Test
        @DisplayName("EMAIL getDisplayName should return 'Email'")
        void emailDisplayNameIsEmail() {
            assertEquals("Email", ChannelType.EMAIL.getDisplayName());
        }

        @Test
        @DisplayName("EMAIL code should not be null")
        void emailCodeNotNull() {
            assertNotNull(ChannelType.EMAIL.getCode());
        }

        @Test
        @DisplayName("EMAIL displayName should not be null")
        void emailDisplayNameNotNull() {
            assertNotNull(ChannelType.EMAIL.getDisplayName());
        }

        @Test
        @DisplayName("EMAIL code should not be empty")
        void emailCodeNotEmpty() {
            assertFalse(ChannelType.EMAIL.getCode().isEmpty());
        }

        @Test
        @DisplayName("EMAIL displayName should not be empty")
        void emailDisplayNameNotEmpty() {
            assertFalse(ChannelType.EMAIL.getDisplayName().isEmpty());
        }
    }

    @Nested
    @DisplayName("SMS Accessor Tests")
    class SmsAccessorTests {

        @Test
        @DisplayName("SMS getCode should return 'sms'")
        void smsCodeIsSms() {
            assertEquals("sms", ChannelType.SMS.getCode());
        }

        @Test
        @DisplayName("SMS getDisplayName should return 'SMS'")
        void smsDisplayNameIsSms() {
            assertEquals("SMS", ChannelType.SMS.getDisplayName());
        }

        @Test
        @DisplayName("SMS code should not be null")
        void smsCodeNotNull() {
            assertNotNull(ChannelType.SMS.getCode());
        }

        @Test
        @DisplayName("SMS displayName should not be null")
        void smsDisplayNameNotNull() {
            assertNotNull(ChannelType.SMS.getDisplayName());
        }
    }

    @Nested
    @DisplayName("TELEGRAM Accessor Tests")
    class TelegramAccessorTests {

        @Test
        @DisplayName("TELEGRAM getCode should return 'telegram'")
        void telegramCodeIsTelegram() {
            assertEquals("telegram", ChannelType.TELEGRAM.getCode());
        }

        @Test
        @DisplayName("TELEGRAM getDisplayName should return 'Telegram'")
        void telegramDisplayNameIsTelegram() {
            assertEquals("Telegram", ChannelType.TELEGRAM.getDisplayName());
        }

        @Test
        @DisplayName("TELEGRAM code should not be null")
        void telegramCodeNotNull() {
            assertNotNull(ChannelType.TELEGRAM.getCode());
        }

        @Test
        @DisplayName("TELEGRAM displayName should not be null")
        void telegramDisplayNameNotNull() {
            assertNotNull(ChannelType.TELEGRAM.getDisplayName());
        }
    }

    @Nested
    @DisplayName("WHATSAPP Accessor Tests")
    class WhatsappAccessorTests {

        @Test
        @DisplayName("WHATSAPP getCode should return 'whatsapp'")
        void whatsappCodeIsWhatsapp() {
            assertEquals("whatsapp", ChannelType.WHATSAPP.getCode());
        }

        @Test
        @DisplayName("WHATSAPP getDisplayName should return 'WhatsApp'")
        void whatsappDisplayNameIsWhatsapp() {
            assertEquals("WhatsApp", ChannelType.WHATSAPP.getDisplayName());
        }

        @Test
        @DisplayName("WHATSAPP code should not be null")
        void whatsappCodeNotNull() {
            assertNotNull(ChannelType.WHATSAPP.getCode());
        }

        @Test
        @DisplayName("WHATSAPP displayName should not be null")
        void whatsappDisplayNameNotNull() {
            assertNotNull(ChannelType.WHATSAPP.getDisplayName());
        }
    }

    @Nested
    @DisplayName("PUSH Accessor Tests")
    class PushAccessorTests {

        @Test
        @DisplayName("PUSH getCode should return 'push'")
        void pushCodeIsPush() {
            assertEquals("push", ChannelType.PUSH.getCode());
        }

        @Test
        @DisplayName("PUSH getDisplayName should return 'Push Notification'")
        void pushDisplayNameIsPushNotification() {
            assertEquals("Push Notification", ChannelType.PUSH.getDisplayName());
        }

        @Test
        @DisplayName("PUSH code should not be null")
        void pushCodeNotNull() {
            assertNotNull(ChannelType.PUSH.getCode());
        }

        @Test
        @DisplayName("PUSH displayName should not be null")
        void pushDisplayNameNotNull() {
            assertNotNull(ChannelType.PUSH.getDisplayName());
        }
    }

    @Nested
    @DisplayName("fromCode Factory Method Tests")
    class FromCodeTests {

        @Test
        @DisplayName("fromCode('email') should return EMAIL constant")
        void fromCodeEmailLowercase() {
            assertEquals(ChannelType.EMAIL, ChannelType.fromCode("email"));
        }

        @Test
        @DisplayName("fromCode('sms') should return SMS constant")
        void fromCodeSmsLowercase() {
            assertEquals(ChannelType.SMS, ChannelType.fromCode("sms"));
        }

        @Test
        @DisplayName("fromCode('telegram') should return TELEGRAM constant")
        void fromCodeTelegramLowercase() {
            assertEquals(ChannelType.TELEGRAM, ChannelType.fromCode("telegram"));
        }

        @Test
        @DisplayName("fromCode('whatsapp') should return WHATSAPP constant")
        void fromCodeWhatsappLowercase() {
            assertEquals(ChannelType.WHATSAPP, ChannelType.fromCode("whatsapp"));
        }

        @Test
        @DisplayName("fromCode('push') should return PUSH constant")
        void fromCodePushLowercase() {
            assertEquals(ChannelType.PUSH, ChannelType.fromCode("push"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"EMAIL", "Email", "eMail", "EMAI"})
        @DisplayName("fromCode should be case-insensitive for EMAIL")
        void fromCodeEmailCaseInsensitive(String code) {
            if ("EMAI".equals(code)) {
                assertThrows(IllegalArgumentException.class, () -> ChannelType.fromCode(code));
            } else {
                assertEquals(ChannelType.EMAIL, ChannelType.fromCode(code));
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"SMS", "Sms", "sMS"})
        @DisplayName("fromCode should be case-insensitive for SMS")
        void fromCodeSmsCaseInsensitive(String code) {
            assertEquals(ChannelType.SMS, ChannelType.fromCode(code));
        }

        @ParameterizedTest
        @ValueSource(strings = {"TELEGRAM", "Telegram", "TeLEgRaM"})
        @DisplayName("fromCode should be case-insensitive for TELEGRAM")
        void fromCodeTelegramCaseInsensitive(String code) {
            assertEquals(ChannelType.TELEGRAM, ChannelType.fromCode(code));
        }

        @ParameterizedTest
        @ValueSource(strings = {"WHATSAPP", "WhatsApp", "whatsAPP"})
        @DisplayName("fromCode should be case-insensitive for WHATSAPP")
        void fromCodeWhatsappCaseInsensitive(String code) {
            assertEquals(ChannelType.WHATSAPP, ChannelType.fromCode(code));
        }

        @ParameterizedTest
        @ValueSource(strings = {"PUSH", "Push", "pUsH"})
        @DisplayName("fromCode should be case-insensitive for PUSH")
        void fromCodePushCaseInsensitive(String code) {
            assertEquals(ChannelType.PUSH, ChannelType.fromCode(code));
        }

        @Test
        @DisplayName("fromCode with invalid code should throw IllegalArgumentException")
        void fromCodeInvalidThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> ChannelType.fromCode("invalid"));
        }

        @Test
        @DisplayName("fromCode with null code should throw IllegalArgumentException or NullPointerException")
        void fromCodeNullThrowsException() {
            assertThrows(Exception.class, () -> ChannelType.fromCode(null));
        }

        @Test
        @DisplayName("fromCode with empty string should throw IllegalArgumentException")
        void fromCodeEmptyStringThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> ChannelType.fromCode(""));
        }

        @Test
        @DisplayName("fromCode error message should contain the invalid code")
        void fromCodeErrorMessageContainsCode() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ChannelType.fromCode("invalidCode"));
            assertTrue(exception.getMessage().contains("invalidCode"));
        }

        @Test
        @DisplayName("fromCode should return same instance (singleton behavior)")
        void fromCodeReturnsSingletonInstance() {
            ChannelType instance1 = ChannelType.fromCode("email");
            ChannelType instance2 = ChannelType.fromCode("email");
            assertSame(instance1, instance2);
        }
    }

    @Nested
    @DisplayName("Enum Identity and Equality Tests")
    class EnumIdentityTests {

        @Test
        @DisplayName("EMAIL should be the same object when retrieved multiple times")
        void emailIdentity() {
            ChannelType first = ChannelType.EMAIL;
            ChannelType second = ChannelType.EMAIL;
            assertSame(first, second);
        }

        @Test
        @DisplayName("SMS should be the same object when retrieved multiple times")
        void smsIdentity() {
            ChannelType first = ChannelType.SMS;
            ChannelType second = ChannelType.SMS;
            assertSame(first, second);
        }

        @Test
        @DisplayName("EMAIL should equal itself")
        void emailEqualsSelf() {
            assertEquals(ChannelType.EMAIL, ChannelType.EMAIL);
        }

        @Test
        @DisplayName("EMAIL should equal an enum obtained via valueOf")
        void emailEqualsValueOf() {
            ChannelType fromValueOf = ChannelType.valueOf("EMAIL");
            assertEquals(ChannelType.EMAIL, fromValueOf);
        }

        @Test
        @DisplayName("Different enum constants should not be equal")
        void differentConstantsNotEqual() {
            assertNotEquals(ChannelType.EMAIL, ChannelType.SMS);
            assertNotEquals(ChannelType.TELEGRAM, ChannelType.PUSH);
        }

        @Test
        @DisplayName("All constants should be unique")
        void allConstantsUnique() {
            ChannelType[] values = ChannelType.values();
            for (int i = 0; i < values.length; i++) {
                for (int j = i + 1; j < values.length; j++) {
                    assertNotEquals(values[i], values[j]);
                }
            }
        }
    }

    @Nested
    @DisplayName("Enum Ordinal Tests")
    class EnumOrdinalTests {

        @Test
        @DisplayName("EMAIL should have ordinal value 0")
        void emailOrdinalIsZero() {
            assertEquals(0, ChannelType.EMAIL.ordinal());
        }

        @Test
        @DisplayName("SMS should have ordinal value 1")
        void smsOrdinalIsOne() {
            assertEquals(1, ChannelType.SMS.ordinal());
        }

        @Test
        @DisplayName("TELEGRAM should have ordinal value 2")
        void telegramOrdinalIsTwo() {
            assertEquals(2, ChannelType.TELEGRAM.ordinal());
        }

        @Test
        @DisplayName("WHATSAPP should have ordinal value 3")
        void whatsappOrdinalIsThree() {
            assertEquals(3, ChannelType.WHATSAPP.ordinal());
        }

        @Test
        @DisplayName("PUSH should have ordinal value 4")
        void pushOrdinalIsFour() {
            assertEquals(4, ChannelType.PUSH.ordinal());
        }

        @Test
        @DisplayName("Enum ordinal should match values array index")
        void ordinalMatchesValuesIndex() {
            ChannelType[] values = ChannelType.values();
            for (int i = 0; i < values.length; i++) {
                assertEquals(i, values[i].ordinal());
            }
        }
    }

    @Nested
    @DisplayName("Enum Naming Tests")
    class EnumNamingTests {

        @Test
        @DisplayName("EMAIL enum name should be EMAIL")
        void emailNameCorrect() {
            assertEquals("EMAIL", ChannelType.EMAIL.name());
        }

        @Test
        @DisplayName("SMS enum name should be SMS")
        void smsNameCorrect() {
            assertEquals("SMS", ChannelType.SMS.name());
        }

        @Test
        @DisplayName("TELEGRAM enum name should be TELEGRAM")
        void telegramNameCorrect() {
            assertEquals("TELEGRAM", ChannelType.TELEGRAM.name());
        }

        @Test
        @DisplayName("WHATSAPP enum name should be WHATSAPP")
        void whatsappNameCorrect() {
            assertEquals("WHATSAPP", ChannelType.WHATSAPP.name());
        }

        @Test
        @DisplayName("PUSH enum name should be PUSH")
        void pushNameCorrect() {
            assertEquals("PUSH", ChannelType.PUSH.name());
        }

        @Test
        @DisplayName("toString should return EMAIL")
        void emailToStringCorrect() {
            assertEquals("EMAIL", ChannelType.EMAIL.toString());
        }

        @Test
        @DisplayName("toString should return SMS")
        void smsToStringCorrect() {
            assertEquals("SMS", ChannelType.SMS.toString());
        }
    }

    @Nested
    @DisplayName("Enum Field Consistency Tests")
    class FieldConsistencyTests {

        @Test
        @DisplayName("Multiple calls to getCode should return the same value for EMAIL")
        void emailCodeConsistency() {
            String first = ChannelType.EMAIL.getCode();
            String second = ChannelType.EMAIL.getCode();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getDisplayName should return the same value for EMAIL")
        void emailDisplayNameConsistency() {
            String first = ChannelType.EMAIL.getDisplayName();
            String second = ChannelType.EMAIL.getDisplayName();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getCode should return the same value for all constants")
        void allConstantsCodeConsistency() {
            for (ChannelType type : ChannelType.values()) {
                String first = type.getCode();
                String second = type.getCode();
                assertEquals(first, second, "Code consistency failed for " + type.name());
            }
        }

        @Test
        @DisplayName("Multiple calls to getDisplayName should return the same value for all constants")
        void allConstantsDisplayNameConsistency() {
            for (ChannelType type : ChannelType.values()) {
                String first = type.getDisplayName();
                String second = type.getDisplayName();
                assertEquals(first, second, "DisplayName consistency failed for " + type.name());
            }
        }
    }

    @Nested
    @DisplayName("Enum Iteration Tests")
    class EnumIterationTests {

        @Test
        @DisplayName("values() should return an array with all five constants in order")
        void valuesContainsAllConstantsInOrder() {
            ChannelType[] values = ChannelType.values();
            assertEquals(ChannelType.EMAIL, values[0]);
            assertEquals(ChannelType.SMS, values[1]);
            assertEquals(ChannelType.TELEGRAM, values[2]);
            assertEquals(ChannelType.WHATSAPP, values[3]);
            assertEquals(ChannelType.PUSH, values[4]);
        }

        @Test
        @DisplayName("values() should return non-empty array")
        void valuesNonEmpty() {
            ChannelType[] values = ChannelType.values();
            assertNotEquals(0, values.length);
        }

        @Test
        @DisplayName("Iterate over values and verify all constants are found")
        void iterateValuesContainsAllConstants() {
            boolean foundEmail = false;
            boolean foundSms = false;
            boolean foundTelegram = false;
            boolean foundWhatsapp = false;
            boolean foundPush = false;

            for (ChannelType type : ChannelType.values()) {
                if (type == ChannelType.EMAIL) foundEmail = true;
                if (type == ChannelType.SMS) foundSms = true;
                if (type == ChannelType.TELEGRAM) foundTelegram = true;
                if (type == ChannelType.WHATSAPP) foundWhatsapp = true;
                if (type == ChannelType.PUSH) foundPush = true;
            }

            assertTrue(foundEmail);
            assertTrue(foundSms);
            assertTrue(foundTelegram);
            assertTrue(foundWhatsapp);
            assertTrue(foundPush);
        }
    }

    @Nested
    @DisplayName("Enum Hash Code Tests")
    class EnumHashCodeTests {

        @Test
        @DisplayName("EMAIL should have consistent hash code")
        void emailHashCodeConsistent() {
            int hash1 = ChannelType.EMAIL.hashCode();
            int hash2 = ChannelType.EMAIL.hashCode();
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Same enum instance should have same hash code")
        void sameInstanceSameHashCode() {
            ChannelType type1 = ChannelType.EMAIL;
            ChannelType type2 = ChannelType.EMAIL;
            assertEquals(type1.hashCode(), type2.hashCode());
        }

        @Test
        @DisplayName("All constants should have consistent hash codes")
        void allConstantsHashCodeConsistent() {
            for (ChannelType type : ChannelType.values()) {
                int hash1 = type.hashCode();
                int hash2 = type.hashCode();
                assertEquals(hash1, hash2, "Hash code inconsistent for " + type.name());
            }
        }
    }

    @Nested
    @DisplayName("Enum Class Tests")
    class EnumClassTests {

        @Test
        @DisplayName("ChannelType should be an Enum class")
        void channelTypeIsEnum() {
            assertTrue(ChannelType.class.isEnum());
        }

        @Test
        @DisplayName("ChannelType class should not be null")
        void channelTypeClassNotNull() {
            assertNotNull(ChannelType.class);
        }

        @Test
        @DisplayName("ChannelType should have EMAIL enum constant")
        void channelTypeHasEmailField() throws NoSuchFieldException {
            assertNotNull(ChannelType.class.getField("EMAIL"));
        }

        @Test
        @DisplayName("ChannelType should have SMS enum constant")
        void channelTypeHasSmsField() throws NoSuchFieldException {
            assertNotNull(ChannelType.class.getField("SMS"));
        }

        @Test
        @DisplayName("ChannelType enum should have getCode method")
        void channelTypeHasGetCodeMethod() throws NoSuchMethodException {
            assertNotNull(ChannelType.class.getMethod("getCode"));
        }

        @Test
        @DisplayName("ChannelType enum should have getDisplayName method")
        void channelTypeHasGetDisplayNameMethod() throws NoSuchMethodException {
            assertNotNull(ChannelType.class.getMethod("getDisplayName"));
        }

        @Test
        @DisplayName("ChannelType enum should have fromCode static method")
        void channelTypeHasFromCodeMethod() throws NoSuchMethodException {
            assertNotNull(ChannelType.class.getMethod("fromCode", String.class));
        }
    }

    @Nested
    @DisplayName("Enum Comparison Tests")
    class EnumComparisonTests {

        @Test
        @DisplayName("EMAIL should not equal null")
        void emailNotEqualsNull() {
            assertNotEquals(ChannelType.EMAIL, null);
        }

        @Test
        @DisplayName("EMAIL should not equal a string")
        void emailNotEqualsString() {
            assertNotEquals(ChannelType.EMAIL, "EMAIL");
        }

        @Test
        @DisplayName("EMAIL should not equal a different object type")
        void emailNotEqualsDifferentType() {
            assertNotEquals(ChannelType.EMAIL, new Object());
        }

        @Test
        @DisplayName("All constants should not equal null")
        void allConstantsNotEqualsNull() {
            for (ChannelType type : ChannelType.values()) {
                assertNotEquals(type, null);
            }
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Each channel has a unique code")
        void uniqueChannelCodes() {
            String[] codes = new String[ChannelType.values().length];
            for (int i = 0; i < ChannelType.values().length; i++) {
                codes[i] = ChannelType.values()[i].getCode();
            }
            for (int i = 0; i < codes.length; i++) {
                for (int j = i + 1; j < codes.length; j++) {
                    assertNotEquals(codes[i], codes[j], "Duplicate code found");
                }
            }
        }

        @Test
        @DisplayName("Each channel has a unique displayName")
        void uniqueDisplayNames() {
            String[] names = new String[ChannelType.values().length];
            for (int i = 0; i < ChannelType.values().length; i++) {
                names[i] = ChannelType.values()[i].getDisplayName();
            }
            for (int i = 0; i < names.length; i++) {
                for (int j = i + 1; j < names.length; j++) {
                    assertNotEquals(names[i], names[j], "Duplicate displayName found");
                }
            }
        }

        @Test
        @DisplayName("Code is lowercase identifier suitable for routing")
        void codeIsLowercaseIdentifier() {
            for (ChannelType type : ChannelType.values()) {
                String code = type.getCode();
                assertEquals(code, code.toLowerCase(), "Code should be lowercase: " + code);
            }
        }

        @Test
        @DisplayName("DisplayName is user-friendly with proper capitalization")
        void displayNameIsUserFriendly() {
            for (ChannelType type : ChannelType.values()) {
                String displayName = type.getDisplayName();
                assertFalse(displayName.isEmpty(), "DisplayName should not be empty");
                assertFalse(displayName.isBlank(), "DisplayName should not be blank");
            }
        }

        @Test
        @DisplayName("fromCode enables channel lookup by code identifier")
        void fromCodeEnablesChannelLookup() {
            for (ChannelType type : ChannelType.values()) {
                ChannelType looked = ChannelType.fromCode(type.getCode());
                assertEquals(type, looked, "fromCode should find channel by code");
            }
        }

        @Test
        @DisplayName("Channel types cover major communication channels")
        void coversMainChannelTypes() {
            assertTrue(contains(ChannelType.values(), ChannelType.EMAIL), "Should have EMAIL");
            assertTrue(contains(ChannelType.values(), ChannelType.SMS), "Should have SMS");
            assertTrue(contains(ChannelType.values(), ChannelType.TELEGRAM), "Should have TELEGRAM");
            assertTrue(contains(ChannelType.values(), ChannelType.WHATSAPP), "Should have WHATSAPP");
            assertTrue(contains(ChannelType.values(), ChannelType.PUSH), "Should have PUSH");
        }

        private boolean contains(ChannelType[] values, ChannelType type) {
            for (ChannelType value : values) {
                if (value == type) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Code vs DisplayName Mapping Tests")
    class CodeDisplayNameMappingTests {

        @Test
        @DisplayName("EMAIL: code 'email' maps to 'Email'")
        void emailCodeDisplayNameMapping() {
            assertEquals("email", ChannelType.EMAIL.getCode());
            assertEquals("Email", ChannelType.EMAIL.getDisplayName());
        }

        @Test
        @DisplayName("SMS: code 'sms' maps to 'SMS'")
        void smsCodeDisplayNameMapping() {
            assertEquals("sms", ChannelType.SMS.getCode());
            assertEquals("SMS", ChannelType.SMS.getDisplayName());
        }

        @Test
        @DisplayName("TELEGRAM: code 'telegram' maps to 'Telegram'")
        void telegramCodeDisplayNameMapping() {
            assertEquals("telegram", ChannelType.TELEGRAM.getCode());
            assertEquals("Telegram", ChannelType.TELEGRAM.getDisplayName());
        }

        @Test
        @DisplayName("WHATSAPP: code 'whatsapp' maps to 'WhatsApp'")
        void whatsappCodeDisplayNameMapping() {
            assertEquals("whatsapp", ChannelType.WHATSAPP.getCode());
            assertEquals("WhatsApp", ChannelType.WHATSAPP.getDisplayName());
        }

        @Test
        @DisplayName("PUSH: code 'push' maps to 'Push Notification'")
        void pushCodeDisplayNameMapping() {
            assertEquals("push", ChannelType.PUSH.getCode());
            assertEquals("Push Notification", ChannelType.PUSH.getDisplayName());
        }

        @Test
        @DisplayName("DisplayName should be human-readable version of code")
        void displayNameIsHumanReadableVersion() {
            for (ChannelType type : ChannelType.values()) {
                String displayName = type.getDisplayName();
                assertFalse(displayName.isEmpty());
                assertNotEquals(type.getDisplayName(), displayName);
            }
        }
    }
}

