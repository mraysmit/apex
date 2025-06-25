# Rule Engine Performance Monitoring Guide

## Overview

The SpEL Rules Engine now includes comprehensive performance monitoring capabilities that allow you to track, analyze, and optimize rule evaluation performance. This feature provides detailed metrics about rule execution times, memory usage, complexity analysis, and performance insights.

## Key Features

### 1. **Automatic Performance Tracking**
- Transparent monitoring of all rule evaluations
- Minimal overhead on rule execution
- Configurable monitoring options

### 2. **Comprehensive Metrics Collection**
- Evaluation timing (nanosecond precision)
- Memory usage tracking
- Expression complexity analysis
- Success/failure rates
- Cache hit tracking

### 3. **Performance Analysis and Insights**
- Automated performance bottleneck detection
- Trend analysis and recommendations
- Comparative performance analysis
- System-wide performance reporting

### 4. **Integration with Existing API**
- Backward compatible with existing code
- Optional performance metrics in RuleResult
- Non-intrusive monitoring

## Core Components

### RulePerformanceMetrics
Captures detailed performance data for individual rule evaluations:

```java
RulePerformanceMetrics metrics = result.getPerformanceMetrics();
System.out.println("Evaluation time: " + metrics.getEvaluationTimeMillis() + "ms");
System.out.println("Memory used: " + metrics.getMemoryUsedBytes() + " bytes");
System.out.println("Complexity score: " + metrics.getExpressionComplexity());
```

### RulePerformanceMonitor
Central service for tracking and managing performance metrics:

```java
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();
List<RulePerformanceMetrics> history = monitor.getRuleHistory("my-rule");
PerformanceSnapshot snapshot = monitor.getRuleSnapshot("my-rule");
```

### PerformanceSnapshot
Aggregated performance statistics for rules:

```java
PerformanceSnapshot snapshot = monitor.getRuleSnapshot("my-rule");
System.out.println("Average time: " + snapshot.getAverageEvaluationTimeMillis() + "ms");
System.out.println("Success rate: " + (snapshot.getSuccessRate() * 100) + "%");
System.out.println("Total evaluations: " + snapshot.getEvaluationCount());
```

### PerformanceAnalyzer
Utility for analyzing performance data and generating insights:

```java
Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
List<PerformanceInsight> insights = PerformanceAnalyzer.analyzePerformance(snapshots);
String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
```

## Getting Started

### Basic Usage

Performance monitoring is enabled by default. Simply use the rules engine as normal:

```java
RulesEngineConfiguration config = new RulesEngineConfiguration();
RulesEngine engine = new RulesEngine(config);

Rule rule = config.rule("performance-test")
    .condition("#value > 100")
    .message("Value exceeds threshold")
    .build();

Map<String, Object> facts = Map.of("value", 150);
RuleResult result = engine.executeRule(rule, facts);

// Access performance metrics
if (result.hasPerformanceMetrics()) {
    RulePerformanceMetrics metrics = result.getPerformanceMetrics();
    System.out.println("Execution time: " + metrics.getEvaluationTimeMillis() + "ms");
}
```

### Accessing Performance Data

```java
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

// Get global statistics
System.out.println("Total evaluations: " + monitor.getTotalEvaluations());
System.out.println("Average time: " + monitor.getAverageEvaluationTimeMillis() + "ms");

// Get rule-specific data
PerformanceSnapshot snapshot = monitor.getRuleSnapshot("my-rule");
if (snapshot != null) {
    System.out.println(snapshot.getPerformanceSummary());
}

// Get detailed history
List<RulePerformanceMetrics> history = monitor.getRuleHistory("my-rule");
for (RulePerformanceMetrics metrics : history) {
    System.out.println(metrics.getPerformanceSummary());
}
```

## Performance Analysis

### Generating Insights

```java
Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
List<PerformanceAnalyzer.PerformanceInsight> insights = 
    PerformanceAnalyzer.analyzePerformance(snapshots);

for (PerformanceAnalyzer.PerformanceInsight insight : insights) {
    System.out.println(insight.getSeverity() + ": " + insight.getMessage());
}
```

### Performance Report

```java
String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
System.out.println(report);
```

Sample output:
```
RULE ENGINE PERFORMANCE REPORT
==============================

SYSTEM SUMMARY:
Total Rules Monitored: 5
Total Evaluations: 1000
Average Evaluation Time: 2.34ms

SLOWEST RULES:
  complex-validation: 15.67ms (98.5% success)
  data-enrichment: 8.23ms (100.0% success)
  
PERFORMANCE INSIGHTS:
  [HIGH] SLOW_RULE: complex-validation - Rule is 6.7x slower than average
  [MEDIUM] HIGH_COMPLEXITY: complex-validation - Rule has high complexity score
  
RECOMMENDATIONS:
  • Consider optimizing slow rules by simplifying complex expressions
  • Reduce rule complexity by breaking complex rules into rule groups
```

