package dev.mars.apex.core.service.monitoring;

import java.util.*;
import java.util.stream.Collectors;

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
 * Utility class for analyzing rule performance metrics and providing insights.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Utility class for analyzing rule performance metrics and providing insights.
 * This class provides methods to identify performance bottlenecks, trends,
 * and optimization opportunities.
 */
public class PerformanceAnalyzer {

    /**
     * Represents a performance insight or recommendation.
     */
    public static class PerformanceInsight {
        private final String type;
        private final String ruleName;
        private final String message;
        private final String severity;
        private final Map<String, Object> details;

        public PerformanceInsight(String type, String ruleName, String message, String severity) {
            this.type = type;
            this.ruleName = ruleName;
            this.message = message;
            this.severity = severity;
            this.details = new HashMap<>();
        }

        public PerformanceInsight withDetail(String key, Object value) {
            details.put(key, value);
            return this;
        }

        // Getters
        public String getType() { return type; }
        public String getRuleName() { return ruleName; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
        public Map<String, Object> getDetails() { return new HashMap<>(details); }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s - %s", severity, type, ruleName, message);
        }
    }

    /**
     * Analyze performance snapshots and generate insights.
     * 
     * @param snapshots Map of rule names to performance snapshots
     * @return List of performance insights
     */
    public static List<PerformanceInsight> analyzePerformance(Map<String, PerformanceSnapshot> snapshots) {
        List<PerformanceInsight> insights = new ArrayList<>();
        
        if (snapshots.isEmpty()) {
            return insights;
        }

        // Calculate overall statistics
        double avgEvaluationTime = snapshots.values().stream()
                .mapToDouble(PerformanceSnapshot::getAverageEvaluationTimeMillis)
                .average()
                .orElse(0.0);

        double avgMemoryUsage = snapshots.values().stream()
                .mapToLong(PerformanceSnapshot::getAverageMemoryUsed)
                .average()
                .orElse(0.0);

        double avgComplexity = snapshots.values().stream()
                .mapToDouble(PerformanceSnapshot::getAverageComplexity)
                .average()
                .orElse(0.0);

        // Analyze each rule
        for (PerformanceSnapshot snapshot : snapshots.values()) {
            insights.addAll(analyzeRule(snapshot, avgEvaluationTime, avgMemoryUsage, avgComplexity));
        }

        // Add system-wide insights
        insights.addAll(analyzeSystemWide(snapshots));

        return insights;
    }

    /**
     * Analyze a single rule's performance.
     */
    private static List<PerformanceInsight> analyzeRule(PerformanceSnapshot snapshot, 
                                                       double avgEvaluationTime, 
                                                       double avgMemoryUsage, 
                                                       double avgComplexity) {
        List<PerformanceInsight> insights = new ArrayList<>();
        String ruleName = snapshot.getRuleName();

        // Check for slow rules
        if (snapshot.getAverageEvaluationTimeMillis() > avgEvaluationTime * 2) {
            insights.add(new PerformanceInsight("SLOW_RULE", ruleName,
                    String.format("Rule is %.1fx slower than average (%.2fms vs %.2fms)",
                            snapshot.getAverageEvaluationTimeMillis() / avgEvaluationTime,
                            snapshot.getAverageEvaluationTimeMillis(), avgEvaluationTime),
                    "HIGH")
                    .withDetail("averageTime", snapshot.getAverageEvaluationTimeMillis())
                    .withDetail("systemAverage", avgEvaluationTime));
        }

        // Check for high memory usage
        if (snapshot.getAverageMemoryUsed() > avgMemoryUsage * 2) {
            insights.add(new PerformanceInsight("HIGH_MEMORY", ruleName,
                    String.format("Rule uses %.1fx more memory than average (%d vs %.0f bytes)",
                            snapshot.getAverageMemoryUsed() / avgMemoryUsage,
                            snapshot.getAverageMemoryUsed(), avgMemoryUsage),
                    "MEDIUM")
                    .withDetail("averageMemory", snapshot.getAverageMemoryUsed())
                    .withDetail("systemAverage", avgMemoryUsage));
        }

        // Check for high complexity
        if (snapshot.getAverageComplexity() > avgComplexity * 1.5) {
            insights.add(new PerformanceInsight("HIGH_COMPLEXITY", ruleName,
                    String.format("Rule has high complexity score (%.1f vs %.1f average)",
                            snapshot.getAverageComplexity(), avgComplexity),
                    "MEDIUM")
                    .withDetail("complexity", snapshot.getAverageComplexity())
                    .withDetail("systemAverage", avgComplexity));
        }

        // Check for low success rate
        if (snapshot.getSuccessRate() < 0.95) {
            insights.add(new PerformanceInsight("LOW_SUCCESS_RATE", ruleName,
                    String.format("Rule has low success rate (%.1f%%)",
                            snapshot.getSuccessRate() * 100),
                    "HIGH")
                    .withDetail("successRate", snapshot.getSuccessRate())
                    .withDetail("failedEvaluations", snapshot.getFailedEvaluations()));
        }

        // Check for high variance in execution time
        double variance = calculateTimeVariance(snapshot);
        if (variance > snapshot.getAverageEvaluationTimeMillis()) {
            insights.add(new PerformanceInsight("HIGH_VARIANCE", ruleName,
                    String.format("Rule has inconsistent execution times (variance: %.2fms)",
                            variance),
                    "MEDIUM")
                    .withDetail("variance", variance)
                    .withDetail("minTime", snapshot.getMinEvaluationTimeMillis())
                    .withDetail("maxTime", snapshot.getMaxEvaluationTimeMillis()));
        }

        return insights;
    }

