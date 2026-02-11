package dev.mars.apex.core.config.yaml;
import dev.mars.apex.core.config.model.*;
import dev.mars.apex.core.config.loader.*;
import dev.mars.apex.core.config.exception.*;
import dev.mars.apex.core.config.service.*;

import dev.mars.apex.core.util.PropertyResolver;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for property resolution methods in PropertyResolver.
 * 
 * This test verifies the property resolution functionality that is now centralized
 * in the PropertyResolver utility class, used by YamlConfigurationLoader, 
 * DataSourceResolver, and YamlDataSource.
 */
public class PropertyResolutionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyResolutionTest.class);

    @BeforeEach
    void setUp() throws Exception {
        LOGGER.info("Setting up PropertyResolutionTest");
        
        // Set up test environment variables and system properties
        System.setProperty("TEST_PROP", "test_value");
        System.setProperty("TEST_PASSWORD", "secret123");
        System.setProperty("TEST_DEFAULT", "default_used");
    }

    @AfterEach
    void tearDown() {
        LOGGER.info("Cleaning up PropertyResolutionTest");
        
        // Clean up test properties
        System.clearProperty("TEST_PROP");
        System.clearProperty("TEST_PASSWORD");
        System.clearProperty("TEST_DEFAULT");
    }

    @Test
    @DisplayName("Should resolve simple property placeholder")
    void testSimplePropertyResolution() throws Exception {
        LOGGER.info("TEST: Simple property resolution");
        
        String input = "${TEST_PROP}";
        String result = PropertyResolver.resolve(input);
        
        assertEquals("test_value", result);
        LOGGER.info("[OK] Simple property resolution works: " + input + " -> " + result);
    }

    @Test
    @DisplayName("Should resolve property with default value")
    void testPropertyWithDefault() throws Exception {
        LOGGER.info("TEST: Property with default value");
        
        String input = "${NONEXISTENT_PROP:default_value}";
        String result = PropertyResolver.resolve(input);
        
        assertEquals("default_value", result);
        LOGGER.info("[OK] Property with default works: " + input + " -> " + result);
    }

    @Test
    @DisplayName("Should resolve multiple properties in single string")
    void testMultipleProperties() throws Exception {
        LOGGER.info("TEST: Multiple properties in single string");
        
        String input = "jdbc:postgresql://${TEST_PROP}:5432/db?user=${TEST_DEFAULT:admin}";
        String result = PropertyResolver.resolve(input);
        
        assertEquals("jdbc:postgresql://test_value:5432/db?user=default_used", result);
        LOGGER.info("[OK] Multiple properties work: " + input + " -> " + result);
    }

    @Test
    @DisplayName("Should return unchanged string without placeholders")
    void testNoPlaceholders() throws Exception {
        LOGGER.info("TEST: String without placeholders");
        
        String input = "plain_string_no_placeholders";
        String result = PropertyResolver.resolve(input);
        
        assertEquals(input, result);
        LOGGER.info("[OK] No placeholders unchanged: " + input + " -> " + result);
    }

    @Test
    @DisplayName("Should handle null input")
    void testNullInput() throws Exception {
        LOGGER.info("TEST: Null input");
        
        String result = PropertyResolver.resolve(null);
        
        assertNull(result);
        LOGGER.info("[OK] Null input handled correctly");
    }

    @Test
    @DisplayName("Should return original placeholder for missing property")
    void testMissingRequiredProperty() throws Exception {
        LOGGER.info("TEST: Missing required property");

        String input = "${DEFINITELY_NONEXISTENT_PROPERTY}";
        String result = PropertyResolver.resolve(input, false); // Don't throw on unresolved

        // Should return the original placeholder when property is not found
        assertEquals("${DEFINITELY_NONEXISTENT_PROPERTY}", result);
        LOGGER.info("[OK] Missing required property returns original placeholder: " + result);
    }

    @Test
    @DisplayName("Should identify sensitive properties correctly")
    void testSensitivePropertyDetection() throws Exception {
        LOGGER.info("TEST: Sensitive property detection");
        
        String[] sensitiveKeys = {"PASSWORD", "password", "SECRET", "secret", "TOKEN", "token", "KEY", "key", "PWD", "pwd"};
        String[] normalKeys = {"HOST", "PORT", "DATABASE", "USERNAME", "TIMEOUT"};
        
        for (String key : sensitiveKeys) {
            boolean result = PropertyResolver.isSensitiveProperty(key);
            assertTrue(result, "Should detect " + key + " as sensitive");
        }
        
        for (String key : normalKeys) {
            boolean result = PropertyResolver.isSensitiveProperty(key);
            assertFalse(result, "Should not detect " + key + " as sensitive");
        }
        
        LOGGER.info("[OK] Sensitive property detection works correctly");
    }

    @Test
    @DisplayName("Should mask sensitive values in logs")
    void testSensitiveValueMasking() throws Exception {
        LOGGER.info("TEST: Sensitive value masking");
        
        String sensitiveValue = "jdbc:postgresql://host:5432/db?password=${DB_PASSWORD}";
        String result = PropertyResolver.maskSensitiveValue(sensitiveValue);
        
        assertEquals("[MASKED_VALUE_WITH_SENSITIVE_PLACEHOLDERS]", result);
        LOGGER.info("[OK] Sensitive value masking works");
        
        String normalValue = "jdbc:postgresql://host:5432/db?user=${DB_USER}";
        String normalResult = PropertyResolver.maskSensitiveValue(normalValue);
        
        assertEquals(normalValue, normalResult);
        LOGGER.info("[OK] Normal value not masked");
    }

    @Test
    @DisplayName("Should resolve system properties")
    void testResolutionPriority() throws Exception {
        LOGGER.info("TEST: System property resolution");
        
        // This test verifies TEST_PROP is resolved from system property
        String result = PropertyResolver.resolve("${TEST_PROP}");
        
        assertEquals("test_value", result);
        LOGGER.info("[OK] System property resolution works");
    }

    @Test
    @DisplayName("Should handle complex placeholder patterns")
    void testComplexPlaceholders() throws Exception {
        LOGGER.info("TEST: Complex placeholder patterns");

        String input = "host=${TEST_PROP},port=5432,password=${TEST_PASSWORD:fallback},timeout=${TIMEOUT:30}";
        String result = PropertyResolver.resolve(input);

        assertEquals("host=test_value,port=5432,password=secret123,timeout=30", result);
        LOGGER.info("[OK] Complex placeholders work: " + input + " -> [RESULT_MASKED_FOR_SECURITY]");
    }

    @Test
    @DisplayName("Should resolve simple property placeholder with parentheses syntax")
    void testSimpleParenthesesPropertyResolution() throws Exception {
        LOGGER.info("TEST: Simple parentheses property resolution");

        String input = "$(TEST_PROP)";
        String result = PropertyResolver.resolve(input);

        assertEquals("test_value", result);
        LOGGER.info("[OK] Simple parentheses property resolution works: " + input + " -> " + result);
    }

    @Test
    @DisplayName("Should resolve password property with parentheses syntax")
    void testParenthesesPasswordResolution() throws Exception {
        LOGGER.info("TEST: Parentheses password property resolution");

        String input = "$(TEST_PASSWORD)";
        String result = PropertyResolver.resolve(input);

        assertEquals("secret123", result);
        LOGGER.info("[OK] Parentheses password property resolution works: " + input + " -> [MASKED]");
    }

    @Test
    @DisplayName("Should resolve property with default value using parentheses syntax")
    void testParenthesesPropertyWithDefault() throws Exception {
        LOGGER.info("TEST: Parentheses property with default value");

        String input = "$(NONEXISTENT_PROP:default_value)";
        String result = PropertyResolver.resolve(input);

        assertEquals("default_value", result);
        LOGGER.info("[OK] Parentheses property with default works: " + input + " -> " + result);
    }

    @Test
    @DisplayName("Should resolve mixed curly and parentheses placeholders")
    void testMixedPlaceholderSyntax() throws Exception {
        LOGGER.info("TEST: Mixed placeholder syntax");

        String input = "host=${TEST_PROP},password=$(TEST_PASSWORD),timeout=$(TIMEOUT:30)";
        String result = PropertyResolver.resolve(input);

        assertEquals("host=test_value,password=secret123,timeout=30", result);
        LOGGER.info("[OK] Mixed placeholder syntax works: " + input + " -> [RESULT_MASKED_FOR_SECURITY]");
    }
}

