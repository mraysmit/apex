package dev.mars.apex.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the PropertyResolver utility class.
 * 
 * @author Mark A Ray-Smith Cityline Ltd
 * @since 2025-01-19
 */
@DisplayName("PropertyResolver Tests")
public class PropertyResolverTest {

    private static final String TEST_PROP_1 = "TEST_APEX_PROP_1";
    private static final String TEST_PROP_2 = "TEST_APEX_PROP_2";
    private static final String TEST_PASSWORD = "TEST_APEX_PASSWORD";

    @BeforeEach
    void setUp() {
        // Clear any test properties
        System.clearProperty(TEST_PROP_1);
        System.clearProperty(TEST_PROP_2);
        System.clearProperty(TEST_PASSWORD);
    }

    @AfterEach
    void tearDown() {
        // Clean up test properties
        System.clearProperty(TEST_PROP_1);
        System.clearProperty(TEST_PROP_2);
        System.clearProperty(TEST_PASSWORD);
    }

    @Nested
    @DisplayName("Basic Resolution")
    class BasicResolution {

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(PropertyResolver.resolve(null));
        }

        @Test
        @DisplayName("Should return unchanged value without placeholders")
        void shouldReturnUnchangedValueWithoutPlaceholders() {
            String value = "hello world";
            assertEquals(value, PropertyResolver.resolve(value));
        }

        @Test
        @DisplayName("Should resolve ${VAR} from system property")
        void shouldResolveCurlyBraceFromSystemProperty() {
            System.setProperty(TEST_PROP_1, "resolved-value");
            
            String result = PropertyResolver.resolve("prefix-${TEST_APEX_PROP_1}-suffix");
            
            assertEquals("prefix-resolved-value-suffix", result);
        }

        @Test
        @DisplayName("Should resolve $(VAR) from system property")
        void shouldResolveParenthesisFromSystemProperty() {
            System.setProperty(TEST_PROP_1, "paren-value");
            
            String result = PropertyResolver.resolve("prefix-$(TEST_APEX_PROP_1)-suffix");
            
            assertEquals("prefix-paren-value-suffix", result);
        }

        @Test
        @DisplayName("Should resolve multiple placeholders")
        void shouldResolveMultiplePlaceholders() {
            System.setProperty(TEST_PROP_1, "value1");
            System.setProperty(TEST_PROP_2, "value2");
            
            String result = PropertyResolver.resolve("${TEST_APEX_PROP_1} and ${TEST_APEX_PROP_2}");
            
            assertEquals("value1 and value2", result);
        }

        @Test
        @DisplayName("Should resolve mixed placeholder styles")
        void shouldResolveMixedPlaceholderStyles() {
            System.setProperty(TEST_PROP_1, "curly");
            System.setProperty(TEST_PROP_2, "paren");
            
            String result = PropertyResolver.resolve("${TEST_APEX_PROP_1}-$(TEST_APEX_PROP_2)");
            
            assertEquals("curly-paren", result);
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should use default value when property not found - curly braces")
        void shouldUseDefaultValueCurlyBraces() {
            String result = PropertyResolver.resolve("${NONEXISTENT_PROP:default-value}");
            
            assertEquals("default-value", result);
        }

        @Test
        @DisplayName("Should use default value when property not found - parenthesis")
        void shouldUseDefaultValueParenthesis() {
            String result = PropertyResolver.resolve("$(NONEXISTENT_PROP:default-value)");
            
            assertEquals("default-value", result);
        }

        @Test
        @DisplayName("Should prefer actual value over default")
        void shouldPreferActualValueOverDefault() {
            System.setProperty(TEST_PROP_1, "actual-value");
            
            String result = PropertyResolver.resolve("${TEST_APEX_PROP_1:default-value}");
            
            assertEquals("actual-value", result);
        }

        @Test
        @DisplayName("Should handle empty default value")
        void shouldHandleEmptyDefaultValue() {
            String result = PropertyResolver.resolve("${NONEXISTENT_PROP:}");
            
            assertEquals("", result);
        }

        @Test
        @DisplayName("Should handle default value with colons")
        void shouldHandleDefaultValueWithColons() {
            String result = PropertyResolver.resolve("${NONEXISTENT_PROP:http://localhost:8080}");
            
            assertEquals("http://localhost:8080", result);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw exception for unresolved placeholder when requested")
        void shouldThrowExceptionForUnresolvedPlaceholder() {
            PropertyResolver.PropertyResolutionException exception = assertThrows(
                    PropertyResolver.PropertyResolutionException.class,
                    () -> PropertyResolver.resolve("${NONEXISTENT_REQUIRED_PROP}", true)
            );
            
            assertTrue(exception.getMessage().contains("Property not found"));
            assertTrue(exception.getMessage().contains("NONEXISTENT_REQUIRED_PROP"));
        }

        @Test
        @DisplayName("Should leave placeholder when throwOnUnresolved is false")
        void shouldLeavePlaceholderWhenNotThrowing() {
            String result = PropertyResolver.resolve("${NONEXISTENT_PROP}", false);
            
            assertEquals("${NONEXISTENT_PROP}", result);
        }

        @Test
        @DisplayName("Default resolve() should throw on unresolved")
        void defaultResolveShouldThrowOnUnresolved() {
            assertThrows(
                    PropertyResolver.PropertyResolutionException.class,
                    () -> PropertyResolver.resolve("${NONEXISTENT_REQUIRED_PROP}")
            );
        }
    }

    @Nested
    @DisplayName("Sensitive Property Detection")
    class SensitivePropertyDetection {

        @Test
        @DisplayName("Should detect password as sensitive")
        void shouldDetectPasswordAsSensitive() {
            assertTrue(PropertyResolver.isSensitiveProperty("DB_PASSWORD"));
            assertTrue(PropertyResolver.isSensitiveProperty("password"));
            assertTrue(PropertyResolver.isSensitiveProperty("USER_PASSWORD"));
        }

        @Test
        @DisplayName("Should detect secret as sensitive")
        void shouldDetectSecretAsSensitive() {
            assertTrue(PropertyResolver.isSensitiveProperty("API_SECRET"));
            assertTrue(PropertyResolver.isSensitiveProperty("secret"));
            assertTrue(PropertyResolver.isSensitiveProperty("SECRET_KEY"));
        }

        @Test
        @DisplayName("Should detect token as sensitive")
        void shouldDetectTokenAsSensitive() {
            assertTrue(PropertyResolver.isSensitiveProperty("AUTH_TOKEN"));
            assertTrue(PropertyResolver.isSensitiveProperty("token"));
            assertTrue(PropertyResolver.isSensitiveProperty("ACCESS_TOKEN"));
        }

        @Test
        @DisplayName("Should detect key as sensitive")
        void shouldDetectKeyAsSensitive() {
            assertTrue(PropertyResolver.isSensitiveProperty("API_KEY"));
            assertTrue(PropertyResolver.isSensitiveProperty("key"));
            assertTrue(PropertyResolver.isSensitiveProperty("PRIVATE_KEY"));
        }

        @Test
        @DisplayName("Should detect pwd as sensitive")
        void shouldDetectPwdAsSensitive() {
            assertTrue(PropertyResolver.isSensitiveProperty("DB_PWD"));
            assertTrue(PropertyResolver.isSensitiveProperty("pwd"));
        }

        @Test
        @DisplayName("Should detect credential as sensitive")
        void shouldDetectCredentialAsSensitive() {
            assertTrue(PropertyResolver.isSensitiveProperty("USER_CREDENTIAL"));
            assertTrue(PropertyResolver.isSensitiveProperty("credential"));
        }

        @Test
        @DisplayName("Should not detect non-sensitive properties")
        void shouldNotDetectNonSensitiveProperties() {
            assertFalse(PropertyResolver.isSensitiveProperty("DB_HOST"));
            assertFalse(PropertyResolver.isSensitiveProperty("PORT"));
            assertFalse(PropertyResolver.isSensitiveProperty("USERNAME"));
            assertFalse(PropertyResolver.isSensitiveProperty("DATABASE_URL"));
        }

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(PropertyResolver.isSensitiveProperty(null));
        }
    }

