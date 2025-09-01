package dev.mars.apex.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for RuleConfigurationException.
 * 
 * Tests cover:
 * - Exception hierarchy and inheritance from RuleEngineException
 * - Constructor variations and parameter handling
 * - Configuration element and expected format management
 * - Error message formatting and context information
 * - Integration with base exception functionality
 * 
 * @author APEX Test Team
 * @since 1.0.0
 */
class RuleConfigurationExceptionTest {

    // ========================================
    // Constructor Tests
    // ========================================

    @Nested
    @DisplayName("Constructor Variations")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with configuration element and message")
        void testBasicConstructor() {
            String configElement = "rule.condition";
            String message = "Invalid condition format";
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals(configElement, exception.getConfigurationElement(), "Configuration element should match");
            assertNull(exception.getExpectedFormat(), "Expected format should be null");
            assertEquals("RULE_CONFIGURATION_ERROR", exception.getErrorCode(), "Should have configuration error code");
            assertTrue(exception.getContext().contains(configElement), "Context should contain configuration element");
        }

        @Test
        @DisplayName("Should create exception with configuration element, message, and expected format")
        void testConstructorWithExpectedFormat() {
            String configElement = "rule.condition";
            String message = "Invalid condition format";
            String expectedFormat = "#field operator value";
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message, expectedFormat);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals(configElement, exception.getConfigurationElement(), "Configuration element should match");
            assertEquals(expectedFormat, exception.getExpectedFormat(), "Expected format should match");
            assertEquals("RULE_CONFIGURATION_ERROR", exception.getErrorCode(), "Should have configuration error code");
        }

        @Test
        @DisplayName("Should create exception with configuration element, message, and cause")
        void testConstructorWithCause() {
            String configElement = "rule.condition";
            String message = "Invalid condition format";
            Throwable cause = new IllegalArgumentException("Invalid syntax");
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message, cause);
            
            assertEquals(message, exception.getMessage(), "Message should match");
            assertEquals(configElement, exception.getConfigurationElement(), "Configuration element should match");
            assertNull(exception.getExpectedFormat(), "Expected format should be null");
            assertEquals(cause, exception.getCause(), "Cause should match");
            assertEquals("RULE_CONFIGURATION_ERROR", exception.getErrorCode(), "Should have configuration error code");
        }
    }

    // ========================================
    // Configuration Element Tests
    // ========================================

    @Nested
    @DisplayName("Configuration Element Management")
    class ConfigurationElementTests {

        @Test
        @DisplayName("Should preserve configuration element")
        void testConfigurationElementPreservation() {
            String configElement = "enrichment.lookup-config.lookup-service";
            RuleConfigurationException exception = new RuleConfigurationException(configElement, "Test message");
            assertEquals(configElement, exception.getConfigurationElement(), "Should preserve configuration element");
        }

        @Test
        @DisplayName("Should handle null configuration element")
        void testNullConfigurationElement() {
            RuleConfigurationException exception = new RuleConfigurationException(null, "Test message");
            assertNull(exception.getConfigurationElement(), "Should handle null configuration element");
        }

        @Test
        @DisplayName("Should handle empty configuration element")
        void testEmptyConfigurationElement() {
            String emptyElement = "";
            RuleConfigurationException exception = new RuleConfigurationException(emptyElement, "Test message");
            assertEquals(emptyElement, exception.getConfigurationElement(), "Should preserve empty configuration element");
        }

        @Test
        @DisplayName("Should handle complex configuration paths")
        void testComplexConfigurationPaths() {
            String complexPath = "rules[0].enrichments[2].lookup-config.field-mappings[1].target-field";
            RuleConfigurationException exception = new RuleConfigurationException(complexPath, "Test message");
            assertEquals(complexPath, exception.getConfigurationElement(), "Should handle complex paths");
        }
    }

    // ========================================
    // Expected Format Tests
    // ========================================

    @Nested
    @DisplayName("Expected Format Management")
    class ExpectedFormatTests {

        @Test
        @DisplayName("Should preserve expected format when provided")
        void testExpectedFormatPreservation() {
            String expectedFormat = "{ \"type\": \"lookup-enrichment\", \"lookup-config\": {...} }";
            RuleConfigurationException exception = new RuleConfigurationException(
                "enrichment.config", "Invalid format", expectedFormat);
            assertEquals(expectedFormat, exception.getExpectedFormat(), "Should preserve expected format");
        }

        @Test
        @DisplayName("Should handle null expected format")
        void testNullExpectedFormat() {
            RuleConfigurationException exception = new RuleConfigurationException(
                "test.config", "Test message", (String) null);
            assertNull(exception.getExpectedFormat(), "Should handle null expected format");
        }

        @Test
        @DisplayName("Should handle empty expected format")
        void testEmptyExpectedFormat() {
            String emptyFormat = "";
            RuleConfigurationException exception = new RuleConfigurationException(
                "test.config", "Test message", emptyFormat);
            assertEquals(emptyFormat, exception.getExpectedFormat(), "Should preserve empty expected format");
        }

        @Test
        @DisplayName("Should handle complex expected format descriptions")
        void testComplexExpectedFormat() {
            String complexFormat = """
                Expected format:
                {
                  "type": "lookup-enrichment",
                  "condition": "#field != null",
                  "lookup-config": {
                    "lookup-service": "service-name",
                    "lookup-key": "#key-field"
                  }
                }
                """;
            RuleConfigurationException exception = new RuleConfigurationException(
                "enrichment", "Invalid format", complexFormat);
            assertEquals(complexFormat, exception.getExpectedFormat(), "Should handle complex format descriptions");
        }
    }

    // ========================================
    // Inheritance Tests
    // ========================================

    @Nested
    @DisplayName("Exception Hierarchy")
    class InheritanceTests {

        @Test
        @DisplayName("Should extend RuleEngineException")
        void testRuleEngineExceptionInheritance() {
            RuleConfigurationException exception = new RuleConfigurationException("test", "message");
            assertTrue(exception instanceof RuleEngineException, "Should extend RuleEngineException");
        }

        @Test
        @DisplayName("Should inherit error code from base class")
        void testErrorCodeInheritance() {
            RuleConfigurationException exception = new RuleConfigurationException("test", "message");
            assertEquals("RULE_CONFIGURATION_ERROR", exception.getErrorCode(), "Should have configuration error code");
        }

        @Test
        @DisplayName("Should inherit context functionality")
        void testContextInheritance() {
            String configElement = "test.element";
            RuleConfigurationException exception = new RuleConfigurationException(configElement, "message");
            String context = exception.getContext();
            assertNotNull(context, "Context should not be null");
            assertTrue(context.contains(configElement), "Context should contain configuration element");
        }

        @Test
        @DisplayName("Should support polymorphic usage")
        void testPolymorphicUsage() {
            RuleEngineException exception = new RuleConfigurationException("test", "message");
            assertNotNull(exception, "Should support polymorphic assignment");
            assertEquals("RULE_CONFIGURATION_ERROR", exception.getErrorCode(), "Should maintain functionality");
        }
    }

    // ========================================
    // Error Context Tests
    // ========================================

    @Nested
    @DisplayName("Error Context Information")
    class ErrorContextTests {

        @Test
        @DisplayName("Should include configuration element in context")
        void testContextIncludesConfigElement() {
            String configElement = "rule.validation.pattern";
            RuleConfigurationException exception = new RuleConfigurationException(configElement, "Invalid pattern");
            String context = exception.getContext();
            assertTrue(context.contains(configElement), "Context should include configuration element");
        }

        @Test
        @DisplayName("Should format context appropriately")
        void testContextFormatting() {
            String configElement = "enrichment.lookup";
            RuleConfigurationException exception = new RuleConfigurationException(configElement, "Missing lookup service");
            String context = exception.getContext();
            assertTrue(context.startsWith("Configuration element:"), "Context should be properly formatted");
        }
    }

    // ========================================
    // Real-world Scenario Tests
    // ========================================

    @Nested
    @DisplayName("Real-world Configuration Scenarios")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("Should handle YAML configuration errors")
        void testYamlConfigurationError() {
            String configElement = "enrichments[0].type";
            String message = "Invalid enrichment type 'expression-enrichment'. Valid types are: [field-enrichment, calculation-enrichment, lookup-enrichment]";
            String expectedFormat = "type: \"lookup-enrichment\" | \"field-enrichment\" | \"calculation-enrichment\"";
            
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message, expectedFormat);
            
            assertEquals(configElement, exception.getConfigurationElement());
            assertEquals(message, exception.getMessage());
            assertEquals(expectedFormat, exception.getExpectedFormat());
        }

        @Test
        @DisplayName("Should handle missing required field errors")
        void testMissingRequiredFieldError() {
            String configElement = "enrichments[1].lookup-config";
            String message = "lookup-enrichment type requires 'lookup-config' field";
            String expectedFormat = "lookup-config: { lookup-service: \"service-name\", lookup-key: \"#key-field\" }";
            
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message, expectedFormat);
            
            assertNotNull(exception.getConfigurationElement());
            assertNotNull(exception.getMessage());
            assertNotNull(exception.getExpectedFormat());
        }

        @Test
        @DisplayName("Should handle invalid field mapping errors")
        void testInvalidFieldMappingError() {
            String configElement = "enrichments[0].field-mappings[0].source-field";
            String message = "source-field cannot be null or empty";
            
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message);
            
            assertEquals(configElement, exception.getConfigurationElement());
            assertEquals(message, exception.getMessage());
        }

        @Test
        @DisplayName("Should handle data source reference errors")
        void testDataSourceReferenceError() {
            String configElement = "enrichments[0].lookup-config.lookup-service";
            String message = "references unknown lookup service: 'non-existent-service'";
            Throwable cause = new IllegalArgumentException("Service not found");
            
            RuleConfigurationException exception = new RuleConfigurationException(configElement, message, cause);
            
            assertEquals(configElement, exception.getConfigurationElement());
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long configuration paths")
        void testLongConfigurationPath() {
            String longPath = "rules[0].enrichments[0].lookup-config.field-mappings[0].transformation-rules[0].conditions[0].expression";
            RuleConfigurationException exception = new RuleConfigurationException(longPath, "Test message");
            assertEquals(longPath, exception.getConfigurationElement(), "Should handle long paths");
        }

        @Test
        @DisplayName("Should handle special characters in configuration element")
        void testSpecialCharactersInConfigElement() {
            String specialElement = "rule['special-name'].condition[\"complex\"]";
            RuleConfigurationException exception = new RuleConfigurationException(specialElement, "Test message");
            assertEquals(specialElement, exception.getConfigurationElement(), "Should handle special characters");
        }

        @Test
        @DisplayName("Should support toString method")
        void testToString() {
            RuleConfigurationException exception = new RuleConfigurationException("test.config", "Test message");
            String toString = exception.toString();
            
            assertNotNull(toString, "toString should not be null");
            assertTrue(toString.contains("RuleConfigurationException"), "Should contain class name");
            assertTrue(toString.contains("Test message"), "Should contain message");
        }
    }
}
