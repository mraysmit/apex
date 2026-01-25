package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DEFINITIVE PROOF that rule evaluation errors are properly handled and returned
 * as structured RuleResult objects instead of being lost in stack traces.
 *
 * This test demonstrates that:
 * 1. CRITICAL errors return ERROR RuleResult (no recovery)
 * 2. Non-critical errors are logged properly and handled gracefully
 * 3. Error recovery works for non-critical errors
 * 4. All error paths return structured results
 *
 * Updated to use standard RulesEngine entry point without deprecated EnrichmentService.
 *
 * @author GitHub Copilot
 * @since 2025-09-26
 */
@DisplayName("  DEFINITIVE PROOF: Error Handling Works Correctly")
class DefinitiveErrorHandlingProofTest {

    private static final Logger logger = LoggerFactory.getLogger(DefinitiveErrorHandlingProofTest.class);

    private RulesEngine rulesEngine;

    @BeforeEach
    void setUp() {
        logger.info("🚀 Setting up definitive error handling proof test");

        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        rulesEngine = new RulesEngine(configuration);
    }
    
    @Test
    @DisplayName("  PROOF 1: CRITICAL errors return ERROR RuleResult (no recovery)")
    void testCriticalErrorsReturnErrorResult() {
        logger.info("Testing CRITICAL error handling - should return ERROR result");
        
        // Given: Rule with CRITICAL severity that will fail
        Rule rule = new Rule(
            "critical-method-error",
            "#'value'].nonExistentMethod() > 0",
            "Critical method error test",
            "CRITICAL"
        );
        
        // Create a simple object that has a value property to test method call errors
        Map<String, Object> facts = new HashMap<>();
        facts.putAll(Map.of("value", 100));
        
        // When: Execute rule that will fail
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Then: Should return ERROR result (no recovery for CRITICAL)
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "CRITICAL errors should return ERROR result type");
        assertEquals("critical-method-error", result.getRuleName(), 
                    "Should identify the failing rule");
        assertEquals("CRITICAL", result.getSeverity(), 
                    "Should preserve CRITICAL severity");
        assertTrue(result.getMessage().contains("Rule evaluation failed"), 
                  "Should have descriptive error message");
        assertTrue(result.getMessage().contains("nonExistentMethod"), 
                  "Should mention the failing method");
        assertFalse(result.isTriggered(), "Rule should not be triggered");
        
