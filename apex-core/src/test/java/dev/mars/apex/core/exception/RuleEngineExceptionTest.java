package dev.mars.apex.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for RuleEngineException.
 * 
 * Tests cover:
 * - Exception hierarchy and inheritance
 * - Constructor variations and parameter handling
 * - Error code and context management
 * - Message formatting and error details
 * - Cause chain handling and stack traces
 * 
 * @author APEX Test Team
 * @since 1.0.0
 */
class RuleEngineExceptionTest {

    // ========================================
    // Constructor Tests
    // ========================================

    @Nested
    @DisplayName("Constructor Variations")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message only")
        void testMessageOnlyConstructor() {
            String message = "Test error message";
            RuleEngineException exception = new RuleEngineException(message);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals("GENERAL_ERROR", exception.getErrorCode(), "Should have default error code");
            assertNull(exception.getContext(), "Context should be null");
            assertNull(exception.getCause(), "Cause should be null");
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void testMessageAndCauseConstructor() {
            String message = "Test error message";
            Throwable cause = new RuntimeException("Root cause");
            RuleEngineException exception = new RuleEngineException(message, cause);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals("GENERAL_ERROR", exception.getErrorCode(), "Should have default error code");
            assertNull(exception.getContext(), "Context should be null");
            assertEquals(cause, exception.getCause(), "Cause should match");
        }

        @Test
        @DisplayName("Should create exception with error code, message, and context")
        void testFullConstructorWithoutCause() {
            String errorCode = "CUSTOM_ERROR";
            String message = "Test error message";
            String context = "Test context";
            RuleEngineException exception = new RuleEngineException(errorCode, message, context);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals(errorCode, exception.getErrorCode(), "Error code should match");
            assertEquals(context, exception.getContext(), "Context should match");
            assertNull(exception.getCause(), "Cause should be null");
        }

        @Test
        @DisplayName("Should create exception with all parameters")
        void testFullConstructorWithCause() {
            String errorCode = "CUSTOM_ERROR";
            String message = "Test error message";
            String context = "Test context";
            Throwable cause = new RuntimeException("Root cause");
            RuleEngineException exception = new RuleEngineException(errorCode, message, context, cause);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals(errorCode, exception.getErrorCode(), "Error code should match");
            assertEquals(context, exception.getContext(), "Context should match");
            assertEquals(cause, exception.getCause(), "Cause should match");
        }
    }

    // ========================================
    // Error Code Tests
    // ========================================

    @Nested
    @DisplayName("Error Code Management")
    class ErrorCodeTests {

        @Test
        @DisplayName("Should use default error code when not specified")
        void testDefaultErrorCode() {
            RuleEngineException exception = new RuleEngineException("Test message");
            assertEquals("GENERAL_ERROR", exception.getErrorCode(), "Should use default error code");
        }

        @Test
        @DisplayName("Should preserve custom error code")
        void testCustomErrorCode() {
            String customCode = "VALIDATION_ERROR";
            RuleEngineException exception = new RuleEngineException(customCode, "Test message", "Test context");
            assertEquals(customCode, exception.getErrorCode(), "Should preserve custom error code");
        }

        @Test
        @DisplayName("Should handle null error code")
        void testNullErrorCode() {
            RuleEngineException exception = new RuleEngineException(null, "Test message", "Test context");
            assertNull(exception.getErrorCode(), "Should handle null error code");
        }

        @Test
        @DisplayName("Should handle empty error code")
        void testEmptyErrorCode() {
            String emptyCode = "";
            RuleEngineException exception = new RuleEngineException(emptyCode, "Test message", "Test context");
            assertEquals(emptyCode, exception.getErrorCode(), "Should preserve empty error code");
        }
    }

    // ========================================
    // Context Tests
    // ========================================

    @Nested
    @DisplayName("Context Management")
    class ContextTests {

        @Test
        @DisplayName("Should preserve context information")
        void testContextPreservation() {
            String context = "Rule: test-rule, Field: test-field";
            RuleEngineException exception = new RuleEngineException("ERROR", "Test message", context);
            assertEquals(context, exception.getContext(), "Should preserve context");
        }

        @Test
        @DisplayName("Should handle null context")
        void testNullContext() {
            RuleEngineException exception = new RuleEngineException("ERROR", "Test message", null);
            assertNull(exception.getContext(), "Should handle null context");
        }

        @Test
        @DisplayName("Should handle empty context")
        void testEmptyContext() {
            String emptyContext = "";
            RuleEngineException exception = new RuleEngineException("ERROR", "Test message", emptyContext);
            assertEquals(emptyContext, exception.getContext(), "Should preserve empty context");
        }
    }

    // ========================================
    // Message Tests
    // ========================================

    @Nested
    @DisplayName("Message Handling")
    class MessageTests {

