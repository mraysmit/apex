package dev.mars.apex.core.service.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3B: Unit tests for recovery metrics in PerformanceSnapshot.
 * Tests the aggregation of recovery metrics across multiple rule evaluations.
 */
class PerformanceSnapshotRecoveryTest {

    @Test
    @DisplayName("Should initialize recovery metrics from first metrics with successful recovery")
    void testInitialSnapshotWithSuccessfulRecovery() {
        // Given
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(100))
            .memoryUsed(1024)
            .expressionComplexity(5)
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryStrategy("RULE_DEFAULT_VALUE")
            .recoveryTime(Duration.ofMillis(25))
            .build();
        
        // When
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", metrics);
        
        // Then
        assertEquals(1L, snapshot.getRecoveryAttempts(), "Should have 1 recovery attempt");
        assertEquals(1L, snapshot.getSuccessfulRecoveries(), "Should have 1 successful recovery");
        assertEquals(1.0, snapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 100%");
        assertEquals(Duration.ofMillis(25), snapshot.getTotalRecoveryTime(), "Total recovery time should match");
        assertEquals(Duration.ofMillis(25), snapshot.getAverageRecoveryTime(), "Average recovery time should match");
    }

    @Test
    @DisplayName("Should initialize recovery metrics from first metrics with failed recovery")
    void testInitialSnapshotWithFailedRecovery() {
        // Given
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(150))
            .recoveryAttempted(true)
            .recoverySuccessful(false)
            .recoveryTime(Duration.ofMillis(40))
            .build();
        
        // When
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", metrics);
        
        // Then
        assertEquals(1L, snapshot.getRecoveryAttempts(), "Should have 1 recovery attempt");
        assertEquals(0L, snapshot.getSuccessfulRecoveries(), "Should have 0 successful recoveries");
        assertEquals(0.0, snapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 0%");
        assertEquals(Duration.ofMillis(40), snapshot.getTotalRecoveryTime(), "Total recovery time should match");
        assertEquals(Duration.ofMillis(40), snapshot.getAverageRecoveryTime(), "Average recovery time should match");
    }

    @Test
    @DisplayName("Should initialize recovery metrics from first metrics without recovery")
    void testInitialSnapshotWithoutRecovery() {
        // Given
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(75))
            .memoryUsed(512)
            .build();
        
        // When
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", metrics);
        
        // Then
        assertEquals(0L, snapshot.getRecoveryAttempts(), "Should have 0 recovery attempts");
        assertEquals(0L, snapshot.getSuccessfulRecoveries(), "Should have 0 successful recoveries");
        assertEquals(0.0, snapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 0%");
        assertEquals(Duration.ZERO, snapshot.getTotalRecoveryTime(), "Total recovery time should be zero");
        assertEquals(Duration.ZERO, snapshot.getAverageRecoveryTime(), "Average recovery time should be zero");
    }

    @Test
    @DisplayName("Should update recovery metrics when adding successful recovery")
    void testUpdateWithSuccessfulRecovery() {
        // Given - Initial snapshot without recovery
        RulePerformanceMetrics initialMetrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(2))
            .evaluationTime(Duration.ofMillis(50))
            .build();
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", initialMetrics);
        
