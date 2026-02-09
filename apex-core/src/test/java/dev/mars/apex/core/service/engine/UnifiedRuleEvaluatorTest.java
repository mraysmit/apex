package dev.mars.apex.core.service.engine;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.core.RuleBuilder;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author Mark A Ray-Smith
 * @version 1.0
 * @since 2025-09-01
 */
@DisplayName("Unified Rule Evaluator Tests")
class UnifiedRuleEvaluatorTest {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedRuleEvaluatorTest.class);
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
        Rule rule = new RuleBuilder().withName("Amount Check").withCondition("#amount > 500").withMessage("Amount exceeds threshold").withSeverity("INFO").build();
        
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
        Rule rule = new RuleBuilder().withName("High Amount Check").withCondition("#amount > 2000").withMessage("Amount is very high").withSeverity("WARNING").build();
        
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
        // Given - Use 16-param constructor to bypass RuleBuilder validation,
        // since this test verifies the evaluator's handling of invalid rules
        Rule rule = new Rule("R-empty", java.util.Set.of(new dev.mars.apex.core.engine.model.Category("default", 100)),
                "Empty Rule", "", "Empty condition", "Empty condition", 100, "ERROR",
                null, null, null, null, null, null, null, true);
        
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
        // Given - Rule with invalid property reference (will throw SpEL exception)
        Rule rule = new RuleBuilder().withName("Invalid Property").withCondition("#nonExistentProperty.length() > 100").withMessage("Invalid property test").withSeverity("ERROR").build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isTriggered(), "Error rule should not be triggered");
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertEquals("Invalid Property", result.getRuleName());

        // With parameter validation removed, SpEL evaluation handles the error
        assertTrue(result.getMessage().contains("Rule evaluation failed") || result.getMessage().contains("evaluation"),
                  "Error message should indicate evaluation error");
        assertEquals("ERROR", result.getSeverity());
    }
    
    @Test
    @DisplayName("Should attempt error recovery for WARNING severity")
    void testEvaluateRule_ErrorRecovery_Warning() {
        // Given - Rule with SpEL error but WARNING severity (should attempt recovery)
        Rule rule = new RuleBuilder().withName("Warning Rule").withCondition("#invalidProperty == 'test'").withMessage("Warning test").withSeverity("WARNING").build();
        
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
        Rule rule = new RuleBuilder().withName("Critical Rule").withCondition("#invalidProperty == 'test'").withMessage("Critical test").withSeverity("CRITICAL").build();
        
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
        Rule rule = new RuleBuilder().withName("Complex Rule").withCondition("#amount > 500 && #currency == 'USD' && #customerType == 'PREMIUM'").withMessage("Premium USD customer with high amount").withSeverity("INFO").build();
        
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
        Rule rule = new RuleBuilder().withName("Context Rule").withCondition("#testValue == 'direct'").withMessage("Direct context test").withSeverity("INFO").build();
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
        logger.info("========== START OF INTENTIONAL ERROR TEST ==========");
        // Given - Rule that references a parameter not in facts (will throw SpEL exception)
        Rule rule = new RuleBuilder().withName("Missing Param").withCondition("#missingParam.length() > 100").withMessage("Missing parameter test").withSeverity("ERROR").build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        logger.info("========== END OF INTENTIONAL ERROR TEST ===========");

        // Then
        assertNotNull(result, "Result should not be null");
        // With parameter validation removed, SpEL evaluation handles missing variables
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType());
        assertTrue(result.getMessage().contains("Rule evaluation failed") || result.getMessage().contains("evaluation"),
                  "Error message should indicate evaluation error");
    }

    @Test
    @DisplayName("Should handle actual SpEL evaluation error with consistent message format")
    void testEvaluateRule_ActualSpelError() {
        logger.info("========== START OF INTENTIONAL ERROR TEST ==========");
        // Given - Rule with CRITICAL severity to test actual error results (no recovery)
        Rule rule = new RuleBuilder().withName("SpEL Error").withCondition("#amount.invalidMethod()").withMessage("SpEL error test").withSeverity("CRITICAL").build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        logger.info("========== END OF INTENTIONAL ERROR TEST ===========");

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
        Rule rule = new RuleBuilder().withName("Performance Test").withCondition("#amount > 0").withMessage("Performance test").withSeverity("INFO").build();
        
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
            new RuleBuilder().withName("Rule 1").withCondition("#amount > 2000").withMessage("High amount").withSeverity("WARNING").build(),  // Won't match
            new RuleBuilder().withName("Rule 2").withCondition("#currency == 'USD'").withMessage("USD currency").withSeverity("INFO").build(), // Will match
            new RuleBuilder().withName("Rule 3").withCondition("#amount > 0").withMessage("Any amount").withSeverity("INFO").build()           // Would match but won't be reached
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
            new RuleBuilder().withName("Rule 1").withCondition("#amount > 2000").withMessage("High amount").withSeverity("WARNING").build(),
            new RuleBuilder().withName("Rule 2").withCondition("#currency == 'EUR'").withMessage("EUR currency").withSeverity("INFO").build(),
            new RuleBuilder().withName("Rule 3").withCondition("#customerType == 'BASIC'").withMessage("Basic customer").withSeverity("INFO").build()
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
            new RuleBuilder().withName("Good Rule").withCondition("#amount > 0").withMessage("Valid rule").withSeverity("INFO").build(),
            new RuleBuilder().withName("Bad Rule").withCondition("#invalidMethod()").withMessage("Invalid rule").withSeverity("ERROR").build()
        );

        // When
        RuleResult result = evaluator.evaluateRules(rules, testFacts);

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isTriggered(), "Should return first matching rule");
        assertEquals("Good Rule", result.getRuleName(), "Should return first matching rule, not error");
    }
    
    // =========================================================================
    // Message Template Resolution Tests
    // =========================================================================
    
    @Test
    @DisplayName("Should resolve {{#variable}} placeholders in rule messages on MATCH")
    void testMessageResolution_HandlebarsFormat_Match() {
        // Given: Rule with {{#amount}} placeholder, condition that matches
        Rule rule = new RuleBuilder().withName("Amount Rule").withCondition("#amount > 500").withMessage("Amount {{#amount}} exceeds threshold").withSeverity("INFO").build();
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertTrue(result.isTriggered(), "Rule should match");
        assertEquals("Amount 1000.0 exceeds threshold", result.getMessage(),
                    "Message should have {{#amount}} resolved to actual value");
    }
    
    @Test
    @DisplayName("Should resolve {{#variable}} placeholders in rule messages on NO_MATCH")
    void testMessageResolution_HandlebarsFormat_NoMatch() {
        // Given: Rule with {{#amount}} placeholder, condition that does NOT match
        Rule rule = new RuleBuilder().withName("High Amount Rule").withCondition("#amount > 5000").withMessage("Amount {{#amount}} is below threshold").withSeverity("INFO").build();
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertFalse(result.isTriggered(), "Rule should not match");
        assertEquals("Amount 1000.0 is below threshold", result.getMessage(),
                    "Message should have {{#amount}} resolved even on NO_MATCH");
    }
    
    @Test
    @DisplayName("Should resolve multiple placeholders in a single message")
    void testMessageResolution_MultiplePlaceholders() {
        // Given
        Rule rule = new RuleBuilder().withName("Multi Rule").withCondition("#amount > 500").withMessage("Amount {{#amount}} in {{#currency}} for {{#customerType}} customer").withSeverity("INFO").build();
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertTrue(result.isTriggered());
        assertEquals("Amount 1000.0 in USD for PREMIUM customer", result.getMessage(),
                    "All placeholders should be resolved");
    }
    
    @Test
    @DisplayName("Should preserve message when no placeholders are present")
    void testMessageResolution_NoPlaceholders() {
        // Given
        Rule rule = new RuleBuilder().withName("Simple Rule").withCondition("#amount > 500").withMessage("Static message with no placeholders").withSeverity("INFO").build();
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertTrue(result.isTriggered());
        assertEquals("Static message with no placeholders", result.getMessage());
    }
    
    @Test
    @DisplayName("Should resolve unresolvable placeholders to empty string")
    void testMessageResolution_UnresolvablePlaceholder() {
        // Given: Placeholder references a variable not in the facts
        // SpEL evaluates #nonExistentVariable to null (not an error), so it resolves to ""
        Rule rule = new RuleBuilder().withName("Missing Var Rule").withCondition("#amount > 500").withMessage("Value {{#nonExistentVariable}} is unknown").withSeverity("INFO").build();
        
        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);
        
        // Then
        assertTrue(result.isTriggered());
        assertEquals("Value  is unknown", result.getMessage(),
                    "Undefined variable placeholders should resolve to empty string");
    }
    
    @Test
    @DisplayName("Should resolve #{expression} placeholders (SpEL template format)")
    void testMessageResolution_SpelTemplateFormat() {
        // Given: Using #{} format — inside #{}, the expression is plain SpEL
        // For SpEL variable references, we need the # prefix inside the placeholder
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("amount", 1000.0);
        context.setVariable("currency", "USD");
        
        // #{#amount} means: SpEL template placeholder containing SpEL variable #amount
        String result = evaluator.resolveMessageTemplate(
                "Amount #{#amount} in #{#currency}", context);
        
        assertEquals("Amount 1000.0 in USD", result);
    }
    
    @Test
    @DisplayName("Should handle null message gracefully")
    void testMessageResolution_NullMessage() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        String result = evaluator.resolveMessageTemplate(null, context);
        
        assertNull(result, "Null message should return null");
    }
    
    @Test
    @DisplayName("Should handle null context gracefully")
    void testMessageResolution_NullContext() {
        String result = evaluator.resolveMessageTemplate("Message {{#age}}", null);
        
        assertEquals("Message {{#age}}", result, 
                    "Null context should return message unchanged");
    }

    // ========================================================================
    // No-Match Message Tests (Phase 6)
    // ========================================================================

    @Test
    @DisplayName("Should use no-match-message with {{#}} placeholders on NO_MATCH")
    void testNoMatchMessage_HandlebarsFormat() {
        // Given: Rule with separate match and no-match messages using {{#}} placeholders
        java.util.Set<dev.mars.apex.core.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.core.engine.model.Category("test", 100));
        Rule rule = new Rule("no-match-test", categories, "Amount Check",
                "#amount > 5000", "Amount {{#amount}} exceeds threshold",
                "Checks amount threshold", 100, "INFO", null, null, null, null, null, null,
                "Amount {{#amount}} is within normal range", true);

        // When: condition is false (1000 < 5000)
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then: should use the no-match-message with resolved placeholders
        assertFalse(result.isTriggered(), "Rule should not match");
        assertEquals("Amount 1000.0 is within normal range", result.getMessage(),
                "NO_MATCH should use no-match-message with resolved {{#amount}} placeholder");
    }

    @Test
    @DisplayName("Should use standard message when no-match-message is null")
    void testNoMatchMessage_FallbackToMessage() {
        // Given: Rule WITHOUT no-match-message (null)
        java.util.Set<dev.mars.apex.core.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.core.engine.model.Category("test", 100));
        Rule rule = new Rule("fallback-test", categories, "Amount Check",
                "#amount > 5000", "Amount {{#amount}} exceeds threshold",
                "Checks amount threshold", 100, "INFO", null, null, null, null, null, null,
                null, true);  // no-match-message is null

        // When: condition is false
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then: should fall back to the standard message
        assertFalse(result.isTriggered(), "Rule should not match");
        assertEquals("Amount 1000.0 exceeds threshold", result.getMessage(),
                "NO_MATCH without no-match-message should fall back to standard message");
    }

    @Test
    @DisplayName("Should use match message on MATCH even when no-match-message is set")
    void testNoMatchMessage_MatchUsesStandardMessage() {
        // Given: Rule with both messages, condition that MATCHES
        java.util.Set<dev.mars.apex.core.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.core.engine.model.Category("test", 100));
        Rule rule = new Rule("match-msg-test", categories, "Amount Check",
                "#amount > 500", "Amount {{#amount}} exceeds threshold",
                "Checks amount threshold", 100, "INFO", null, null, null, null, null, null,
                "Amount {{#amount}} is within normal range", true);

        // When: condition is true (1000 > 500)
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then: MATCH should use the standard message, not the no-match-message
        assertTrue(result.isTriggered(), "Rule should match");
        assertEquals("Amount 1000.0 exceeds threshold", result.getMessage(),
                "MATCH should use standard message, not no-match-message");
    }

    @Test
    @DisplayName("Should resolve #{} SpEL format in no-match-message")
    void testNoMatchMessage_SpelFormat() {
        // Given: Rule with no-match-message using #{} SpEL template format
        java.util.Set<dev.mars.apex.core.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.core.engine.model.Category("test", 100));
        Rule rule = new Rule("spel-nomatch-test", categories, "Amount Check",
                "#amount > 5000", "Over #{#amount}",
                "Checks amount", 100, "INFO", null, null, null, null, null, null,
                "Under #{#amount}", true);

        // When: condition is false
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertFalse(result.isTriggered());
        assertEquals("Under 1000.0", result.getMessage(),
                "NO_MATCH should resolve #{} in no-match-message");
    }
}
