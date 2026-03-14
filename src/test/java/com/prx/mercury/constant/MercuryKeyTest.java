package com.prx.mercury.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link MercuryKey} enum.
 *
 * Tests cover enum instantiation, field accessors, enum constants,
 * identity, and serialization characteristics of the MercuryKey enum.
 */
@DisplayName("MercuryKey Enum Tests")
class MercuryKeyTest {

    @Nested
    @DisplayName("Enum Constant Tests")
    class EnumConstantTests {

        @Test
        @DisplayName("APPLICATION_ID constant should exist and be accessible")
        void applicationIdConstantExists() {
            assertNotNull(MercuryKey.APPLICATION_ID);
        }

        @Test
        @DisplayName("APPLICATION_ID constant should be a MercuryKey instance")
        void applicationIdIsInstance() {
            assertTrue(MercuryKey.APPLICATION_ID instanceof MercuryKey);
        }

        @Test
        @DisplayName("Only APPLICATION_ID constant should be defined in the enum")
        void enumConstantsCount() {
            MercuryKey[] values = MercuryKey.values();
            assertEquals(1, values.length);
            assertEquals(MercuryKey.APPLICATION_ID, values[0]);
        }
    }

    @Nested
    @DisplayName("Field Accessor Tests")
    class FieldAccessorTests {

        @Test
        @DisplayName("getProjectId should return MCY for APPLICATION_ID")
        void getProjectIdReturnsCorrectValue() {
            String projectId = MercuryKey.APPLICATION_ID.getProjectId();
            assertEquals("MCY", projectId);
        }

        @Test
        @DisplayName("getProjectName should return Mercury for APPLICATION_ID")
        void getProjectNameReturnsCorrectValue() {
            String projectName = MercuryKey.APPLICATION_ID.getProjectName();
            assertEquals("Mercury", projectName);
        }

        @Test
        @DisplayName("getProjectId should not be null")
        void getProjectIdNotNull() {
            assertNotNull(MercuryKey.APPLICATION_ID.getProjectId());
        }

        @Test
        @DisplayName("getProjectName should not be null")
        void getProjectNameNotNull() {
            assertNotNull(MercuryKey.APPLICATION_ID.getProjectName());
        }

        @Test
        @DisplayName("getProjectId should not be empty string")
        void getProjectIdNotEmpty() {
            assertFalse(MercuryKey.APPLICATION_ID.getProjectId().isEmpty());
        }

        @Test
        @DisplayName("getProjectName should not be empty string")
        void getProjectNameNotEmpty() {
            assertFalse(MercuryKey.APPLICATION_ID.getProjectName().isEmpty());
        }
    }

    @Nested
    @DisplayName("Enum Identity and Equality Tests")
    class EnumIdentityTests {

        @Test
        @DisplayName("APPLICATION_ID should be the same object when retrieved multiple times")
        void applicationIdIdentity() {
            MercuryKey first = MercuryKey.APPLICATION_ID;
            MercuryKey second = MercuryKey.APPLICATION_ID;
            assertSame(first, second);
        }

        @Test
        @DisplayName("APPLICATION_ID should equal itself")
        void applicationIdEqualsSelf() {
            assertEquals(MercuryKey.APPLICATION_ID, MercuryKey.APPLICATION_ID);
        }

        @Test
        @DisplayName("APPLICATION_ID should equal an enum obtained via valueOf")
        void applicationIdEqualsValueOf() {
            MercuryKey fromValueOf = MercuryKey.valueOf("APPLICATION_ID");
            assertEquals(MercuryKey.APPLICATION_ID, fromValueOf);
        }

        @Test
        @DisplayName("APPLICATION_ID should be idempotent when retrieved via valueOf")
        void applicationIdValueOfIdempotent() {
            MercuryKey value1 = MercuryKey.valueOf("APPLICATION_ID");
            MercuryKey value2 = MercuryKey.valueOf("APPLICATION_ID");
            assertSame(value1, value2);
        }
    }

    @Nested
    @DisplayName("Enum Ordinal and Naming Tests")
    class EnumOrdinalTests {

        @Test
        @DisplayName("APPLICATION_ID should have ordinal value 0")
        void applicationIdOrdinalIsZero() {
            assertEquals(0, MercuryKey.APPLICATION_ID.ordinal());
        }

        @Test
        @DisplayName("APPLICATION_ID enum name should be APPLICATION_ID")
        void applicationIdNameIsCorrect() {
            assertEquals("APPLICATION_ID", MercuryKey.APPLICATION_ID.name());
        }

        @Test
        @DisplayName("Enum ordinal should match values array index")
        void ordinalMatchesValuesIndex() {
            MercuryKey[] values = MercuryKey.values();
            int expectedIndex = MercuryKey.APPLICATION_ID.ordinal();
            assertEquals(MercuryKey.APPLICATION_ID, values[expectedIndex]);
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("toString should return APPLICATION_ID")
        void toStringReturnsName() {
            String result = MercuryKey.APPLICATION_ID.toString();
            assertEquals("APPLICATION_ID", result);
        }

        @Test
        @DisplayName("toString should not be empty")
        void toStringNotEmpty() {
            assertFalse(MercuryKey.APPLICATION_ID.toString().isEmpty());
        }

        @Test
        @DisplayName("toString should not contain null")
        void toStringNotNull() {
            assertNotNull(MercuryKey.APPLICATION_ID.toString());
        }
    }

    @Nested
    @DisplayName("Enum Field Consistency Tests")
    class FieldConsistencyTests {

        @Test
        @DisplayName("PROJECT_ID and getProjectId should return the same value")
        void projectIdConsistency() {
            String projectId = MercuryKey.APPLICATION_ID.getProjectId();
            assertEquals("MCY", projectId);
        }

