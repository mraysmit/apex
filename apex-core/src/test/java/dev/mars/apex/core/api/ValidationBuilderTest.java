package dev.mars.apex.core.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ValidationBuilder.
 * 
 * Tests cover:
 * - Builder pattern implementation
 * - Fluent API method chaining
 * - Validation rule configuration
 * - Error handling and edge cases
 * - Integration with ValidationResult
 * 
 * @author APEX Test Team
 * @since 1.0.0
 */
class ValidationBuilderTest {

    private ValidationBuilder validationBuilder;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        testData = new HashMap<>();
        testData.put("name", "John Doe");
        testData.put("age", 30);
        testData.put("email", "john.doe@example.com");
        testData.put("salary", 50000.0);
        validationBuilder = new ValidationBuilder(testData);
    }

    // ========================================
    // Constructor and Basic Setup Tests
    // ========================================

    @Test
    @DisplayName("Should create ValidationBuilder with test data")
    void testDataConstructor() {
        ValidationBuilder builder = new ValidationBuilder(testData);
        assertNotNull(builder, "ValidationBuilder should be created successfully");
    }

    @Test
    @DisplayName("Should create ValidationBuilder with object data")
    void testObjectConstructor() {
        Object testObject = "test";
        ValidationBuilder builder = new ValidationBuilder(testObject);
        assertNotNull(builder, "ValidationBuilder should be created successfully");
    }

    // ========================================
    // Fluent API Method Chaining Tests
    // ========================================

    @Nested
    @DisplayName("Fluent API Method Chaining")
    class FluentApiTests {

        @Test
        @DisplayName("Should support method chaining for rule addition")
        void testMethodChaining() {
            ValidationBuilder result = validationBuilder
                .that("#name != null", "Name is required")
                .that("#age > 0", "Age must be positive")
                .that("#email != null && #email.contains('@')", "Valid email required");

            assertSame(validationBuilder, result, "Method chaining should return same instance");
        }

        @Test
        @DisplayName("Should support complex method chaining")
        void testComplexMethodChaining() {
            assertDoesNotThrow(() -> {
                validationBuilder
                    .that("#name != null", "Name is required")
                    .that("#age >= 18", "Must be adult")
                    .that("#salary > 0", "Salary must be positive")
                    .minimumAge(18)
                    .emailRequired();
            }, "Complex method chaining should work without errors");
        }
    }

    // ========================================
    // Rule Addition Tests
    // ========================================

    @Nested
    @DisplayName("Rule Addition")
    class RuleAdditionTests {

        @Test
        @DisplayName("Should add single validation rule")
        void testAddSingleRule() {
            assertDoesNotThrow(() -> {
                validationBuilder.that("#name != null", "Name is required");
            }, "Should add single rule without error");
        }

        @Test
        @DisplayName("Should add multiple validation rules")
        void testAddMultipleRules() {
            assertDoesNotThrow(() -> {
                validationBuilder
                    .that("#name != null", "Name is required")
                    .that("#age > 0", "Age must be positive")
                    .that("#email != null", "Email is required");
            }, "Should add multiple rules without error");
        }

        @Test
        @DisplayName("Should handle null condition gracefully")
        void testAddRuleWithNullCondition() {
            assertDoesNotThrow(() -> {
                validationBuilder.that(null, "Test message");
            }, "Should handle null condition gracefully");
        }

        @Test
        @DisplayName("Should handle empty condition gracefully")
        void testAddRuleWithEmptyCondition() {
            assertDoesNotThrow(() -> {
                validationBuilder.that("", "Test message");
            }, "Should handle empty condition gracefully");
        }

        @Test
        @DisplayName("Should handle null message gracefully")
        void testAddRuleWithNullMessage() {
            assertDoesNotThrow(() -> {
                validationBuilder.that("#name != null", null);
            }, "Should handle null message gracefully");
        }
    }

    // ========================================
    // Built-in Validation Methods Tests
    // ========================================

    @Nested
    @DisplayName("Built-in Validation Methods")
    class BuiltInValidationTests {

        @Test
        @DisplayName("Should support minimum age validation")
        void testMinimumAge() {
            assertDoesNotThrow(() -> {
                validationBuilder.minimumAge(18);
            }, "Should support minimum age validation");
        }

        @Test
        @DisplayName("Should support email required validation")
        void testEmailRequired() {
            assertDoesNotThrow(() -> {
                validationBuilder.emailRequired();
            }, "Should support email required validation");
        }

        @Test
        @DisplayName("Should support passes method")
        void testPasses() {
            assertDoesNotThrow(() -> {
                boolean result = validationBuilder.that("#age > 0", "Age must be positive").passes();
                assertTrue(result, "Should pass validation for valid data");
            }, "Should support passes method");
        }
    }

    // ========================================
    // Validation Execution Tests
    // ========================================

    @Nested
    @DisplayName("Validation Execution")
    class ValidationExecutionTests {

        @Test
        @DisplayName("Should validate data and return ValidationResult")
        void testValidateData() {
            validationBuilder
                .that("#name != null", "Name is required")
                .that("#age > 0", "Age must be positive");

            ValidationResult result = validationBuilder.validate();

            assertNotNull(result, "ValidationResult should not be null");
        }

        @Test
        @DisplayName("Should handle validation with empty data")
        void testValidateEmptyData() {
            ValidationBuilder emptyBuilder = new ValidationBuilder(new HashMap<>());
            emptyBuilder.that("#name != null", "Name is required");

            ValidationResult result = emptyBuilder.validate();

            assertNotNull(result, "ValidationResult should not be null for empty data");
        }

        @Test
        @DisplayName("Should validate without any rules")
        void testValidateWithoutRules() {
            ValidationResult result = validationBuilder.validate();

            assertNotNull(result, "ValidationResult should not be null even without rules");
        }

        @Test
        @DisplayName("Should support passes method")
        void testPassesMethod() {
            boolean result = validationBuilder
                .that("#age > 0", "Age must be positive")
                .passes();

            assertTrue(result, "Should pass for valid data");
        }
    }

    // ========================================
    // Edge Cases and Error Handling Tests
    // ========================================

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle complex expressions")
        void testComplexExpressions() {
            assertDoesNotThrow(() -> {
                ValidationResult result = validationBuilder
                    .that("#age >= 18 && #salary > 30000 && #name.length() > 2", "Complex validation")
                    .validate();
                assertNotNull(result, "Should return result for complex expressions");
            }, "Should handle complex expressions");
        }

        @Test
        @DisplayName("Should handle invalid expressions gracefully")
        void testInvalidExpressions() {
            assertDoesNotThrow(() -> {
                ValidationResult result = validationBuilder
                    .that("#invalid.syntax", "Invalid expression")
                    .validate();
                assertNotNull(result, "Should return result even with invalid expressions");
            }, "Should handle invalid expressions gracefully");
        }

        @Test
        @DisplayName("Should handle multiple conditions")
        void testMultipleConditions() {
            assertDoesNotThrow(() -> {
                validationBuilder
                    .that("#age > 0", "First rule")
                    .that("#age < 100", "Second rule");
            }, "Should handle multiple conditions");
        }
    }
}