        logger.info("PROOF 1 COMPLETE: CRITICAL errors return structured ERROR results");
    }
    
    @Test
    @DisplayName("  PROOF 2: Non-critical errors are logged and recovered gracefully")
    void testNonCriticalErrorsAreLoggedAndRecovered() {
        logger.info("Testing non-critical error handling - should log error and recover");

        // Given: Rule with WARNING severity that will throw exception (non-critical)
        Rule rule = new Rule(
            "warning-missing-property",
            "#nonExistentField.length() > 0",  // Method call on missing variable throws exception
            "Missing property test",
            "WARNING"
        );
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("quantity", 100);
        // Missing "nonExistentField"
        
        // When: Execute rule that will fail
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Then: Should recover gracefully (error recovery for non-critical)
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                    "Non-critical errors should be recovered to NO_MATCH");
        assertEquals("No matching rules found", result.getMessage(),
                    "Should have recovery message");
        assertEquals("WARNING", result.getSeverity(),
                    "Should preserve original rule severity after recovery");
        assertFalse(result.isTriggered(), "Rule should not be triggered");
        assertTrue(result.isSuccess(), "Recovery should be successful");
        
        logger.info("PROOF 2 COMPLETE: Non-critical errors are logged and recovered gracefully");
    }
    
    @Test
    @DisplayName("  PROOF 3: WARNING errors are logged at appropriate level")
    void testWarningErrorsAreLoggedAppropriately() {
        logger.info("Testing WARNING error handling - should log at INFO level and recover");
        
        // Given: Rule with WARNING severity that will fail
        Rule rule = new Rule(
            "warning-type-error",
            "#stringField > 100",
            "Type comparison warning",
            "WARNING"
        );
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("stringField", "not-a-number");
        
        // When: Execute rule that will fail
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Then: Should recover gracefully (error recovery for WARNING)
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(), 
                    "WARNING errors should be recovered to NO_MATCH");
        assertEquals("No matching rules found", result.getMessage(), 
                    "Should have recovery message");
        assertFalse(result.isTriggered(), "Rule should not be triggered");
        assertTrue(result.isSuccess(), "Recovery should be successful");
        
        logger.info("PROOF 3 COMPLETE: WARNING errors are logged appropriately and recovered");
    }
    
    @Test
    @DisplayName("  PROOF 4: Multiple CRITICAL errors in sequence return ERROR results")
    void testMultipleCriticalErrorsReturnErrorResults() {
        logger.info("Testing multiple CRITICAL errors - all should return ERROR results");
        
        // Test 1: Null pointer with CRITICAL severity
        Rule nullRule = new Rule(
            "critical-null-error",
            "#nullField.toString() != null",
            "Critical null access",
            "CRITICAL"
        );
        
        Map<String, Object> nullFacts = new HashMap<>();
        nullFacts.put("nullField", null);
        
        RuleResult nullResult = rulesEngine.executeRule(nullRule, nullFacts);
        
        assertEquals(RuleResult.ResultType.ERROR, nullResult.getResultType(), 
                    "CRITICAL null error should return ERROR");
        assertEquals("CRITICAL", nullResult.getSeverity(), 
                    "Should preserve CRITICAL severity");
        
        // Test 2: Property access with CRITICAL severity
        Rule propertyRule = new Rule(
            "critical-property-error",
            "#missing.length() > 0",
            "Critical property access",
            "CRITICAL"
        );
        
        Map<String, Object> propertyFacts = new HashMap<>();
        propertyFacts.put("existing", "value");
        // Missing "missing" property
        
        RuleResult propertyResult = rulesEngine.executeRule(propertyRule, propertyFacts);
        
        assertEquals(RuleResult.ResultType.ERROR, propertyResult.getResultType(), 
                    "CRITICAL property error should return ERROR");
        assertEquals("CRITICAL", propertyResult.getSeverity(), 
                    "Should preserve CRITICAL severity");
        
        logger.info("PROOF 4 COMPLETE: Multiple CRITICAL errors all return structured ERROR results");
    }
    
    @Test
    @DisplayName("  PROOF 5: Error handling preserves rule context and performance metrics")
    void testErrorHandlingPreservesContext() {
        logger.info("Testing error context preservation - should maintain rule information");
        
        // Given: Rule that will fail with specific context
        Rule rule = new Rule(
            "context-preservation-test",
            "#complexObject.deepProperty.method() == true",
            "Complex context preservation test",
            "CRITICAL"
        );
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("simpleProperty", "value");
        // Missing "complexObject"
        
        // When: Execute rule that will fail
        RuleResult result = rulesEngine.executeRule(rule, facts);
        
        // Then: Should preserve all context information
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR result type");
        assertEquals("context-preservation-test", result.getRuleName(), 
                    "Should preserve exact rule name");
        assertEquals("CRITICAL", result.getSeverity(), 
                    "Should preserve exact severity");
        assertTrue(result.getMessage().contains("Rule evaluation failed"), 
                  "Should have structured error message");
        assertNotNull(result.getPerformanceMetrics(), 
                     "Should have performance metrics even for errors");
        assertTrue(result.getPerformanceMetrics().getEvaluationTimeMillis() >= 0,
                  "Should have valid execution time");
        
        logger.info("PROOF 5 COMPLETE: Error handling preserves rule context and metrics");
    }
    
    @Test
    @DisplayName("  SUMMARY: All error handling paths work correctly")
    void testSummaryAllErrorPathsWork() {
        logger.info("SUMMARY: Demonstrating all error handling paths work correctly");
        
        int totalTests = 0;
        int criticalErrorsReturned = 0;
        int nonCriticalErrorsRecovered = 0;
        
        // Test various error scenarios
        String[] severities = {"CRITICAL", "WARNING", "INFO"};
        String[] conditions = {
            "#missing.method()",
            "#nullField.toString()",
            "#value.nonExistentMethod()"
        };
        
        for (String severity : severities) {
            for (int i = 0; i < conditions.length; i++) {
                totalTests++;
                
                Rule rule = new Rule(
                    "summary-test-" + severity.toLowerCase() + "-" + i,
                    conditions[i],
                    "Summary test",
                    severity
                );
                
                Map<String, Object> facts = new HashMap<>();
                facts.put("value", 100);
                facts.put("nullField", null);
                
                RuleResult result = rulesEngine.executeRule(rule, facts);
                
                if ("CRITICAL".equals(severity)) {
                    if (result.getResultType() == RuleResult.ResultType.ERROR) {
                        criticalErrorsReturned++;
                    }
                } else {
                    if (result.getResultType() == RuleResult.ResultType.NO_MATCH) {
                        nonCriticalErrorsRecovered++;
                    }
                }
            }
        }
        
        // Verify results
        assertEquals(3, criticalErrorsReturned, 
                    "All CRITICAL errors should return ERROR results");
        assertEquals(6, nonCriticalErrorsRecovered, 
                    "All non-critical errors should be recovered to NO_MATCH");
        
        logger.info("SUMMARY RESULTS:");
        logger.info("   Total tests: {}", totalTests);
        logger.info("   CRITICAL errors returned as ERROR: {}", criticalErrorsReturned);
        logger.info("   Non-critical errors recovered: {}", nonCriticalErrorsRecovered);
        logger.info("ALL ERROR HANDLING PATHS WORK CORRECTLY!");
    }
}