        // When - Add metrics with successful recovery
        RulePerformanceMetrics newMetrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(100))
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryTime(Duration.ofMillis(30))
            .build();
        PerformanceSnapshot updatedSnapshot = snapshot.update(newMetrics);
        
        // Then
        assertEquals(1L, updatedSnapshot.getRecoveryAttempts(), "Should have 1 recovery attempt");
        assertEquals(1L, updatedSnapshot.getSuccessfulRecoveries(), "Should have 1 successful recovery");
        assertEquals(1.0, updatedSnapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 100%");
        assertEquals(Duration.ofMillis(30), updatedSnapshot.getTotalRecoveryTime(), "Total recovery time should be 30ms");
        assertEquals(Duration.ofMillis(30), updatedSnapshot.getAverageRecoveryTime(), "Average recovery time should be 30ms");
    }

    @Test
    @DisplayName("Should aggregate recovery metrics across multiple updates")
    void testAggregateRecoveryMetrics() {
        // Given - Initial snapshot with successful recovery
        RulePerformanceMetrics initialMetrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(3))
            .evaluationTime(Duration.ofMillis(100))
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryTime(Duration.ofMillis(20))
            .build();
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", initialMetrics);
        
        // When - Add failed recovery
        RulePerformanceMetrics failedRecovery = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(2))
            .evaluationTime(Duration.ofMillis(150))
            .recoveryAttempted(true)
            .recoverySuccessful(false)
            .recoveryTime(Duration.ofMillis(50))
            .build();
        snapshot = snapshot.update(failedRecovery);
        
        // And - Add another successful recovery
        RulePerformanceMetrics successfulRecovery = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(80))
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryTime(Duration.ofMillis(15))
            .build();
        PerformanceSnapshot finalSnapshot = snapshot.update(successfulRecovery);
        
        // Then
        assertEquals(3L, finalSnapshot.getRecoveryAttempts(), "Should have 3 recovery attempts");
        assertEquals(2L, finalSnapshot.getSuccessfulRecoveries(), "Should have 2 successful recoveries");
        assertEquals(2.0/3.0, finalSnapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 66.67%");
        assertEquals(Duration.ofMillis(85), finalSnapshot.getTotalRecoveryTime(), "Total recovery time should be 85ms (20+50+15)");
        
        // Average recovery time should be 85/3 ≈ 28.33ms
        long expectedAverageNanos = Duration.ofMillis(85).toNanos() / 3;
        assertEquals(Duration.ofNanos(expectedAverageNanos), finalSnapshot.getAverageRecoveryTime(), 
            "Average recovery time should be approximately 28.33ms");
    }

    @Test
    @DisplayName("Should handle mixed recovery and non-recovery updates")
    void testMixedRecoveryUpdates() {
        // Given - Initial snapshot without recovery
        RulePerformanceMetrics initialMetrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(4))
            .evaluationTime(Duration.ofMillis(50))
            .build();
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", initialMetrics);
        
        // When - Add successful evaluation without recovery
        RulePerformanceMetrics noRecovery = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(3))
            .evaluationTime(Duration.ofMillis(60))
            .build();
        snapshot = snapshot.update(noRecovery);
        
        // And - Add evaluation with recovery
        RulePerformanceMetrics withRecovery = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(2))
            .evaluationTime(Duration.ofMillis(120))
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            .recoveryTime(Duration.ofMillis(35))
            .build();
        PerformanceSnapshot finalSnapshot = snapshot.update(withRecovery);
        
        // Then
        assertEquals(3L, finalSnapshot.getEvaluationCount(), "Should have 3 total evaluations");
        assertEquals(1L, finalSnapshot.getRecoveryAttempts(), "Should have 1 recovery attempt");
        assertEquals(1L, finalSnapshot.getSuccessfulRecoveries(), "Should have 1 successful recovery");
        assertEquals(1.0, finalSnapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 100%");
        assertEquals(Duration.ofMillis(35), finalSnapshot.getTotalRecoveryTime(), "Total recovery time should be 35ms");
        assertEquals(Duration.ofMillis(35), finalSnapshot.getAverageRecoveryTime(), "Average recovery time should be 35ms");
    }

    @Test
    @DisplayName("Should handle null recovery time in updates")
    void testNullRecoveryTimeInUpdates() {
        // Given - Initial snapshot
        RulePerformanceMetrics initialMetrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(2))
            .evaluationTime(Duration.ofMillis(100))
            .build();
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", initialMetrics);
        
        // When - Add metrics with recovery but null recovery time
        RulePerformanceMetrics nullRecoveryTime = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(150))
            .recoveryAttempted(true)
            .recoverySuccessful(true)
            // Note: not setting recoveryTime, should be null
            .build();
        PerformanceSnapshot updatedSnapshot = snapshot.update(nullRecoveryTime);
        
        // Then
        assertEquals(1L, updatedSnapshot.getRecoveryAttempts(), "Should have 1 recovery attempt");
        assertEquals(1L, updatedSnapshot.getSuccessfulRecoveries(), "Should have 1 successful recovery");
        assertEquals(1.0, updatedSnapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 100%");
        assertEquals(Duration.ZERO, updatedSnapshot.getTotalRecoveryTime(), "Total recovery time should remain zero");
        assertEquals(Duration.ZERO, updatedSnapshot.getAverageRecoveryTime(), "Average recovery time should be zero");
    }

    @Test
    @DisplayName("Should calculate recovery success rate correctly with zero attempts")
    void testRecoverySuccessRateWithZeroAttempts() {
        // Given - Metrics without recovery
        RulePerformanceMetrics metrics = new RulePerformanceMetrics.Builder("test-rule")
            .startTime(Instant.now().minusSeconds(1))
            .evaluationTime(Duration.ofMillis(75))
            .build();
        
        // When
        PerformanceSnapshot snapshot = new PerformanceSnapshot("test-rule", metrics);
        
        // Then
        assertEquals(0L, snapshot.getRecoveryAttempts(), "Should have 0 recovery attempts");
        assertEquals(0.0, snapshot.getRecoverySuccessRate(), 0.001, "Recovery success rate should be 0% when no attempts");
    }
}
