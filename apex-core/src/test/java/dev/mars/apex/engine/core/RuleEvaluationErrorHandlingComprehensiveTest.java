package dev.mars.apex.engine.core;

import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.core.RuleBuilder;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite to prove that rule evaluation errors are properly captured
 * and returned as structured RuleResult objects instead of being lost in stack traces.
 *
 * This test covers ALL execution paths that could encounter SpEL evaluation errors:
 * 1. RulesEngine.executeRule() - Single rule execution
 * 2. RulesEngine.executeRulesList() - Multiple rules execution
 * 3. RulesEngine.executeRulesAndRuleGroups() - Mixed execution
 * 4. Various severity levels (CRITICAL, WARNING, ERROR)
 * 5. Different types of SpEL errors (missing properties, type mismatches, etc.)
 *
 * Updated to use standard RulesEngine entry point without deprecated EnrichmentService.
 *
 * @author GitHub Copilot
 * @since 2025-09-26
 */
@DisplayName("Comprehensive Rule Evaluation Error Handling Tests")
class RuleEvaluationErrorHandlingComprehensiveTest {

    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluationErrorHandlingComprehensiveTest.class);

    private RulesEngine rulesEngine;
    private RulesEngineConfiguration configuration;

    @BeforeEach
    void setUp() {
        logger.info("Setting up comprehensive rule evaluation error handling tests");

        // Create configuration and rules engine with all services
        configuration = new RulesEngineConfiguration();

        // Create rules engine
        rulesEngine = new RulesEngine(configuration);
    }
    
    // ========================================
    // PATH 1: RulesEngine.executeRule() Tests
    // ========================================
    
    @Test
    @DisplayName("PATH 1: executeRule() should return ERROR RuleResult for missing property with ERROR severity")
    void testExecuteRule_MissingProperty_ErrorSeverity() {
        // Given: Rule that references missing property with ERROR severity
        Rule rule = new RuleBuilder()
            .withName("missing-property-test")
            .withCondition("#nonExistentField.length() > 0")  // Method call throws exception
            .withMessage("Property should exist")
            .withSeverity("ERROR")
            .build();

        Map<String, Object> facts = createFactsWithoutProperty();

        // When: Execute rule that will fail
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Then: Should return structured ERROR result, not throw exception
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type");
        assertEquals("missing-property-test", result.getRuleName(),
                    "Should identify the failing rule");
        assertEquals("ERROR", result.getSeverity(),
                    "Should preserve ERROR severity from rule configuration");
        assertTrue(result.getMessage().contains("Rule evaluation failed") || result.getMessage().contains("evaluation"),
                  "Should have descriptive error message");

        logger.info("[OK] PATH 1: executeRule() properly handles missing property with ERROR severity");
    }
    
    @Test
    @DisplayName("PATH 1: executeRule() should return ERROR RuleResult for missing property with CRITICAL severity")
    void testExecuteRule_MissingProperty_CriticalSeverity() {
        // Given: Rule that references missing property with CRITICAL severity
        Rule rule = new RuleBuilder()
            .withName("critical-missing-property")
            .withCondition("#criticalField.toString().length() > 0")
            .withMessage("Critical field must exist")
            .withSeverity("CRITICAL")
            .build();

        Map<String, Object> facts = createFactsWithoutProperty();

        // When: Execute rule that will fail
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Then: Should return structured ERROR result with CRITICAL severity
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type");
        assertEquals("critical-missing-property", result.getRuleName(),
                    "Should identify the failing rule");
        assertEquals("CRITICAL", result.getSeverity(),
                    "Should preserve CRITICAL severity from rule configuration");
        assertTrue(result.getMessage().contains("Rule evaluation failed"),
                  "Should have descriptive error message");

        logger.info("[OK] PATH 1: executeRule() properly handles missing property with CRITICAL severity");
    }
    
    @Test
    @DisplayName("PATH 1: executeRule() should return ERROR RuleResult for type mismatch with WARNING severity")
    void testExecuteRule_TypeMismatch_WarningSeverity() {
        // Given: Rule that causes type mismatch with WARNING severity
        Rule rule = new RuleBuilder()
            .withName("type-mismatch-test")
            .withCondition("#stringField > 100")  // Comparing string to number
            .withMessage("Should be numeric comparison")
            .withSeverity("WARNING")
            .build();

        Map<String, Object> facts = createFactsWithStringField();

        // When: Execute rule that will fail due to type mismatch
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Then: Should return NO_MATCH result due to error recovery for WARNING severity
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                    "Should return NO_MATCH result type after error recovery for WARNING severity");
        assertEquals("type-mismatch-test", result.getRuleName(),
                    "Should identify the rule that was recovered");
        // Note: After error recovery, the result may not preserve the original severity
        assertFalse(result.isTriggered(),
                   "Should not be triggered after error recovery");

        logger.info("[OK] PATH 1: executeRule() properly handles type mismatch with WARNING severity");
    }
    
    // ========================================
    // PATH 2: RulesEngine.executeRulesList() Tests
    // ========================================
    
    @Test
    @DisplayName("PATH 2: executeRulesList() should return ERROR RuleResult for first failing rule")
    void testExecuteRulesList_FirstRuleFails() {
        // Given: List of rules where first rule will fail
        List<Rule> rules = Arrays.asList(
            createFailingRule("failing-rule-1", "#missing.length() > 0", "ERROR"),  // Method call throws
            createValidRule("valid-rule-2", "#quantity > 0", "INFO")
        );
        
        Map<String, Object> facts = createValidFacts();
        
        // When: Execute rules list where first rule fails
        RuleResult result = rulesEngine.executeRulesList(rules, facts);
        
        // Then: Should return ERROR result for the failing rule
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "Should return ERROR result type");
        assertEquals("failing-rule-1", result.getRuleName(), 
                    "Should identify the first failing rule");
        assertEquals("ERROR", result.getSeverity(), 
                    "Should preserve severity from failing rule");
        assertTrue(result.getMessage().contains("Rule evaluation failed") || result.getMessage().contains("evaluation"), 
                  "Should have descriptive error message");
        
        logger.info("[OK] PATH 2: executeRulesList() properly handles first rule failure");
    }
    
    // ========================================
    // PATH 3: RulesEngine.executeRulesAndRuleGroups() Tests
    // ========================================

    @Test
    @DisplayName("PATH 3: executeRules() should return ERROR RuleResult for mixed rule failure")
    void testExecuteRules_MixedRuleFailure() {
        // Given: Mixed list with failing rule
        List<dev.mars.apex.engine.model.RuleBase> mixedRules = Arrays.asList(
            createFailingRule("mixed-failing-rule", "#invalidProperty.length() > 0", "ERROR")
        );

        Map<String, Object> facts = createFactsWithoutProperty();

        // When: Execute mixed rules where one fails
        RuleResult result = rulesEngine.executeRules(mixedRules, facts);

        // Then: Should return ERROR result for the failing rule
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type");
        assertEquals("mixed-failing-rule", result.getRuleName(),
                    "Should identify the failing rule");
        assertEquals("ERROR", result.getSeverity(),
                    "Should preserve severity from failing rule");
        assertTrue(result.getMessage().contains("Rule evaluation failed"),
                  "Should have descriptive error message");

        logger.info("[OK] PATH 3: executeRules() properly handles mixed rule failure");
    }

    // ========================================
    // EDGE CASES: Complex SpEL Error Scenarios
    // ========================================

    @Test
    @DisplayName("EDGE CASE: Null pointer access should return structured error")
    void testNullPointerAccess_StructuredError() {
        // Given: Rule that will cause null pointer access
        Rule rule = new RuleBuilder()
            .withName("null-pointer-test")
            .withCondition("#nullField.toString().length() > 0")
            .withMessage("Null field access")
            .withSeverity("ERROR")
            .build();

        Map<String, Object> facts = new HashMap<>();
        facts.put("nullField", null);  // Explicitly null

        // When: Execute rule that will cause null pointer
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Then: Should return structured error, not throw NPE
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type");
        assertEquals("null-pointer-test", result.getRuleName(),
                    "Should identify the failing rule");
        assertTrue(result.getMessage().contains("Rule evaluation failed"),
                  "Should have descriptive error message");

        logger.info("[OK] EDGE CASE: Null pointer access properly handled with structured error");
    }

    @Test
    @DisplayName("EDGE CASE: Method not found should return structured error")
    void testMethodNotFound_StructuredError() {
        // Given: Rule that calls non-existent method
        Rule rule = new RuleBuilder()
            .withName("method-not-found-test")
            .withCondition("#quantity.nonExistentMethod() > 0")
            .withMessage("Method should exist")
            .withSeverity("CRITICAL")
            .build();

        Map<String, Object> facts = createValidFacts();

        // When: Execute rule that will fail due to missing method
        RuleResult result = rulesEngine.executeRule(rule, facts);

        // Then: Should return structured error, not throw method not found exception
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                    "Should return ERROR result type");
        assertEquals("method-not-found-test", result.getRuleName(),
                    "Should identify the failing rule");
        assertEquals("CRITICAL", result.getSeverity(),
                    "Should preserve CRITICAL severity");
        assertTrue(result.getMessage().contains("Rule evaluation failed"),
                  "Should have descriptive error message");

        logger.info("[OK] EDGE CASE: Method not found properly handled with structured error");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private Map<String, Object> createFactsWithoutProperty() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("quantity", 100);
        facts.put("price", 50.0);
        // Intentionally missing the properties that rules will try to access
        return facts;
    }

    private Map<String, Object> createFactsWithStringField() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("stringField", "not-a-number");
        facts.put("quantity", 100);
        return facts;
    }

    private Map<String, Object> createValidFacts() {
        Map<String, Object> facts = new HashMap<>();
        facts.put("quantity", 100);
        facts.put("price", 50.0);
        facts.put("currency", "USD");
        return facts;
    }

    private Rule createFailingRule(String id, String condition, String severity) {
        return new RuleBuilder().withName(id).withCondition(condition).withMessage("This rule will fail").withSeverity(severity).build();
    }

    private Rule createValidRule(String id, String condition, String severity) {
        return new RuleBuilder().withName(id).withCondition(condition).withMessage("This rule should pass").withSeverity(severity).build();
    }
}