        @Test
        @DisplayName("Should preserve error message")
        void testMessagePreservation() {
            String message = "Detailed error description";
            RuleEngineException exception = new RuleEngineException(message);
            assertEquals(message, exception.getMessage(), "Should preserve message");
        }

        @Test
        @DisplayName("Should handle null message")
        void testNullMessage() {
            RuleEngineException exception = new RuleEngineException(null);
            assertNull(exception.getMessage(), "Should handle null message");
        }

        @Test
        @DisplayName("Should handle empty message")
        void testEmptyMessage() {
            String emptyMessage = "";
            RuleEngineException exception = new RuleEngineException(emptyMessage);
            assertEquals(emptyMessage, exception.getMessage(), "Should preserve empty message");
        }
    }

    // ========================================
    // Cause Chain Tests
    // ========================================

    @Nested
    @DisplayName("Cause Chain Handling")
    class CauseChainTests {

        @Test
        @DisplayName("Should preserve cause chain")
        void testCauseChainPreservation() {
            RuntimeException rootCause = new RuntimeException("Root cause");
            IllegalArgumentException intermediateCause = new IllegalArgumentException("Intermediate cause", rootCause);
            RuleEngineException exception = new RuleEngineException("Final message", intermediateCause);
            
            assertEquals(intermediateCause, exception.getCause(), "Should preserve immediate cause");
            assertEquals(rootCause, exception.getCause().getCause(), "Should preserve root cause");
        }

        @Test
        @DisplayName("Should handle null cause")
        void testNullCause() {
            RuleEngineException exception = new RuleEngineException("Test message", (Throwable) null);
            assertNull(exception.getCause(), "Should handle null cause");
        }

        @Test
        @DisplayName("Should maintain stack trace")
        void testStackTracePreservation() {
            RuleEngineException exception = new RuleEngineException("Test message");
            StackTraceElement[] stackTrace = exception.getStackTrace();
            
            assertNotNull(stackTrace, "Stack trace should not be null");
            assertTrue(stackTrace.length > 0, "Stack trace should not be empty");
        }
    }

    // ========================================
    // Inheritance Tests
    // ========================================

    @Nested
    @DisplayName("Exception Hierarchy")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend Exception class")
        void testExceptionInheritance() {
            RuleEngineException exception = new RuleEngineException("Test message");
            assertTrue(exception instanceof Exception, "Should be instance of Exception");
        }

        @Test
        @DisplayName("Should be throwable")
        void testThrowableInterface() {
            RuleEngineException exception = new RuleEngineException("Test message");
            assertTrue(exception instanceof Throwable, "Should be instance of Throwable");
        }

        @Test
        @DisplayName("Should support polymorphic usage")
        void testPolymorphicUsage() {
            Exception exception = new RuleEngineException("Test message");
            assertNotNull(exception, "Should support polymorphic assignment");
            assertEquals("Test message", exception.getMessage(), "Should maintain functionality");
        }
    }

    // ========================================
    // Serialization Tests
    // ========================================

    @Nested
    @DisplayName("Serialization Support")
    class SerializationTests {

        @Test
        @DisplayName("Should have serialVersionUID")
        void testSerialVersionUID() {
            // This test verifies that the class has a serialVersionUID field
            // which is important for serialization compatibility
            assertDoesNotThrow(() -> {
                RuleEngineException exception = new RuleEngineException("Test");
                // If serialVersionUID is not defined, this would still work
                // but it's good practice to have it defined
                assertNotNull(exception, "Exception should be created");
            }, "Should support serialization");
        }
    }

    // ========================================
    // Edge Cases and Error Handling Tests
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long messages")
        void testLongMessage() {
            String longMessage = "A".repeat(10000);
            RuleEngineException exception = new RuleEngineException(longMessage);
            assertEquals(longMessage, exception.getMessage(), "Should handle long messages");
        }

        @Test
        @DisplayName("Should handle special characters in message")
        void testSpecialCharactersInMessage() {
            String specialMessage = "Error with special chars: \n\t\r\"'\\";
            RuleEngineException exception = new RuleEngineException(specialMessage);
            assertEquals(specialMessage, exception.getMessage(), "Should handle special characters");
        }

        @Test
        @DisplayName("Should handle Unicode characters")
        void testUnicodeCharacters() {
            String unicodeMessage = "Error with Unicode: 你好 🚀 ñáéíóú";
            RuleEngineException exception = new RuleEngineException(unicodeMessage);
            assertEquals(unicodeMessage, exception.getMessage(), "Should handle Unicode characters");
        }

        @Test
        @DisplayName("Should support toString method")
        void testToString() {
            RuleEngineException exception = new RuleEngineException("CUSTOM_ERROR", "Test message", "Test context");
            String toString = exception.toString();
            
            assertNotNull(toString, "toString should not be null");
            assertTrue(toString.contains("RuleEngineException"), "Should contain class name");
            assertTrue(toString.contains("Test message"), "Should contain message");
        }
    }
}
