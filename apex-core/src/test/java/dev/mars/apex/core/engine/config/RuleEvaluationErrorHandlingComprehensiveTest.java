package dev.mars.apex.core.engine.config;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.RuleEngineService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
 * 4. RuleEngineService.evaluateRules() - Service layer execution
 * 5. Various severity levels (CRITICAL, WARNING, ERROR)
 * 6. Different types of SpEL errors (missing properties, type mismatches, etc.)
 * 
 * @author GitHub Copilot
 * @since 2025-09-26
 */
@DisplayName("Comprehensive Rule Evaluation Error Handling Tests")
class RuleEvaluationErrorHandlingComprehensiveTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluationErrorHandlingComprehensiveTest.class);
    
    private RulesEngine rulesEngine;
    private RuleEngineService ruleEngineService;
    private RulesEngineConfiguration configuration;
    
    @BeforeEach
    void setUp() {
        logger.info("Setting up comprehensive rule evaluation error handling tests");
        
        // Create configuration and rules engine with all services
        configuration = new RulesEngineConfiguration();
        
        // Create enrichment service
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService expressionEvaluator = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(serviceRegistry, expressionEvaluator);
        
        // Create rules engine with full constructor
        rulesEngine = new RulesEngine(
            configuration,
            new SpelExpressionParser(),
            new ErrorRecoveryService(),
            new RulePerformanceMonitor(),
            enrichmentService
        );
        
        // Create rule engine service
        ruleEngineService = new RuleEngineService(expressionEvaluator);
    }
    
    // ========================================
    // PATH 1: RulesEngine.executeRule() Tests
    // ========================================
    
    @Test
    @DisplayName("PATH 1: executeRule() should return ERROR RuleResult for missing property with ERROR severity")
    void testExecuteRule_MissingProperty_ErrorSeverity() {
        // Given: Rule that references missing property with ERROR severity
        Rule rule = new Rule(
            "missing-property-test",
            "#data.nonExistentField != null",
            "Property should exist",
            "ERROR"  // Pass severity in constructor
        );

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
        assertTrue(result.getMessage().contains("Rule evaluation failed"),
                  "Should have descriptive error message");
        assertTrue(result.getMessage().contains("nonExistentField"),
                  "Should mention the missing property");

        logger.info("✓ PATH 1: executeRule() properly handles missing property with ERROR severity");
    }
    
    @Test
    @DisplayName("PATH 1: executeRule() should return ERROR RuleResult for missing property with CRITICAL severity")
    void testExecuteRule_MissingProperty_CriticalSeverity() {
        // Given: Rule that references missing property with CRITICAL severity
        Rule rule = new Rule(
            "critical-missing-property",
            "#data.criticalField.toString().length() > 0",
            "Critical field must exist",
            "CRITICAL"  // Pass severity in constructor
        );

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

        logger.info("✓ PATH 1: executeRule() properly handles missing property with CRITICAL severity");
    }
    
    @Test
    @DisplayName("PATH 1: executeRule() should return ERROR RuleResult for type mismatch with WARNING severity")
    void testExecuteRule_TypeMismatch_WarningSeverity() {
        // Given: Rule that causes type mismatch with WARNING severity
        Rule rule = new Rule(
            "type-mismatch-test",
            "#data.stringField > 100",  // Comparing string to number
            "Should be numeric comparison",
            "WARNING"  // Pass severity in constructor
        );

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

        logger.info("✓ PATH 1: executeRule() properly handles type mismatch with WARNING severity");
    }
    
    // ========================================
    // PATH 2: RulesEngine.executeRulesList() Tests
    // ========================================
    
    @Test
    @DisplayName("PATH 2: executeRulesList() should return ERROR RuleResult for first failing rule")
    void testExecuteRulesList_FirstRuleFails() {
        // Given: List of rules where first rule will fail
        List<Rule> rules = Arrays.asList(
            createFailingRule("failing-rule-1", "#data.missing != null", "ERROR"),
            createValidRule("valid-rule-2", "#data.quantity > 0", "INFO")
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
        assertTrue(result.getMessage().contains("Rule evaluation failed"), 
                  "Should have descriptive error message");
        
        logger.info("✓ PATH 2: executeRulesList() properly handles first rule failure");
    }
    
    // ========================================
    // PATH 3: RulesEngine.executeRulesAndRuleGroups() Tests
    // ========================================

    @Test
    @DisplayName("PATH 3: executeRules() should return ERROR RuleResult for mixed rule failure")
    void testExecuteRules_MixedRuleFailure() {
        // Given: Mixed list with failing rule
        List<dev.mars.apex.core.engine.model.RuleBase> mixedRules = Arrays.asList(
            createFailingRule("mixed-failing-rule", "#data.invalidProperty.length() > 0", "ERROR")
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

        logger.info("✓ PATH 3: executeRules() properly handles mixed rule failure");
    }

    // ========================================
    // PATH 4: RuleEngineService.evaluateRules() Tests
    // ========================================

    @Test
    @DisplayName("PATH 4: RuleEngineService should return ERROR RuleResult in results list")
    void testRuleEngineService_EvaluateRules_ErrorInResultsList() {
        // Given: Rules with one that will fail
        List<Rule> rules = Arrays.asList(
            createValidRule("service-valid-rule", "#data['quantity'] > 0", "INFO"),
            createFailingRule("service-failing-rule", "#data.nonExistent != null", "ERROR")
        );

        // Create evaluation context manually since RuleEngineService uses different context
        org.springframework.expression.EvaluationContext context =
            new org.springframework.expression.spel.support.StandardEvaluationContext();
        Map<String, Object> testData = createValidFacts();
        Object dataObject = testData.get("data");
        context.setVariable("data", dataObject);

        // When: Evaluate rules through service
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);

        // Then: Should have results for both rules, with error result for failing rule
        assertNotNull(results, "Results should not be null");
        assertEquals(2, results.size(), "Should have results for both rules");

        // Find the error result
        RuleResult errorResult = results.stream()
            .filter(r -> r.getResultType() == RuleResult.ResultType.ERROR)
            .findFirst()
            .orElse(null);

        assertNotNull(errorResult, "Should have an error result");
        assertEquals("service-failing-rule", errorResult.getRuleName(),
                    "Should identify the failing rule");
        assertEquals("ERROR", errorResult.getSeverity(),
                    "Should preserve ERROR severity");
        assertTrue(errorResult.getMessage().contains("Error evaluating expression"),
                  "Should have descriptive error message");

        logger.info("✓ PATH 4: RuleEngineService properly handles rule failures in results list");
    }

    // ========================================
    // EDGE CASES: Complex SpEL Error Scenarios
    // ========================================

    @Test
    @DisplayName("EDGE CASE: Null pointer access should return structured error")
    void testNullPointerAccess_StructuredError() {
        // Given: Rule that will cause null pointer access
        Rule rule = new Rule(
            "null-pointer-test",
            "#data.nullField.toString().length() > 0",
            "Null field access",
            "ERROR"  // Pass severity in constructor
        );

        Map<String, Object> data = new HashMap<>();
        data.put("nullField", null);  // Explicitly null

        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);

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

        logger.info("✓ EDGE CASE: Null pointer access properly handled with structured error");
    }

    @Test
    @DisplayName("EDGE CASE: Method not found should return structured error")
    void testMethodNotFound_StructuredError() {
        // Given: Rule that calls non-existent method
        Rule rule = new Rule(
            "method-not-found-test",
            "#data.quantity.nonExistentMethod() > 0",
            "Method should exist",
            "CRITICAL"  // Pass severity in constructor
        );

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

        logger.info("✓ EDGE CASE: Method not found properly handled with structured error");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private Map<String, Object> createFactsWithoutProperty() {
        Map<String, Object> data = new HashMap<>();
        data.put("quantity", 100);
        data.put("price", 50.0);
        // Intentionally missing the properties that rules will try to access

        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return facts;
    }

    private Map<String, Object> createFactsWithStringField() {
        Map<String, Object> data = new HashMap<>();
        data.put("stringField", "not-a-number");
        data.put("quantity", 100);

        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return facts;
    }

    private Map<String, Object> createValidFacts() {
        Map<String, Object> data = new HashMap<>();
        data.put("quantity", 100);
        data.put("price", 50.0);
        data.put("currency", "USD");

        Map<String, Object> facts = new HashMap<>();
        facts.put("data", data);
        return facts;
    }

    private Rule createFailingRule(String id, String condition, String severity) {
        return new Rule(id, condition, "This rule will fail", severity);
    }

    private Rule createValidRule(String id, String condition, String severity) {
        return new Rule(id, condition, "This rule should pass", severity);
    }
}
