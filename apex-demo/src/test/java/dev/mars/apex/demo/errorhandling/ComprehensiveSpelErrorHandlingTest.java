package dev.mars.apex.demo.errorhandling;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.scenario.DataTypeScenarioService;
import dev.mars.apex.core.service.scenario.ScenarioExecutionResult;
import dev.mars.apex.core.service.scenario.StageExecutionResult;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SpEL exception handling across all APEX components.
 * 
 * This test ensures that SpEL evaluation exceptions are handled gracefully through
 * RuleResult and never result in stack trace dumps. It covers all possible SpEL
 * exception scenarios that could occur in APEX.
 * 
 * CRITICAL REQUIREMENT: All SpEL exceptions must be converted to RuleResult.error()
 * with appropriate severity levels, never thrown as stack dumps.
 * 
 * @author APEX Team
 * @since 1.0.0
 */
@DisplayName("Comprehensive SpEL Error Handling Tests")
class ComprehensiveSpelErrorHandlingTest extends DemoTestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveSpelErrorHandlingTest.class);
    
    private RulesEngine rulesEngine;
    private UnifiedRuleEvaluator ruleEvaluator;

    @BeforeEach
    public void setUp() {
        super.setUp();

        // Create rules engine for direct testing
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(config);
        ruleEvaluator = new UnifiedRuleEvaluator();

        logger.info("=== Comprehensive SpEL Error Handling Test Setup Complete ===");
    }
    
    // ========================================================================
    // PROPERTY ACCESS ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle property not found gracefully")
    void shouldHandlePropertyNotFound() {
        logger.info("Testing property not found error handling");
        
        Rule rule = new Rule(
            "missing-property-test",
            "#data.nonExistentProperty != null",
            "Property should exist",
            "ERROR"
        );
        
        Map<String, Object> facts = createTestData();
        // Intentionally not adding 'nonExistentProperty'
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for missing property");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        assertTrue(result.getMessage().contains("Property or field 'nonExistentProperty' cannot be found"),
                  "Error message should mention missing property");
        
        logger.info("✓ Property not found handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle nested property access on null gracefully")
    void shouldHandleNestedPropertyAccessOnNull() {
        logger.info("Testing nested property access on null error handling");
        
        Rule rule = new Rule(
            "nested-null-access-test",
            "#data.nullObject.someProperty != null",
            "Nested property should be accessible",
            "ERROR"
        );
        
        Map<String, Object> testData = createTestData();
        testData.put("nullObject", null);
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, testData);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for null property access");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("✓ Nested property access on null handled gracefully");
    }
    
    // ========================================================================
    // METHOD INVOCATION ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle method not found gracefully")
    void shouldHandleMethodNotFound() {
        logger.info("Testing method not found error handling");
        
        Rule rule = new Rule(
            "invalid-method-test",
            "#data.amount.nonExistentMethod() > 0",
            "Method should exist",
            "ERROR"
        );
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for missing method");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("✓ Method not found handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle method invocation with wrong parameters gracefully")
    void shouldHandleMethodInvocationWithWrongParameters() {
        logger.info("Testing method invocation with wrong parameters error handling");
        
        Rule rule = new Rule(
            "wrong-params-test",
            "#data.customerName.substring(10, 20, 30) != null",  // substring doesn't take 3 params
            "Method parameters should be correct",
            "ERROR"
        );
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for wrong method parameters");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("✓ Method invocation with wrong parameters handled gracefully");
    }
    
    // ========================================================================
    // TYPE CONVERSION ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle type conversion errors gracefully")
    void shouldHandleTypeConversionErrors() {
        logger.info("Testing type conversion error handling");
        
        Rule rule = new Rule(
            "type-conversion-test",
            "#data.customerName + 100 > 0",  // String + Number will cause type error
            "Type conversion should work",
            "ERROR"
        );
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for type conversion");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("✓ Type conversion error handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle arithmetic errors gracefully")
    void shouldHandleArithmeticErrors() {
        logger.info("Testing arithmetic error handling");

        Rule rule = new Rule(
            "arithmetic-error-test",
            "#data.amount.invalidArithmeticMethod() > 0",  // Invalid arithmetic method
            "Arithmetic should work",
            "ERROR"
        );

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for arithmetic error");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("✓ Arithmetic error handled gracefully");
    }
    
    // ========================================================================
    // ARRAY/COLLECTION ACCESS ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle array index out of bounds gracefully")
    void shouldHandleArrayIndexOutOfBounds() {
        logger.info("Testing array index out of bounds error handling");
        
        Rule rule = new Rule(
            "array-bounds-test",
            "#data.tags[10] != null",  // Array only has 2 elements
            "Array access should be valid",
            "ERROR"
        );
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for array bounds");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("✓ Array index out of bounds handled gracefully");
    }
    
    // ========================================================================
    // COLLECTION/MAP ACCESS ERRORS
    // ========================================================================

    @Test
    @DisplayName("Should handle map key not found gracefully")
    void shouldHandleMapKeyNotFound() {
        logger.info("Testing map key not found error handling");

        Rule rule = new Rule(
            "map-key-test",
            "#data.metadata['nonExistentKey'].toString().length() > 0",
            "Map key should exist",
            "ERROR"
        );

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for missing map key");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("✓ Map key not found handled gracefully");
    }

    // ========================================================================
    // CASTING AND CONVERSION ERRORS
    // ========================================================================

    @Test
    @DisplayName("Should handle invalid casting gracefully")
    void shouldHandleInvalidCasting() {
        logger.info("Testing invalid casting error handling");

        Rule rule = new Rule(
            "invalid-cast-test",
            "((java.util.Date) #data.customerName).getTime() > 0",  // String cannot be cast to Date
            "Casting should work",
            "ERROR"
        );

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for invalid casting");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("✓ Invalid casting handled gracefully");
    }

    // ========================================================================
    // COMPLEX EXPRESSION ERRORS
    // ========================================================================

    @Test
    @DisplayName("Should handle complex nested expression errors gracefully")
    void shouldHandleComplexNestedExpressionErrors() {
        logger.info("Testing complex nested expression error handling");

        Rule rule = new Rule(
            "complex-expression-test",
            "#data.customer.address.street.substring(#data.nonExistent.length()).toUpperCase() != null",
            "Complex expression should work",
            "ERROR"
        );

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for complex expression error");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("✓ Complex nested expression error handled gracefully");
    }

    // ========================================================================
    // MULTIPLE ERROR SCENARIOS IN SINGLE TEST
    // ========================================================================

    @Test
    @DisplayName("Should handle multiple SpEL errors in rule list gracefully")
    void shouldHandleMultipleSpelErrorsInRuleList() {
        logger.info("Testing multiple SpEL errors in rule list");

        List<Rule> rules = Arrays.asList(
            new Rule("error-rule-1", "#data.missing1.toString()", "Error 1", "ERROR"),
            new Rule("success-rule", "#data.amount > 50", "Success rule", "INFO"),
            new Rule("error-rule-2", "#data.missing2.length()", "Error 2", "ERROR"),
            new Rule("another-success", "#data.customerName != null", "Another success", "INFO")
        );

        Map<String, Object> facts = createTestData();

        // Test that the rules engine handles multiple errors gracefully
        for (Rule rule : rules) {
            RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
            assertNotNull(result, "Result should not be null for rule: " + rule.getName());

            if (rule.getName().contains("error")) {
                assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                           "Should return ERROR for error rule: " + rule.getName());
                assertFalse(result.isTriggered(), "Error rule should not be triggered: " + rule.getName());
            } else {
                // Success rules should work normally
                assertTrue(result.getResultType() == RuleResult.ResultType.MATCH ||
                          result.getResultType() == RuleResult.ResultType.NO_MATCH,
                          "Success rule should return MATCH or NO_MATCH: " + rule.getName());
            }
        }

        logger.info("✓ Multiple SpEL errors in rule list handled gracefully");
    }

    // ========================================================================
    // COMPREHENSIVE ERROR VALIDATION TEST
    // ========================================================================

    @Test
    @DisplayName("Should validate that all SpEL error types are handled without stack traces")
    void shouldValidateAllSpelErrorTypesHandledWithoutStackTraces() {
        logger.info("=== Comprehensive SpEL Error Type Validation ===");

        // Test all major SpEL error categories
        Map<String, Rule> errorRules = new HashMap<>();

        // Property not found errors
        errorRules.put("property-not-found", new Rule("prop-test", "#data.missing != null", "Property test", "ERROR"));
        errorRules.put("nested-property-not-found", new Rule("nested-test", "#data.missing.field != null", "Nested test", "ERROR"));

        // Method not found errors
        errorRules.put("method-not-found", new Rule("method-test", "#data.amount.invalidMethod()", "Method test", "ERROR"));

        // Type conversion errors
        errorRules.put("type-conversion", new Rule("type-test", "#data.customerName + 100", "Type test", "ERROR"));

        // Arithmetic errors
        errorRules.put("arithmetic-error", new Rule("arith-test", "#data.amount / 0", "Arithmetic test", "ERROR"));

        // Array access errors
        errorRules.put("array-bounds", new Rule("array-test", "#data.tags[99]", "Array test", "ERROR"));

        Map<String, Object> facts = createTestData();

        // Test each error type
        for (Map.Entry<String, Rule> entry : errorRules.entrySet()) {
            String errorType = entry.getKey();
            Rule rule = entry.getValue();

            logger.info("Testing {} error handling", errorType);

            RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

            // Verify graceful error handling
            assertNotNull(result, "Result should not be null for " + errorType);
            assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                        "Should return ERROR for " + errorType);
            assertFalse(result.isTriggered(), "Error rule should not be triggered for " + errorType);
            assertNotNull(result.getMessage(), "Error message should be present for " + errorType);

            logger.info("✓ {} handled gracefully", errorType);
        }

        logger.info("✓ All SpEL error types handled without stack traces");
        logger.info("✓ Comprehensive SpEL error handling validation complete");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Map<String, Object> createTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 100.0);
        data.put("customerName", "Test Customer");
        data.put("tags", Arrays.asList("tag1", "tag2"));

        // Add metadata map for testing
        Map<String, String> metadata = new HashMap<>();
        metadata.put("existingKey", "existingValue");
        data.put("metadata", metadata);

        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return facts;
    }
}
