package com.prx.mercury.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link MercuryMessage} enum.
 *
 * Tests cover enum instantiation, field accessors, interface implementation,
 * enum constants, identity, and business logic characteristics of the MercuryMessage enum.
 */
@DisplayName("MercuryMessage Enum Tests")
class MercuryMessageTest {

    @Nested
    @DisplayName("Enum Constant Tests")
    class EnumConstantTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED constant should exist and be accessible")
        void methodNotImplementedConstantExists() {
            assertNotNull(MercuryMessage.METHOD_NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION constant should exist and be accessible")
        void runtimeExceptionConstantExists() {
            assertNotNull(MercuryMessage.RUNTIME_EXCEPTION);
        }

        @Test
        @DisplayName("Both constants should be MercuryMessage instances")
        void constantsAreInstances() {
            assertTrue(MercuryMessage.METHOD_NOT_IMPLEMENTED instanceof MercuryMessage);
            assertTrue(MercuryMessage.RUNTIME_EXCEPTION instanceof MercuryMessage);
        }

        @Test
        @DisplayName("Enum should have exactly two constants")
        void enumConstantsCount() {
            MercuryMessage[] values = MercuryMessage.values();
            assertEquals(2, values.length);
        }

        @Test
        @DisplayName("Both constants should be present in values array")
        void allConstantsInValuesArray() {
            MercuryMessage[] values = MercuryMessage.values();
            assertTrue(contains(values, MercuryMessage.METHOD_NOT_IMPLEMENTED));
            assertTrue(contains(values, MercuryMessage.RUNTIME_EXCEPTION));
        }

        private boolean contains(MercuryMessage[] values, MercuryMessage message) {
            for (MercuryMessage value : values) {
                if (value == message) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("METHOD_NOT_IMPLEMENTED Accessor Tests")
    class MethodNotImplementedTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED getCode should return 501")
        void methodNotImplementedCodeIs501() {
            assertEquals(501, MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode());
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED getStatus should return 'Method not implemented'")
        void methodNotImplementedStatusCorrect() {
            assertEquals("Method not implemented", MercuryMessage.METHOD_NOT_IMPLEMENTED.getStatus());
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED getCodeToString should return '501'")
        void methodNotImplementedCodeToStringIs501() {
            assertEquals("501", MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString());
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED status should not be null")
        void methodNotImplementedStatusNotNull() {
            assertNotNull(MercuryMessage.METHOD_NOT_IMPLEMENTED.getStatus());
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED status should not be empty")
        void methodNotImplementedStatusNotEmpty() {
            assertFalse(MercuryMessage.METHOD_NOT_IMPLEMENTED.getStatus().isEmpty());
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED code should be positive")
        void methodNotImplementedCodePositive() {
            assertTrue(MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode() > 0);
        }
    }

    @Nested
    @DisplayName("RUNTIME_EXCEPTION Accessor Tests")
    class RuntimeExceptionTests {

        @Test
        @DisplayName("RUNTIME_EXCEPTION getCode should return 500")
        void runtimeExceptionCodeIs500() {
            assertEquals(500, MercuryMessage.RUNTIME_EXCEPTION.getCode());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION getStatus should return 'Internal server error'")
        void runtimeExceptionStatusCorrect() {
            assertEquals("Internal server error", MercuryMessage.RUNTIME_EXCEPTION.getStatus());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION getCodeToString should return '500'")
        void runtimeExceptionCodeToStringIs500() {
            assertEquals("500", MercuryMessage.RUNTIME_EXCEPTION.getCodeToString());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION status should not be null")
        void runtimeExceptionStatusNotNull() {
            assertNotNull(MercuryMessage.RUNTIME_EXCEPTION.getStatus());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION status should not be empty")
        void runtimeExceptionStatusNotEmpty() {
            assertFalse(MercuryMessage.RUNTIME_EXCEPTION.getStatus().isEmpty());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION code should be positive")
        void runtimeExceptionCodePositive() {
            assertTrue(MercuryMessage.RUNTIME_EXCEPTION.getCode() > 0);
        }
    }

    @Nested
    @DisplayName("Code Comparison Tests")
    class CodeComparisonTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED code (501) should be greater than RUNTIME_EXCEPTION code (500)")
        void methodNotImplementedCodeGreaterThanRuntimeException() {
            assertTrue(MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode() > MercuryMessage.RUNTIME_EXCEPTION.getCode());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION code (500) should be less than METHOD_NOT_IMPLEMENTED code (501)")
        void runtimeExceptionCodeLessThanMethodNotImplemented() {
            assertTrue(MercuryMessage.RUNTIME_EXCEPTION.getCode() < MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode());
        }

        @Test
        @DisplayName("Codes should not be equal")
        void codesNotEqual() {
            assertNotEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode(), MercuryMessage.RUNTIME_EXCEPTION.getCode());
        }

        @Test
        @DisplayName("Both codes should be valid HTTP status codes (400-599)")
        void codesAreValidHttpCodes() {
            int methodNotImplCode = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode();
            int runtimeExceptionCode = MercuryMessage.RUNTIME_EXCEPTION.getCode();

            assertTrue(methodNotImplCode >= 400 && methodNotImplCode < 600);
            assertTrue(runtimeExceptionCode >= 400 && runtimeExceptionCode < 600);
        }
    }

    @Nested
    @DisplayName("Enum Identity and Equality Tests")
    class EnumIdentityTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should be the same object when retrieved multiple times")
        void methodNotImplementedIdentity() {
            MercuryMessage first = MercuryMessage.METHOD_NOT_IMPLEMENTED;
            MercuryMessage second = MercuryMessage.METHOD_NOT_IMPLEMENTED;
            assertSame(first, second);
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should be the same object when retrieved multiple times")
        void runtimeExceptionIdentity() {
            MercuryMessage first = MercuryMessage.RUNTIME_EXCEPTION;
            MercuryMessage second = MercuryMessage.RUNTIME_EXCEPTION;
            assertSame(first, second);
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should equal itself")
        void methodNotImplementedEqualsSelf() {
            assertEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, MercuryMessage.METHOD_NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should equal itself")
        void runtimeExceptionEqualsSelf() {
            assertEquals(MercuryMessage.RUNTIME_EXCEPTION, MercuryMessage.RUNTIME_EXCEPTION);
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should equal an enum obtained via valueOf")
        void methodNotImplementedEqualsValueOf() {
            MercuryMessage fromValueOf = MercuryMessage.valueOf("METHOD_NOT_IMPLEMENTED");
            assertEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, fromValueOf);
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should equal an enum obtained via valueOf")
        void runtimeExceptionEqualsValueOf() {
            MercuryMessage fromValueOf = MercuryMessage.valueOf("RUNTIME_EXCEPTION");
            assertEquals(MercuryMessage.RUNTIME_EXCEPTION, fromValueOf);
        }

        @Test
        @DisplayName("Different enum constants should not be equal")
        void differentConstantsNotEqual() {
            assertNotEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, MercuryMessage.RUNTIME_EXCEPTION);
        }
    }

    @Nested
    @DisplayName("Enum Ordinal Tests")
    class EnumOrdinalTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should have ordinal value 0")
        void methodNotImplementedOrdinalIsZero() {
            assertEquals(0, MercuryMessage.METHOD_NOT_IMPLEMENTED.ordinal());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should have ordinal value 1")
        void runtimeExceptionOrdinalIsOne() {
            assertEquals(1, MercuryMessage.RUNTIME_EXCEPTION.ordinal());
        }

        @Test
        @DisplayName("Enum ordinal should match values array index")
        void ordinalMatchesValuesIndex() {
            MercuryMessage[] values = MercuryMessage.values();
            assertEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, values[0]);
            assertEquals(MercuryMessage.RUNTIME_EXCEPTION, values[1]);
        }

        @Test
        @DisplayName("Ordinals should be different for different constants")
        void ordinalsAreDifferent() {
            assertNotEquals(
                MercuryMessage.METHOD_NOT_IMPLEMENTED.ordinal(),
                MercuryMessage.RUNTIME_EXCEPTION.ordinal()
            );
        }
    }

    @Nested
    @DisplayName("Enum Naming Tests")
    class EnumNamingTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED enum name should be METHOD_NOT_IMPLEMENTED")
        void methodNotImplementedNameCorrect() {
            assertEquals("METHOD_NOT_IMPLEMENTED", MercuryMessage.METHOD_NOT_IMPLEMENTED.name());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION enum name should be RUNTIME_EXCEPTION")
        void runtimeExceptionNameCorrect() {
            assertEquals("RUNTIME_EXCEPTION", MercuryMessage.RUNTIME_EXCEPTION.name());
        }

        @Test
        @DisplayName("toString should return METHOD_NOT_IMPLEMENTED")
        void methodNotImplementedToStringCorrect() {
            assertEquals("METHOD_NOT_IMPLEMENTED", MercuryMessage.METHOD_NOT_IMPLEMENTED.toString());
        }

        @Test
        @DisplayName("toString should return RUNTIME_EXCEPTION")
        void runtimeExceptionToStringCorrect() {
            assertEquals("RUNTIME_EXCEPTION", MercuryMessage.RUNTIME_EXCEPTION.toString());
        }
    }

    @Nested
    @DisplayName("Interface Implementation Tests")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("MercuryMessage should implement MessageType interface")
        void implementsMessageType() {
            assertTrue(MercuryMessage.METHOD_NOT_IMPLEMENTED instanceof com.prx.commons.constants.httpstatus.type.MessageType);
        }

        @Test
        @DisplayName("getCode should be defined by MessageType interface")
        void getCodeMethodExists() throws NoSuchMethodException {
            assertNotNull(MercuryMessage.class.getMethod("getCode"));
        }

        @Test
        @DisplayName("getCodeToString should be defined by MessageType interface")
        void getCodeToStringMethodExists() throws NoSuchMethodException {
            assertNotNull(MercuryMessage.class.getMethod("getCodeToString"));
        }

        @Test
        @DisplayName("getStatus should be defined by MessageType interface")
        void getStatusMethodExists() throws NoSuchMethodException {
            assertNotNull(MercuryMessage.class.getMethod("getStatus"));
        }
    }

    @Nested
    @DisplayName("Enum Field Consistency Tests")
    class FieldConsistencyTests {

        @Test
        @DisplayName("Multiple calls to getCode should return the same value for METHOD_NOT_IMPLEMENTED")
        void methodNotImplementedCodeConsistency() {
            int first = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode();
            int second = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getCode should return the same value for RUNTIME_EXCEPTION")
        void runtimeExceptionCodeConsistency() {
            int first = MercuryMessage.RUNTIME_EXCEPTION.getCode();
            int second = MercuryMessage.RUNTIME_EXCEPTION.getCode();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getStatus should return the same value for METHOD_NOT_IMPLEMENTED")
        void methodNotImplementedStatusConsistency() {
            String first = MercuryMessage.METHOD_NOT_IMPLEMENTED.getStatus();
            String second = MercuryMessage.METHOD_NOT_IMPLEMENTED.getStatus();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getStatus should return the same value for RUNTIME_EXCEPTION")
        void runtimeExceptionStatusConsistency() {
            String first = MercuryMessage.RUNTIME_EXCEPTION.getStatus();
            String second = MercuryMessage.RUNTIME_EXCEPTION.getStatus();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getCodeToString should return the same value for METHOD_NOT_IMPLEMENTED")
        void methodNotImplementedCodeToStringConsistency() {
            String first = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString();
            String second = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getCodeToString should return the same value for RUNTIME_EXCEPTION")
        void runtimeExceptionCodeToStringConsistency() {
            String first = MercuryMessage.RUNTIME_EXCEPTION.getCodeToString();
            String second = MercuryMessage.RUNTIME_EXCEPTION.getCodeToString();
            assertEquals(first, second);
        }
    }

    @Nested
    @DisplayName("Enum Iteration Tests")
    class EnumIterationTests {

        @Test
        @DisplayName("values() should return an array with both constants in order")
        void valuesContainsBothConstants() {
            MercuryMessage[] values = MercuryMessage.values();
            assertEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, values[0]);
            assertEquals(MercuryMessage.RUNTIME_EXCEPTION, values[1]);
        }

        @Test
        @DisplayName("values() should return non-empty array")
        void valuesNonEmpty() {
            MercuryMessage[] values = MercuryMessage.values();
            assertNotEquals(0, values.length);
        }

        @Test
        @DisplayName("Iterate over values and verify both constants are found")
        void iterateValuesContainsAllConstants() {
            boolean foundMethod = false;
            boolean foundRuntime = false;

            for (MercuryMessage message : MercuryMessage.values()) {
                if (message == MercuryMessage.METHOD_NOT_IMPLEMENTED) {
                    foundMethod = true;
                }
                if (message == MercuryMessage.RUNTIME_EXCEPTION) {
                    foundRuntime = true;
                }
            }

            assertTrue(foundMethod);
            assertTrue(foundRuntime);
        }
    }

    @Nested
    @DisplayName("Enum Hash Code Tests")
    class EnumHashCodeTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should have consistent hash code")
        void methodNotImplementedHashCodeConsistent() {
            int hash1 = MercuryMessage.METHOD_NOT_IMPLEMENTED.hashCode();
            int hash2 = MercuryMessage.METHOD_NOT_IMPLEMENTED.hashCode();
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should have consistent hash code")
        void runtimeExceptionHashCodeConsistent() {
            int hash1 = MercuryMessage.RUNTIME_EXCEPTION.hashCode();
            int hash2 = MercuryMessage.RUNTIME_EXCEPTION.hashCode();
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Same enum instance should have same hash code")
        void sameInstanceSameHashCode() {
            MercuryMessage msg1 = MercuryMessage.METHOD_NOT_IMPLEMENTED;
            MercuryMessage msg2 = MercuryMessage.METHOD_NOT_IMPLEMENTED;
            assertEquals(msg1.hashCode(), msg2.hashCode());
        }

        @Test
        @DisplayName("Different constants may have different hash codes")
        void differentInstancesMayHaveDifferentHashCode() {
            // Note: Hash codes may theoretically be the same for different objects,
            // but it's unlikely for enum constants
            int hash1 = MercuryMessage.METHOD_NOT_IMPLEMENTED.hashCode();
            int hash2 = MercuryMessage.RUNTIME_EXCEPTION.hashCode();
            // We don't assert inequality here since it's not guaranteed
            assertNotNull(hash1);
            assertNotNull(hash2);
        }
    }

    @Nested
    @DisplayName("Enum Class Tests")
    class EnumClassTests {

        @Test
        @DisplayName("MercuryMessage should be an Enum class")
        void mercuryMessageIsEnum() {
            assertTrue(MercuryMessage.class.isEnum());
        }

        @Test
        @DisplayName("MercuryMessage class should not be null")
        void mercuryMessageClassNotNull() {
            assertNotNull(MercuryMessage.class);
        }

        @Test
        @DisplayName("MercuryMessage should have METHOD_NOT_IMPLEMENTED enum constant")
        void mercuryMessageHasMethodNotImplementedField() throws NoSuchFieldException {
            assertNotNull(MercuryMessage.class.getField("METHOD_NOT_IMPLEMENTED"));
        }

        @Test
        @DisplayName("MercuryMessage should have RUNTIME_EXCEPTION enum constant")
        void mercuryMessageHasRuntimeExceptionField() throws NoSuchFieldException {
            assertNotNull(MercuryMessage.class.getField("RUNTIME_EXCEPTION"));
        }

        @Test
        @DisplayName("MercuryMessage enum should have getCode method")
        void mercuryMessageHasGetCodeMethod() throws NoSuchMethodException {
            assertNotNull(MercuryMessage.class.getMethod("getCode"));
        }

        @Test
        @DisplayName("MercuryMessage enum should have getStatus method")
        void mercuryMessageHasGetStatusMethod() throws NoSuchMethodException {
            assertNotNull(MercuryMessage.class.getMethod("getStatus"));
        }

        @Test
        @DisplayName("MercuryMessage enum should have getCodeToString method")
        void mercuryMessageHasGetCodeToStringMethod() throws NoSuchMethodException {
            assertNotNull(MercuryMessage.class.getMethod("getCodeToString"));
        }
    }

    @Nested
    @DisplayName("Enum Comparison Tests")
    class EnumComparisonTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should not equal null")
        void methodNotImplementedNotEqualsNull() {
            assertNotEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, null);
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should not equal null")
        void runtimeExceptionNotEqualsNull() {
            assertNotEquals(MercuryMessage.RUNTIME_EXCEPTION, null);
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should not equal a string")
        void methodNotImplementedNotEqualsString() {
            assertNotEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, "METHOD_NOT_IMPLEMENTED");
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should not equal a string")
        void runtimeExceptionNotEqualsString() {
            assertNotEquals(MercuryMessage.RUNTIME_EXCEPTION, "RUNTIME_EXCEPTION");
        }

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED should not equal a different object type")
        void methodNotImplementedNotEqualsDifferentType() {
            assertNotEquals(MercuryMessage.METHOD_NOT_IMPLEMENTED, new Object());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION should not equal a different object type")
        void runtimeExceptionNotEqualsDifferentType() {
            assertNotEquals(MercuryMessage.RUNTIME_EXCEPTION, new Object());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("METHOD_NOT_IMPLEMENTED represents HTTP 501 Not Implemented status")
        void methodNotImplementedHttp501() {
            MercuryMessage message = MercuryMessage.METHOD_NOT_IMPLEMENTED;
            assertEquals(501, message.getCode());
            assertEquals("Method not implemented", message.getStatus());
        }

        @Test
        @DisplayName("RUNTIME_EXCEPTION represents HTTP 500 Internal Server Error status")
        void runtimeExceptionHttp500() {
            MercuryMessage message = MercuryMessage.RUNTIME_EXCEPTION;
            assertEquals(500, message.getCode());
            assertEquals("Internal server error", message.getStatus());
        }

        @Test
        @DisplayName("getCodeToString converts numeric code to string for serialization")
        void codeToStringForSerialization() {
            String code = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString();
            assertTrue(code.matches("\\d+"));
            assertEquals("501", code);
        }

        @Test
        @DisplayName("Status messages are user-friendly and descriptive")
        void statusMessagesAreDescriptive() {
            assertFalse(MercuryMessage.METHOD_NOT_IMPLEMENTED.getStatus().isEmpty());
            assertFalse(MercuryMessage.RUNTIME_EXCEPTION.getStatus().isEmpty());
        }

        @Test
        @DisplayName("Both constants use server error status codes (5xx range)")
        void serverErrorStatusCodes() {
            assertTrue(MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode() >= 500);
            assertTrue(MercuryMessage.RUNTIME_EXCEPTION.getCode() >= 500);
        }

        @Test
        @DisplayName("Constants can be used as MessageType in standardized error handling")
        void usableAsMessageType() {
            com.prx.commons.constants.httpstatus.type.MessageType methodNotImpl = MercuryMessage.METHOD_NOT_IMPLEMENTED;
            com.prx.commons.constants.httpstatus.type.MessageType runtimeException = MercuryMessage.RUNTIME_EXCEPTION;

            assertNotNull(methodNotImpl);
            assertNotNull(runtimeException);
            assertEquals(501, methodNotImpl.getCode());
            assertEquals(500, runtimeException.getCode());
        }
    }

    @Nested
    @DisplayName("Code to String Conversion Tests")
    class CodeToStringConversionTests {

        @Test
        @DisplayName("getCodeToString for METHOD_NOT_IMPLEMENTED should be numeric string")
        void methodNotImplementedCodeToStringIsNumeric() {
            String codeStr = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString();
            assertTrue(codeStr.matches("^\\d+$"));
        }

        @Test
        @DisplayName("getCodeToString for RUNTIME_EXCEPTION should be numeric string")
        void runtimeExceptionCodeToStringIsNumeric() {
            String codeStr = MercuryMessage.RUNTIME_EXCEPTION.getCodeToString();
            assertTrue(codeStr.matches("^\\d+$"));
        }

        @Test
        @DisplayName("getCodeToString should match String.valueOf(getCode)")
        void codeToStringMatchesValueOf() {
            String methodStr = MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString();
            String methodValueOf = String.valueOf(MercuryMessage.METHOD_NOT_IMPLEMENTED.getCode());
            assertEquals(methodStr, methodValueOf);

            String runtimeStr = MercuryMessage.RUNTIME_EXCEPTION.getCodeToString();
            String runtimeValueOf = String.valueOf(MercuryMessage.RUNTIME_EXCEPTION.getCode());
            assertEquals(runtimeStr, runtimeValueOf);
        }

        @Test
        @DisplayName("getCodeToString should not be empty")
        void codeToStringNotEmpty() {
            assertFalse(MercuryMessage.METHOD_NOT_IMPLEMENTED.getCodeToString().isEmpty());
            assertFalse(MercuryMessage.RUNTIME_EXCEPTION.getCodeToString().isEmpty());
        }
    }
}

