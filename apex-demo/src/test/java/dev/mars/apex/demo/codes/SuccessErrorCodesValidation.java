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
 * Comprehensive tests for success-code and error-code features.
 * Tests both constant codes and SpEL expression codes.
 * Rules are loaded from external YAML configuration file.
 */
public class SuccessErrorCodesValidation {

    private static final Logger logger = LoggerFactory.getLogger(SuccessErrorCodesValidation.class);
    private UnifiedRuleEvaluator evaluator;
    private YamlConfigurationLoader yamlLoader;
    private YamlRuleConfiguration config;
    private YamlRuleFactory ruleFactory;

    @BeforeEach
    public void setUp() throws Exception {
        evaluator = new UnifiedRuleEvaluator();
        yamlLoader = new YamlConfigurationLoader();
        ruleFactory = new YamlRuleFactory();
        config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/codes/SuccessErrorCodesValidation-rules.yaml");
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
     * Test 1: Constant success code when rule matches
     */
    @Test
    public void testConstantSuccessCodeOnMatch() {
        logger.info("=== Test 1: Constant Success Code on Match ===");
        logger.info("Scenario: Rule matches and returns constant success code");

        // Get rule from YAML configuration
        Rule rule = getRuleById("constant-success-code");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("Success code: SUCCESS_AMOUNT_VALID (constant)");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");
        assertEquals("SUCCESS_AMOUNT_VALID", result.getSuccessCode(), "Success code should be set");
        logger.info("✓ Success code: {}", result.getSuccessCode());
        assertNull(result.getErrorCode(), "Error code should be null");
        logger.info("✓ Error code: null (not applicable when rule matches)");
    }

    /**
     * Test 2: Constant error code when rule does not match
     */
    @Test
    public void testConstantErrorCodeOnNoMatch() {
        logger.info("=== Test 2: Constant Error Code on No Match ===");
        logger.info("Scenario: Rule does not match and returns constant error code");

        // Get rule from YAML configuration
        Rule rule = getRuleById("constant-error-code");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with non-matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 50);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should NOT MATCH)");
        logger.info("Error code: ERROR_AMOUNT_INVALID (constant)");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertFalse(result.isTriggered(), "Rule should not match");
        logger.info("✓ Rule triggered: false");
        assertNull(result.getSuccessCode(), "Success code should be null");
        logger.info("✓ Success code: null (not applicable when rule fails)");
        assertEquals("ERROR_AMOUNT_INVALID", result.getErrorCode(), "Error code should be set");
        logger.info("✓ Error code: {}", result.getErrorCode());
    }

    /**
     * Test 3: SpEL expression success code
     */
    @Test
    public void testSpelExpressionSuccessCode() {
        logger.info("=== Test 3: SpEL Expression Success Code ===");
        logger.info("Scenario: Success code is determined by SpEL expression");

        // Get rule from YAML configuration
        Rule rule = getRuleById("spel-expression-success-code");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("Success code expression: #amount > 1000 ? 'HIGH_VALUE' : 'NORMAL_VALUE'");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");
        assertEquals("HIGH_VALUE", result.getSuccessCode(), "Success code should be HIGH_VALUE");
        logger.info("✓ Success code: {} (1500 > 1000)", result.getSuccessCode());
    }

    /**
     * Test 4: Backward compatibility - no codes specified
     */
    @Test
    public void testBackwardCompatibilityNoCodes() {
        logger.info("=== Test 4: Backward Compatibility - No Codes Specified ===");
        logger.info("Scenario: Rule has no success-code or error-code (backward compatibility)");

        // Get rule from YAML configuration
        Rule rule = getRuleById("backward-compatibility-no-codes");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("Success code: NOT SPECIFIED");
        logger.info("Error code: NOT SPECIFIED");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");
        assertNull(result.getSuccessCode(), "Success code should be null");
        logger.info("✓ Success code: null (not specified)");
        assertNull(result.getErrorCode(), "Error code should be null");
        logger.info("✓ Error code: null (not specified)");
    }

    /**
     * Test 5: Invalid SpEL expression in code (should handle gracefully)
     */
    @Test
    public void testInvalidSpelExpressionInCode() {
        logger.info("=== Test 5: Invalid SpEL Expression in Code ===");
        logger.info("Scenario: Success code contains invalid SpEL expression (error handling)");

        // Get rule from YAML configuration
        Rule rule = getRuleById("invalid-spel-expression-code");
        assertNotNull(rule, "Rule should be found in configuration");
        logger.info("Rule loaded: {}", rule.getId());

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);
        logger.info("Input facts: amount={}", facts.get("amount"));
        logger.info("Rule condition: #amount > 100 (should MATCH)");
        logger.info("Success code expression: #invalidVariable.method() (INVALID - variable doesn't exist)");

        RuleResult result = evaluator.evaluateRule(rule, facts);
        logger.info("Rule evaluation completed");

        assertTrue(result.isTriggered(), "Rule should match");
        logger.info("✓ Rule triggered: true");
        assertNull(result.getSuccessCode(), "Success code should be null due to invalid SpEL");
        logger.info("✓ Success code: null (invalid SpEL expression handled gracefully)");
        logger.info("✓ Error handling: Invalid SpEL expressions are caught and ignored");
    }
}