    /**
     * Analyze system-wide performance patterns.
     */
    private static List<PerformanceInsight> analyzeSystemWide(Map<String, PerformanceSnapshot> snapshots) {
        List<PerformanceInsight> insights = new ArrayList<>();

        // Find the slowest rules
        List<PerformanceSnapshot> slowestRules = snapshots.values().stream()
                .sorted((a, b) -> Double.compare(b.getAverageEvaluationTimeMillis(), a.getAverageEvaluationTimeMillis()))
                .limit(3)
                .collect(Collectors.toList());

        if (!slowestRules.isEmpty()) {
            PerformanceSnapshot slowest = slowestRules.get(0);
            insights.add(new PerformanceInsight("SLOWEST_RULE", slowest.getRuleName(),
                    String.format("Slowest rule in system (%.2fms average)",
                            slowest.getAverageEvaluationTimeMillis()),
                    "INFO")
                    .withDetail("averageTime", slowest.getAverageEvaluationTimeMillis())
                    .withDetail("evaluationCount", slowest.getEvaluationCount()));
        }

        // Find rules with most failures
        List<PerformanceSnapshot> mostFailures = snapshots.values().stream()
                .filter(s -> s.getFailedEvaluations() > 0)
                .sorted((a, b) -> Long.compare(b.getFailedEvaluations(), a.getFailedEvaluations()))
                .limit(3)
                .collect(Collectors.toList());

        if (!mostFailures.isEmpty()) {
            PerformanceSnapshot mostFailed = mostFailures.get(0);
            insights.add(new PerformanceInsight("MOST_FAILURES", mostFailed.getRuleName(),
                    String.format("Rule with most failures (%d failures, %.1f%% success rate)",
                            mostFailed.getFailedEvaluations(), mostFailed.getSuccessRate() * 100),
                    "WARNING")
                    .withDetail("failedEvaluations", mostFailed.getFailedEvaluations())
                    .withDetail("successRate", mostFailed.getSuccessRate()));
        }

        // Calculate total system performance
        long totalEvaluations = snapshots.values().stream()
                .mapToLong(PerformanceSnapshot::getEvaluationCount)
                .sum();

        double totalTime = snapshots.values().stream()
                .mapToDouble(s -> s.getTotalEvaluationTime().toMillis())
                .sum();

        if (totalEvaluations > 0) {
            insights.add(new PerformanceInsight("SYSTEM_SUMMARY", "SYSTEM",
                    String.format("System processed %d evaluations in %.2fms total (%.2fms average)",
                            totalEvaluations, totalTime, totalTime / totalEvaluations),
                    "INFO")
                    .withDetail("totalEvaluations", totalEvaluations)
                    .withDetail("totalTime", totalTime)
                    .withDetail("averageTime", totalTime / totalEvaluations));
        }

        return insights;
    }

    /**
     * Calculate time variance for a rule (simplified calculation).
     */
    private static double calculateTimeVariance(PerformanceSnapshot snapshot) {
        // Simplified variance calculation using min/max
        double range = snapshot.getMaxEvaluationTimeMillis() - snapshot.getMinEvaluationTimeMillis();
        return range / 4.0; // Rough approximation of standard deviation
    }

