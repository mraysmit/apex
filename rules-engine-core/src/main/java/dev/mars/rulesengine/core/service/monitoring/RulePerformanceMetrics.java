package dev.mars.rulesengine.core.service.monitoring;

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
 * Represents performance metrics for a single rule evaluation.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Represents performance metrics for a single rule evaluation.
 * This class captures timing, memory usage, and other performance indicators
 * to help monitor and optimize rule engine performance.
 */
public class RulePerformanceMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String ruleName;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration evaluationTime;
    private final long memoryUsedBytes;
    private final long memoryBeforeBytes;
    private final long memoryAfterBytes;
    private final int expressionComplexity;
    private final boolean cacheHit;
    private final String evaluationPhase;
    private final Exception evaluationException;

    /**
     * Private constructor for builder pattern.
     */
    private RulePerformanceMetrics(Builder builder) {
        this.ruleName = builder.ruleName;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.evaluationTime = builder.evaluationTime;
        this.memoryUsedBytes = builder.memoryUsedBytes;
        this.memoryBeforeBytes = builder.memoryBeforeBytes;
        this.memoryAfterBytes = builder.memoryAfterBytes;
        this.expressionComplexity = builder.expressionComplexity;
        this.cacheHit = builder.cacheHit;
        this.evaluationPhase = builder.evaluationPhase;
        this.evaluationException = builder.evaluationException;
    }

    /**
     * Get the name of the rule that was evaluated.
     * 
     * @return The rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Get the start time of the rule evaluation.
     * 
     * @return The start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Get the end time of the rule evaluation.
     * 
     * @return The end time
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Get the total evaluation time.
     * 
     * @return The evaluation duration
     */
    public Duration getEvaluationTime() {
        return evaluationTime;
    }

    /**
     * Get the evaluation time in milliseconds.
     * 
     * @return The evaluation time in milliseconds
     */
    public long getEvaluationTimeMillis() {
        return evaluationTime != null ? evaluationTime.toMillis() : 0;
    }

    /**
     * Get the evaluation time in nanoseconds.
     * 
     * @return The evaluation time in nanoseconds
     */
    public long getEvaluationTimeNanos() {
        return evaluationTime != null ? evaluationTime.toNanos() : 0;
    }

    /**
     * Get the memory used during evaluation in bytes.
     * 
     * @return The memory used in bytes
     */
    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    /**
     * Get the memory usage before evaluation in bytes.
     * 
     * @return The memory before evaluation in bytes
     */
    public long getMemoryBeforeBytes() {
        return memoryBeforeBytes;
    }

    /**
     * Get the memory usage after evaluation in bytes.
     * 
     * @return The memory after evaluation in bytes
     */
    public long getMemoryAfterBytes() {
        return memoryAfterBytes;
    }

    /**
     * Get the expression complexity score.
     * 
     * @return The expression complexity score
     */
    public int getExpressionComplexity() {
        return expressionComplexity;
    }

    /**
     * Check if this evaluation was a cache hit.
     * 
     * @return true if cache hit, false otherwise
     */
    public boolean isCacheHit() {
        return cacheHit;
    }

    /**
     * Get the evaluation phase where metrics were captured.
     * 
     * @return The evaluation phase
     */
    public String getEvaluationPhase() {
        return evaluationPhase;
    }

    /**
     * Get the exception that occurred during evaluation, if any.
     * 
     * @return The evaluation exception, or null if no exception occurred
     */
    public Exception getEvaluationException() {
        return evaluationException;
    }

    /**
     * Check if an exception occurred during evaluation.
     * 
     * @return true if an exception occurred, false otherwise
     */
    public boolean hasException() {
        return evaluationException != null;
    }

    /**
     * Get a performance summary string.
     * 
     * @return A formatted performance summary
     */
    public String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Rule: ").append(ruleName);
        summary.append(", Time: ").append(getEvaluationTimeMillis()).append("ms");
        summary.append(", Memory: ").append(getMemoryUsedBytes()).append(" bytes");
        summary.append(", Complexity: ").append(expressionComplexity);
        if (cacheHit) {
            summary.append(", Cache: HIT");
        }
        if (hasException()) {
            summary.append(", Error: ").append(evaluationException.getMessage());
        }
        return summary.toString();
    }

    @Override
    public String toString() {
        return "RulePerformanceMetrics{" +
                "ruleName='" + ruleName + '\'' +
                ", evaluationTime=" + evaluationTime +
                ", memoryUsedBytes=" + memoryUsedBytes +
                ", expressionComplexity=" + expressionComplexity +
                ", cacheHit=" + cacheHit +
                ", evaluationPhase='" + evaluationPhase + '\'' +
                ", hasException=" + hasException() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RulePerformanceMetrics that = (RulePerformanceMetrics) o;
        return memoryUsedBytes == that.memoryUsedBytes &&
                memoryBeforeBytes == that.memoryBeforeBytes &&
                memoryAfterBytes == that.memoryAfterBytes &&
                expressionComplexity == that.expressionComplexity &&
                cacheHit == that.cacheHit &&
                Objects.equals(ruleName, that.ruleName) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(evaluationTime, that.evaluationTime) &&
                Objects.equals(evaluationPhase, that.evaluationPhase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleName, startTime, endTime, evaluationTime, memoryUsedBytes,
                memoryBeforeBytes, memoryAfterBytes, expressionComplexity, cacheHit, evaluationPhase);
    }

    /**
     * Builder class for creating RulePerformanceMetrics instances.
     */
    public static class Builder {
        private String ruleName;
        private Instant startTime;
        private Instant endTime;
        private Duration evaluationTime;
        private long memoryUsedBytes;
        private long memoryBeforeBytes;
        private long memoryAfterBytes;
        private int expressionComplexity;
        private boolean cacheHit;
        private String evaluationPhase = "evaluation";
        private Exception evaluationException;

        public Builder(String ruleName) {
            this.ruleName = ruleName;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder evaluationTime(Duration evaluationTime) {
            this.evaluationTime = evaluationTime;
            return this;
        }

        public Builder memoryUsed(long memoryUsedBytes) {
            this.memoryUsedBytes = memoryUsedBytes;
            return this;
        }

        public Builder memoryBefore(long memoryBeforeBytes) {
            this.memoryBeforeBytes = memoryBeforeBytes;
            return this;
        }

        public Builder memoryAfter(long memoryAfterBytes) {
            this.memoryAfterBytes = memoryAfterBytes;
            return this;
        }

        public Builder expressionComplexity(int expressionComplexity) {
            this.expressionComplexity = expressionComplexity;
            return this;
        }

        public Builder cacheHit(boolean cacheHit) {
            this.cacheHit = cacheHit;
            return this;
        }

        public Builder evaluationPhase(String evaluationPhase) {
            this.evaluationPhase = evaluationPhase;
            return this;
        }

        public Builder evaluationException(Exception evaluationException) {
            this.evaluationException = evaluationException;
            return this;
        }

        public RulePerformanceMetrics build() {
            // Calculate evaluation time if not provided but start and end times are available
            if (evaluationTime == null && startTime != null && endTime != null) {
                evaluationTime = Duration.between(startTime, endTime);
            }
            
            // Calculate memory used if not provided but before and after are available
            if (memoryUsedBytes == 0 && memoryBeforeBytes > 0 && memoryAfterBytes > 0) {
                memoryUsedBytes = memoryAfterBytes - memoryBeforeBytes;
            }
            
            return new RulePerformanceMetrics(this);
        }
    }
}
