package dev.mars.apex.demo.codes;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for map-to-field feature.
 * Tests single and multiple field mappings with SpEL expressions.
 * Rules are loaded from external YAML configuration file.
 */
public class FieldMappingValidation {

    private static final Logger logger = LoggerFactory.getLogger(FieldMappingValidation.class);
    private UnifiedRuleEvaluator evaluator;
    private YamlConfigurationLoader yamlLoader;
    private YamlRuleConfiguration config;
    private YamlRuleFactory ruleFactory;

    @BeforeEach
    public void setUp() throws Exception {
        evaluator = new UnifiedRuleEvaluator();
        yamlLoader = new YamlConfigurationLoader();
        ruleFactory = new YamlRuleFactory();
        config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/codes/FieldMappingValidation-rules.yaml");
        assertNotNull(config, "Configuration should be loaded");
        logger.info("✓ Configuration loaded: {} rules", config.getRules().size());
    }

    /**
     * Helper method to get a rule by ID from the configuration.
     */
    private Rule getRuleById(String ruleId) {
        if (config.getRules() == null) {
            return null;
        }
        return config.getRules().stream()
            .filter(yamlRule -> ruleId.equals(yamlRule.getId()))
            .map(ruleFactory::createRuleWithMetadata)
            .findFirst()
            .orElse(null);
    }

    /**
     * Test 1: Single field mapping with success code
     */
    @Test
    public void testSingleFieldMappingWithSuccessCode() {
        logger.info("=== Test 1: Single Field Mapping with Success Code ===");
        logger.info("Scenario: Rule matches and maps success code to single field");

        // Get rule from YAML configuration
        Rule rule = getRuleById("single-field-mapping-success");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("MAP-TO-FIELD directive: validationStatus = #success_code");

        logger.info("");
        logger.info("BEFORE RULE EVALUATION:");
        logger.info("  Dataset: {}", facts);

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");
        assertEquals("AMOUNT_VALID", result.getSuccessCode(), "Success code should be set");
        logger.info("✓ Success code: {}", result.getSuccessCode());

        // Check enriched data contains the mapped field
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("AMOUNT_VALID", enrichedData.get("validationStatus"), "validationStatus should be mapped");
        logger.info("");
        logger.info("AFTER RULE EVALUATION (with map-to-field applied):");
        logger.info("  Dataset: {}", enrichedData);
        logger.info("");
        logger.info("✓ Field mapping applied: validationStatus={}", enrichedData.get("validationStatus"));
    }

    /**
     * Test 2: Single field mapping with error code
     */
    @Test
    public void testSingleFieldMappingWithErrorCode() {
        logger.info("=== Test 2: Single Field Mapping with Error Code ===");
        logger.info("Scenario: Rule does not match and maps error code to single field");

        // Get rule from YAML configuration
        Rule rule = getRuleById("single-field-mapping-error");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with non-matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 50);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should NOT MATCH)");
        logger.info("MAP-TO-FIELD directive: validationStatus = #error_code");

        logger.info("");
        logger.info("BEFORE RULE EVALUATION:");
        logger.info("  Dataset: {}", facts);

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertFalse(result.isTriggered(), "Rule should not match");
        logger.info("✓ Rule triggered: false");
        assertEquals("AMOUNT_INVALID", result.getErrorCode(), "Error code should be set");
        logger.info("✓ Error code: {}", result.getErrorCode());

        // Check enriched data contains the mapped field
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("AMOUNT_INVALID", enrichedData.get("validationStatus"), "validationStatus should be mapped");
        logger.info("");
        logger.info("AFTER RULE EVALUATION (with map-to-field applied):");
        logger.info("  Dataset: {}", enrichedData);
        logger.info("");
        logger.info("✓ Field mapping applied: validationStatus={}", enrichedData.get("validationStatus"));
    }

    /**
     * Test 3: Multiple field mappings
     */
    @Test
    public void testMultipleFieldMappings() {
        logger.info("=== Test 3: Multiple Field Mappings ===");
        logger.info("Scenario: Rule matches and maps success code to multiple fields");

        // Get rule from YAML configuration
        Rule rule = getRuleById("multiple-field-mappings");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("MAP-TO-FIELD directives:");
        logger.info("  1. validationStatus = #success_code");
        logger.info("  2. severity = 'INFO'");

        logger.info("");
        logger.info("BEFORE RULE EVALUATION:");
        logger.info("  Dataset: {}", facts);

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");

        // Check enriched data contains all mapped fields
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("AMOUNT_VALID", enrichedData.get("validationStatus"), "validationStatus should be mapped");
        assertEquals("INFO", enrichedData.get("severity"), "severity should be mapped");
        logger.info("");
        logger.info("AFTER RULE EVALUATION (with map-to-field applied):");
        logger.info("  Dataset: {}", enrichedData);
        logger.info("");
        logger.info("✓ Field mapping 1: validationStatus={}", enrichedData.get("validationStatus"));
        logger.info("✓ Field mapping 2: severity={}", enrichedData.get("severity"));
    }

    /**
     * Test 4: Field mapping with complex SpEL expression
     */
    @Test
    public void testFieldMappingWithComplexSpelExpression() {
        logger.info("=== Test 4: Field Mapping with Complex SpEL Expression ===");
        logger.info("Scenario: Field mapping uses SpEL expression to determine mapped value");

        // Get rule from YAML configuration
        Rule rule = getRuleById("complex-spel-mapping");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("Field mapping expression: #amount > 1000 ? 'HIGH' : 'LOW'");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");

        // Check enriched data contains the mapped field with correct value
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("HIGH", enrichedData.get("riskLevel"), "riskLevel should be HIGH");
        logger.info("✓ Field mapping applied with SpEL: riskLevel={} (1500 > 1000)", enrichedData.get("riskLevel"));
    }

    /**
     * Test 5: Backward compatibility - no field mappings
     */
    @Test
    public void testBackwardCompatibilityNoMappings() {
        logger.info("=== Test 5: Backward Compatibility - No Field Mappings ===");
        logger.info("Scenario: Rule has success code but no field mappings (backward compatibility)");

        // Get rule from YAML configuration
        Rule rule = getRuleById("backward-compatibility-no-mappings");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("Field mappings: NONE (backward compatibility test)");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");
        assertEquals("AMOUNT_VALID", result.getSuccessCode(), "Success code should be set");
        logger.info("✓ Success code: {}", result.getSuccessCode());

        // Enriched data should be empty (no mappings)
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData.isEmpty(), "Enriched data should be empty");
        logger.info("✓ Enriched data is empty (no field mappings defined)");
    }
}

