package dev.mars.apex.demo.codes;

import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for map-to-field feature.
 * Tests single and multiple field mappings with SpEL expressions.
 */
public class FieldMappingValidation {

    private UnifiedRuleEvaluator evaluator;

    @BeforeEach
    public void setUp() {
        evaluator = new UnifiedRuleEvaluator();
    }

    /**
     * Test 1: Single field mapping with success code
     */
    @Test
    public void testSingleFieldMappingWithSuccessCode() {
        // Create a rule with single field mapping
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
            null,                                       // metadata
            null,                                       // defaultValue
            "AMOUNT_VALID",                             // successCode
            null,                                       // errorCode
            "validationStatus = #success_code"          // mapToField
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        assertEquals("AMOUNT_VALID", result.getSuccessCode(), "Success code should be set");
        
        // Check enriched data contains the mapped field
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("AMOUNT_VALID", enrichedData.get("validationStatus"), "validationStatus should be mapped");
    }

    /**
     * Test 2: Single field mapping with error code
     */
    @Test
    public void testSingleFieldMappingWithErrorCode() {
        // Create a rule with single field mapping
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
            null,                                       // metadata
            null,                                       // defaultValue
            null,                                       // successCode
            "AMOUNT_INVALID",                           // errorCode
            "validationStatus = #error_code"            // mapToField
        );

        // Evaluate with non-matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 50);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertFalse(result.isTriggered(), "Rule should not match");
        assertEquals("AMOUNT_INVALID", result.getErrorCode(), "Error code should be set");
        
        // Check enriched data contains the mapped field
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("AMOUNT_INVALID", enrichedData.get("validationStatus"), "validationStatus should be mapped");
    }

    /**
     * Test 3: Multiple field mappings
     */
    @Test
    public void testMultipleFieldMappings() {
        // Create a rule with multiple field mappings
        List<String> mappings = new ArrayList<>();
        mappings.add("validationStatus = #success_code");
        mappings.add("severity = 'INFO'");

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
            null,                   // metadata
            null,                   // defaultValue
            "AMOUNT_VALID",         // successCode
            null,                   // errorCode
            mappings                // mapToField (List)
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        
        // Check enriched data contains all mapped fields
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("AMOUNT_VALID", enrichedData.get("validationStatus"), "validationStatus should be mapped");
        assertEquals("INFO", enrichedData.get("severity"), "severity should be mapped");
    }

    /**
     * Test 4: Field mapping with complex SpEL expression
     */
    @Test
    public void testFieldMappingWithComplexSpelExpression() {
        // Create a rule with complex SpEL mapping
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
            null,                                                               // metadata
            null,                                                               // defaultValue
            "AMOUNT_VALID",                                                     // successCode
            null,                                                               // errorCode
            "riskLevel = #amount > 1000 ? 'HIGH' : 'NORMAL'"                   // mapToField with ternary
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        
        // Check enriched data contains the mapped field with correct value
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("HIGH", enrichedData.get("riskLevel"), "riskLevel should be HIGH");
    }

    /**
     * Test 5: Backward compatibility - no field mappings
     */
    @Test
    public void testBackwardCompatibilityNoMappings() {
        // Create a rule without field mappings
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
            null,               // metadata
            null,               // defaultValue
            "AMOUNT_VALID",     // successCode
            null,               // errorCode
            null                // mapToField
        );

        // Evaluate with matching condition
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 150);

        RuleResult result = evaluator.evaluateRule(rule, facts);

        assertTrue(result.isTriggered(), "Rule should match");
        assertEquals("AMOUNT_VALID", result.getSuccessCode(), "Success code should be set");
        
        // Enriched data should be empty (no mappings)
        Map<String, Object> enrichedData = result.getEnrichedData();
        assertNotNull(enrichedData, "Enriched data should not be null");
        assertTrue(enrichedData.isEmpty(), "Enriched data should be empty");
    }
}

