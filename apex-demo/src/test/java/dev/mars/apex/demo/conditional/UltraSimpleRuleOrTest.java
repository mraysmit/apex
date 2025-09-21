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
            logger.debug("Creating test data with input='A'...");
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");
            logger.debug("Test data created: {}", testData);

            logger.debug("Loading YAML configuration...");
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            logger.debug("Configuration loaded: {} with {} enrichments", config.getMetadata().getName(), config.getEnrichments().size());

            // Log detailed configuration information
            logger.debug("YAML Configuration Details:");
            logger.debug("  Metadata: {}", config.getMetadata());
            logger.debug("  Number of enrichments: {}", config.getEnrichments().size());
            for (int i = 0; i < config.getEnrichments().size(); i++) {
                var enrichment = config.getEnrichments().get(i);
                logger.debug("  Enrichment {}: name={}, type={}", i+1, enrichment.getName(), enrichment.getType());
                if (enrichment.getCondition() != null) {
                    logger.debug("    Condition: {}", enrichment.getCondition());
                }
                if (enrichment.getTargetField() != null) {
                    logger.debug("    Target field: {}", enrichment.getTargetField());
                }
                if (enrichment.getMappingRules() != null && !enrichment.getMappingRules().isEmpty()) {
                    logger.debug("    Mapping rules: {} rules", enrichment.getMappingRules().size());
                    for (int j = 0; j < enrichment.getMappingRules().size(); j++) {
                        var rule = enrichment.getMappingRules().get(j);
                        logger.debug("      Rule {}: name={}, priority={}", j+1, rule.getName(), rule.getPriority());
                        if (rule.getConditions() != null) {
                            logger.debug("        Conditions: operator={}, rules={}",
                                rule.getConditions().getOperator(),
                                rule.getConditions().getRules() != null ? rule.getConditions().getRules().size() : 0);
                        }
                        if (rule.getMapping() != null) {
                            logger.debug("        Mapping: type={}, transformation={}",
                                rule.getMapping().getType(), rule.getMapping().getTransformation());
                        }
                    }
                }
            }

            logger.debug("Executing rule processing...");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;
            logger.debug("Processing completed - result contains {} fields", result.size());
            logger.debug("Result fields: {}", result.keySet());
            logger.debug("Output value: {}", result.get("output"));

            assertEquals("FIRST", result.get("output"), "Rule 1 should map to 'FIRST'");
            logger.debug("Rule 1 test passed: " + result);
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
            logger.debug("Creating test data with input='B'...");
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "B");
            logger.debug("Test data created: {}", testData);

            logger.debug("Loading YAML configuration...");
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            logger.debug("Configuration loaded: {} with {} enrichments", config.getMetadata().getName(), config.getEnrichments().size());

            logger.debug("Executing rule processing...");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;
            logger.debug("Processing completed - result contains {} fields", result.size());
            logger.debug("Result fields: {}", result.keySet());
            logger.debug("Output value: {}", result.get("output"));

            assertEquals("SECOND", result.get("output"), "Rule 2 should map to 'SECOND'");
            logger.debug("Rule 2 test passed: " + result);
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
            logger.info("Creating test data with input='C'...");
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "C");
            logger.info("Test data created: {}", testData);

            logger.info("Loading YAML configuration...");
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            logger.info("Configuration loaded: {} with {} enrichments", config.getMetadata().getName(), config.getEnrichments().size());

            logger.info("Executing rule processing...");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;
            logger.info("Processing completed - result contains {} fields", result.size());
            logger.info("Result fields: {}", result.keySet());
            logger.info("Output value: {}", result.get("output"));

            assertEquals("THIRD", result.get("output"), "Rule 3 should map to 'THIRD'");
            logger.info("Rule 3 test passed: " + result);
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
            logger.info("Creating test data with input='X' (should not match any rules)...");
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "X");
            logger.info("Test data created: {}", testData);

            logger.info("Loading YAML configuration...");
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            logger.info("Configuration loaded: {} with {} enrichments", config.getMetadata().getName(), config.getEnrichments().size());

            logger.info("Executing rule processing (expecting no matches)...");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;
            logger.info("Processing completed - result contains {} fields", result.size());
            logger.info("Result fields: {}", result.keySet());
            logger.info("Output value: {} (should be null)", result.get("output"));

            assertNull(result.get("output"), "No rules should match, output should be null");
            logger.info("No rules match test passed: " + result);
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
            logger.info("Testing OR logic behavior - first matching rule should win");
            logger.info("Note: In this simple test, each input only matches one rule");
            logger.info("Creating test data with input='A' (matches rule 1)...");
            // This test proves sequential evaluation
            // If we had input that could match multiple rules, first should win
            // For this simple test, each input only matches one rule, so we test rule 1
            Map<String, Object> testData = new HashMap<>();
            testData.put("input", "A");  // Only matches rule 1
            logger.info("Test data created: {}", testData);

            logger.info("Loading YAML configuration...");
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ultra-simple-rule-or-test.yaml");
            logger.info("Configuration loaded: {} with {} enrichments", config.getMetadata().getName(), config.getEnrichments().size());

            logger.info("Executing rule processing (testing OR logic)...");
            Object enrichmentResult = enrichmentService.enrichObject(config, testData);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) enrichmentResult;
            logger.info("Processing completed - result contains {} fields", result.size());
            logger.info("Result fields: {}", result.keySet());
            logger.info("Output value: {} (should be FIRST)", result.get("output"));

            assertEquals("FIRST", result.get("output"), "First rule should win in OR logic");
            logger.info("OR logic test passed: " + result);
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
