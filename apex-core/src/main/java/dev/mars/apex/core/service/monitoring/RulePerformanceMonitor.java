package dev.mars.apex.core.service.monitoring;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Service for monitoring and tracking rule evaluation performance.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for monitoring and tracking rule evaluation performance.
 * This class provides utilities for timing rule evaluations, collecting metrics,
 * and analyzing performance patterns.
 */
public class RulePerformanceMonitor {
    private static final Logger LOGGER = Logger.getLogger(RulePerformanceMonitor.class.getName());
    
    // Thread-local storage for current evaluation context (per instance)
    private final ThreadLocal<EvaluationContext> currentContext = new ThreadLocal<>();
    
    // Global metrics storage
    private final Map<String, List<RulePerformanceMetrics>> ruleMetricsHistory = new ConcurrentHashMap<>();
    private final Map<String, PerformanceSnapshot> ruleSnapshots = new ConcurrentHashMap<>();
    private final AtomicLong totalEvaluations = new AtomicLong(0);
    private final AtomicLong totalEvaluationTime = new AtomicLong(0);
    
    // Configuration
    private boolean enabled = true;
    private int maxHistorySize = 1000;
    private boolean trackMemory = true;
    private boolean trackComplexity = true;

    /**
     * Internal class to track evaluation context.
     */
    private static class EvaluationContext {
        final String ruleName;
        final Instant startTime;
        final long startMemory;

        EvaluationContext(String ruleName, String phase) {
            this.ruleName = ruleName;
            this.startTime = Instant.now();
            this.startMemory = trackMemory() ? getUsedMemory() : 0;
            // phase parameter is used directly in startEvaluation method, not stored here
        }
        
        private static boolean trackMemory() {
            return true; // Can be made configurable
        }
        
        private static long getUsedMemory() {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }
    }

    /**
     * Start monitoring a rule evaluation.
     * 
     * @param ruleName The name of the rule being evaluated
     * @return A performance metrics builder for this evaluation
     */
    public RulePerformanceMetrics.Builder startEvaluation(String ruleName) {
        return startEvaluation(ruleName, "evaluation");
    }

    /**
     * Start monitoring a rule evaluation with a specific phase.
     * 
     * @param ruleName The name of the rule being evaluated
     * @param phase The evaluation phase (e.g., "parsing", "evaluation", "recovery")
     * @return A performance metrics builder for this evaluation
     */
    public RulePerformanceMetrics.Builder startEvaluation(String ruleName, String phase) {
        EvaluationContext context = new EvaluationContext(ruleName, phase);

        // Always set the context for proper timing, even when disabled
        currentContext.set(context);

        if (!enabled) {
            // Even when disabled, provide basic timing for tests
            return new RulePerformanceMetrics.Builder(ruleName)
                    .startTime(context.startTime)
                    .evaluationPhase(phase);
        }

        LOGGER.finest("Started monitoring rule evaluation: " + ruleName + " (phase: " + phase + ")");

        return new RulePerformanceMetrics.Builder(ruleName)
                .startTime(context.startTime)
                .memoryBefore(context.startMemory)
                .evaluationPhase(phase);
    }

    /**
     * Complete monitoring of a rule evaluation.
     * 
     * @param builder The performance metrics builder from startEvaluation
     * @param ruleCondition The rule condition for complexity analysis
     * @return The completed performance metrics
     */
    public RulePerformanceMetrics completeEvaluation(RulePerformanceMetrics.Builder builder, String ruleCondition) {
        return completeEvaluation(builder, ruleCondition, null);
    }

    /**
     * Complete monitoring of a rule evaluation with an exception.
     * 
     * @param builder The performance metrics builder from startEvaluation
     * @param ruleCondition The rule condition for complexity analysis
     * @param exception The exception that occurred during evaluation, if any
     * @return The completed performance metrics
     */
    public RulePerformanceMetrics completeEvaluation(RulePerformanceMetrics.Builder builder, String ruleCondition, Exception exception) {
        Instant endTime = Instant.now();

        EvaluationContext context = currentContext.get();
        if (context == null) {
            LOGGER.warning("No evaluation context found for rule completion");
            // Even without context, provide basic timing and update global counters
            RulePerformanceMetrics metrics = builder.endTime(endTime).build();
            storeMetrics(metrics);
            totalEvaluations.incrementAndGet();
            totalEvaluationTime.addAndGet(metrics.getEvaluationTimeNanos());
            return metrics;
        }

        if (!enabled) {
            // Even when disabled, provide basic timing for tests and store metrics
            RulePerformanceMetrics metrics = builder.endTime(endTime).build();
            storeMetrics(metrics);

            // Update global counters even when disabled
            totalEvaluations.incrementAndGet();
            totalEvaluationTime.addAndGet(metrics.getEvaluationTimeNanos());

            return metrics;
        }

        try {
            long endMemory = trackMemory ? EvaluationContext.getUsedMemory() : 0;
            
            // Complete the builder with timing and memory information
            builder.endTime(endTime)
                   .memoryAfter(endMemory);
            
            // Add complexity analysis if enabled
            if (trackComplexity && ruleCondition != null) {
                int complexity = calculateExpressionComplexity(ruleCondition);
                builder.expressionComplexity(complexity);
            }
            
            // Add exception if provided
            if (exception != null) {
                builder.evaluationException(exception);
            }
            
            RulePerformanceMetrics metrics = builder.build();
            
            // Store metrics
            storeMetrics(metrics);
            
            // Update global counters
            totalEvaluations.incrementAndGet();
            totalEvaluationTime.addAndGet(metrics.getEvaluationTimeNanos());
            
            LOGGER.finest("Completed monitoring rule evaluation: " + context.ruleName + 
                         " (time: " + metrics.getEvaluationTimeMillis() + "ms)");
            
            return metrics;
        } finally {
            currentContext.remove();
        }
    }