    /**
     * Generate performance recommendations based on insights.
     * 
     * @param insights List of performance insights
     * @return List of actionable recommendations
     */
    public static List<String> generateRecommendations(List<PerformanceInsight> insights) {
        List<String> recommendations = new ArrayList<>();
        
        Map<String, List<PerformanceInsight>> insightsByType = insights.stream()
                .collect(Collectors.groupingBy(PerformanceInsight::getType));

        // Recommendations for slow rules
        if (insightsByType.containsKey("SLOW_RULE")) {
            recommendations.add("Consider optimizing slow rules by:");
            recommendations.add("  - Simplifying complex expressions");
            recommendations.add("  - Reducing the number of method calls");
            recommendations.add("  - Implementing caching for expensive operations");
            recommendations.add("  - Breaking complex rules into simpler sub-rules");
        }

        // Recommendations for high memory usage
        if (insightsByType.containsKey("HIGH_MEMORY")) {
            recommendations.add("Reduce memory usage by:");
            recommendations.add("  - Avoiding large object creation in expressions");
            recommendations.add("  - Using more efficient data structures");
            recommendations.add("  - Implementing object pooling for frequently used objects");
        }

        // Recommendations for low success rates
        if (insightsByType.containsKey("LOW_SUCCESS_RATE")) {
            recommendations.add("Improve rule reliability by:");
            recommendations.add("  - Adding null checks and validation");
            recommendations.add("  - Implementing proper error handling");
            recommendations.add("  - Testing rules with edge cases");
            recommendations.add("  - Using the error recovery service");
        }

        // Recommendations for high complexity
        if (insightsByType.containsKey("HIGH_COMPLEXITY")) {
            recommendations.add("Reduce rule complexity by:");
            recommendations.add("  - Breaking complex rules into rule groups");
            recommendations.add("  - Using helper methods to encapsulate logic");
            recommendations.add("  - Simplifying boolean expressions");
            recommendations.add("  - Avoiding deeply nested conditions");
        }

        // Recommendations for high variance
        if (insightsByType.containsKey("HIGH_VARIANCE")) {
            recommendations.add("Improve execution consistency by:");
            recommendations.add("  - Identifying and eliminating performance bottlenecks");
            recommendations.add("  - Implementing consistent data access patterns");
            recommendations.add("  - Using connection pooling for external resources");
            recommendations.add("  - Monitoring system resource usage");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("No specific performance issues detected.");
            recommendations.add("Continue monitoring for performance trends.");
        }

        return recommendations;
    }

    /**
     * Generate a performance report from snapshots.
     * 
     * @param snapshots Map of rule names to performance snapshots
     * @return A formatted performance report
     */
    public static String generatePerformanceReport(Map<String, PerformanceSnapshot> snapshots) {
        StringBuilder report = new StringBuilder();
        
        report.append("RULE ENGINE PERFORMANCE REPORT\n");
        report.append("==============================\n\n");

        if (snapshots.isEmpty()) {
            report.append("No performance data available.\n");
            return report.toString();
        }

        // System summary
        long totalEvaluations = snapshots.values().stream()
                .mapToLong(PerformanceSnapshot::getEvaluationCount)
                .sum();
        
        double avgTime = snapshots.values().stream()
                .mapToDouble(PerformanceSnapshot::getAverageEvaluationTimeMillis)
                .average()
                .orElse(0.0);

        report.append("SYSTEM SUMMARY:\n");
        report.append(String.format("Total Rules Monitored: %d\n", snapshots.size()));
        report.append(String.format("Total Evaluations: %d\n", totalEvaluations));
        report.append(String.format("Average Evaluation Time: %.2fms\n\n", avgTime));

        // Top performers and bottlenecks
        List<PerformanceSnapshot> sortedByTime = snapshots.values().stream()
                .sorted((a, b) -> Double.compare(b.getAverageEvaluationTimeMillis(), a.getAverageEvaluationTimeMillis()))
                .collect(Collectors.toList());

        report.append("SLOWEST RULES:\n");
        sortedByTime.stream().limit(5).forEach(snapshot -> 
            report.append(String.format("  %s: %.2fms (%.1f%% success)\n", 
                snapshot.getRuleName(), 
                snapshot.getAverageEvaluationTimeMillis(),
                snapshot.getSuccessRate() * 100)));

        report.append("\nFASTEST RULES:\n");
        sortedByTime.stream()
                .skip(Math.max(0, sortedByTime.size() - 5))
                .forEach(snapshot -> 
                    report.append(String.format("  %s: %.2fms (%.1f%% success)\n", 
                        snapshot.getRuleName(), 
                        snapshot.getAverageEvaluationTimeMillis(),
                        snapshot.getSuccessRate() * 100)));

        // Performance insights
        List<PerformanceInsight> insights = analyzePerformance(snapshots);
        if (!insights.isEmpty()) {
            report.append("\nPERFORMANCE INSIGHTS:\n");
            insights.stream()
                    .filter(insight -> !"INFO".equals(insight.getSeverity()))
                    .forEach(insight -> report.append("  ").append(insight.toString()).append("\n"));
        }

        // Recommendations
        List<String> recommendations = generateRecommendations(insights);
        if (!recommendations.isEmpty()) {
            report.append("\nRECOMMENDATIONS:\n");
            recommendations.forEach(rec -> report.append("  ").append(rec).append("\n"));
        }

        return report.toString();
    }
}
