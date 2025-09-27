package dev.mars.apex.core.service.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3B: Unit tests for recovery metrics in RulePerformanceMetrics.
 * Tests the new recovery-related fields and methods added to support recovery metrics collection.
 */
class RulePerformanceMetricsRecoveryTest {

    @Test
    @DisplayName("Should create metrics with recovery information when recovery attempted and successful")
    void testMetricsWithSuccessfulRecovery() {
        // Given
        String ruleName = "test-rule";
        Instant startTime = Instant.now().minusSeconds(1);
        Instant endTime = Instant.now();
        Duration evaluationTime = Duration.ofMillis(100);
        Duration recoveryTime = Duration.ofMillis(50);
        
        // When
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder(ruleName)
            .startTime(startTime)
            .endTime(endTime)
            .evaluationTime(evaluationTime)
            .memoryUsed(1024)
            .expressionComplexity(5)
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryStrategy("RULE_DEFAULT_VALUE")
            .recoveryReason("NullPointerException")
            .recoveryTime(recoveryTime)
            .build();
        
        // Then
        assertTrue(metrics.isRecoveryAttempted(), "Recovery should be marked as attempted");
        assertTrue(metrics.isRecoverySuccessful(), "Recovery should be marked as successful");
        assertEquals("RULE_DEFAULT_VALUE", metrics.getRecoveryStrategy(), "Recovery strategy should match");
        assertEquals("NullPointerException", metrics.getRecoveryReason(), "Recovery reason should match");
        assertEquals(recoveryTime, metrics.getRecoveryTime(), "Recovery time should match");
        assertEquals(50L, metrics.getRecoveryTimeMillis(), "Recovery time in millis should match");
    }

    @Test
    @DisplayName("Should create metrics with failed recovery information")
    void testMetricsWithFailedRecovery() {
        // Given
        String ruleName = "failing-rule";
        Duration recoveryTime = Duration.ofMillis(75);
        
        // When
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder(ruleName)
            .evaluationTime(Duration.ofMillis(200))
            .recoveryAttempted(true)
            .recoverySuccessful(false)
            .recoveryStrategy("DEFAULT_STRATEGY")
            .recoveryReason("IllegalArgumentException")
            .recoveryTime(recoveryTime)
            .build();
        
        // Then
        assertTrue(metrics.isRecoveryAttempted(), "Recovery should be marked as attempted");
        assertFalse(metrics.isRecoverySuccessful(), "Recovery should be marked as failed");
        assertEquals("DEFAULT_STRATEGY", metrics.getRecoveryStrategy(), "Recovery strategy should match");
        assertEquals("IllegalArgumentException", metrics.getRecoveryReason(), "Recovery reason should match");
        assertEquals(75L, metrics.getRecoveryTimeMillis(), "Recovery time in millis should match");
    }

    @Test
    @DisplayName("Should create metrics without recovery information when no recovery attempted")
    void testMetricsWithoutRecovery() {
        // Given
        String ruleName = "normal-rule";
        
        // When
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder(ruleName)
            .evaluationTime(Duration.ofMillis(50))
            .memoryUsed(512)
            .expressionComplexity(3)
            .build();
        
        // Then
        assertFalse(metrics.isRecoveryAttempted(), "Recovery should not be marked as attempted");
        assertFalse(metrics.isRecoverySuccessful(), "Recovery should not be marked as successful");
        assertNull(metrics.getRecoveryStrategy(), "Recovery strategy should be null");
        assertNull(metrics.getRecoveryReason(), "Recovery reason should be null");
        assertNull(metrics.getRecoveryTime(), "Recovery time should be null");
        assertEquals(0L, metrics.getRecoveryTimeMillis(), "Recovery time in millis should be 0");
    }

