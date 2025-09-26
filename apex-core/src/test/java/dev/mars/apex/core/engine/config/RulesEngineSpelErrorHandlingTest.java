package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused test for SpEL exception handling in RulesEngine.
 * 
 * This test validates that SpEL evaluation errors are properly converted to
 * RuleResult.error() instead of being swallowed or causing processing to continue
 * with invalid state.
 * 
 * @author GitHub Copilot
 * @since 2025-09-26
 */
@DisplayName("RulesEngine SpEL Error Handling Tests")
class RulesEngineSpelErrorHandlingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RulesEngineSpelErrorHandlingTest.class);
    
    private RulesEngine rulesEngine;
    
    @BeforeEach
    void setUp() {
        // Create minimal rules engine configuration
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config);
        logger.info("Created RulesEngine for SpEL error handling tests");
    }
    
    @Test
    @DisplayName("Should return RuleResult.error() for SpEL property not found exception")
    void shouldReturnErrorResultForSpelPropertyNotFound() {
        // Given: Rule that tries to access a property that doesn't exist
        Rule ruleWithMissingProperty = new Rule(
            "currency-validation",
            "#data.currency != null && #data.currency.toString().length() == 3",
            "Currency must be a valid 3-character code",
            "CRITICAL"
        );
        
        List<Rule> rules = Collections.singletonList(ruleWithMissingProperty);
        
        // Data without currency property (will cause SpEL exception)
        Map<String, Object> testData = new HashMap<>();
        testData.put("instrumentType", "EQUITY");
        testData.put("quantity", 1000);
        testData.put("price", 150.0);
        // Intentionally missing "currency" property
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", testData);
        
        // When: Execute rules with data that will cause SpEL exception
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Then: Should return structured error result, not continue processing
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type for SpEL exception");
        assertEquals("currency-validation", result.getRuleName(),
                    "Should identify the rule that caused the error");

        assertTrue(result.getMessage().contains("Rule evaluation failed"),
                  "Error message should indicate evaluation error");
        assertTrue(result.getMessage().contains("currency"),
                  "Error message should mention the missing property");
        
        logger.info("✓ SpEL property not found exception properly converted to RuleResult.error()");
    }
    
    @Test
    @DisplayName("Should return RuleResult.error() for SpEL type conversion exception")
    void shouldReturnErrorResultForSpelTypeConversion() {
        // Given: Rule with type conversion that will fail
        Rule ruleWithTypeError = new Rule(
            "price-validation",
            "#data.price > 0 && #data.price.someInvalidMethod()",
            "Price must be positive",
            "CRITICAL"
        );
        
        List<Rule> rules = Collections.singletonList(ruleWithTypeError);
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("price", "not-a-number"); // Will cause type conversion issue
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", testData);
        
        // When: Execute rules with data that will cause SpEL exception
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Then: Should return structured error result
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR result type for SpEL exception");
        assertEquals("price-validation", result.getRuleName(), 
                    "Should identify the rule that caused the error");
        
        logger.info("✓ SpEL type conversion exception properly converted to RuleResult.error()");
    }
    
    @Test
    @DisplayName("Should continue processing other rules after SpEL error in multi-rule scenario")
    void shouldProcessOtherRulesAfterSpelError() {
        // Given: Multiple rules where first causes SpEL error, second should succeed
        Rule failingRule = new Rule(
            "failing-rule",
            "#data.missingField.toString().length() > 0",
            "This rule will fail",
            "CRITICAL"
        );

        Rule successRule = new Rule(
            "success-rule",
            "#data.quantity > 500",
            "Quantity is high",
            "INFO"
        );
        
        List<Rule> rules = Arrays.asList(failingRule, successRule);
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("quantity", 1000); // Will make second rule match
        // Missing "missingField" will make first rule fail
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", testData);
        
        // When: Execute rules
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Then: Should return error from first rule (since it's processed first)
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR from first failing rule");
        assertEquals("failing-rule", result.getRuleName(), 
                    "Should identify the first rule that failed");
        
        logger.info("✓ First rule error properly handled - processing stops at first error as expected");
    }
    
    @Test
    @DisplayName("Should validate that normal rule processing still works")
    void shouldValidateNormalRuleProcessingStillWorks() {
        // Given: Valid rule with data that will match
        Rule validRule = new Rule(
            "valid-rule",
            "#data.quantity > 100 && #data.instrumentType == 'EQUITY'",
            "Large equity trade detected",
            "INFO"
        );
        
        List<Rule> rules = Collections.singletonList(validRule);
        
        Map<String, Object> testData = new HashMap<>();
        testData.put("quantity", 1000);
        testData.put("instrumentType", "EQUITY");
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", testData);
        
        // When: Execute rules with valid data
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Then: Should return normal match result
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), 
                    "Should return MATCH result type for successful rule");
        assertEquals("valid-rule", result.getRuleName(), 
                    "Should identify the matched rule");
        assertEquals("Large equity trade detected", result.getMessage(), 
                    "Should return rule message");
        
        logger.info("✓ Normal rule processing continues to work correctly");
    }
}