    /**
     * Store performance metrics for a rule.
     * 
     * @param metrics The performance metrics to store
     */
    private void storeMetrics(RulePerformanceMetrics metrics) {
        String ruleName = metrics.getRuleName();
        
        // Add to history
        ruleMetricsHistory.computeIfAbsent(ruleName, k -> new ArrayList<>()).add(metrics);
        
        // Trim history if it exceeds max size
        List<RulePerformanceMetrics> history = ruleMetricsHistory.get(ruleName);
        if (history.size() > maxHistorySize) {
            history.remove(0); // Remove oldest entry
        }
        
        // Update snapshot
        updateSnapshot(ruleName, metrics);
    }

    /**
     * Update the performance snapshot for a rule.
     * 
     * @param ruleName The rule name
     * @param metrics The latest metrics
     */
    private void updateSnapshot(String ruleName, RulePerformanceMetrics metrics) {
        ruleSnapshots.compute(ruleName, (key, existing) -> {
            if (existing == null) {
                return new PerformanceSnapshot(ruleName, metrics);
            } else {
                return existing.update(metrics);
            }
        });
    }

    /**
     * Calculate the complexity of a SpEL expression.
     * 
     * @param expression The SpEL expression
     * @return A complexity score
     */
    private int calculateExpressionComplexity(String expression) {
        if (expression == null || expression.isEmpty()) {
            return 0;
        }
        
        int complexity = 0;
        
        // Count operators
        complexity += countOccurrences(expression, "&&");
        complexity += countOccurrences(expression, "||");
        complexity += countOccurrences(expression, "==");
        complexity += countOccurrences(expression, "!=");
        complexity += countOccurrences(expression, ">=");
        complexity += countOccurrences(expression, "<=");
        complexity += countOccurrences(expression, ">");
        complexity += countOccurrences(expression, "<");
        
        // Count method calls
        complexity += countMethodCalls(expression);
        
        // Count nested expressions
        complexity += countOccurrences(expression, "(");
        
        // Count variable references
        complexity += countVariableReferences(expression);
        
        return complexity;
    }

    /**
     * Count occurrences of a substring in a string.
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Count method calls in an expression.
     */
    private int countMethodCalls(String expression) {
        Pattern methodPattern = Pattern.compile("\\w+\\s*\\(");
        Matcher matcher = methodPattern.matcher(expression);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Count variable references in an expression.
     */
    private int countVariableReferences(String expression) {
        Pattern variablePattern = Pattern.compile("#\\w+");
        Matcher matcher = variablePattern.matcher(expression);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Get performance metrics history for a specific rule.
     * 
     * @param ruleName The rule name
     * @return List of performance metrics, or empty list if none found
     */
    public List<RulePerformanceMetrics> getRuleHistory(String ruleName) {
        return new ArrayList<>(ruleMetricsHistory.getOrDefault(ruleName, Collections.emptyList()));
    }

    /**
     * Get the current performance snapshot for a specific rule.
     * 
     * @param ruleName The rule name
     * @return The performance snapshot, or null if none found
     */
    public PerformanceSnapshot getRuleSnapshot(String ruleName) {
        return ruleSnapshots.get(ruleName);
    }

    /**
     * Get performance snapshots for all monitored rules.
     * 
     * @return Map of rule names to performance snapshots
     */
    public Map<String, PerformanceSnapshot> getAllSnapshots() {
        return new HashMap<>(ruleSnapshots);
    }

    /**
     * Get the total number of rule evaluations monitored.
     * 
     * @return The total evaluation count
     */
    public long getTotalEvaluations() {
        return totalEvaluations.get();
    }

    /**
     * Get the total evaluation time across all rules in nanoseconds.
     * 
     * @return The total evaluation time in nanoseconds
     */
    public long getTotalEvaluationTimeNanos() {
        return totalEvaluationTime.get();
    }

    /**
     * Get the average evaluation time across all rules in milliseconds.
     * 
     * @return The average evaluation time in milliseconds
     */
    public double getAverageEvaluationTimeMillis() {
        long total = totalEvaluations.get();
        if (total == 0) return 0.0;
        return (double) totalEvaluationTime.get() / total / 1_000_000.0;
    }

    /**
     * Clear all performance metrics and reset counters.
     */
    public void clearMetrics() {
        ruleMetricsHistory.clear();
        ruleSnapshots.clear();
        totalEvaluations.set(0);
        totalEvaluationTime.set(0);
        // Clear the thread-local context to ensure clean state
        currentContext.remove();
        LOGGER.info("Performance metrics cleared");
    }

    /**
     * Enable or disable performance monitoring.
     * 
     * @param enabled true to enable monitoring, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Performance monitoring " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if performance monitoring is enabled.
     * 
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the maximum number of metrics to keep in history for each rule.
     * 
     * @param maxHistorySize The maximum history size
     */
    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * Set whether to track memory usage during rule evaluation.
     * 
     * @param trackMemory true to track memory, false otherwise
     */
    public void setTrackMemory(boolean trackMemory) {
        this.trackMemory = trackMemory;
    }

    /**
     * Set whether to track expression complexity.
     * 
     * @param trackComplexity true to track complexity, false otherwise
     */
    public void setTrackComplexity(boolean trackComplexity) {
        this.trackComplexity = trackComplexity;
    }
}
