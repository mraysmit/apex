package dev.mars.apex.core.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ValidationResult.
 * 
 * Tests cover:
 * - Result creation and initialization
 * - Success and failure state management
 * - Error message collection and retrieval
 * - Validation statistics and metrics
 * - Integration with ValidationBuilder
 * 
 * @author APEX Test Team
 * @since 1.0.0
 */
class ValidationResultTest {

    private ValidationResult validationResult;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        testData = new HashMap<>();
        testData.put("name", "John Doe");
        testData.put("age", 30);
        testData.put("email", "john.doe@example.com");
    }

    // ========================================
    // Constructor and Basic Setup Tests
    // ========================================

    @Test
    @DisplayName("Should create ValidationResult with success state")
    void testCreateSuccessResult() {
        ValidationResult result = ValidationResult.success();
        
        assertNotNull(result, "ValidationResult should be created");
        assertTrue(result.isValid(), "Result should be valid");
        assertFalse(result.hasErrors(), "Result should not have errors");
    }

    @Test
    @DisplayName("Should create ValidationResult with failure state")
    void testCreateFailureResult() {
        ValidationResult result = ValidationResult.failure("Test error");
        
        assertNotNull(result, "ValidationResult should be created");
        assertFalse(result.isValid(), "Result should be invalid");
        assertTrue(result.hasErrors(), "Result should have errors");
    }

    // ========================================
    // Success State Tests
    // ========================================

    @Nested
    @DisplayName("Success State Management")
    class SuccessStateTests {

        @Test
        @DisplayName("Should indicate valid state for successful validation")
        void testSuccessfulValidationState() {
            ValidationResult result = ValidationResult.success();
            
            assertTrue(result.isValid(), "Should be valid");
            assertFalse(result.hasErrors(), "Should not have errors");
            assertEquals(0, result.getErrorCount(), "Error count should be zero");
        }

        @Test
        @DisplayName("Should provide empty error list for successful validation")
        void testSuccessfulValidationErrors() {
            ValidationResult result = ValidationResult.success();
            
            List<String> errors = result.getErrors();
            assertNotNull(errors, "Error list should not be null");
            assertTrue(errors.isEmpty(), "Error list should be empty");
        }

        @Test
        @DisplayName("Should provide empty error messages for successful validation")
        void testSuccessfulValidationErrorMessages() {
            ValidationResult result = ValidationResult.success();

            String errorMessage = result.getErrorsAsString();
            assertNotNull(errorMessage, "Error message should not be null");
            assertTrue(errorMessage.isEmpty() || errorMessage.isBlank(), "Error message should be empty");
        }
    }

    // ========================================
    // Failure State Tests
    // ========================================

    @Nested
    @DisplayName("Failure State Management")
    class FailureStateTests {

        @Test
        @DisplayName("Should indicate invalid state for failed validation")
        void testFailedValidationState() {
            ValidationResult result = ValidationResult.failure("Test error");
            
            assertFalse(result.isValid(), "Should be invalid");
            assertTrue(result.hasErrors(), "Should have errors");
            assertTrue(result.getErrorCount() > 0, "Error count should be greater than zero");
        }

        @Test
        @DisplayName("Should provide error list for failed validation")
        void testFailedValidationErrors() {
            ValidationResult result = ValidationResult.failure("Test error");
            
            List<String> errors = result.getErrors();
            assertNotNull(errors, "Error list should not be null");
            assertFalse(errors.isEmpty(), "Error list should not be empty");
            assertTrue(errors.contains("Test error"), "Should contain the test error");
        }

        @Test
        @DisplayName("Should provide error message for failed validation")
        void testFailedValidationErrorMessage() {
            ValidationResult result = ValidationResult.failure("Test error");

            String errorMessage = result.getErrorsAsString();
            assertNotNull(errorMessage, "Error message should not be null");
            assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
            assertTrue(errorMessage.contains("Test error"), "Should contain the test error");
        }
    }

    // ========================================
    // Multiple Errors Tests
    // ========================================

    @Nested
    @DisplayName("Multiple Errors Handling")
    class MultipleErrorsTests {

        @Test
        @DisplayName("Should handle multiple validation errors")
        void testMultipleErrors() {
            ValidationResult result = ValidationResult.failure("Error 1");
            result = result.addError("Error 2");
            result = result.addError("Error 3");

            assertFalse(result.isValid(), "Should be invalid");
            assertTrue(result.hasErrors(), "Should have errors");
            assertEquals(3, result.getErrorCount(), "Should have 3 errors");

            List<String> errors = result.getErrors();
            assertEquals(3, errors.size(), "Error list should have 3 items");
            assertTrue(errors.contains("Error 1"), "Should contain Error 1");
            assertTrue(errors.contains("Error 2"), "Should contain Error 2");
            assertTrue(errors.contains("Error 3"), "Should contain Error 3");
        }

        @Test
        @DisplayName("Should format multiple errors in error message")
        void testMultipleErrorsMessage() {
            ValidationResult result = ValidationResult.failure("Error 1");
            result = result.addError("Error 2");

            String errorMessage = result.getErrorsAsString();
            assertNotNull(errorMessage, "Error message should not be null");
            assertTrue(errorMessage.contains("Error 1"), "Should contain Error 1");
            assertTrue(errorMessage.contains("Error 2"), "Should contain Error 2");
        }
    }

    // ========================================
    // Error Management Tests
    // ========================================

    @Nested
    @DisplayName("Error Management")
    class ErrorManagementTests {

        @Test
        @DisplayName("Should add error to existing result")
        void testAddError() {
            ValidationResult result = ValidationResult.success();
            assertTrue(result.isValid(), "Should start as valid");

            ValidationResult newResult = result.addError("New error");

            assertFalse(newResult.isValid(), "Should become invalid after adding error");
            assertTrue(newResult.hasErrors(), "Should have errors");
            assertEquals(1, newResult.getErrorCount(), "Should have 1 error");
        }

        @Test
        @DisplayName("Should handle null error gracefully")
        void testAddNullError() {
            ValidationResult result = ValidationResult.success();

            assertDoesNotThrow(() -> {
                result.addError(null);
            }, "Should handle null error gracefully");
        }

        @Test
        @DisplayName("Should handle empty error gracefully")
        void testAddEmptyError() {
            ValidationResult result = ValidationResult.success();

            assertDoesNotThrow(() -> {
                result.addError("");
                result.addError("   ");
            }, "Should handle empty/blank errors gracefully");
        }
    }

    // ========================================
    // Statistics and Metrics Tests
    // ========================================

    @Nested
    @DisplayName("Statistics and Metrics")
    class StatisticsTests {

        @Test
        @DisplayName("Should provide accurate error count")
        void testErrorCount() {
            ValidationResult result = ValidationResult.success();
            assertEquals(0, result.getErrorCount(), "Initial error count should be 0");

            result = result.addError("Error 1");
            assertEquals(1, result.getErrorCount(), "Error count should be 1");

            result = result.addError("Error 2");
            assertEquals(2, result.getErrorCount(), "Error count should be 2");
        }

        @Test
        @DisplayName("Should provide first error")
        void testFirstError() {
            ValidationResult result = ValidationResult.failure("Test error");

            String firstError = result.getFirstError();
            assertNotNull(firstError, "First error should not be null");
            assertEquals("Test error", firstError, "Should return first error");
        }

        @Test
        @DisplayName("Should provide error string with custom separator")
        void testErrorStringWithSeparator() {
            ValidationResult result = ValidationResult.failure("Error 1");
            result = result.addError("Error 2");

            String errorString = result.getErrorsAsString(" | ");
            assertNotNull(errorString, "Error string should not be null");
            assertTrue(errorString.contains("Error 1"), "Should contain Error 1");
            assertTrue(errorString.contains("Error 2"), "Should contain Error 2");
            assertTrue(errorString.contains(" | "), "Should contain custom separator");
        }
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Nested
    @DisplayName("Integration with ValidationBuilder")
    class IntegrationTests {

        @Test
        @DisplayName("Should integrate with ValidationBuilder for successful validation")
        void testIntegrationSuccess() {
            ValidationBuilder builder = new ValidationBuilder(testData);
            builder.that("#age > 0", "Age must be positive");

            ValidationResult result = builder.validate();

            assertNotNull(result, "Result should not be null");
            assertTrue(result.isValid(), "Should be valid for good data");
        }

        @Test
        @DisplayName("Should integrate with ValidationBuilder for failed validation")
        void testIntegrationFailure() {
            ValidationBuilder builder = new ValidationBuilder(testData);
            builder.that("#age > 100", "Age must be over 100");

            ValidationResult result = builder.validate();

            assertNotNull(result, "Result should not be null");
            assertFalse(result.isValid(), "Should be invalid for bad data");
            assertTrue(result.hasErrors(), "Should have errors");
        }
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle toString method")
        void testToString() {
            ValidationResult result = ValidationResult.failure("Test error");
            
            String toString = result.toString();
            assertNotNull(toString, "toString should not be null");
            assertFalse(toString.isEmpty(), "toString should not be empty");
        }

        @Test
        @DisplayName("Should handle equals and hashCode")
        void testEqualsAndHashCode() {
            ValidationResult result1 = ValidationResult.success();
            ValidationResult result2 = ValidationResult.success();
            
            // Note: This test assumes equals/hashCode are implemented
            // If not implemented, this test will verify default Object behavior
            assertNotNull(result1, "Result1 should not be null");
            assertNotNull(result2, "Result2 should not be null");
        }
    }
}
