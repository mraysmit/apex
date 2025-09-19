package dev.mars.apex.demo.database;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class validating property injection syntax features and edge cases.
 *
 * This focused test suite validates specific syntax features and edge cases
 * for the APEX Rules Engine's property injection mechanism, ensuring robust
 * handling of various syntax patterns and error conditions.
 *
 * SYNTAX FEATURES TESTED:
 *
 * 1. **Curly Brace Syntax with Defaults**
 *    - Pattern: ${PROPERTY:default_value}
 *    - Use case: Spring Boot style configuration
 *    - Validation: Fallback behavior when properties are missing
 *
 * 2. **Mixed Syntax Support**
 *    - Patterns: ${PROPERTY} and $(PROPERTY) in same configuration
 *    - Use case: Migration scenarios and tool compatibility
 *    - Validation: Both syntax types resolve correctly
 *
 * 3. **Resolution Priority Testing**
 *    - Order: System Properties > Environment Variables > Defaults
 *    - Use case: Environment-specific overrides
 *    - Validation: Higher priority sources take precedence
 *
 * 4. **Edge Cases and Error Handling**
 *    - Malformed syntax: $(UNCLOSED, ${EMPTY}
 *    - Empty placeholders: $(), ${}
 *    - Use case: Robust error handling in production
 *    - Validation: Graceful degradation without exceptions
 *
 * 5. **Complex Default Values**
 *    - Special characters: URLs, email addresses, file paths
 *    - Use case: Real-world configuration values
 *    - Validation: Complex strings handled correctly
 *
 * TECHNICAL APPROACH:
 * - Uses reflection to test YamlConfigurationLoader.resolveProperties() directly
 * - Focuses on syntax validation rather than end-to-end functionality
 * - Provides clear logging for each syntax pattern tested
 * - Maintains simple, focused test methods for easy debugging
 *
 * RELATIONSHIP TO OTHER TESTS:
 * - Complements ExternalSourceInjectionTest (external source patterns)
 * - Complements SimplePasswordInjectionTest (end-to-end functionality)
 * - Fills syntax coverage gaps identified in comprehensive analysis
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Property Injection Syntax Tests")
class PropertyInjectionSyntaxTest {

    private static final Logger logger = LoggerFactory.getLogger(PropertyInjectionSyntaxTest.class);
    
    private YamlConfigurationLoader loader;
    private Method resolvePropertiesMethod;

    @BeforeEach
    void setUp() throws Exception {
        loader = new YamlConfigurationLoader();
        
        // Access private resolveProperties method for direct testing
        resolvePropertiesMethod = YamlConfigurationLoader.class.getDeclaredMethod("resolveProperties", String.class);
        resolvePropertiesMethod.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        // Clean up test properties
        System.clearProperty("TEST_PROP");
        System.clearProperty("PRIORITY_TEST");
    }

    @Test
    @DisplayName("Should resolve ${PROPERTY:default} syntax")
    void testCurlyBracesWithDefaults() throws Exception {
        logger.info("TEST: ${PROPERTY:default} syntax validation");
        
        String input = "user: ${MISSING_USER:admin}, pass: ${MISSING_PASS:secret}";
        String result = (String) resolvePropertiesMethod.invoke(loader, input);
        
        logger.info("Input: {}", input);
        logger.info("Output: {}", result);
        
        assertEquals("user: admin, pass: secret", result);
        logger.info("✅ ${PROPERTY:default} syntax works correctly");
    }

    @Test
    @DisplayName("Should test system property priority over defaults")
    void testSystemPropertyPriority() throws Exception {
        logger.info("TEST: System property priority over defaults");
        
        // Test with system property set
        System.setProperty("PRIORITY_TEST", "system_value");
        
        String input = "value: $(PRIORITY_TEST:default_value)";
        String result = (String) resolvePropertiesMethod.invoke(loader, input);
        
        assertEquals("value: system_value", result);
        logger.info("✅ System property takes precedence: {}", result);
        
        // Test fallback to default when property cleared
        System.clearProperty("PRIORITY_TEST");
        
        String result2 = (String) resolvePropertiesMethod.invoke(loader, input);
        assertEquals("value: default_value", result2);
        logger.info("✅ Falls back to default when property missing: {}", result2);
    }

    @Test
    @DisplayName("Should handle mixed syntax correctly")
    void testMixedSyntax() throws Exception {
        logger.info("TEST: Mixed ${} and $() syntax");
        
        System.setProperty("TEST_PROP", "test_value");
        
        String input = "curly: ${TEST_PROP}, paren: $(TEST_PROP), default: ${MISSING:fallback}";
        String result = (String) resolvePropertiesMethod.invoke(loader, input);
        
        logger.info("Input: {}", input);
        logger.info("Output: {}", result);
        
        assertTrue(result.contains("curly: test_value"));
        assertTrue(result.contains("paren: test_value"));
        assertTrue(result.contains("default: fallback"));
        
        logger.info("✅ Mixed syntax works correctly");
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() throws Exception {
        logger.info("TEST: Edge cases and malformed syntax");
        
        String[] testCases = {
            "empty: $()",                    // Empty property name
            "empty: ${}",                    // Empty property name  
            "unclosed: $(UNCLOSED",          // Unclosed parenthesis
            "unclosed: ${UNCLOSED",          // Unclosed brace
            "valid: $(MISSING:default)"      // Valid case for comparison
        };
        
        for (String testCase : testCases) {
            String result = (String) resolvePropertiesMethod.invoke(loader, testCase);
            logger.info("Input: {} -> Output: {}", testCase, result);
            assertNotNull(result, "Should handle edge case gracefully: " + testCase);
        }
        
        logger.info("✅ Edge cases handled gracefully");
    }

    @Test
    @DisplayName("Should handle complex defaults with special characters")
    void testComplexDefaults() throws Exception {
        logger.info("TEST: Complex defaults with special characters");
        
        String input = """
            url: $(DB_URL:jdbc:postgresql://localhost:5432/testdb)
            email: $(ADMIN_EMAIL:admin@example.com)
            path: $(CONFIG_PATH:/opt/app/config.yml)
            """;
        
        String result = (String) resolvePropertiesMethod.invoke(loader, input);
        
        logger.info("Input:\n{}", input);
        logger.info("Output:\n{}", result);
        
        assertTrue(result.contains("jdbc:postgresql://localhost:5432/testdb"));
        assertTrue(result.contains("admin@example.com"));
        assertTrue(result.contains("/opt/app/config.yml"));
        
        logger.info("✅ Complex defaults with special characters work correctly");
    }
}
