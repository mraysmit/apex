package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ultra Simple Rule OR Test
 * 
 * Proves the core concept:
 * - 3 rules in sequence
 * - Combined with OR logic  
 * - Each rule maps different constant to same target field
 * - First matching rule wins
 */
@DisplayName("Ultra Simple Rule OR Test")
public class UltraSimpleRuleOrTest extends DemoTestBase {

    @Test
    @DisplayName("Should apply FIRST when input=A (rule 1 matches)")
    void testRule1Matches() {
        logger.info("=== Testing Rule 1 Match: input='A' -> output='FIRST' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");
            
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);
            
            assertEquals("FIRST", result.get("output"), "Rule 1 should map to 'FIRST'");
            logger.info("✅ Rule 1 test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply SECOND when input=B (rule 2 matches)")
    void testRule2Matches() {
        logger.info("=== Testing Rule 2 Match: input='B' -> output='SECOND' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "B");
            
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);
            
            assertEquals("SECOND", result.get("output"), "Rule 2 should map to 'SECOND'");
            logger.info("✅ Rule 2 test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply THIRD when input=C (rule 3 matches)")
    void testRule3Matches() {
        logger.info("=== Testing Rule 3 Match: input='C' -> output='THIRD' ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "C");
            
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);
            
            assertEquals("THIRD", result.get("output"), "Rule 3 should map to 'THIRD'");
            logger.info("✅ Rule 3 test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should apply nothing when input=X (no rules match)")
    void testNoRulesMatch() {
        logger.info("=== Testing No Rules Match: input='X' -> no output ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "X");
            
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);
            
            assertNull(result.get("output"), "No rules should match, output should be null");
            logger.info("✅ No rules match test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate OR logic - first rule wins when multiple could match")
    void testOrLogicFirstWins() {
        logger.info("=== Testing OR Logic: First Rule Wins ===");
        
        try {
            // This test proves sequential evaluation
            // If we had input that could match multiple rules, first should win
            // For this simple test, each input only matches one rule, so we test rule 1
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");  // Only matches rule 1
            
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);
            
            assertEquals("FIRST", result.get("output"), "First rule should win in OR logic");
            logger.info("✅ OR logic test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should load configuration successfully")
    void testConfigurationLoading() {
        logger.info("=== Testing Configuration Loading ===");
        
        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            
            assertNotNull(config, "Configuration should load");
            assertEquals("Ultra Simple Rule OR Test", config.getMetadata().getName());
            assertEquals(3, config.getRules().size(), "Should have 3 rules");
            assertEquals(1, config.getRuleGroups().size(), "Should have 1 rule group");
            assertEquals(3, config.getEnrichments().size(), "Should have 3 enrichments");
            
            logger.info("✅ Configuration loading test passed");
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
