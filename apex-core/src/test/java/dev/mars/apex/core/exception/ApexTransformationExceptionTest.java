package dev.mars.apex.core.exception;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApexTransformationException}.
 * 
 * Tests verify that the exception class properly captures transformation
 * context including transformation ID, expression, error codes, and provides
 * appropriate factory methods for common error scenarios.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-24
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("ApexTransformationException Tests")
class ApexTransformationExceptionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApexTransformationExceptionTest.class);

    // ========================================
    // Constructor Tests
    // ========================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with all fields populated")
        void shouldCreateExceptionWithAllFields() {
            LOGGER.info("=== Testing full constructor ===");
            
            ApexTransformationException exception = new ApexTransformationException(
                ApexTransformationException.ErrorType.EXPRESSION_ERROR,
                "calculate-premium",
                "#value * rate",
                "Variable 'rate' not found",
                100.0
            );

            assertEquals(ApexTransformationException.ErrorType.EXPRESSION_ERROR, exception.getErrorType());
            assertEquals("calculate-premium", exception.getTransformationId());
            assertEquals("#value * rate", exception.getExpression());
            assertEquals("Variable 'rate' not found", exception.getMessage());
            assertEquals(100.0, exception.getOriginalValue());
            assertEquals("APEX-TRANS-001", exception.getErrorCode());
            assertNotNull(exception.getContext());
            assertTrue(exception.getContext().contains("calculate-premium"));
            
            LOGGER.info("[OK] Exception created with all fields correctly");
        }

        @Test
        @DisplayName("Should create exception with cause")
        void shouldCreateExceptionWithCause() {
            LOGGER.info("=== Testing constructor with cause ===");
            
            RuntimeException cause = new RuntimeException("Original error");
            
            ApexTransformationException exception = new ApexTransformationException(
                ApexTransformationException.ErrorType.TYPE_CONVERSION_ERROR,
                "format-date",
                "#value.format()",
                "Cannot convert to date",
                "invalid-date",
                cause
            );

            assertEquals(cause, exception.getCause());
            assertEquals("Cannot convert to date", exception.getMessage());
            assertEquals("APEX-TRANS-002", exception.getErrorCode());
            
            LOGGER.info("[OK] Exception preserves cause correctly");
        }
    }

    // ========================================
    // Error Type Tests
    // ========================================

    @Nested
    @DisplayName("Error Type Tests")
    class ErrorTypeTests {

        @Test
        @DisplayName("Should have correct error codes for all types")
        void shouldHaveCorrectErrorCodes() {
            LOGGER.info("=== Testing error type codes ===");
            
            assertEquals("APEX-TRANS-001", ApexTransformationException.ErrorType.EXPRESSION_ERROR.getCode());
            assertEquals("APEX-TRANS-002", ApexTransformationException.ErrorType.TYPE_CONVERSION_ERROR.getCode());
            assertEquals("APEX-TRANS-003", ApexTransformationException.ErrorType.SOURCE_FIELD_ERROR.getCode());
            assertEquals("APEX-TRANS-004", ApexTransformationException.ErrorType.TARGET_FIELD_ERROR.getCode());
            assertEquals("APEX-TRANS-005", ApexTransformationException.ErrorType.CONFIGURATION_ERROR.getCode());
            assertEquals("APEX-TRANS-006", ApexTransformationException.ErrorType.CONDITION_ERROR.getCode());
            assertEquals("APEX-TRANS-007", ApexTransformationException.ErrorType.NULL_VALUE_ERROR.getCode());
            assertEquals("APEX-TRANS-999", ApexTransformationException.ErrorType.GENERAL_ERROR.getCode());
            
            LOGGER.info("[OK] All error types have correct codes");
        }

        @Test
        @DisplayName("Should have descriptions for all types")
        void shouldHaveDescriptions() {
            LOGGER.info("=== Testing error type descriptions ===");
            
            for (ApexTransformationException.ErrorType type : ApexTransformationException.ErrorType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isEmpty());
                LOGGER.info("  {} -> {}", type.getCode(), type.getDescription());
            }
            
            LOGGER.info("[OK] All error types have descriptions");
        }
    }

    // ========================================
    // Factory Method Tests
    // ========================================

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create expression error")
        void shouldCreateExpressionError() {
            LOGGER.info("=== Testing expressionError factory method ===");
            
            ApexTransformationException exception = ApexTransformationException.expressionError(
                "calc-total",
                "#items.sum()",
                "Method sum() not found"
            );

            assertEquals(ApexTransformationException.ErrorType.EXPRESSION_ERROR, exception.getErrorType());
            assertEquals("calc-total", exception.getTransformationId());
            assertEquals("#items.sum()", exception.getExpression());
            assertTrue(exception.getMessage().contains("sum()"));
            assertEquals("APEX-TRANS-001", exception.getErrorCode());
            
            LOGGER.info("[OK] Expression error factory method works correctly");
        }

        @Test
        @DisplayName("Should create expression error with cause")
        void shouldCreateExpressionErrorWithCause() {
            LOGGER.info("=== Testing expressionError factory method with cause ===");
            
            Exception cause = new IllegalArgumentException("Invalid method");
            
            ApexTransformationException exception = ApexTransformationException.expressionError(
                "calc-total",
                "#items.invalid()",
                "Expression failed",
                cause
            );

            assertEquals(cause, exception.getCause());
            assertEquals(ApexTransformationException.ErrorType.EXPRESSION_ERROR, exception.getErrorType());
            
            LOGGER.info("[OK] Expression error with cause works correctly");
        }

        @Test
        @DisplayName("Should create type conversion error")
        void shouldCreateTypeConversionError() {
            LOGGER.info("=== Testing typeConversionError factory method ===");
            
            ApexTransformationException exception = ApexTransformationException.typeConversionError(
                "parse-amount",
                "Cannot convert 'abc' to Integer",
                "abc"
            );

            assertEquals(ApexTransformationException.ErrorType.TYPE_CONVERSION_ERROR, exception.getErrorType());
            assertEquals("parse-amount", exception.getTransformationId());
            assertEquals("abc", exception.getOriginalValue());
            assertEquals("APEX-TRANS-002", exception.getErrorCode());
            
            LOGGER.info("[OK] Type conversion error factory method works correctly");
        }

        @Test
        @DisplayName("Should create source field error")
        void shouldCreateSourceFieldError() {
            LOGGER.info("=== Testing sourceFieldError factory method ===");
            
            ApexTransformationException exception = ApexTransformationException.sourceFieldError(
                "copy-value",
                "sourceAmount",
                "Field 'sourceAmount' not found in data"
            );

            assertEquals(ApexTransformationException.ErrorType.SOURCE_FIELD_ERROR, exception.getErrorType());
            assertEquals("copy-value", exception.getTransformationId());
            assertEquals("sourceAmount", exception.getExpression()); // Field name stored in expression
            assertEquals("APEX-TRANS-003", exception.getErrorCode());
            
            LOGGER.info("[OK] Source field error factory method works correctly");
        }

        @Test
        @DisplayName("Should create target field error")
        void shouldCreateTargetFieldError() {
            LOGGER.info("=== Testing targetFieldError factory method ===");
            
            ApexTransformationException exception = ApexTransformationException.targetFieldError(
                "set-result",
                "readonly_field",
                "Cannot set field 'readonly_field': field is not writable"
            );

            assertEquals(ApexTransformationException.ErrorType.TARGET_FIELD_ERROR, exception.getErrorType());
            assertEquals("APEX-TRANS-004", exception.getErrorCode());
            
            LOGGER.info("[OK] Target field error factory method works correctly");
        }

        @Test
        @DisplayName("Should create configuration error")
        void shouldCreateConfigurationError() {
            LOGGER.info("=== Testing configurationError factory method ===");
            
            ApexTransformationException exception = ApexTransformationException.configurationError(
                "invalid-transform",
                "Transformation type 'invalid' is not supported"
            );

            assertEquals(ApexTransformationException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
            assertEquals("invalid-transform", exception.getTransformationId());
            assertEquals("APEX-TRANS-005", exception.getErrorCode());
            assertNull(exception.getExpression());
            
            LOGGER.info("[OK] Configuration error factory method works correctly");
        }

        @Test
        @DisplayName("Should create condition error")
        void shouldCreateConditionError() {
            LOGGER.info("=== Testing conditionError factory method ===");
            
            Exception cause = new RuntimeException("SpEL parse error");
            
            ApexTransformationException exception = ApexTransformationException.conditionError(
                "conditional-copy",
                "#value > 100",
                "Failed to evaluate condition",
                cause
            );

            assertEquals(ApexTransformationException.ErrorType.CONDITION_ERROR, exception.getErrorType());
            assertEquals("#value > 100", exception.getExpression());
            assertEquals(cause, exception.getCause());
            assertEquals("APEX-TRANS-006", exception.getErrorCode());
            
            LOGGER.info("[OK] Condition error factory method works correctly");
        }

        @Test
        @DisplayName("Should create null value error")
        void shouldCreateNullValueError() {
            LOGGER.info("=== Testing nullValueError factory method ===");
            
            ApexTransformationException exception = ApexTransformationException.nullValueError(
                "format-name",
                "firstName",
                "Field 'firstName' is null but required for transformation"
            );

            assertEquals(ApexTransformationException.ErrorType.NULL_VALUE_ERROR, exception.getErrorType());
            assertEquals("APEX-TRANS-007", exception.getErrorCode());
            
            LOGGER.info("[OK] Null value error factory method works correctly");
        }

        @Test
        @DisplayName("Should wrap unexpected exception")
        void shouldWrapUnexpectedException() {
            LOGGER.info("=== Testing wrap factory method ===");
            
            NullPointerException npe = new NullPointerException("Unexpected null");
            
            ApexTransformationException exception = ApexTransformationException.wrap(
                "process-data",
                npe
            );

            assertEquals(ApexTransformationException.ErrorType.GENERAL_ERROR, exception.getErrorType());
            assertEquals("process-data", exception.getTransformationId());
            assertEquals(npe, exception.getCause());
            assertEquals("APEX-TRANS-999", exception.getErrorCode());
            assertTrue(exception.getMessage().contains("Transformation failed"));
            
            LOGGER.info("[OK] Wrap factory method works correctly");
        }
    }

    // ========================================
    // Message and Context Tests
    // ========================================

    @Nested
    @DisplayName("Message and Context Tests")
    class MessageAndContextTests {

        @Test
        @DisplayName("Should build detailed message")
        void shouldBuildDetailedMessage() {
            LOGGER.info("=== Testing detailed message ===");
            
            ApexTransformationException exception = ApexTransformationException.expressionError(
                "calculate",
                "#value * 2",
                "Multiplication failed"
            );

            String detailed = exception.getDetailedMessage();
            assertTrue(detailed.contains("APEX-TRANS-001"));
            assertTrue(detailed.contains("Multiplication failed"));
            assertTrue(detailed.contains("Context"));
            
            LOGGER.info("Detailed message: {}", detailed);
            LOGGER.info("[OK] Detailed message formatted correctly");
        }

        @Test
        @DisplayName("Should include expression in context when available")
        void shouldIncludeExpressionInContext() {
            LOGGER.info("=== Testing context with expression ===");
            
            ApexTransformationException exception = ApexTransformationException.expressionError(
                "my-transform",
                "#complex.expression()",
                "Failed"
            );

            String context = exception.getContext();
            assertTrue(context.contains("my-transform"));
            assertTrue(context.contains("#complex.expression()"));
            
            LOGGER.info("Context: {}", context);
            LOGGER.info("[OK] Context includes expression");
        }

        @Test
        @DisplayName("Should handle null expression in context")
        void shouldHandleNullExpressionInContext() {
            LOGGER.info("=== Testing context without expression ===");
            
            ApexTransformationException exception = ApexTransformationException.configurationError(
                "my-transform",
                "Invalid config"
            );

            String context = exception.getContext();
            assertTrue(context.contains("my-transform"));
            assertFalse(context.contains("Expression:"));
            
            LOGGER.info("Context: {}", context);
            LOGGER.info("[OK] Context handles null expression gracefully");
        }

        @Test
        @DisplayName("Should provide meaningful toString")
        void shouldProvideToString() {
            LOGGER.info("=== Testing toString ===");
            
            ApexTransformationException exception = new ApexTransformationException(
                ApexTransformationException.ErrorType.EXPRESSION_ERROR,
                "test-id",
                "#expr",
                "Test message",
                "original"
            );

            String str = exception.toString();
            assertTrue(str.contains("ApexTransformationException"));
            assertTrue(str.contains("EXPRESSION_ERROR"));
            assertTrue(str.contains("test-id"));
            assertTrue(str.contains("#expr"));
            
            LOGGER.info("toString: {}", str);
            LOGGER.info("[OK] toString provides all relevant information");
        }
    }

    // ========================================
    // Integration Tests
    // ========================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should be catchable as RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            LOGGER.info("=== Testing exception hierarchy ===");
            
            ApexTransformationException exception = ApexTransformationException.expressionError(
                "test",
                "#expr",
                "Test error"
            );

            // Should be assignable to parent
            assertTrue(exception instanceof RuntimeException);
            assertTrue(exception instanceof Exception);
            
            // Should be catchable as RuntimeException
            try {
                throw exception;
            } catch (RuntimeException e) {
                assertTrue(e instanceof ApexTransformationException);
                ApexTransformationException ate = (ApexTransformationException) e;
                assertEquals("APEX-TRANS-001", ate.getErrorCode());
                assertNotNull(ate.getContext());
            }
            
            LOGGER.info("[OK] Exception hierarchy is correct");
        }

        @Test
        @DisplayName("Should work with exception chaining")
        void shouldWorkWithExceptionChaining() {
            LOGGER.info("=== Testing exception chaining ===");
            
            IllegalArgumentException root = new IllegalArgumentException("Root cause");
            RuntimeException middle = new RuntimeException("Middle cause", root);
            ApexTransformationException apex = ApexTransformationException.expressionError(
                "chain-test",
                "#expr",
                "Top level error",
                middle
            );

            assertEquals(middle, apex.getCause());
            assertEquals(root, apex.getCause().getCause());
            
            LOGGER.info("[OK] Exception chaining preserved correctly");
        }
    }
}
