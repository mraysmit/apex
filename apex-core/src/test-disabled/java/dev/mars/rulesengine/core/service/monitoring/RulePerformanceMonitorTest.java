package dev.mars.apex.core.service.monitoring;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Test class for RulePerformanceMonitor functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for RulePerformanceMonitor functionality.
 */
public class RulePerformanceMonitorTest {

    private RulesEngineConfiguration configuration;
    private RulesEngine engine;
    private RulePerformanceMonitor monitor;

    @BeforeEach
    void setUp() {
        configuration = new RulesEngineConfiguration();
        engine = new RulesEngine(configuration);
        monitor = engine.getPerformanceMonitor();
        // Ensure performance monitoring is enabled for tests
        monitor.setEnabled(true);
        // Clear any existing metrics to ensure clean state
        monitor.clearMetrics();
    }

    @AfterEach
    void tearDown() {
        // Ensure performance monitoring is enabled for next test
        if (monitor != null) {
            monitor.setEnabled(true);
            monitor.clearMetrics();
        }
    }

    @Test
    void testBasicPerformanceMonitoring() {
        // Create a simple rule
        Rule rule = configuration.registerRule(configuration.rule("test-rule")
                .withCategory("test")
                .withName("Simple Test Rule")
                .withDescription("A simple rule for testing")
                .withPriority(1)
                .withCondition("#value > 10")
                .withMessage("Value is greater than 10")
                .build());

        // Execute the rule with facts
        Map<String, Object> facts = new HashMap<>();
        facts.put("value", 15);

        RuleResult result = engine.executeRule(rule, facts);

        // Verify the result has performance metrics
        assertTrue(result.hasPerformanceMetrics(), "Result should have performance metrics");
        assertNotNull(result.getPerformanceMetrics());

        RulePerformanceMetrics metrics = result.getPerformanceMetrics();
        assertEquals("Simple Test Rule", metrics.getRuleName());
        assertNotNull(metrics.getEvaluationTime());
        assertTrue(metrics.getEvaluationTimeNanos() >= 0, "Evaluation time should be non-negative");
        assertFalse(metrics.hasException());
    }

    @Test
    void testPerformanceMetricsBuilder() {
        RulePerformanceMetrics.Builder builder = new RulePerformanceMetrics.Builder("test-rule");
        
        RulePerformanceMetrics metrics = builder
                .evaluationTime(Duration.ofMillis(10))
                .memoryUsed(1024)
                .expressionComplexity(5)
                .cacheHit(false)
                .evaluationPhase("evaluation")
                .build();

        assertEquals("test-rule", metrics.getRuleName());
        assertEquals(10, metrics.getEvaluationTimeMillis());
        assertEquals(1024, metrics.getMemoryUsedBytes());
        assertEquals(5, metrics.getExpressionComplexity());
        assertFalse(metrics.isCacheHit());
        assertEquals("evaluation", metrics.getEvaluationPhase());
        assertFalse(metrics.hasException());
    }

    @Test
    void testPerformanceMonitorHistory() {
        // Create a rule
        Rule rule = configuration.registerRule(configuration.rule("history-test")
                .withCategory("test")
                .withName("History Test Rule")
                .withCondition("#value > 5")
                .withMessage("Test message")
                .build());

        Map<String, Object> facts = new HashMap<>();
        facts.put("value", 10);

        // Execute the rule multiple times
        for (int i = 0; i < 5; i++) {
            engine.executeRule(rule, facts);
        }

        // Check history
        List<RulePerformanceMetrics> history = monitor.getRuleHistory("History Test Rule");
        assertEquals(5, history.size());

        // Check snapshot
        PerformanceSnapshot snapshot = monitor.getRuleSnapshot("History Test Rule");
        assertNotNull(snapshot);
        assertEquals("History Test Rule", snapshot.getRuleName());
        assertEquals(5, snapshot.getEvaluationCount());
        // For very fast evaluations, the time might be 0 milliseconds but should be >= 0
        assertTrue(snapshot.getAverageEvaluationTimeMillis() >= 0);
        // Also check that we have valid timing data in nanoseconds
        assertTrue(history.get(0).getEvaluationTimeNanos() >= 0);
    }

    @Test
    void testPerformanceSnapshot() {
        RulePerformanceMetrics metrics1 = new RulePerformanceMetrics.Builder("test-rule")
                .startTime(Instant.now().minusMillis(100))
                .endTime(Instant.now().minusMillis(90))
                .evaluationTime(Duration.ofMillis(10))
                .memoryUsed(1000)
                .expressionComplexity(3)
                .build();

        RulePerformanceMetrics metrics2 = new RulePerformanceMetrics.Builder("test-rule")
                .startTime(Instant.now().minusMillis(50))
                .endTime(Instant.now().minusMillis(30))
                .evaluationTime(Duration.ofMillis(20))
                .memoryUsed(2000)
                .expressionComplexity(5)
                .build();

        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", metrics1);
        assertEquals(1, snapshot.getEvaluationCount());
        assertEquals(10.0, snapshot.getAverageEvaluationTimeMillis());

        PerformanceSnapshot updated = snapshot.update(metrics2);
        assertEquals(2, updated.getEvaluationCount());
        assertEquals(15.0, updated.getAverageEvaluationTimeMillis()); // (10 + 20) / 2
        assertEquals(1500, updated.getAverageMemoryUsed()); // (1000 + 2000) / 2
        assertEquals(4.0, updated.getAverageComplexity()); // (3 + 5) / 2
    }