    @Nested
    @DisplayName("Value Masking")
    class ValueMasking {

        @Test
        @DisplayName("Should mask values containing password placeholders")
        void shouldMaskValuesWithPasswordPlaceholders() {
            String result = PropertyResolver.maskSensitiveValue("connection: ${DB_PASSWORD}");
            
            assertEquals("[MASKED_VALUE_WITH_SENSITIVE_PLACEHOLDERS]", result);
        }

        @Test
        @DisplayName("Should not mask values without sensitive content")
        void shouldNotMaskNonSensitiveValues() {
            String value = "connection: ${DB_HOST}:${DB_PORT}";
            
            assertEquals(value, PropertyResolver.maskSensitiveValue(value));
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullMaskInput() {
            assertNull(PropertyResolver.maskSensitiveValue(null));
        }
    }

    @Nested
    @DisplayName("Placeholder Detection")
    class PlaceholderDetection {

        @Test
        @DisplayName("Should detect curly brace placeholders")
        void shouldDetectCurlyBracePlaceholders() {
            assertTrue(PropertyResolver.containsPlaceholders("${VAR}"));
            assertTrue(PropertyResolver.containsPlaceholders("prefix ${VAR} suffix"));
        }

        @Test
        @DisplayName("Should detect parenthesis placeholders")
        void shouldDetectParenthesisPlaceholders() {
            assertTrue(PropertyResolver.containsPlaceholders("$(VAR)"));
            assertTrue(PropertyResolver.containsPlaceholders("prefix $(VAR) suffix"));
        }

        @Test
        @DisplayName("Should return false for no placeholders")
        void shouldReturnFalseForNoPlaceholders() {
            assertFalse(PropertyResolver.containsPlaceholders("plain text"));
            assertFalse(PropertyResolver.containsPlaceholders(""));
        }

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNullContains() {
            assertFalse(PropertyResolver.containsPlaceholders(null));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            assertEquals("", PropertyResolver.resolve(""));
        }

        @Test
        @DisplayName("Should handle incomplete placeholder - no closing brace")
        void shouldHandleIncompleteNoClosingBrace() {
            // Should not match as a placeholder
            assertEquals("${VAR", PropertyResolver.resolve("${VAR", false));
        }

        @Test
        @DisplayName("Should handle consecutive placeholders")
        void shouldHandleConsecutivePlaceholders() {
            System.setProperty(TEST_PROP_1, "a");
            System.setProperty(TEST_PROP_2, "b");
            
            String result = PropertyResolver.resolve("${TEST_APEX_PROP_1}${TEST_APEX_PROP_2}");
            
            assertEquals("ab", result);
        }

        @Test
        @DisplayName("Should handle special characters in resolved value")
        void shouldHandleSpecialCharactersInResolvedValue() {
            System.setProperty(TEST_PROP_1, "value$with\\special|chars");
            
            String result = PropertyResolver.resolve("${TEST_APEX_PROP_1}");
            
            assertEquals("value$with\\special|chars", result);
        }
    }
}
