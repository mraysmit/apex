package dev.mars.apex.demo.codes;

import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for success-code and error-code features.
 * Tests both constant codes and SpEL expression codes.
 */
public class SuccessErrorCodesValidation {

    private UnifiedRuleEvaluator evaluator;

    @BeforeEach
    public void setUp() {
        evaluator = new UnifiedRuleEvaluator();
    }

    /**
     * Test 1: Constant success code when rule matches
     */
    @Test
    public void testConstantSuccessCodeOnMatch() {
        // Create a rule with constant success code
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("test", 100));

        Rule rule = new Rule(
            "test-rule",
            categories,
            "Test Rule",
            "#amount > 100",
            "Amount is greater than 100",
            "Test description",
            100,
            "INFO",
            null,                           // metadata
            null,                           // defaultValue
            "SUCCESS_AMOUNT_VALID",         // successCode
            null,                           // errorCode
            null                            // mapToField
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        assertEquals("SUCCESS_AMOUNT_VALID", result.getSuccessCode(), "Success code should be set");
        assertNull(result.getErrorCode(), "Error code should be null");
    }

    /**
     * Test 2: Constant error code when rule does not match
     */
    @Test
    public void testConstantErrorCodeOnNoMatch() {
        // Create a rule with constant error code
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("test", 100));

        Rule rule = new Rule(
            "test-rule",
            categories,
            "Test Rule",
            "#amount > 100",
            "Amount is greater than 100",
            "Test description",
            100,
            "INFO",
            null,                           // metadata
            null,                           // defaultValue
            null,                           // successCode
            "ERROR_AMOUNT_INVALID",         // errorCode
            null                            // mapToField
        );

        // Evaluate with non-matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 50);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertFalse(result.isTriggered(), "Rule should not match");
        assertNull(result.getSuccessCode(), "Success code should be null");
        assertEquals("ERROR_AMOUNT_INVALID", result.getErrorCode(), "Error code should be set");
    }

    /**
     * Test 3: SpEL expression success code
     */
    @Test
    public void testSpelExpressionSuccessCode() {
        // Create a rule with SpEL success code
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("test", 100));

        Rule rule = new Rule(
            "test-rule",
            categories,
            "Test Rule",
            "#amount > 100",
            "Amount is greater than 100",
            "Test description",
            100,
            "INFO",
            null,                                                // metadata
            null,                                                // defaultValue
            "#amount > 1000 ? 'HIGH_VALUE' : 'NORMAL_VALUE'",  // successCode with SpEL
            null,                                                // errorCode
            null                                                 // mapToField
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        assertEquals("HIGH_VALUE", result.getSuccessCode(), "Success code should be HIGH_VALUE");
    }

    /**
     * Test 4: Backward compatibility - no codes specified
     */
    @Test
    public void testBackwardCompatibilityNoCodes() {
        // Create a rule without codes (backward compatibility)
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("test", 100));

        Rule rule = new Rule(
            "test-rule",
            categories,
            "Test Rule",
            "#amount > 100",
            "Amount is greater than 100",
            "Test description",
            100,
            "INFO",
            null,           // metadata
            null,           // defaultValue
            null,           // successCode
            null,           // errorCode
            null            // mapToField
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        assertNull(result.getSuccessCode(), "Success code should be null");
        assertNull(result.getErrorCode(), "Error code should be null");
    }

    /**
     * Test 5: Invalid SpEL expression in code (should handle gracefully)
     */
    @Test
    public void testInvalidSpelExpressionInCode() {
        // Create a rule with invalid SpEL code
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("test", 100));

        Rule rule = new Rule(
            "test-rule",
            categories,
            "Test Rule",
            "#amount > 100",
            "Amount is greater than 100",
            "Test description",
            100,
            "INFO",
            null,                           // metadata
            null,                           // defaultValue
            "#invalidVariable.method()",    // Invalid SpEL
            null,                           // errorCode
            null                            // mapToField
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        assertNull(result.getSuccessCode(), "Success code should be null due to invalid SpEL");
    }
}