    @Test
    @DisplayName("Should include recovery information in performance summary when recovery attempted")
    void testPerformanceSummaryWithRecovery() {
        // Given
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("test-rule")
            .evaluationTime(Duration.ofMillis(100))
            .memoryUsed(1024)
            .expressionComplexity(5)
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryStrategy("RULE_DEFAULT_VALUE")
            .recoveryTime(Duration.ofMillis(25))
            .build();
        
        // When
        String summary = metrics.getPerformanceSummary();
        
        // Then
        assertNotNull(summary, "Performance summary should not be null");
        assertTrue(summary.contains("Recovery: SUCCESS"), "Summary should indicate successful recovery");
        assertTrue(summary.contains("(RULE_DEFAULT_VALUE)"), "Summary should include recovery strategy");
        assertTrue(summary.contains("[25ms]"), "Summary should include recovery time");
    }

    @Test
    @DisplayName("Should include failed recovery information in performance summary")
    void testPerformanceSummaryWithFailedRecovery() {
        // Given
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("failing-rule")
            .evaluationTime(Duration.ofMillis(150))
            .recoveryAttempted(true)
            .recoverySuccessful(false)
            .recoveryStrategy("DEFAULT_STRATEGY")
            .recoveryTime(Duration.ofMillis(40))
            .build();
        
        // When
        String summary = metrics.getPerformanceSummary();
        
        // Then
        assertNotNull(summary, "Performance summary should not be null");
        assertTrue(summary.contains("Recovery: FAILED"), "Summary should indicate failed recovery");
        assertTrue(summary.contains("(DEFAULT_STRATEGY)"), "Summary should include recovery strategy");
        assertTrue(summary.contains("[40ms]"), "Summary should include recovery time");
    }

    @Test
    @DisplayName("Should not include recovery information in performance summary when no recovery attempted")
    void testPerformanceSummaryWithoutRecovery() {
        // Given
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("normal-rule")
            .evaluationTime(Duration.ofMillis(75))
            .memoryUsed(512)
            .expressionComplexity(3)
            .build();
        
        // When
        String summary = metrics.getPerformanceSummary();
        
        // Then
        assertNotNull(summary, "Performance summary should not be null");
        assertFalse(summary.contains("Recovery:"), "Summary should not contain recovery information");
    }

    @Test
    @DisplayName("Should handle null recovery time gracefully")
    void testNullRecoveryTime() {
        // Given & When
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("test-rule")
            .evaluationTime(Duration.ofMillis(100))
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryStrategy("RULE_DEFAULT_VALUE")
            .recoveryReason("Exception")
            // Note: not setting recoveryTime, should be null
            .build();
        
        // Then
        assertTrue(metrics.isRecoveryAttempted(), "Recovery should be attempted");
        assertTrue(metrics.isRecoverySuccessful(), "Recovery should be successful");
        assertNull(metrics.getRecoveryTime(), "Recovery time should be null");
        assertEquals(0L, metrics.getRecoveryTimeMillis(), "Recovery time in millis should be 0 for null time");
        
        // Performance summary should handle null recovery time
        String summary = metrics.getPerformanceSummary();
        assertTrue(summary.contains("Recovery: SUCCESS"), "Summary should show successful recovery");
        assertFalse(summary.contains("["), "Summary should not contain recovery time brackets for null time");
    }

    @Test
    @DisplayName("Should support builder pattern for all recovery fields")
    void testBuilderPatternForRecoveryFields() {
        // Given
        RulePerformanceMetrics.Builder builder = new RulePerformanceMetrics.Builder("test-rule");
        
        // When - chain all recovery methods
        RulePerformanceMetrics metrics = builder
            .evaluationTime(Duration.ofMillis(100))
            .recoveryAttempted(true)
            .recoverySuccessful(false)
            .recoveryStrategy("CUSTOM_STRATEGY")
            .recoveryReason("CustomException")
            .recoveryTime(Duration.ofMillis(30))
            .build();
        
        // Then
        assertTrue(metrics.isRecoveryAttempted(), "Recovery attempted should be set");
        assertFalse(metrics.isRecoverySuccessful(), "Recovery successful should be set");
        assertEquals("CUSTOM_STRATEGY", metrics.getRecoveryStrategy(), "Recovery strategy should be set");
        assertEquals("CustomException", metrics.getRecoveryReason(), "Recovery reason should be set");
        assertEquals(Duration.ofMillis(30), metrics.getRecoveryTime(), "Recovery time should be set");
    }
}