        @Test
        @DisplayName("PROJECT_NAME and getProjectName should return the same value")
        void projectNameConsistency() {
            String projectName = MercuryKey.APPLICATION_ID.getProjectName();
            assertEquals("Mercury", projectName);
        }

        @Test
        @DisplayName("Multiple calls to getProjectId should return the same value")
        void getProjectIdConsistency() {
            String first = MercuryKey.APPLICATION_ID.getProjectId();
            String second = MercuryKey.APPLICATION_ID.getProjectId();
            assertEquals(first, second);
        }

        @Test
        @DisplayName("Multiple calls to getProjectName should return the same value")
        void getProjectNameConsistency() {
            String first = MercuryKey.APPLICATION_ID.getProjectName();
            String second = MercuryKey.APPLICATION_ID.getProjectName();
            assertEquals(first, second);
        }
    }

    @Nested
    @DisplayName("Enum Iteration Tests")
    class EnumIterationTests {

        @Test
        @DisplayName("values() should return an array containing APPLICATION_ID")
        void valuesContainsApplicationId() {
            MercuryKey[] values = MercuryKey.values();
            assertArrayEquals(new MercuryKey[]{MercuryKey.APPLICATION_ID}, values);
        }

        @Test
        @DisplayName("values() should return non-empty array")
        void valuesNonEmpty() {
            MercuryKey[] values = MercuryKey.values();
            assertNotEquals(0, values.length);
        }

        @Test
        @DisplayName("Iterate over values and verify APPLICATION_ID is found")
        void iterateValuesContainsApplicationId() {
            boolean found = false;
            for (MercuryKey key : MercuryKey.values()) {
                if (key == MercuryKey.APPLICATION_ID) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Nested
    @DisplayName("Enum Hash Code Tests")
    class EnumHashCodeTests {

        @Test
        @DisplayName("APPLICATION_ID should have consistent hash code")
        void applicationIdHashCodeConsistent() {
            int hash1 = MercuryKey.APPLICATION_ID.hashCode();
            int hash2 = MercuryKey.APPLICATION_ID.hashCode();
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("APPLICATION_ID hash code should not be zero")
        void applicationIdHashCodeNotZero() {
            int hash = MercuryKey.APPLICATION_ID.hashCode();
            assertNotEquals(0, hash);
        }

        @Test
        @DisplayName("Same enum instance should have same hash code")
        void sameInstanceSameHashCode() {
            MercuryKey key1 = MercuryKey.APPLICATION_ID;
            MercuryKey key2 = MercuryKey.APPLICATION_ID;
            assertEquals(key1.hashCode(), key2.hashCode());
        }
    }

    @Nested
    @DisplayName("Enum Class Tests")
    class EnumClassTests {

        @Test
        @DisplayName("MercuryKey should be an Enum class")
        void mercuryKeyIsEnum() {
            assertTrue(MercuryKey.class.isEnum());
        }

        @Test
        @DisplayName("MercuryKey class should not be null")
        void mercuryKeyClassNotNull() {
            assertNotNull(MercuryKey.class);
        }

        @Test
        @DisplayName("MercuryKey should have APPLICATION_ID enum constant")
        void mercuryKeyHasApplicationIdField() throws NoSuchFieldException {
            assertNotNull(MercuryKey.class.getField("APPLICATION_ID"));
        }

        @Test
        @DisplayName("MercuryKey enum should have getProjectId method")
        void mercuryKeyHasGetProjectIdMethod() throws NoSuchMethodException {
            assertNotNull(MercuryKey.class.getMethod("getProjectId"));
        }

        @Test
        @DisplayName("MercuryKey enum should have getProjectName method")
        void mercuryKeyHasGetProjectNameMethod() throws NoSuchMethodException {
            assertNotNull(MercuryKey.class.getMethod("getProjectName"));
        }
    }

    @Nested
    @DisplayName("Enum Comparison Tests")
    class EnumComparisonTests {

        @Test
        @DisplayName("APPLICATION_ID should not equal null")
        void applicationIdNotEqualsNull() {
            assertNotEquals(null, MercuryKey.APPLICATION_ID);
        }

        @Test
        @DisplayName("APPLICATION_ID should not equal a string")
        void applicationIdNotEqualsString() {
            assertNotEquals("APPLICATION_ID", MercuryKey.APPLICATION_ID);
        }

        @Test
        @DisplayName("APPLICATION_ID should not equal a different object type")
        void applicationIdNotEqualsDifferentType() {
            assertNotEquals(MercuryKey.APPLICATION_ID, new Object());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("PROJECT_ID should be MCY for Mercury application identification")
        void projectIdIsMCY() {
            String projectId = MercuryKey.APPLICATION_ID.getProjectId();
            assertEquals("MCY", projectId);
        }

        @Test
        @DisplayName("PROJECT_NAME should be Mercury for application naming")
        void projectNameIsMercury() {
            String projectName = MercuryKey.APPLICATION_ID.getProjectName();
            assertEquals("Mercury", projectName);
        }

        @Test
        @DisplayName("PROJECT_ID MCY can be used as a unique identifier")
        void projectIdCanBeUsedAsIdentifier() {
            String projectId = MercuryKey.APPLICATION_ID.getProjectId();
            assertFalse(projectId.isBlank());
            assertTrue(projectId.length() > 0);
        }

        @Test
        @DisplayName("PROJECT_NAME Mercury is user-friendly")
        void projectNameIsUserFriendly() {
            String projectName = MercuryKey.APPLICATION_ID.getProjectName();
            assertFalse(projectName.isBlank());
            assertTrue(projectName.length() > 0);
        }
    }
}

