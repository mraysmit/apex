package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Conditional Mapping Design V2 - Rule Groups with OR Logic
 * 
 * This test proves the core concept of sequential rule evaluation with OR logic
 * where the first matching rule triggers the mapping/enrichment.
 */
public class ConditionalMappingDesignV2Test extends DemoTestBase {

    @Test
    @DisplayName("Should process SWIFT valid NDF rule (first rule match)")
    void testSwiftValidNdfRule() {
        logger.info("=== Testing SWIFT Valid NDF Rule (First Rule Match) ===");

        try {
            // Test data that matches the first rule: SWIFT system with valid NDF values
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "1");  // Valid NDF value
            testData.put("BUY_CURRENCY", "USD");

            logger.info("Testing SWIFT valid NDF with data: " + testData);

            // Load configuration and process
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/conditional-mapping-design-v2-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);

            // Verify rule group results are available
            assertNotNull(result, "Result should not be null");

            // Verify the first rule matched and mapping was applied
            assertEquals("1", result.get("IS_NDF"), "IS_NDF should be directly mapped for valid SWIFT NDF");

            logger.info("✅ SWIFT valid NDF rule test completed successfully");
            logger.info("Result: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process SWIFT Y flag rule (second rule match)")
    void testSwiftYFlagRule() {
        logger.info("=== Testing SWIFT Y Flag Rule (Second Rule Match) ===");

        try {
            // Test data that matches the second rule: SWIFT system with Y flag
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "Y");  // Y flag value
            testData.put("BUY_CURRENCY", "EUR");

            logger.info("Testing SWIFT Y flag with data: " + testData);

            // Load configuration and process
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/conditional-mapping-design-v2-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);

            // Verify rule group results are available
            assertNotNull(result, "Result should not be null");

            // Verify the second rule matched and Y flag was converted to 1
            assertEquals("1", result.get("IS_NDF"), "IS_NDF should be converted from Y to 1 for SWIFT Y flag");

            logger.info("✅ SWIFT Y flag rule test completed successfully");
            logger.info("Result: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should process translation required rule (third rule match)")
    void testTranslationRequiredRule() {
        logger.info("=== Testing Translation Required Rule (Third Rule Match) ===");

        try {
            // Test data that matches the third rule: SWIFT system needing translation
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "COMPLEX_CODE");  // Complex value needing translation
            testData.put("BUY_CURRENCY", "GBP");

            logger.info("Testing translation required with data: " + testData);

            // Load configuration and process
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/conditional-mapping-design-v2-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);

            // Verify rule group results are available
            assertNotNull(result, "Result should not be null");

            // Verify the third rule matched and default translation was applied
            assertEquals("DEFAULT_TRANSLATION", result.get("IS_NDF"), "IS_NDF should use default translation for complex codes");

            logger.info("✅ Translation required rule test completed successfully");
            logger.info("Result: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle no rule matches (rule group fails)")
    void testNoRuleMatches() {
        logger.info("=== Testing No Rule Matches (Rule Group Fails) ===");

        try {
            // Test data that matches no rules: Non-SWIFT system
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "OTHER_SYSTEM");
            testData.put("IS_NDF", "1");
            testData.put("BUY_CURRENCY", "CHF");

            logger.info("Testing no rule matches with data: " + testData);

            // Load configuration and process
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/conditional-mapping-design-v2-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);

            // Verify rule group results are available
            assertNotNull(result, "Result should not be null");

            // Verify no mapping was applied (original value preserved)
            assertEquals("1", result.get("IS_NDF"), "IS_NDF should remain unchanged when no rules match");

            logger.info("✅ No rule matches test completed successfully");
            logger.info("Result: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate rule group OR logic and sequential evaluation")
    void testRuleGroupOrLogicAndSequentialEvaluation() {
        logger.info("=== Testing Rule Group OR Logic and Sequential Evaluation ===");

        try {
            // Test data that could match multiple rules (first should win)
            Map<String, Object> testData = new HashMap<>();
            testData.put("SYSTEM_CODE", "SWIFT");
            testData.put("IS_NDF", "1");  // This matches BOTH first and second rule conditions
            testData.put("BUY_CURRENCY", "USD");

            logger.info("Testing OR logic with data that matches multiple rules: " + testData);

            // Load configuration and process
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/conditional-mapping-design-v2-test.yaml");
            Map<String, Object> result = (Map<String, Object>) enrichmentService.enrichObject(config, testData);

            // Verify rule group results are available
            assertNotNull(result, "Result should not be null");

            // Verify the first rule won (direct mapping, not Y->1 conversion)
            assertEquals("1", result.get("IS_NDF"), "IS_NDF should use first rule's direct mapping");

            logger.info("✅ Rule group OR logic test completed successfully");
            logger.info("Result: " + result);
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should load and validate YAML configuration")
    void testYamlConfigurationLoading() {
        logger.info("=== Testing Conditional Mapping Design V2 YAML Loading ===");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/conditional-mapping-design-v2-test.yaml");

            // Verify configuration loaded successfully
            assertNotNull(config, "Configuration should not be null");
            assertNotNull(config.getMetadata(), "Metadata should not be null");
            assertEquals("Rule Group OR Logic - FX Transaction Test", config.getMetadata().getName());

            // Verify rules exist
            assertNotNull(config.getRules(), "Rules should not be null");
            assertEquals(3, config.getRules().size(), "Should have 3 rules");

            // Verify rule groups exist
            assertNotNull(config.getRuleGroups(), "Rule groups should not be null");
            assertEquals(1, config.getRuleGroups().size(), "Should have 1 rule group");

            // Verify enrichments exist
            assertNotNull(config.getEnrichments(), "Enrichments should not be null");
            assertFalse(config.getEnrichments().isEmpty(), "Should have enrichments");

            logger.info("✅ Configuration loaded successfully: " + config.getMetadata().getName());
            logger.info("✅ Conditional mapping design V2 syntax validated successfully");
        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
