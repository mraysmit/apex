package dev.mars.apex.core.integration;

import dev.mars.apex.core.config.error.ErrorRecoveryConfig;
import dev.mars.apex.core.config.error.SeverityRecoveryPolicy;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.engine.UnifiedRuleEvaluator;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.service.monitoring.PerformanceSnapshot;
import dev.mars.apex.core.util.RulesEngineLogger;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3B: Integration test for end-to-end recovery metrics functionality.
 * Tests the complete flow from rule evaluation through error recovery to metrics collection.
 */
class RecoveryMetricsIntegrationTest {

    private UnifiedRuleEvaluator evaluator;
    private RulePerformanceMonitor performanceMonitor;
    private ErrorRecoveryConfig errorRecoveryConfig;

    @BeforeEach
    void setUp() {
        // Create error recovery configuration with metrics enabled
        errorRecoveryConfig = new ErrorRecoveryConfig();
        errorRecoveryConfig.setEnabled(true);
        errorRecoveryConfig.setMetricsEnabled(true);  // This is the key flag for Phase 3B
        errorRecoveryConfig.setLogRecoveryAttempts(true);
        
        // Configure severity policies
        SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy(true, "CONTINUE_WITH_DEFAULT", 0, 0L);
        SeverityRecoveryPolicy warningPolicy = new SeverityRecoveryPolicy(true, "CONTINUE_WITH_DEFAULT", 0, 0L);
        errorRecoveryConfig.setSeverityPolicy("ERROR", errorPolicy);
        errorRecoveryConfig.setSeverityPolicy("WARNING", warningPolicy);
        
        // Create performance monitor
        performanceMonitor = new RulePerformanceMonitor();
        
        // Create error recovery service
        ErrorRecoveryService errorRecoveryService = new ErrorRecoveryService();
        
        // Create rules engine logger
        RulesEngineLogger rulesLogger = new RulesEngineLogger(RecoveryMetricsIntegrationTest.class);
        
        // Create unified rule evaluator
        evaluator = new UnifiedRuleEvaluator(new SpelExpressionParser(), errorRecoveryService, performanceMonitor, errorRecoveryConfig);
    }

    @Test
    @DisplayName("Should collect recovery metrics when rule has default-value and metrics enabled")
    void testRecoveryMetricsWithRuleDefaultValue() {
        // Given - Rule with default-value that will fail evaluation
        Rule ruleWithDefault = new Rule(
            "test-rule-with-default",
            new HashSet<>(),  // categories
            "Test Rule With Default",
            "nonExistentField == 'value'",  // This will cause NullPointerException
            "Test rule with default value",
            "Test rule description",
            1,  // priority
            "ERROR",
            null,  // metadata
            "defaultValue123"  // Phase 3A default-value
        );

        Map<String, Object> facts = new HashMap<>();
        facts.put("existingField", "someValue");
        // Note: nonExistentField is not in facts, will cause evaluation error

        // When
        RuleResult result = evaluator.evaluateRule(ruleWithDefault, facts);

        // Then - Rule should succeed due to default-value recovery
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Rule should match due to default-value recovery");
        assertEquals("defaultValue123", result.getMessage(), "Result should use default value");

        // Verify recovery metrics are collected
        RulePerformanceMetrics metrics = result.getPerformanceMetrics();
        assertNotNull(metrics, "Metrics should be present");
        assertTrue(metrics.isRecoveryAttempted(), "Recovery should be marked as attempted");
        assertTrue(metrics.isRecoverySuccessful(), "Recovery should be marked as successful");
        assertEquals("RULE_DEFAULT_VALUE", metrics.getRecoveryStrategy(), "Recovery strategy should be RULE_DEFAULT_VALUE");
        assertNotNull(metrics.getRecoveryReason(), "Recovery reason should be present");
        assertNotNull(metrics.getRecoveryTime(), "Recovery time should be measured");
        assertTrue(metrics.getRecoveryTimeMillis() >= 0, "Recovery time should be non-negative");

        // Verify performance summary includes recovery information
        String summary = metrics.getPerformanceSummary();
        assertTrue(summary.contains("Recovery: SUCCESS"), "Summary should show successful recovery");
        assertTrue(summary.contains("(RULE_DEFAULT_VALUE)"), "Summary should show recovery strategy");
    }

    @Test
    @DisplayName("Should not collect recovery metrics when metrics disabled")
    void testNoRecoveryMetricsWhenDisabled() {
        // Given - Error recovery config with metrics disabled
        ErrorRecoveryConfig configWithoutMetrics = new ErrorRecoveryConfig();
        configWithoutMetrics.setEnabled(true);
        configWithoutMetrics.setMetricsEnabled(false);  // This disables recovery metrics
        configWithoutMetrics.setLogRecoveryAttempts(true);
        
        // Configure severity policies
        SeverityRecoveryPolicy errorPolicy = new SeverityRecoveryPolicy(true, "CONTINUE_WITH_DEFAULT", 0, 0L);
        configWithoutMetrics.setSeverityPolicy("ERROR", errorPolicy);
        
        // Create evaluator with metrics disabled
        ErrorRecoveryService errorRecoveryService = new ErrorRecoveryService();
        RulesEngineLogger rulesLogger = new RulesEngineLogger(RecoveryMetricsIntegrationTest.class);
        UnifiedRuleEvaluator evaluatorWithoutMetrics = new UnifiedRuleEvaluator(
            new SpelExpressionParser(), errorRecoveryService, performanceMonitor, configWithoutMetrics);
        
        Rule ruleWithDefault = new Rule(
            "test-rule",
            new HashSet<>(),  // categories
            "Test Rule",
            "nonExistentField == 'value'",
            "Test rule",
            "Test rule description",
            1,  // priority
            "ERROR",
            null,  // metadata
            "defaultValue"
        );
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("existingField", "someValue");
        
        // When
        RuleResult result = evaluatorWithoutMetrics.evaluateRule(ruleWithDefault, facts);
        
        // Then - Recovery should work but metrics should not include recovery information
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Rule should still match due to recovery");
        assertEquals("defaultValue", result.getMessage(), "Result should use default value");
        
        RulePerformanceMetrics metrics = result.getPerformanceMetrics();
        assertNotNull(metrics, "Basic metrics should be present");
        assertFalse(metrics.isRecoveryAttempted(), "Recovery metrics should not be collected when disabled");
        assertFalse(metrics.isRecoverySuccessful(), "Recovery metrics should not be collected when disabled");
        assertNull(metrics.getRecoveryStrategy(), "Recovery strategy should be null when metrics disabled");
        assertNull(metrics.getRecoveryReason(), "Recovery reason should be null when metrics disabled");
        assertNull(metrics.getRecoveryTime(), "Recovery time should be null when metrics disabled");
        assertEquals(0L, metrics.getRecoveryTimeMillis(), "Recovery time millis should be 0 when metrics disabled");
    }

