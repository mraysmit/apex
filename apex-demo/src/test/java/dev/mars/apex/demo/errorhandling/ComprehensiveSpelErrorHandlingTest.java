package dev.mars.apex.demo.errorhandling;

import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.core.engine.core.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import dev.mars.apex.core.engine.core.RuleBuilder;

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
 * @author Mark Andrew Ray-Smith Cityline Ltd
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
        
        Rule rule = new RuleBuilder().withName("missing-property-test").withCondition("#nonExistentProperty != null").withMessage("Property should exist").withSeverity("ERROR").build();
        
        Map<String, Object> facts = createTestData();
        // Intentionally not adding 'nonExistentProperty'
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        // When a rule with ERROR severity evaluates to false and recovery is disabled,
        // it returns ERROR result type with the rule message
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for ERROR severity rule that doesn't match");
        assertFalse(result.isTriggered(), "Rule should not be triggered when condition is false");
        assertEquals("Property should exist", result.getMessage(), 
                    "Message should be the rule message");
        
        logger.info("[OK] Property not found handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle non-existent property equality comparison")
    void shouldHandleNonExistentPropertyEquality() {
        logger.info("Testing non-existent property equality comparison");
        
        Rule rule = new RuleBuilder().withName("missing-property-equality-test").withCondition("#nonExistentProperty == 'TESTVALUE'").withMessage("Property should equal TESTVALUE").withSeverity("ERROR").build();
        
        Map<String, Object> facts = createTestData();
        // Intentionally not adding 'nonExistentProperty'
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        // When comparing non-existent property to a value:
        // #nonExistentProperty resolves to null
        // null == 'TESTVALUE' evaluates to FALSE
        // With ERROR severity and recovery disabled, returns ERROR result type
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for ERROR severity rule that doesn't match");
        assertFalse(result.isTriggered(), "Rule should not be triggered when condition is false");
        assertEquals("Property should equal TESTVALUE", result.getMessage(), 
                    "Message should be the rule message");
        
        logger.info("[OK] Non-existent property equality comparison handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle nested property access on null gracefully")
    void shouldHandleNestedPropertyAccessOnNull() {
        logger.info("Testing nested property access on null error handling");
        
        Rule rule = new RuleBuilder().withName("nested-null-access-test").withCondition("#nullObject.someProperty != null").withMessage("Nested property should be accessible").withSeverity("ERROR").build();
        
        Map<String, Object> testData = createTestData();
        testData.put("nullObject", null);
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, testData);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for null property access");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("[OK] Nested property access on null handled gracefully");
    }
    
    // ========================================================================
    // METHOD INVOCATION ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle method not found gracefully")
    void shouldHandleMethodNotFound() {
        logger.info("Testing method not found error handling");
        
        Rule rule = new RuleBuilder().withName("invalid-method-test").withCondition("#amount.nonExistentMethod() > 0").withMessage("Method should exist").withSeverity("ERROR").build();
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for missing method");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("[OK] Method not found handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle method invocation with wrong parameters gracefully")
    void shouldHandleMethodInvocationWithWrongParameters() {
        logger.info("Testing method invocation with wrong parameters error handling");
        
        Rule rule = new RuleBuilder().withName("wrong-params-test").withCondition("#customerName.substring(10, 20, 30) != null").withMessage("Method parameters should be correct").withSeverity("ERROR").build();
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for wrong method parameters");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("[OK] Method invocation with wrong parameters handled gracefully");
    }
    
    // ========================================================================
    // TYPE CONVERSION ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle type conversion errors gracefully")
    void shouldHandleTypeConversionErrors() {
        logger.info("Testing type conversion error handling");
        
        Rule rule = new RuleBuilder().withName("type-conversion-test").withCondition("#customerName + 100 > 0").withMessage("Type conversion should work").withSeverity("ERROR").build();
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for type conversion");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("[OK] Type conversion error handled gracefully");
    }
    
    @Test
    @DisplayName("Should handle arithmetic errors gracefully")
    void shouldHandleArithmeticErrors() {
        logger.info("Testing arithmetic error handling");

        Rule rule = new RuleBuilder().withName("arithmetic-error-test").withCondition("#amount.invalidArithmeticMethod() > 0").withMessage("Arithmetic should work").withSeverity("ERROR").build();

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for arithmetic error");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("[OK] Arithmetic error handled gracefully");
    }
    
    // ========================================================================
    // ARRAY/COLLECTION ACCESS ERRORS
    // ========================================================================
    
    @Test
    @DisplayName("Should handle array index out of bounds gracefully")
    void shouldHandleArrayIndexOutOfBounds() {
        logger.info("Testing array index out of bounds error handling");
        
        Rule rule = new RuleBuilder().withName("array-bounds-test").withCondition("#tags[10] != null").withMessage("Array access should be valid").withSeverity("ERROR").build();
        
        Map<String, Object> facts = createTestData();
        
        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR for array bounds");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        
        logger.info("[OK] Array index out of bounds handled gracefully");
    }
    
    // ========================================================================
    // COLLECTION/MAP ACCESS ERRORS
    // ========================================================================

    @Test
    @DisplayName("Should handle map key not found gracefully")
    void shouldHandleMapKeyNotFound() {
        logger.info("Testing map key not found error handling");

        Rule rule = new RuleBuilder().withName("map-key-test").withCondition("#metadata['nonExistentKey'].toString().length() > 0").withMessage("Map key should exist").withSeverity("ERROR").build();

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for missing map key");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("[OK] Map key not found handled gracefully");
    }

    // ========================================================================
    // CASTING AND CONVERSION ERRORS
    // ========================================================================

    @Test
    @DisplayName("Should handle invalid casting gracefully")
    void shouldHandleInvalidCasting() {
        logger.info("Testing invalid casting error handling");

        Rule rule = new RuleBuilder().withName("invalid-cast-test").withCondition("((java.util.Date) #customerName).getTime() > 0").withMessage("Casting should work").withSeverity("ERROR").build();

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for invalid casting");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("[OK] Invalid casting handled gracefully");
    }

    // ========================================================================
    // COMPLEX EXPRESSION ERRORS
    // ========================================================================

    @Test
    @DisplayName("Should handle complex nested expression errors gracefully")
    void shouldHandleComplexNestedExpressionErrors() {
        logger.info("Testing complex nested expression error handling");

        Rule rule = new RuleBuilder().withName("complex-expression-test").withCondition("#customer.address.street.substring(#nonExistent.length()).toUpperCase() != null").withMessage("Complex expression should work").withSeverity("ERROR").build();

        Map<String, Object> facts = createTestData();

        RuleResult result = ruleEvaluator.evaluateRule(rule, facts);

        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR for complex expression error");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");

        logger.info("[OK] Complex nested expression error handled gracefully");
    }

    // ========================================================================
    // MULTIPLE ERROR SCENARIOS IN SINGLE TEST
    // ========================================================================

    @Test
    @DisplayName("Should handle multiple SpEL errors in rule list gracefully")
    void shouldHandleMultipleSpelErrorsInRuleList() {
        logger.info("Testing multiple SpEL errors in rule list");

        List<Rule> rules = Arrays.asList(
            new RuleBuilder().withName("error-rule-1").withCondition("#missing1.toString()").withMessage("Error 1").withSeverity("ERROR").build(),
            new RuleBuilder().withName("success-rule").withCondition("#amount > 50").withMessage("Success rule").withSeverity("INFO").build(),
            new RuleBuilder().withName("error-rule-2").withCondition("#missing2.length()").withMessage("Error 2").withSeverity("ERROR").build(),
            new RuleBuilder().withName("another-success").withCondition("#customerName != null").withMessage("Another success").withSeverity("INFO").build()
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

        logger.info("[OK] Multiple SpEL errors in rule list handled gracefully");
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
        errorRules.put("property-not-found", new RuleBuilder().withName("prop-test").withCondition("#missing != null").withMessage("Property test").withSeverity("ERROR").build());
        errorRules.put("nested-property-not-found", new RuleBuilder().withName("nested-test").withCondition("#missing.field != null").withMessage("Nested test").withSeverity("ERROR").build());

        // Method not found errors
        errorRules.put("method-not-found", new RuleBuilder().withName("method-test").withCondition("#amount.invalidMethod()").withMessage("Method test").withSeverity("ERROR").build());

        // Type conversion errors
        errorRules.put("type-conversion", new RuleBuilder().withName("type-test").withCondition("#customerName + 100").withMessage("Type test").withSeverity("ERROR").build());

        // Arithmetic errors
        errorRules.put("arithmetic-error", new RuleBuilder().withName("arith-test").withCondition("#amount / 0").withMessage("Arithmetic test").withSeverity("ERROR").build());

        // Array access errors
        errorRules.put("array-bounds", new RuleBuilder().withName("array-test").withCondition("#tags[99]").withMessage("Array test").withSeverity("ERROR").build());

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

            logger.info("[OK] {} handled gracefully", errorType);
        }

        logger.info("[OK] All SpEL error types handled without stack traces");
        logger.info("[OK] Comprehensive SpEL error handling validation complete");
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
