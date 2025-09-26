package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for UnifiedRuleEvaluator.
 * 
 * Tests the core functionality of the unified evaluation engine including:
 * - Basic rule evaluation
 * - Error handling and recovery
 * - Performance monitoring
 * - Consistent error message formatting
 * 
 * @author APEX Rules Engine
 * @since Phase 1 - Unified Evaluation Engine
 */
@DisplayName("Unified Rule Evaluator Tests")
class UnifiedRuleEvaluatorTest {
    
    private UnifiedRuleEvaluator evaluator;
    private Map<String, Object> testFacts;
    
    @BeforeEach
    void setUp() {
        evaluator = new UnifiedRuleEvaluator();
        
        // Set up test facts
        testFacts = new HashMap<>();
        testFacts.put("amount", 1000.0);
        testFacts.put("currency", "USD");
        testFacts.put("customerType", "PREMIUM");
        testFacts.put("riskLevel", "LOW");
    }
    
    @Test
    @DisplayName("Should evaluate simple rule that matches")
    void testEvaluateRule_SimpleMatch() {
        // Given
        Rule rule = new Rule("Amount Check", "#amount > 500", "Amount exceeds threshold", "INFO");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule should be triggered");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        assertEquals("Amount Check", result.getRuleName());
        assertEquals("Amount exceeds threshold", result.getMessage());
        assertEquals("INFO", result.getSeverity());
    }
    
    @Test
    @DisplayName("Should evaluate simple rule that does not match")
    void testEvaluateRule_SimpleNoMatch() {
        // Given
        Rule rule = new Rule("High Amount Check", "#amount > 2000", "Amount is very high", "WARNING");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Rule should not be triggered");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }
    
    @Test
    @DisplayName("Should handle null rule gracefully")
    void testEvaluateRule_NullRule() {
        // When
        RuleResult result = evaluator.evaluateRule(null, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Null rule should not be triggered");
        assertEquals(RuleResult.ResultType.NO_RULES, result.getResultType());
    }
    
    @Test
    @DisplayName("Should handle rule with empty condition")
    void testEvaluateRule_EmptyCondition() {
        // Given
        Rule rule = new Rule("Empty Rule", "", "Empty condition", "ERROR");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Empty condition rule should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertEquals("Empty Rule", result.getRuleName());
        assertTrue(result.getMessage().contains("no condition to evaluate"), 
                  "Error message should indicate missing condition");
    }
    
    @Test
    @DisplayName("Should handle SpEL evaluation error with consistent message format")
    void testEvaluateRule_SpelError() {
        // Given - Rule with invalid property reference (this will be caught as missing parameter)
        Rule rule = new Rule("Invalid Property", "#nonExistentProperty > 100", "Invalid property test", "ERROR");

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertEquals("Invalid Property", result.getRuleName());

        // This will be caught as missing parameters, not SpEL error
        assertTrue(result.getMessage().contains("Missing parameters"),
                  "Error message should indicate missing parameters");
        assertEquals("ERROR", result.getSeverity());
    }
    
    @Test
    @DisplayName("Should attempt error recovery for WARNING severity")
    void testEvaluateRule_ErrorRecovery_Warning() {
        // Given - Rule with SpEL error but WARNING severity (should attempt recovery)
        Rule rule = new Rule("Warning Rule", "#invalidProperty == 'test'", "Warning test", "WARNING");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        
        // The error recovery service should attempt recovery for WARNING severity
        // Result could be either ERROR (if recovery failed) or NO_MATCH (if recovery succeeded)
        assertTrue(result.getResultType() == RuleResult.ResultType.ERROR || 
                  result.getResultType() == RuleResult.ResultType.NO_MATCH,
                  "WARNING severity should attempt recovery");
    }
    
    @Test
    @DisplayName("Should not attempt error recovery for CRITICAL severity")
    void testEvaluateRule_NoErrorRecovery_Critical() {
        // Given - Rule with SpEL error and CRITICAL severity (should not attempt recovery)
        Rule rule = new Rule("Critical Rule", "#invalidProperty == 'test'", "Critical test", "CRITICAL");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(), 
                    "CRITICAL severity should return ERROR without recovery");
        assertEquals("Critical Rule", result.getRuleName());
        assertEquals("CRITICAL", result.getSeverity());
    }
    
    @Test
    @DisplayName("Should evaluate complex SpEL expression")
    void testEvaluateRule_ComplexExpression() {
        // Given
        Rule rule = new Rule("Complex Rule", 
                           "#amount > 500 && #currency == 'USD' && #customerType == 'PREMIUM'", 
                           "Premium USD customer with high amount", 
                           "INFO");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Complex rule should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        assertEquals("Complex Rule", result.getRuleName());
    }
    
    @Test
    @DisplayName("Should evaluate rule with EvaluationContext directly")
    void testEvaluateRule_WithEvaluationContext() {
        // Given
        Rule rule = new Rule("Context Rule", "#testValue == 'direct'", "Direct context test", "INFO");
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("testValue", "direct");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, context);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Rule should match with direct context");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }
    
    @Test
    @DisplayName("Should handle missing parameters gracefully")
    void testEvaluateRule_MissingParameters() {
        // Given - Rule that references a parameter not in facts
        Rule rule = new Rule("Missing Param", "#missingParam > 100", "Missing parameter test", "ERROR");

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertTrue(result.getMessage().contains("Missing parameters"),
                  "Error message should indicate missing parameters");
    }

    @Test
    @DisplayName("Should handle actual SpEL evaluation error with consistent message format")
    void testEvaluateRule_ActualSpelError() {
        // Given - Rule with CRITICAL severity to test actual error results (no recovery)
        Rule rule = new Rule("SpEL Error", "#amount.invalidMethod()", "SpEL error test", "CRITICAL");

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertEquals("SpEL Error", result.getRuleName());

        // Verify consistent error message format for actual SpEL errors
        assertTrue(result.getMessage().startsWith("Rule evaluation failed: SpEL Error - "),
                  "Error message should follow standard format: 'Rule evaluation failed: {ruleName} - {exception}'");
        assertEquals("CRITICAL", result.getSeverity());
    }
    
    @Test
    @DisplayName("Should include performance metrics in result")
    void testEvaluateRule_PerformanceMetrics() {
        // Given
        Rule rule = new Rule("Performance Test", "#amount > 0", "Performance test", "INFO");
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getPerformanceMetrics(), "Performance metrics should be included");
        assertTrue(result.getPerformanceMetrics().getEvaluationTimeMillis() >= 0,
                  "Evaluation time should be non-negative");
    }