    @Test
    @DisplayName("Should aggregate recovery metrics in PerformanceSnapshot")
    void testRecoveryMetricsAggregation() {
        // Given - Rule that will trigger recovery
        Rule rule = new Rule(
            "aggregation-test-rule",
            new HashSet<>(),  // categories
            "Aggregation Test Rule",
            "nonExistentField == 'value'",
            "Test rule for aggregation",
            "Test rule description",
            1,  // priority
            "ERROR",
            null,  // metadata
            "defaultValue"
        );
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("existingField", "someValue");
        
        // When - Evaluate rule multiple times to build up metrics
        RuleResult result1 = evaluator.evaluateRule(rule, facts);
        RuleResult result2 = evaluator.evaluateRule(rule, facts);
        RuleResult result3 = evaluator.evaluateRule(rule, facts);
        
        // Then - All evaluations should succeed with recovery
        assertEquals(RuleResult.ResultType.MATCH, result1.getResultType(), "First evaluation should succeed with recovery");
        assertEquals(RuleResult.ResultType.MATCH, result2.getResultType(), "Second evaluation should succeed with recovery");
        assertEquals(RuleResult.ResultType.MATCH, result3.getResultType(), "Third evaluation should succeed with recovery");
        
        // Verify individual metrics have recovery information
        assertTrue(result1.getPerformanceMetrics().isRecoveryAttempted(), "First result should have recovery metrics");
        assertTrue(result2.getPerformanceMetrics().isRecoveryAttempted(), "Second result should have recovery metrics");
        assertTrue(result3.getPerformanceMetrics().isRecoveryAttempted(), "Third result should have recovery metrics");
        
        // Create performance snapshot to test aggregation
        PerformanceSnapshot snapshot = new PerformanceSnapshot(rule.getName(), result1.getPerformanceMetrics());
        snapshot = snapshot.update(result2.getPerformanceMetrics());
        snapshot = snapshot.update(result3.getPerformanceMetrics());
        
        // Verify aggregated recovery metrics
        assertEquals(3L, snapshot.getEvaluationCount(), "Should have 3 total evaluations");
        assertEquals(3L, snapshot.getRecoveryAttempts(), "Should have 3 recovery attempts");
        assertEquals(3L, snapshot.getSuccessfulRecoveries(), "Should have 3 successful recoveries");
        assertEquals(1.0, snapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 100%");
        assertTrue(snapshot.getTotalRecoveryTime().toMillis() >= 0, "Total recovery time should be non-negative");
        assertTrue(snapshot.getAverageRecoveryTime().toMillis() >= 0, "Average recovery time should be non-negative");
    }

    @Test
    @DisplayName("Should handle rules without recovery gracefully in metrics")
    void testNoRecoveryMetricsForSuccessfulRules() {
        // Given - Rule that will succeed without recovery
        Rule successfulRule = new Rule(
            "successful-rule",
            new HashSet<>(),  // categories
            "Successful Rule",
            "existingField == 'someValue'",  // This will succeed
            "Test successful rule",
            "Test rule description",
            1,  // priority
            "ERROR",
            null,  // metadata
            null   // no default value
        );
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("existingField", "someValue");
        
        // When
        RuleResult result = evaluator.evaluateRule(successfulRule, facts);
        
        // Then - Rule should succeed without recovery
        assertEquals(RuleResult.ResultType.MATCH, result.getResultType(), "Rule should match without recovery");
        assertEquals("Test successful rule", result.getMessage(), "Result should be the rule message");
        
        // Verify no recovery metrics are collected for successful evaluation
        RulePerformanceMetrics metrics = result.getPerformanceMetrics();
        assertNotNull(metrics, "Metrics should be present");
        assertFalse(metrics.isRecoveryAttempted(), "Recovery should not be attempted for successful rule");
        assertFalse(metrics.isRecoverySuccessful(), "Recovery should not be marked as successful");
        assertNull(metrics.getRecoveryStrategy(), "Recovery strategy should be null");
        assertNull(metrics.getRecoveryReason(), "Recovery reason should be null");
        assertNull(metrics.getRecoveryTime(), "Recovery time should be null");
        assertEquals(0L, metrics.getRecoveryTimeMillis(), "Recovery time millis should be 0");
        
        // Performance summary should not include recovery information
        String summary = metrics.getPerformanceSummary();
        assertFalse(summary.contains("Recovery:"), "Summary should not contain recovery information");
    }
}