    @Test
    void testPerformanceAnalyzer() {
        // Create multiple rules with different performance characteristics
        Rule fastRule = configuration.registerRule(configuration.rule("fast-rule")
                .withCategory("test")
                .withName("Fast Rule")
                .withCondition("#value == 1")
                .withMessage("Fast rule")
                .build());

        Rule slowRule = configuration.registerRule(configuration.rule("slow-rule")
                .withCategory("test")
                .withName("Slow Rule")
                .withCondition("#value > 0 && #value < 100 && #value != 50")
                .withMessage("Slow rule")
                .build());

        Map<String, Object> facts = new HashMap<>();
        facts.put("value", 25);

        // Execute rules multiple times
        for (int i = 0; i < 10; i++) {
            engine.executeRule(fastRule, facts);
            engine.executeRule(slowRule, facts);
        }

        // Get snapshots and analyze
        Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
        List<PerformanceAnalyzer.PerformanceInsight> insights = PerformanceAnalyzer.analyzePerformance(snapshots);

        assertFalse(insights.isEmpty());
        
        // Generate report
        String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
        assertNotNull(report);
        assertTrue(report.contains("RULE ENGINE PERFORMANCE REPORT"));
        assertTrue(report.contains("Fast Rule"));
        assertTrue(report.contains("Slow Rule"));
    }

    @Test
    void testPerformanceMonitorConfiguration() {
        // Test enabling/disabling monitoring
        assertTrue(monitor.isEnabled());
        
        monitor.setEnabled(false);
        assertFalse(monitor.isEnabled());
        
        monitor.setEnabled(true);
        assertTrue(monitor.isEnabled());

        // Test configuration options
        monitor.setMaxHistorySize(100);
        monitor.setTrackMemory(true);
        monitor.setTrackComplexity(true);

        // These should not throw exceptions
        assertDoesNotThrow(() -> monitor.clearMetrics());
    }

    @Test
    void testRuleWithException() {
        // Create a rule that will cause an exception
        Rule errorRule = configuration.registerRule(configuration.rule("error-rule")
                .withCategory("test")
                .withName("Error Rule")
                .withCondition("#nonExistentProperty.someMethod()")
                .withMessage("This will fail")
                .build());

        Map<String, Object> facts = new HashMap<>();
        facts.put("value", 10);
        facts.put("nonExistentProperty", null); // This will cause a NullPointerException when someMethod() is called

        RuleResult result = engine.executeRule(errorRule, facts);

        // The result should still have performance metrics even for failed evaluations
        assertTrue(result.hasPerformanceMetrics());
        RulePerformanceMetrics metrics = result.getPerformanceMetrics();
        assertTrue(metrics.hasException());
        assertNotNull(metrics.getEvaluationException());
    }

    @Test
    void testGlobalPerformanceMetrics() {
        // Create and execute multiple rules
        Rule rule1 = configuration.registerRule(configuration.rule("global-test-1")
                .withCategory("test")
                .withName("Global Test 1")
                .withCondition("#value > 0")
                .withMessage("Test 1")
                .build());

        Rule rule2 = configuration.registerRule(configuration.rule("global-test-2")
                .withCategory("test")
                .withName("Global Test 2")
                .withCondition("#value < 100")
                .withMessage("Test 2")
                .build());

        Map<String, Object> facts = new HashMap<>();
        facts.put("value", 50);

        // Execute rules multiple times
        for (int i = 0; i < 5; i++) {
            engine.executeRule(rule1, facts);
            engine.executeRule(rule2, facts);
        }

        // Check global metrics
        assertEquals(10, monitor.getTotalEvaluations());
        assertTrue(monitor.getTotalEvaluationTimeNanos() >= 0, "Total evaluation time should be non-negative");
        assertTrue(monitor.getAverageEvaluationTimeMillis() >= 0, "Average evaluation time should be non-negative");
    }

    @Test
    void testPerformanceInsightGeneration() {
        Map<String, PerformanceSnapshot> snapshots = new HashMap<>();
        
        // Create a snapshot with poor performance
        RulePerformanceMetrics slowMetrics = new RulePerformanceMetrics.Builder("slow-rule")
                .evaluationTime(Duration.ofMillis(500))  // Increased to ensure it's > 2x average
                .memoryUsed(10000)
                .expressionComplexity(20)
                .build();

        snapshots.put("slow-rule", new PerformanceSnapshot("slow-rule", slowMetrics));

        // Create a snapshot with good performance
        RulePerformanceMetrics fastMetrics = new RulePerformanceMetrics.Builder("fast-rule")
                .evaluationTime(Duration.ofMillis(1))
                .memoryUsed(100)
                .expressionComplexity(2)
                .build();
        
        snapshots.put("fast-rule", new PerformanceSnapshot("fast-rule", fastMetrics));

        List<PerformanceAnalyzer.PerformanceInsight> insights = PerformanceAnalyzer.analyzePerformance(snapshots);

        // Should have insights about the slow rule (either SLOW_RULE or SLOWEST_RULE)
        assertTrue(insights.stream().anyMatch(insight ->
            ("SLOW_RULE".equals(insight.getType()) || "SLOWEST_RULE".equals(insight.getType())) && "slow-rule".equals(insight.getRuleName())));
        
        // Generate recommendations
        List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
        assertFalse(recommendations.isEmpty());
    }
}
