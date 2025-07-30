package dev.mars.apex.core.service.monitoring;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

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
 * Represents an aggregated performance snapshot for a rule.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Represents an aggregated performance snapshot for a rule.
 * This class maintains running statistics about rule performance
 * including averages, minimums, maximums, and trends.
 */
public class PerformanceSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String ruleName;
    private final long evaluationCount;
    private final Duration totalEvaluationTime;
    private final Duration averageEvaluationTime;
    private final Duration minEvaluationTime;
    private final Duration maxEvaluationTime;
    private final long totalMemoryUsed;
    private final long averageMemoryUsed;
    private final long minMemoryUsed;
    private final long maxMemoryUsed;
    private final double averageComplexity;
    private final int minComplexity;
    private final int maxComplexity;
    private final long successfulEvaluations;
    private final long failedEvaluations;
    private final double successRate;
    private final Instant firstEvaluation;
    private final Instant lastEvaluation;
    private final Instant lastUpdated;

    /**
     * Create a new performance snapshot from a single metrics instance.
     * 
     * @param ruleName The rule name
     * @param metrics The initial metrics
     */
    public PerformanceSnapshot(String ruleName, RulePerformanceMetrics metrics) {
        this.ruleName = ruleName;
        this.evaluationCount = 1;
        this.totalEvaluationTime = metrics.getEvaluationTime();
        this.averageEvaluationTime = metrics.getEvaluationTime();
        this.minEvaluationTime = metrics.getEvaluationTime();
        this.maxEvaluationTime = metrics.getEvaluationTime();
        this.totalMemoryUsed = metrics.getMemoryUsedBytes();
        this.averageMemoryUsed = metrics.getMemoryUsedBytes();
        this.minMemoryUsed = metrics.getMemoryUsedBytes();
        this.maxMemoryUsed = metrics.getMemoryUsedBytes();
        this.averageComplexity = metrics.getExpressionComplexity();
        this.minComplexity = metrics.getExpressionComplexity();
        this.maxComplexity = metrics.getExpressionComplexity();
        this.successfulEvaluations = metrics.hasException() ? 0 : 1;
        this.failedEvaluations = metrics.hasException() ? 1 : 0;
        this.successRate = metrics.hasException() ? 0.0 : 1.0;
        this.firstEvaluation = metrics.getStartTime();
        this.lastEvaluation = metrics.getStartTime();
        this.lastUpdated = Instant.now();
    }

    /**
     * Private constructor for creating updated snapshots.
     */
    private PerformanceSnapshot(String ruleName, long evaluationCount, Duration totalEvaluationTime,
                               Duration averageEvaluationTime, Duration minEvaluationTime, Duration maxEvaluationTime,
                               long totalMemoryUsed, long averageMemoryUsed, long minMemoryUsed, long maxMemoryUsed,
                               double averageComplexity, int minComplexity, int maxComplexity,
                               long successfulEvaluations, long failedEvaluations, double successRate,
                               Instant firstEvaluation, Instant lastEvaluation, Instant lastUpdated) {
        this.ruleName = ruleName;
        this.evaluationCount = evaluationCount;
        this.totalEvaluationTime = totalEvaluationTime;
        this.averageEvaluationTime = averageEvaluationTime;
        this.minEvaluationTime = minEvaluationTime;
        this.maxEvaluationTime = maxEvaluationTime;
        this.totalMemoryUsed = totalMemoryUsed;
        this.averageMemoryUsed = averageMemoryUsed;
        this.minMemoryUsed = minMemoryUsed;
        this.maxMemoryUsed = maxMemoryUsed;
        this.averageComplexity = averageComplexity;
        this.minComplexity = minComplexity;
        this.maxComplexity = maxComplexity;
        this.successfulEvaluations = successfulEvaluations;
        this.failedEvaluations = failedEvaluations;
        this.successRate = successRate;
        this.firstEvaluation = firstEvaluation;
        this.lastEvaluation = lastEvaluation;
        this.lastUpdated = lastUpdated;
    }

    /**
     * Update this snapshot with new metrics.
     * 
     * @param metrics The new metrics to incorporate
     * @return A new updated snapshot
     */
    public PerformanceSnapshot update(RulePerformanceMetrics metrics) {
        long newCount = evaluationCount + 1;
        
        // Update timing statistics
        Duration newTotalTime = totalEvaluationTime.plus(metrics.getEvaluationTime());
        Duration newAverageTime = Duration.ofNanos(newTotalTime.toNanos() / newCount);
        Duration newMinTime = minEvaluationTime.compareTo(metrics.getEvaluationTime()) <= 0 ? 
                             minEvaluationTime : metrics.getEvaluationTime();
        Duration newMaxTime = maxEvaluationTime.compareTo(metrics.getEvaluationTime()) >= 0 ? 
                             maxEvaluationTime : metrics.getEvaluationTime();
        
        // Update memory statistics
        long newTotalMemory = totalMemoryUsed + metrics.getMemoryUsedBytes();
        long newAverageMemory = newTotalMemory / newCount;
        long newMinMemory = Math.min(minMemoryUsed, metrics.getMemoryUsedBytes());
        long newMaxMemory = Math.max(maxMemoryUsed, metrics.getMemoryUsedBytes());
        
        // Update complexity statistics
        double newAverageComplexity = (averageComplexity * evaluationCount + metrics.getExpressionComplexity()) / newCount;
        int newMinComplexity = Math.min(minComplexity, metrics.getExpressionComplexity());
        int newMaxComplexity = Math.max(maxComplexity, metrics.getExpressionComplexity());
        
        // Update success/failure statistics
        long newSuccessful = successfulEvaluations + (metrics.hasException() ? 0 : 1);
        long newFailed = failedEvaluations + (metrics.hasException() ? 1 : 0);
        double newSuccessRate = (double) newSuccessful / newCount;
        
        // Update timestamps
        Instant newLastEvaluation = metrics.getStartTime().isAfter(lastEvaluation) ? 
                                   metrics.getStartTime() : lastEvaluation;
        
        return new PerformanceSnapshot(
            ruleName, newCount, newTotalTime, newAverageTime, newMinTime, newMaxTime,
            newTotalMemory, newAverageMemory, newMinMemory, newMaxMemory,
            newAverageComplexity, newMinComplexity, newMaxComplexity,
            newSuccessful, newFailed, newSuccessRate,
            firstEvaluation, newLastEvaluation, Instant.now()
        );
    }

    // Getters
    public String getRuleName() { return ruleName; }
    public long getEvaluationCount() { return evaluationCount; }
    public Duration getTotalEvaluationTime() { return totalEvaluationTime; }
    public Duration getAverageEvaluationTime() { return averageEvaluationTime; }
    public Duration getMinEvaluationTime() { return minEvaluationTime; }
    public Duration getMaxEvaluationTime() { return maxEvaluationTime; }
    public long getTotalMemoryUsed() { return totalMemoryUsed; }
    public long getAverageMemoryUsed() { return averageMemoryUsed; }
    public long getMinMemoryUsed() { return minMemoryUsed; }
    public long getMaxMemoryUsed() { return maxMemoryUsed; }
    public double getAverageComplexity() { return averageComplexity; }
    public int getMinComplexity() { return minComplexity; }
    public int getMaxComplexity() { return maxComplexity; }
    public long getSuccessfulEvaluations() { return successfulEvaluations; }
    public long getFailedEvaluations() { return failedEvaluations; }
    public double getSuccessRate() { return successRate; }
    public Instant getFirstEvaluation() { return firstEvaluation; }
    public Instant getLastEvaluation() { return lastEvaluation; }
    public Instant getLastUpdated() { return lastUpdated; }

    /**
     * Get the average evaluation time in milliseconds.
     *
     * @return The average evaluation time in milliseconds
     */
    public double getAverageEvaluationTimeMillis() {
        if (averageEvaluationTime == null) {
            return 0.0;
        }
        // Use nanoseconds for more precision, then convert to milliseconds
        return averageEvaluationTime.toNanos() / 1_000_000.0;
    }

    /**
     * Get the minimum evaluation time in milliseconds.
     *
     * @return The minimum evaluation time in milliseconds
     */
    public double getMinEvaluationTimeMillis() {
        if (minEvaluationTime == null) {
            return 0.0;
        }
        return minEvaluationTime.toNanos() / 1_000_000.0;
    }

    /**
     * Get the maximum evaluation time in milliseconds.
     *
     * @return The maximum evaluation time in milliseconds
     */
    public double getMaxEvaluationTimeMillis() {
        if (maxEvaluationTime == null) {
            return 0.0;
        }
        return maxEvaluationTime.toNanos() / 1_000_000.0;
    }

    /**
     * Get the failure rate (1 - success rate).
     * 
     * @return The failure rate as a percentage (0.0 to 1.0)
     */
    public double getFailureRate() {
        return 1.0 - successRate;
    }

    /**
     * Get a performance summary string.
     * 
     * @return A formatted performance summary
     */
    public String getPerformanceSummary() {
        return String.format(
            "Rule: %s | Evaluations: %d | Avg Time: %.2fms | Success Rate: %.1f%% | Avg Memory: %d bytes | Avg Complexity: %.1f",
            ruleName, evaluationCount, getAverageEvaluationTimeMillis(), 
            successRate * 100, averageMemoryUsed, averageComplexity
        );
    }

    /**
     * Get a detailed performance report.
     * 
     * @return A detailed formatted performance report
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Report for Rule: ").append(ruleName).append("\n");
        report.append("=====================================\n");
        report.append("Evaluation Count: ").append(evaluationCount).append("\n");
        report.append("Success Rate: ").append(String.format("%.1f%%", successRate * 100)).append("\n");
        report.append("Timing Statistics:\n");
        report.append("  Average: ").append(String.format("%.2fms", getAverageEvaluationTimeMillis())).append("\n");
        report.append("  Minimum: ").append(String.format("%.2fms", getMinEvaluationTimeMillis())).append("\n");
        report.append("  Maximum: ").append(String.format("%.2fms", getMaxEvaluationTimeMillis())).append("\n");
        report.append("Memory Statistics:\n");
        report.append("  Average: ").append(averageMemoryUsed).append(" bytes\n");
        report.append("  Minimum: ").append(minMemoryUsed).append(" bytes\n");
        report.append("  Maximum: ").append(maxMemoryUsed).append(" bytes\n");
        report.append("Complexity Statistics:\n");
        report.append("  Average: ").append(String.format("%.1f", averageComplexity)).append("\n");
        report.append("  Minimum: ").append(minComplexity).append("\n");
        report.append("  Maximum: ").append(maxComplexity).append("\n");
        report.append("First Evaluation: ").append(firstEvaluation).append("\n");
        report.append("Last Evaluation: ").append(lastEvaluation).append("\n");
        report.append("Last Updated: ").append(lastUpdated).append("\n");
        return report.toString();
    }

    @Override
    public String toString() {
        return "PerformanceSnapshot{" +
                "ruleName='" + ruleName + '\'' +
                ", evaluationCount=" + evaluationCount +
                ", averageEvaluationTime=" + averageEvaluationTime +
                ", successRate=" + successRate +
                ", averageMemoryUsed=" + averageMemoryUsed +
                ", averageComplexity=" + averageComplexity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerformanceSnapshot that = (PerformanceSnapshot) o;
        return evaluationCount == that.evaluationCount &&
                Objects.equals(ruleName, that.ruleName) &&
                Objects.equals(averageEvaluationTime, that.averageEvaluationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleName, evaluationCount, averageEvaluationTime);
    }
}
