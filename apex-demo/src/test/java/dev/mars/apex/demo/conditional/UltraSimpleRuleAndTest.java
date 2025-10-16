package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ultra Simple Rule AND Test - Demonstrates rule groups with AND logic
 * 
 * Tests 3 rules in sequence with AND logic where ALL rules must pass for the 
 * group to pass. Demonstrates stop-on-first-failure behavior.
 * 
 * Key Features:
 * - Rule group with AND operator
 * - All rules must pass for group to pass
 * - Stop-on-first-failure: true (stops evaluating when first rule fails)
 * - Comparison with OR logic (UltraSimpleRuleOrTest)
 */
@DisplayName("Ultra Simple Rule AND Test")
public class UltraSimpleRuleAndTest extends DemoTestBase {

    @Test
    @DisplayName("Should pass when all rules match (input=ABC)")
    void testAllRulesMatch() {
        logger.info("=== Testing Rule AND: input='ABC' -> ALL_PASSED ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "ABC");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UltraSimpleRuleAndTest.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("ALL_PASSED", result.get("output"), "Should output ALL_PASSED when all rules match");
            logger.info("✅ Rule AND ABC test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should fail when first rule fails (input=X)")
    void testFirstRuleFails() {
        logger.info("=== Testing Rule AND: input='X' -> FAILED (first rule fails) ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "X");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UltraSimpleRuleAndTest.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("FAILED", result.get("output"), "Should output FAILED when first rule fails");
            logger.info("✅ Rule AND X test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should fail when second rule fails (input=A)")
    void testSecondRuleFails() {
        logger.info("=== Testing Rule AND: input='A' -> FAILED (second rule fails) ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UltraSimpleRuleAndTest.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("FAILED", result.get("output"), "Should output FAILED when second rule fails");
            logger.info("✅ Rule AND A test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should fail when third rule fails (input=AB)")
    void testThirdRuleFails() {
        logger.info("=== Testing Rule AND: input='AB' -> FAILED (third rule fails) ===");
        
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "AB");

            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/UltraSimpleRuleAndTest.yaml");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;

            assertEquals("FAILED", result.get("output"), "Should output FAILED when third rule fails");
            logger.info("✅ Rule AND AB test passed: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}