## Configuration Options

### Monitor Configuration

```java
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

// Enable/disable monitoring
monitor.setEnabled(true);

// Configure history retention
monitor.setMaxHistorySize(1000);

// Configure tracking options
monitor.setTrackMemory(true);
monitor.setTrackComplexity(true);

// Clear accumulated metrics
monitor.clearMetrics();
```

### Custom Performance Monitoring

For advanced scenarios, you can manually track performance:

```java
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

// Start manual monitoring
RulePerformanceMetrics.Builder builder = monitor.startEvaluation("custom-rule");

// ... perform custom logic ...

// Complete monitoring
RulePerformanceMetrics metrics = monitor.completeEvaluation(
    builder, 
    "custom expression", 
    null // no exception
);
```

## Performance Metrics Reference

### Timing Metrics
- **Evaluation Time**: Total time spent evaluating the rule
- **Start/End Time**: Precise timestamps of evaluation
- **Nanosecond Precision**: High-resolution timing

### Memory Metrics
- **Memory Used**: Memory consumed during evaluation
- **Memory Before/After**: Memory state before and after evaluation
- **Memory Delta**: Net memory change

### Complexity Metrics
- **Expression Complexity**: Calculated complexity score based on:
  - Number of operators
  - Method calls
  - Variable references
  - Nested expressions

### Success Metrics
- **Success Rate**: Percentage of successful evaluations
- **Exception Tracking**: Details of evaluation failures
- **Error Classification**: Types of errors encountered

## Best Practices

### 1. **Monitor Regularly**
```java
// Set up periodic reporting
Timer timer = new Timer();
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        String report = PerformanceAnalyzer.generatePerformanceReport(
            monitor.getAllSnapshots()
        );
        logger.info("Performance Report:\n" + report);
    }
}, 0, 60000); // Every minute
```

### 2. **Optimize Based on Insights**
```java
List<PerformanceAnalyzer.PerformanceInsight> insights = 
    PerformanceAnalyzer.analyzePerformance(snapshots);

for (PerformanceAnalyzer.PerformanceInsight insight : insights) {
    if ("SLOW_RULE".equals(insight.getType())) {
        logger.warn("Slow rule detected: " + insight.getRuleName());
        // Consider rule optimization
    }
}
```

### 3. **Set Performance Thresholds**
```java
PerformanceSnapshot snapshot = monitor.getRuleSnapshot("critical-rule");
if (snapshot != null && snapshot.getAverageEvaluationTimeMillis() > 10.0) {
    logger.warn("Critical rule exceeding performance threshold");
    // Take corrective action
}
```

### 4. **Use in Testing**
```java
@Test
void testRulePerformance() {
    // Execute rule
    RuleResult result = engine.executeRule(rule, facts);
    
    // Assert performance requirements
    assertTrue(result.hasPerformanceMetrics());
    assertTrue(result.getPerformanceMetrics().getEvaluationTimeMillis() < 5.0);
}
```

## Integration with Monitoring Systems

### Exporting Metrics

```java
// Export to monitoring system
Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
for (PerformanceSnapshot snapshot : snapshots.values()) {
    // Send to monitoring system
    metricsCollector.gauge("rule.evaluation.time", 
        snapshot.getAverageEvaluationTimeMillis(),
        "rule", snapshot.getRuleName());
    
    metricsCollector.gauge("rule.success.rate", 
        snapshot.getSuccessRate(),
        "rule", snapshot.getRuleName());
}
```

### Custom Metrics Collection

```java
// Implement custom metrics collection
public class CustomMetricsCollector {
    public void collectMetrics(RulePerformanceMetrics metrics) {
        // Send to external monitoring system
        sendToInfluxDB(metrics);
        sendToPrometheus(metrics);
        sendToCloudWatch(metrics);
    }
}
```

## Troubleshooting

### Common Issues

1. **High Memory Usage**: Check for memory leaks in rule expressions
2. **Slow Performance**: Use complexity analysis to identify bottlenecks
3. **Low Success Rates**: Review error patterns and implement error handling

### Performance Impact

The monitoring system is designed to have minimal impact:
- Overhead: < 1% of evaluation time
- Memory: Configurable history retention
- Thread-safe: Concurrent evaluation support

### Disabling Monitoring

If needed, monitoring can be disabled:
```java
monitor.setEnabled(false);
```

## Examples

See the `PerformanceMonitoringDemo` class for comprehensive examples of all features.

## Future Enhancements

- Integration with APM tools
- Real-time performance dashboards
- Automated performance regression detection
- Machine learning-based optimization suggestions