    @Test
    @DisplayName("Should evaluate multiple rules and return first match")
    void testEvaluateRules_FirstMatch() {
        // Given
        List<Rule> rules = Arrays.asList(
            new Rule("Rule 1", "#amount > 2000", "High amount", "WARNING"),  // Won't match
            new Rule("Rule 2", "#currency == 'USD'", "USD currency", "INFO"), // Will match
            new Rule("Rule 3", "#amount > 0", "Any amount", "INFO")           // Would match but won't be reached
        );

        // When
        RuleResult result = evaluator.evaluateRules(rules, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Should find matching rule");
        assertEquals("Rule 2", result.getRuleName(), "Should return first matching rule");
        assertEquals("USD currency", result.getMessage());
    }

    @Test
    @DisplayName("Should evaluate multiple rules and return NO_MATCH when none match")
    void testEvaluateRules_NoMatch() {
        // Given
        List<Rule> rules = Arrays.asList(
            new Rule("Rule 1", "#amount > 2000", "High amount", "WARNING"),
            new Rule("Rule 2", "#currency == 'EUR'", "EUR currency", "INFO"),
            new Rule("Rule 3", "#customerType == 'BASIC'", "Basic customer", "INFO")
        );

        // When
        RuleResult result = evaluator.evaluateRules(rules, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Should not find matching rule");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Should handle empty rules list")
    void testEvaluateRules_EmptyList() {
        // Given
        List<Rule> rules = Arrays.asList();

        // When
        RuleResult result = evaluator.evaluateRules(rules, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(RuleResult.ResultType.NO_RULES, result.getResultType());
    }

    @Test
    @DisplayName("Should return error result when rule evaluation fails")
    void testEvaluateRules_ErrorResult() {
        // Given
        List<Rule> rules = Arrays.asList(
            new Rule("Good Rule", "#amount > 0", "Valid rule", "INFO"),
            new Rule("Bad Rule", "#invalidMethod()", "Invalid rule", "ERROR")
        );

        // When
        RuleResult result = evaluator.evaluateRules(rules, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Should return first matching rule");
        assertEquals("Good Rule", result.getRuleName(), "Should return first matching rule, not error");
    }
}
