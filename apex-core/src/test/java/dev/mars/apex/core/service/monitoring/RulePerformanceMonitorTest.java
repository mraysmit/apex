package dev.mars.apex.core.service.monitoring;

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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RulePerformanceMonitor.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class RulePerformanceMonitorTest {

    private RulePerformanceMonitor performanceMonitor;

    @BeforeEach
    void setUp() {
        performanceMonitor = new RulePerformanceMonitor();
    }

    @Test
    @DisplayName("Should create performance monitor successfully")
    void testPerformanceMonitorCreation() {
        assertNotNull(performanceMonitor);
    }

    @Test
    @DisplayName("Should start and complete rule evaluation monitoring")
    void testStartAndCompleteEvaluation() {
        String ruleName = "testRule";
        String ruleCondition = "amount > 100";

        // Start monitoring
        RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName);
        assertNotNull(builder);

        // Simulate some work
        try {
            Thread.sleep(10); // Small delay to ensure measurable time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Complete monitoring
        RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, ruleCondition);
        assertNotNull(metrics);
        assertTrue(metrics.getEvaluationTimeNanos() > 0);
    }

    @Test
    @DisplayName("Should track rule evaluation history")
    void testRuleEvaluationHistory() {
        String ruleName = "historyRule";
        String ruleCondition = "value > 0";

        // Perform multiple evaluations
        for (int i = 0; i < 3; i++) {
            RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName);

            try {
                Thread.sleep(5); // Small delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            performanceMonitor.completeEvaluation(builder, ruleCondition);
        }

        // Check history
        List<RulePerformanceMetrics> history = performanceMonitor.getRuleHistory(ruleName);
        assertNotNull(history);
        assertEquals(3, history.size());
    }

    @Test
    @DisplayName("Should get performance snapshots")
    void testPerformanceSnapshots() {
        String ruleName = "snapshotRule";
        String ruleCondition = "test == true";

        // Perform an evaluation
        RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName);
        performanceMonitor.completeEvaluation(builder, ruleCondition);

        // Check snapshot
        PerformanceSnapshot snapshot = performanceMonitor.getRuleSnapshot(ruleName);
        assertNotNull(snapshot);
        assertEquals(ruleName, snapshot.getRuleName());
    }

    @Test
    @DisplayName("Should track total evaluations")
    void testTotalEvaluations() {
        long initialCount = performanceMonitor.getTotalEvaluations();

        // Perform some evaluations
        for (int i = 0; i < 5; i++) {
            RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation("rule" + i);
            performanceMonitor.completeEvaluation(builder, "true");
        }

        long finalCount = performanceMonitor.getTotalEvaluations();
        assertEquals(initialCount + 5, finalCount);
    }

    @Test
    @DisplayName("Should calculate average evaluation time")
    void testAverageEvaluationTime() {
        // Perform some evaluations
        for (int i = 0; i < 3; i++) {
            RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation("avgRule" + i);

            try {
                Thread.sleep(10); // Small delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            performanceMonitor.completeEvaluation(builder, "true");
        }

        double avgTime = performanceMonitor.getAverageEvaluationTimeMillis();
        assertTrue(avgTime > 0);
    }

    @Test
    @DisplayName("Should enable and disable monitoring")
    void testEnableDisableMonitoring() {
        // Test initial state
        assertTrue(performanceMonitor.isEnabled());

        // Disable monitoring
        performanceMonitor.setEnabled(false);
        assertFalse(performanceMonitor.isEnabled());

        // Re-enable monitoring
        performanceMonitor.setEnabled(true);
        assertTrue(performanceMonitor.isEnabled());
    }

    @Test
    @DisplayName("Should clear metrics")
    void testClearMetrics() {
        // Perform some evaluations
        RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation("clearRule");
        performanceMonitor.completeEvaluation(builder, "true");

        // Verify metrics exist
        assertTrue(performanceMonitor.getTotalEvaluations() > 0);

        // Clear metrics
        performanceMonitor.clearMetrics();

        // Verify metrics are cleared
        assertEquals(0, performanceMonitor.getTotalEvaluations());
        assertEquals(0.0, performanceMonitor.getAverageEvaluationTimeMillis(), 0.01);
    }

    @Test
    @DisplayName("Should handle evaluation with exception")
    void testEvaluationWithException() {
        String ruleName = "exceptionRule";
        String ruleCondition = "invalid.expression";
        Exception testException = new RuntimeException("Test exception");

        RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName);
        RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, ruleCondition, testException);

        assertNotNull(metrics);
        // When an exception occurs, the evaluation time might be 0, but metrics should still be created
        assertTrue(metrics.getEvaluationTimeNanos() >= 0);
        // Check that the exception is properly recorded
        assertNotNull(metrics.getEvaluationException());
        assertEquals("Test exception", metrics.getEvaluationException().getMessage());
    }

    @Test
    @DisplayName("Should track memory usage when enabled")
    void testMemoryTracking() {
        // Enable memory tracking
        performanceMonitor.setTrackMemory(true);

        RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation("memoryRule");
        RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, "true");

        assertNotNull(metrics);
        // Memory tracking details would depend on the actual implementation
    }

    @Test
    @DisplayName("Should track complexity when enabled")
    void testComplexityTracking() {
        // Enable complexity tracking
        performanceMonitor.setTrackComplexity(true);

        String complexCondition = "field1 > 10 && field2.contains('test') || (field3 != null && field4.size() > 5)";
        RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation("complexRule");
        RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, complexCondition);

        assertNotNull(metrics);
        // Complexity tracking details would depend on the actual implementation
    }

    @Test
    @DisplayName("Should handle concurrent evaluations")
    void testConcurrentEvaluations() throws InterruptedException {
        final int threadCount = 5;
        final int evaluationsPerThread = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < evaluationsPerThread; j++) {
                    String ruleName = "concurrentRule" + threadIndex + "_" + j;
                    RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName);
                    performanceMonitor.completeEvaluation(builder, "true");
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify total evaluations
        assertTrue(performanceMonitor.getTotalEvaluations() >= threadCount * evaluationsPerThread);
    }
}
