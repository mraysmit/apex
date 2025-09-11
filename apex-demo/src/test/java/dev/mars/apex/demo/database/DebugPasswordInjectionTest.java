package dev.mars.apex.demo.database;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to understand why $(PASSWD) syntax is not working.
 */
@DisplayName("Debug Password Injection Tests")
class DebugPasswordInjectionTest {

    private static final Logger logger = LoggerFactory.getLogger(DebugPasswordInjectionTest.class);
    
    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        
        // Set up system properties
        System.setProperty("TEST_PASSWORD", "secret123");
        System.setProperty("TEST_USER", "testuser");
        System.setProperty("TEST_DATABASE", "testdb");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("TEST_PASSWORD");
        System.clearProperty("TEST_USER");
        System.clearProperty("TEST_DATABASE");
    }

    @Test
    @DisplayName("Should resolve $(PASSWD) syntax directly")
    void testDirectPropertyResolution() throws Exception {
        logger.info("TEST: Direct property resolution");
        
        // Use reflection to access the private resolveProperties method
        Method resolvePropertiesMethod = YamlConfigurationLoader.class.getDeclaredMethod("resolveProperties", String.class);
        resolvePropertiesMethod.setAccessible(true);
        
        String input = "database: $(TEST_DATABASE), username: $(TEST_USER), password: $(TEST_PASSWORD)";
        String result = (String) resolvePropertiesMethod.invoke(loader, input);
        
        logger.info("Input: {}", input);
        logger.info("Output: {}", result);
        
        assertTrue(result.contains("testdb"), "Should resolve $(TEST_DATABASE) to testdb");
        assertTrue(result.contains("testuser"), "Should resolve $(TEST_USER) to testuser");
        assertTrue(result.contains("secret123"), "Should resolve $(TEST_PASSWORD) to secret123");
        
        logger.info("✓ Direct property resolution test passed");
    }

    @Test
    @DisplayName("Should resolve $(PASSWD) syntax in simple YAML")
    void testSimpleYamlResolution() throws Exception {
        logger.info("TEST: Simple YAML property resolution");
        
        String yamlContent = """
            test:
              database: "$(TEST_DATABASE)"
              username: "$(TEST_USER)"
              password: "$(TEST_PASSWORD)"
            """;
        
        logger.info("Input YAML: {}", yamlContent);
        
        // Use reflection to access the private resolveProperties method
        Method resolvePropertiesMethod = YamlConfigurationLoader.class.getDeclaredMethod("resolveProperties", String.class);
        resolvePropertiesMethod.setAccessible(true);
        
        String resolvedYaml = (String) resolvePropertiesMethod.invoke(loader, yamlContent);
        
        logger.info("Resolved YAML: {}", resolvedYaml);
        
        assertTrue(resolvedYaml.contains("testdb"), "Should resolve $(TEST_DATABASE) to testdb");
        assertTrue(resolvedYaml.contains("testuser"), "Should resolve $(TEST_USER) to testuser");
        assertTrue(resolvedYaml.contains("secret123"), "Should resolve $(TEST_PASSWORD) to secret123");
        
        logger.info("✓ Simple YAML property resolution test passed");
    }

    @Test
    @DisplayName("Should resolve mixed ${} and $() syntax")
    void testMixedSyntaxResolution() throws Exception {
        logger.info("TEST: Mixed syntax property resolution");
        
        // Set up additional properties
        System.setProperty("CURLY_PROP", "curly_value");
        System.setProperty("PAREN_PROP", "paren_value");
        
        try {
            String input = "curly: ${CURLY_PROP}, paren: $(PAREN_PROP), password: $(TEST_PASSWORD)";
            
            // Use reflection to access the private resolveProperties method
            Method resolvePropertiesMethod = YamlConfigurationLoader.class.getDeclaredMethod("resolveProperties", String.class);
            resolvePropertiesMethod.setAccessible(true);
            
            String result = (String) resolvePropertiesMethod.invoke(loader, input);
            
            logger.info("Input: {}", input);
            logger.info("Output: {}", result);
            
            assertTrue(result.contains("curly_value"), "Should resolve ${CURLY_PROP} to curly_value");
            assertTrue(result.contains("paren_value"), "Should resolve $(PAREN_PROP) to paren_value");
            assertTrue(result.contains("secret123"), "Should resolve $(TEST_PASSWORD) to secret123");
            
            logger.info("✓ Mixed syntax property resolution test passed");
            
        } finally {
            System.clearProperty("CURLY_PROP");
            System.clearProperty("PAREN_PROP");
        }
    }
}
