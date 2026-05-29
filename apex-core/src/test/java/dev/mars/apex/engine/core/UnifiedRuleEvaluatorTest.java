package dev.mars.apex.engine.core;

import dev.mars.apex.engine.model.Rule;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.core.config.model.condition.SharedConditionGroup;
import dev.mars.apex.core.config.model.condition.SharedConditionRule;
import dev.mars.apex.core.service.enrichment.EnrichmentProcessor;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.engine.core.ExpressionEvaluatorService;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.engine.execution.RuleGroupEvaluationService;

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

    private static final String PHASE4_YAML =
            "src/test/resources/dev/mars/apex/engine/core/UnifiedRuleEvaluatorTest_Phase4.yaml";
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedRuleEvaluatorTest.class);
    private UnifiedRuleEvaluator evaluator;
    private Map<String, Object> testFacts;
    
    @BeforeAll
    static void classSetUp() {
        MDC.put("testContext", "[EXPECTED] ");
        LoggerFactory.getLogger(UnifiedRuleEvaluatorTest.class)
                .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] UnifiedRuleEvaluatorTest intentionally triggers ERROR/WARN logs");
        LoggerFactory.getLogger(UnifiedRuleEvaluatorTest.class)
                .info("[INTENTIONAL-FAILURE-TEST-CLASS-START] Expected: SpEL failures, missing properties, invalid expressions");
    }

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
        Rule rule = new Rule("R-empty", java.util.Set.of(new dev.mars.apex.engine.model.Category("default", 100)),
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
        java.util.Set<dev.mars.apex.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.engine.model.Category("test", 100));
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
        java.util.Set<dev.mars.apex.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.engine.model.Category("test", 100));
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
        java.util.Set<dev.mars.apex.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.engine.model.Category("test", 100));
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
        java.util.Set<dev.mars.apex.engine.model.Category> categories = new java.util.HashSet<>();
        categories.add(new dev.mars.apex.engine.model.Category("test", 100));
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

    // =========================================================================
    // Structured Condition Group Evaluation Tests
    // =========================================================================

    @Test
    @DisplayName("Should evaluate structured AND conditions - all true")
    void testStructuredConditions_AndAllTrue() {
        // Given: AND group with two expression predicates, both true
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("AND");

        var pred1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred1.setType("expression");
        pred1.setCondition("#amount > 500");
        pred1.setDescription("Amount check");

        var pred2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred2.setType("expression");
        pred2.setCondition("#currency == 'USD'");
        pred2.setDescription("Currency check");

        group.setRules(List.of(pred1, pred2));

        Rule rule = new RuleBuilder()
                .withName("Structured AND Rule")
                .withConditions(group)
                .withMessage("All conditions met")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertTrue(result.isTriggered(), "AND group with all-true predicates should MATCH");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
        assertEquals("Structured AND Rule", result.getRuleName());
    }

    @Test
    @DisplayName("Should evaluate structured AND conditions - one false")
    void testStructuredConditions_AndOneFalse() {
        // Given: AND group where second predicate is false
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("AND");

        var pred1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred1.setType("expression");
        pred1.setCondition("#amount > 500");

        var pred2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred2.setType("expression");
        pred2.setCondition("#currency == 'EUR'"); // False — USD != EUR

        group.setRules(List.of(pred1, pred2));

        Rule rule = new RuleBuilder()
                .withName("Structured AND Fail")
                .withConditions(group)
                .withMessage("Should not match")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertFalse(result.isTriggered(), "AND group with one false predicate should NOT match");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Should evaluate structured OR conditions - one true")
    void testStructuredConditions_OrOneTrue() {
        // Given: OR group where first predicate is false, second true
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("OR");

        var pred1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred1.setType("expression");
        pred1.setCondition("#amount > 5000"); // False

        var pred2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred2.setType("expression");
        pred2.setCondition("#currency == 'USD'"); // True

        group.setRules(List.of(pred1, pred2));

        Rule rule = new RuleBuilder()
                .withName("Structured OR Rule")
                .withConditions(group)
                .withMessage("At least one condition met")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertTrue(result.isTriggered(), "OR group with one true predicate should MATCH");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Should evaluate structured OR conditions - all false")
    void testStructuredConditions_OrAllFalse() {
        // Given: OR group where all predicates are false
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("OR");

        var pred1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred1.setType("expression");
        pred1.setCondition("#amount > 5000"); // False

        var pred2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred2.setType("expression");
        pred2.setCondition("#currency == 'EUR'"); // False

        group.setRules(List.of(pred1, pred2));

        Rule rule = new RuleBuilder()
                .withName("Structured OR Fail")
                .withConditions(group)
                .withMessage("None matched")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertFalse(result.isTriggered(), "OR group with all-false predicates should NOT match");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Should handle function predicate without condition - defaults to true")
    void testStructuredConditions_FunctionPredicateNoCondition() {
        // Given: AND group with one expression (true) and one function with no condition (implicitly true)
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("AND");

        var expr = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        expr.setType("expression");
        expr.setCondition("#amount > 500");

        var func = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        func.setType("function");
        func.setDescription("Classify risk level");
        func.setEnrichmentGroupRef("risk-classification");
        func.setOutputField("risk_level");
        // Note: no condition — should default to true

        group.setRules(List.of(expr, func));

        Rule rule = new RuleBuilder()
                .withName("Function No Condition Rule")
                .withConditions(group)
                .withMessage("Function predicate passed")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertTrue(result.isTriggered(), "Function predicate without condition should default to true");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Should store result-field from structured conditions evaluation")
    void testStructuredConditions_ResultFieldStored() {
        // Given: Rule with structured conditions AND a result-field
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("AND");

        var pred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred.setType("expression");
        pred.setCondition("#amount > 500");
        group.setRules(List.of(pred));

        // Use 17-param constructor to set result-field (RuleBuilder doesn't expose it)
        java.util.Set<dev.mars.apex.engine.model.Category> cats = java.util.Set.of(
                new dev.mars.apex.engine.model.Category("default", 100));
        Rule rule = new Rule("result-field-test", cats, "Result Field Rule",
                null, "Stored result", "Test result-field with structured conditions",
                100, "INFO", null, null, null, null, null,
                "amountCheck", null, true, group);

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertTrue(result.isTriggered());
        // result-field should be in enrichedData
        assertTrue(result.getEnrichedData().containsKey("amountCheck"),
                "result-field 'amountCheck' should be stored in enrichedData");
        assertEquals(true, result.getEnrichedData().get("amountCheck"));
    }

    @Test
    @DisplayName("Should handle SpEL error in structured conditions via error recovery")
    void testStructuredConditions_SpelErrorRecovery() {
        // Given: Structured condition with invalid SpEL
        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("AND");

        var pred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred.setType("expression");
        pred.setCondition("#nonExistent.invalidMethod()");
        group.setRules(List.of(pred));

        Rule rule = new RuleBuilder()
                .withName("Structured SpEL Error")
                .withConditions(group)
                .withMessage("Should error")
                .withSeverity("CRITICAL")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertEquals(RuleResult.ResultType.ERROR, result.getResultType(),
                "CRITICAL severity structured condition error should return ERROR");
    }

    @Test
    @DisplayName("Should coexist with traditional string conditions in evaluateRules")
    void testStructuredConditions_CoexistsWithTraditional() {
        // Given: Mix of traditional and structured condition rules
        Rule traditionalRule = new RuleBuilder()
                .withName("Traditional Rule")
                .withCondition("#amount > 2000")  // False
                .withMessage("Traditional match")
                .withSeverity("INFO")
                .build();

        var group = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        group.setOperator("AND");
        var pred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred.setType("expression");
        pred.setCondition("#currency == 'USD'");
        group.setRules(List.of(pred));

        Rule structuredRule = new RuleBuilder()
                .withName("Structured Rule")
                .withConditions(group)
                .withMessage("Structured match")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRules(List.of(traditionalRule, structuredRule), testFacts);

        // Then: Traditional doesn't match, structured does → structured is first significant result
        assertTrue(result.isTriggered());
        assertEquals("Structured Rule", result.getRuleName(),
                "Structured condition rule should be the first match");
    }

    // =========================================================================
    // Nested Conditions Gate Tests (conditions field on SharedConditionRule)
    // =========================================================================
    //
    // WHY WERE THERE NO FAILING TESTS FOR THIS GAP?
    //
    // The `conditions` field did not exist on SharedConditionRule before the fix.
    // No test could ever call setConditions() on a predicate, so the code path in
    // evaluateStructuredConditionRule that only checked getCondition() (flat SpEL string)
    // was never challenged. The feature was half-implemented invisibly — the model
    // didn't expose the capability, so no test could exercise or detect its absence.
    // All existing lookup/function predicate tests used no condition (defaults to true)
    // or a flat `condition` string, which already worked. The gap only became visible
    // once the model field and the evaluator branch were added together.

    @Test
    @DisplayName("Nested AND conditions gate on expression predicate - all true → MATCH")
    void testNestedConditions_ExpressionPredicate_AndGate_AllTrue() {
        // Given: expression predicate whose gate is a nested AND group (both sub-conditions true)
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("AND");
        var inner1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner1.setType("expression");
        inner1.setCondition("#amount > 500");        // true  (1000 > 500)
        var inner2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner2.setType("expression");
        inner2.setCondition("#currency == 'USD'");   // true
        innerGroup.setRules(List.of(inner1, inner2));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var pred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred.setType("expression");
        pred.setDescription("Nested AND gate predicate");
        pred.setConditions(innerGroup);              // nested conditions — no flat condition string
        outerGroup.setRules(List.of(pred));

        Rule rule = new RuleBuilder()
                .withName("Nested AND Gate Rule")
                .withConditions(outerGroup)
                .withMessage("Nested conditions matched")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertTrue(result.isTriggered(), "Nested AND gate with all-true sub-conditions should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Nested AND conditions gate on expression predicate - one false → NO_MATCH")
    void testNestedConditions_ExpressionPredicate_AndGate_OneFalse() {
        // Given: expression predicate with nested AND gate where one sub-condition is false
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("AND");
        var inner1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner1.setType("expression");
        inner1.setCondition("#amount > 500");        // true
        var inner2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner2.setType("expression");
        inner2.setCondition("#amount > 5000");       // false (1000 < 5000)
        innerGroup.setRules(List.of(inner1, inner2));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var pred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred.setType("expression");
        pred.setConditions(innerGroup);
        outerGroup.setRules(List.of(pred));

        Rule rule = new RuleBuilder()
                .withName("Nested AND Gate - One False")
                .withConditions(outerGroup)
                .withMessage("Should not match")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertFalse(result.isTriggered(), "Nested AND gate with one false sub-condition should not match");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Nested OR conditions gate on lookup predicate - one true → MATCH (Phase 3 deferred)")
    void testNestedConditions_LookupPredicate_OrGate_OneTrue() {
        // BEFORE FIX: 'conditions' field didn't exist on SharedConditionRule.
        //             evaluateStructuredConditionRule only checked getCondition() (flat string).
        //             Setting a nested group was impossible and the nested-gate code path
        //             was entirely untestable and untested.
        //
        // Given: lookup predicate (Phase 3 deferred — no actual lookup executed) with nested OR gate
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("OR");
        var inner1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner1.setType("expression");
        inner1.setCondition("#amount > 5000");       // false
        var inner2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner2.setType("expression");
        inner2.setCondition("#currency == 'USD'");   // true → OR group resolves to true
        innerGroup.setRules(List.of(inner1, inner2));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var lookupPred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        lookupPred.setType("lookup");
        lookupPred.setDescription("Customer creditworthiness lookup");
        lookupPred.setResultField("creditScore");
        lookupPred.setConditions(innerGroup);        // nested OR gate
        outerGroup.setRules(List.of(lookupPred));

        Rule rule = new RuleBuilder()
                .withName("Lookup Nested OR Gate Rule")
                .withConditions(outerGroup)
                .withMessage("Lookup nested gate passed")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then: OR gate is true (currency == 'USD'), so the lookup predicate resolves to true
        assertTrue(result.isTriggered(),
                "Lookup predicate with nested OR gate (one true sub-condition) should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Nested AND conditions gate on lookup predicate - all false → NO_MATCH (Phase 3 deferred)")
    void testNestedConditions_LookupPredicate_AndGate_AllFalse() {
        // Given: lookup predicate with nested AND gate where both sub-conditions are false
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("AND");
        var inner1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner1.setType("expression");
        inner1.setCondition("#amount > 5000");       // false
        var inner2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner2.setType("expression");
        inner2.setCondition("#currency == 'GBP'");   // false (it's USD)
        innerGroup.setRules(List.of(inner1, inner2));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var lookupPred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        lookupPred.setType("lookup");
        lookupPred.setDescription("Customer lookup");
        lookupPred.setConditions(innerGroup);
        outerGroup.setRules(List.of(lookupPred));

        Rule rule = new RuleBuilder()
                .withName("Lookup Nested AND Gate - All False")
                .withConditions(outerGroup)
                .withMessage("Should not match")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertFalse(result.isTriggered(),
                "Lookup predicate with nested AND gate (all false) should not match");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Nested AND conditions gate on function predicate - all true → MATCH (Phase 3 deferred)")
    void testNestedConditions_FunctionPredicate_AndGate_AllTrue() {
        // BEFORE FIX: SharedConditionRule had no 'conditions' field.
        //             evaluateStructuredConditionRule only checked getCondition() — the nested
        //             group path was entirely missing from the evaluator. Function predicates
        //             with a nested conditions gate could never be configured or tested.
        //
        // Given: function predicate (Phase 3 deferred) with nested AND gate (both true)
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("AND");
        var inner1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner1.setType("expression");
        inner1.setCondition("#amount > 500");        // true
        var inner2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner2.setType("expression");
        inner2.setCondition("#riskLevel == 'LOW'");  // true
        innerGroup.setRules(List.of(inner1, inner2));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var funcPred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        funcPred.setType("function");
        funcPred.setDescription("Risk classification function");
        funcPred.setEnrichmentGroupRef("risk-classification");
        funcPred.setOutputField("riskCategory");
        funcPred.setConditions(innerGroup);          // nested AND gate
        outerGroup.setRules(List.of(funcPred));

        Rule rule = new RuleBuilder()
                .withName("Function Nested AND Gate Rule")
                .withConditions(outerGroup)
                .withMessage("Function nested gate passed")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertTrue(result.isTriggered(),
                "Function predicate with nested AND gate (all true) should match");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }

    @Test
    @DisplayName("Nested OR conditions gate on function predicate - all false → NO_MATCH (Phase 3 deferred)")
    void testNestedConditions_FunctionPredicate_OrGate_AllFalse() {
        // Given: function predicate with nested OR gate where all sub-conditions are false
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("OR");
        var inner1 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner1.setType("expression");
        inner1.setCondition("#amount > 9000");       // false
        var inner2 = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner2.setType("expression");
        inner2.setCondition("#currency == 'EUR'");   // false (it's USD)
        innerGroup.setRules(List.of(inner1, inner2));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var funcPred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        funcPred.setType("function");
        funcPred.setEnrichmentGroupRef("risk-classification");
        funcPred.setConditions(innerGroup);
        outerGroup.setRules(List.of(funcPred));

        Rule rule = new RuleBuilder()
                .withName("Function Nested OR Gate - All False")
                .withConditions(outerGroup)
                .withMessage("Should not match")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then
        assertFalse(result.isTriggered(),
                "Function predicate with nested OR gate (all false) should not match");
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType());
    }

    @Test
    @DisplayName("'conditions' nested group takes precedence over flat 'condition' string on same predicate")
    void testNestedConditions_TakesPrecedenceOverFlatCondition() {
        // Given: predicate with BOTH 'condition' (evaluates false) and 'conditions' (evaluates true)
        //        'conditions' is checked first in the evaluator, so it should win.
        var innerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        innerGroup.setOperator("AND");
        var inner = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        inner.setType("expression");
        inner.setCondition("#amount > 500");         // true
        innerGroup.setRules(List.of(inner));

        var outerGroup = new dev.mars.apex.core.config.model.condition.SharedConditionGroup();
        outerGroup.setOperator("AND");
        var pred = new dev.mars.apex.core.config.model.condition.SharedConditionRule();
        pred.setType("expression");
        pred.setCondition("#amount > 9999");         // false — would produce NO_MATCH if used
        pred.setConditions(innerGroup);              // true  — takes precedence → should MATCH
        outerGroup.setRules(List.of(pred));

        Rule rule = new RuleBuilder()
                .withName("Conditions Precedence Rule")
                .withConditions(outerGroup)
                .withMessage("Conditions group took precedence")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluator.evaluateRule(rule, testFacts);

        // Then: 'conditions' (true) wins over flat 'condition' (false)
        assertTrue(result.isTriggered(),
                "'conditions' nested group should take precedence over flat 'condition' string");
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType());
    }

    // =========================================================================
    // Phase 3 — Lookup Predicate Execution Tests (TDD: RED first, then GREEN)
    // =========================================================================
    //
    // These tests require actual lookup execution inside evaluateStructuredConditionRule.
    // The evaluator must:
    //   1. Resolve the named LookupService from the injected LookupServiceRegistry
    //   2. Evaluate the lookup-key SpEL expression against the current context
    //   3. Call lookupService.transform(key) to obtain the result
    //   4. Stash the result into the EvaluationContext via setVariable(result-field, result)
    //      so the gate condition (#result-field == ...) can resolve it
    //
    // RED STATE (constructor stub only — no executeLookupPredicate wired):
    //   The lookup never executes. The context variable for result-field is never set
    //   (or retains the pre-existing fact value). Gate evaluations that depend on the
    //   stashed result return the wrong answer → assertion failures.
    //
    // GREEN STATE (executeLookupPredicate implemented):
    //   Lookup executes, result stashed, gate re-evaluated → correct MATCH / NO_MATCH.

    @Test
    @DisplayName("[Phase 3] Lookup predicate executes and stashes result; gate overrides pre-existing fact value")
    void testPhase3_LookupExecuted_OverridesExistingFactValue_GatePasses() {
        // Given: LookupService that maps CUST001 → PREMIUM
        LookupService tierLookup = new LookupService("tier-lookup", List.of("CUST001"));
        Map<String, Object> tierData = new HashMap<>();
        tierData.put("CUST001", "PREMIUM");
        tierLookup.setEnrichmentData(tierData);

        LookupServiceRegistry registry = new LookupServiceRegistry();
        registry.registerService(tierLookup);

        UnifiedRuleEvaluator evaluatorWithLookup = new UnifiedRuleEvaluator(registry);

        // Facts deliberately pre-seed customerTier = "BASIC" so that without lookup
        // execution the gate #customerTier == 'PREMIUM' returns false (NO_MATCH).
        // After Phase 3 lookup execution the context variable is overridden to "PREMIUM"
        // and the gate returns true (MATCH).
        Map<String, Object> facts = new HashMap<>(testFacts);
        facts.put("customerId", "CUST001");
        facts.put("customerTier", "BASIC"); // RED: gate reads this → false; GREEN: lookup overrides → PREMIUM → true

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("tier-lookup");
        lookupConfig.setLookupKey("#customerId");

        SharedConditionRule lookupPred = new SharedConditionRule();
        lookupPred.setType("lookup");
        lookupPred.setDescription("Customer tier lookup");
        lookupPred.setLookupConfig(lookupConfig);
        lookupPred.setResultField("customerTier");
        lookupPred.setCondition("#customerTier == 'PREMIUM'");

        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(lookupPred));

        Rule rule = new RuleBuilder()
                .withName("Customer Tier Check")
                .withConditions(group)
                .withMessage("Customer is PREMIUM tier")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluatorWithLookup.evaluateRule(rule, facts);

        // Then: MATCH because lookup overrides "BASIC" with "PREMIUM" in the context
        // RED: fails because lookup not yet executed → gate reads "BASIC" → NO_MATCH
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Lookup should execute and override 'BASIC' with 'PREMIUM' from the lookup service");
        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("[Phase 3] Lookup predicate executes; gate correctly fails when lookup returns non-matching value")
    void testPhase3_LookupExecuted_GateFails_WhenLookupReturnsWrongValue() {
        // Given: LookupService that maps CUST001 → BASIC (not PREMIUM)
        LookupService tierLookup = new LookupService("tier-lookup-basic", List.of("CUST001"));
        Map<String, Object> tierData = new HashMap<>();
        tierData.put("CUST001", "BASIC");
        tierLookup.setEnrichmentData(tierData);

        LookupServiceRegistry registry = new LookupServiceRegistry();
        registry.registerService(tierLookup);

        UnifiedRuleEvaluator evaluatorWithLookup = new UnifiedRuleEvaluator(registry);

        // Facts have no customerTier (no pre-existing value)
        Map<String, Object> facts = new HashMap<>(testFacts);
        facts.put("customerId", "CUST001");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("tier-lookup-basic");
        lookupConfig.setLookupKey("#customerId");

        SharedConditionRule lookupPred = new SharedConditionRule();
        lookupPred.setType("lookup");
        lookupPred.setDescription("Customer tier lookup - expects PREMIUM");
        lookupPred.setLookupConfig(lookupConfig);
        lookupPred.setResultField("customerTier");
        lookupPred.setCondition("#customerTier == 'PREMIUM'"); // gate should fail: lookup returns BASIC

        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(lookupPred));

        Rule rule = new RuleBuilder()
                .withName("Customer PREMIUM Check")
                .withConditions(group)
                .withMessage("Not PREMIUM")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluatorWithLookup.evaluateRule(rule, facts);

        // Then: NO_MATCH because lookup stashes "BASIC" and gate #customerTier == 'PREMIUM' fails
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                "Gate should fail when lookup returns BASIC (not PREMIUM)");
        assertFalse(result.isTriggered());
    }

    @Test
    @DisplayName("[Phase 3] No registry configured — lookup predicate falls back to gate-only evaluation")
    void testPhase3_NoRegistry_LookupPredicate_FallsBackToGateOnly() {
        // Given: evaluator with no LookupServiceRegistry (default constructor)
        // Facts pre-seed customerTier = PREMIUM so gate passes without any lookup
        Map<String, Object> facts = new HashMap<>(testFacts);
        facts.put("customerTier", "PREMIUM");

        YamlEnrichment.LookupConfig lookupConfig = new YamlEnrichment.LookupConfig();
        lookupConfig.setLookupService("any-service");
        lookupConfig.setLookupKey("#customerId");

        SharedConditionRule lookupPred = new SharedConditionRule();
        lookupPred.setType("lookup");
        lookupPred.setDescription("Lookup with no registry");
        lookupPred.setLookupConfig(lookupConfig);
        lookupPred.setResultField("customerTier");
        lookupPred.setCondition("#customerTier == 'PREMIUM'");

        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(lookupPred));

        Rule rule = new RuleBuilder()
                .withName("Fallback Gate Rule")
                .withConditions(group)
                .withMessage("Gate only")
                .withSeverity("INFO")
                .build();

        // When: plain evaluator (no registry)
        RuleResult result = evaluator.evaluateRule(rule, facts);

        // Then: MATCH — no lookup executed, gate reads existing fact value PREMIUM → true
        // This verifies backward compatibility: adding Phase 3 doesn't break no-registry deployments.
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Without registry, gate should evaluate against existing fact value");
        assertTrue(result.isTriggered());
    }

    // =========================================================================
    // Phase 4 — Function Predicate Execution Tests (TDD: RED first, then GREEN)
    // =========================================================================
    //
    // These tests verify that a "function"-type structured condition predicate:
    //   1. Executes the referenced enrichment group (mutates the facts Map in place)
    //   2. Stashes the output-field value from facts Map into the SpEL context
    //   3. Evaluates the gate condition against the updated context
    //
    // RED STATE (before executeFunctionPredicate is wired):
    //   The function branch logs a debug message and falls through to gate evaluation
    //   using whatever value is already in the context from the original facts Map.
    //   Tests 1 and 2 assert the WRONG values from the stub → assertion failures.
    //
    // GREEN STATE (executeFunctionPredicate implemented and wired):
    //   Enrichment group executes, result stashed into context, gate re-evaluated
    //   against updated value → correct MATCH / NO_MATCH.

    /**
     * Helper: build a fully-wired EnrichmentGroupExecutor backed by real APEX services.
     * The executor is generic — it processes whichever enrichment group it is asked to run;
     * the actual enrichment definitions come from the YAML config passed at evaluation time.
     */
    private EnrichmentGroupExecutor buildExecutor() {
        UnifiedRuleEvaluator miniEvaluator = new UnifiedRuleEvaluator();
        RuleGroupEvaluationService rgs = new RuleGroupEvaluationService(miniEvaluator);
        ExpressionEvaluatorService evs = new ExpressionEvaluatorService();
        EnrichmentProcessor ep = new EnrichmentProcessor(
                new LookupServiceRegistry(), evs, null, rgs);
        return new EnrichmentGroupExecutor(ep);
    }

    @Test
    @DisplayName("[Phase 4] Function predicate executes; enrichment group writes outputField; gate passes on updated value")
    void testPhase4_FunctionExecuted_StashesOutput_GatePasses() throws Exception {
        // Given: facts with riskLevel = LOW (gate will FAIL without function execution)
        Map<String, Object> facts = new HashMap<>(testFacts);
        facts.put("riskLevel", "LOW"); // RED: gate reads LOW → NO_MATCH; GREEN: function writes HIGH → MATCH

        // Load enrichment config from YAML — all business logic lives in the YAML file
        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(PHASE4_YAML);
        EnrichmentGroupExecutor executor = buildExecutor();

        UnifiedRuleEvaluator evaluatorWithFunction = new UnifiedRuleEvaluator();
        evaluatorWithFunction.setYamlRuleConfiguration(config);
        evaluatorWithFunction.setEnrichmentGroupExecutorSupplier(() -> executor);

        SharedConditionRule funcPred = new SharedConditionRule();
        funcPred.setType("function");
        funcPred.setDescription("Risk classifier");
        funcPred.setEnrichmentGroupRef("risk-classifier-group");
        funcPred.setOutputField("riskLevel");
        funcPred.setCondition("#riskLevel == 'HIGH'");

        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(funcPred));

        Rule rule = new RuleBuilder()
                .withName("Risk Level Check")
                .withConditions(group)
                .withMessage("Risk is HIGH")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluatorWithFunction.evaluateRule(rule, facts);

        // Then: MATCH because function wrote HIGH into facts, stashed into context, gate passes
        // RED: fails because function not yet executed → gate reads LOW → NO_MATCH
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Function should execute and write riskLevel=HIGH; gate #riskLevel=='HIGH' should pass");
        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("[Phase 4] Function predicate executes; enrichment group overrides pre-seeded value; gate correctly fails")
    void testPhase4_FunctionExecuted_GateFails_WhenGroupOverridesToNonMatchingValue() throws Exception {
        // Given: facts with riskLevel = HIGH (without function exec, gate would PASS)
        Map<String, Object> facts = new HashMap<>(testFacts);
        facts.put("riskLevel", "HIGH"); // RED: gate reads HIGH → MATCH; GREEN: function writes LOW → NO_MATCH

        // Load enrichment config from YAML — all business logic lives in the YAML file
        YamlRuleConfiguration config = new ConfigurationLoader().loadFromFile(PHASE4_YAML);
        EnrichmentGroupExecutor executor = buildExecutor();

        UnifiedRuleEvaluator evaluatorWithFunction = new UnifiedRuleEvaluator();
        evaluatorWithFunction.setYamlRuleConfiguration(config);
        evaluatorWithFunction.setEnrichmentGroupExecutorSupplier(() -> executor);

        SharedConditionRule funcPred = new SharedConditionRule();
        funcPred.setType("function");
        funcPred.setDescription("Risk downgrade check");
        funcPred.setEnrichmentGroupRef("risk-downgrade-group");
        funcPred.setOutputField("riskLevel");
        funcPred.setCondition("#riskLevel == 'HIGH'");

        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(funcPred));

        Rule rule = new RuleBuilder()
                .withName("Risk Level Must Be HIGH")
                .withConditions(group)
                .withMessage("Risk must be HIGH")
                .withSeverity("INFO")
                .build();

        // When
        RuleResult result = evaluatorWithFunction.evaluateRule(rule, facts);

        // Then: NO_MATCH because function overwrote HIGH with LOW; gate #riskLevel=='HIGH' fails
        // RED: fails because function not yet executed → gate reads HIGH → MATCH
        assertEquals(RuleResult.ResultType.NO_MATCH, result.getResultType(),
                "Function should overwrite riskLevel to LOW; gate #riskLevel=='HIGH' should fail → NO_MATCH");
        assertFalse(result.isTriggered());
    }

    @Test
    @DisplayName("[Phase 4] Function predicate with no executor falls back to gate-only evaluation (backward compatibility)")
    void testPhase4_FunctionWithNoExecutor_FallsBackToGateOnly() {
        // Given: facts with riskLevel = HIGH; no executor wired → gate reads pre-seeded value
        Map<String, Object> facts = new HashMap<>(testFacts);
        facts.put("riskLevel", "HIGH");

        // Plain evaluator — no function executor configured
        SharedConditionRule funcPred = new SharedConditionRule();
        funcPred.setType("function");
        funcPred.setDescription("Risk gate fallback");
        funcPred.setEnrichmentGroupRef("any-group");
        funcPred.setOutputField("riskLevel");
        funcPred.setCondition("#riskLevel == 'HIGH'");

        SharedConditionGroup group = new SharedConditionGroup();
        group.setOperator("AND");
        group.setRules(List.of(funcPred));

        Rule rule = new RuleBuilder()
                .withName("Risk Level Gate Fallback")
                .withConditions(group)
                .withMessage("Gate fallback")
                .withSeverity("INFO")
                .build();

        // When: plain evaluator (no executor)
        RuleResult result = evaluator.evaluateRule(rule, facts);

        // Then: MATCH — no function execution, gate reads existing fact value HIGH → true
        // This verifies backward compatibility: adding Phase 4 doesn't break no-executor deployments.
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(),
                "Without executor, gate should evaluate against existing fact value HIGH");
        assertTrue(result.isTriggered());
    }

    @AfterAll
    static void classTearDown() {
        LoggerFactory.getLogger(UnifiedRuleEvaluatorTest.class)
                .info("[INTENTIONAL-FAILURE-TEST-CLASS-END] UnifiedRuleEvaluatorTest intentional error tests completed");
        MDC.remove("testContext");
    }
}
