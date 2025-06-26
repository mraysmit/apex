# Performance Monitoring - Quick Reference

## Overview

The SpEL Rules Engine now includes comprehensive performance monitoring that automatically tracks rule evaluation performance with minimal overhead. This quick reference provides the essential information for getting started.

## Key Features

✅ **Automatic Monitoring** - No code changes required  
✅ **Minimal Overhead** - < 1% performance impact  
✅ **Thread-Safe** - Full concurrent evaluation support  
✅ **Configurable** - Enable/disable features as needed  
✅ **Actionable Insights** - Automated bottleneck detection  

## Basic Usage

### 1. Automatic Performance Tracking

```java
// Performance monitoring works automatically
RulesEngine engine = new RulesEngine(configuration);
RuleResult result = engine.executeRule(rule, facts);

// Access performance metrics (optional)
if (result.hasPerformanceMetrics()) {
    RulePerformanceMetrics metrics = result.getPerformanceMetrics();
    System.out.println("Time: " + metrics.getEvaluationTimeMillis() + "ms");
    System.out.println("Memory: " + metrics.getMemoryUsedBytes() + " bytes");
    System.out.println("Complexity: " + metrics.getExpressionComplexity());
}
```

### 2. Performance Monitoring Service

```java
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

// Global statistics
System.out.println("Total evaluations: " + monitor.getTotalEvaluations());
System.out.println("Average time: " + monitor.getAverageEvaluationTimeMillis() + "ms");

// Rule-specific performance
PerformanceSnapshot snapshot = monitor.getRuleSnapshot("my-rule");
if (snapshot != null) {
    System.out.println("Rule: " + snapshot.getRuleName());
    System.out.println("Avg time: " + snapshot.getAverageEvaluationTimeMillis() + "ms");
    System.out.println("Success rate: " + (snapshot.getSuccessRate() * 100) + "%");
}
```

### 3. Performance Analysis

```java
// Get all performance snapshots
Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();

// Generate insights
List<PerformanceAnalyzer.PerformanceInsight> insights = 
    PerformanceAnalyzer.analyzePerformance(snapshots);

for (PerformanceAnalyzer.PerformanceInsight insight : insights) {
    System.out.println(insight.getSeverity() + ": " + insight.getMessage());
}

// Generate comprehensive report
String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
System.out.println(report);
```

## Configuration Options

### Monitor Configuration

```java
RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

// Enable/disable monitoring
monitor.setEnabled(true);

// Configure history retention
monitor.setMaxHistorySize(1000);

// Configure tracking features
monitor.setTrackMemory(true);
monitor.setTrackComplexity(true);

// Clear accumulated metrics
monitor.clearMetrics();
```

### Check Configuration

```java
// Check current settings
boolean enabled = monitor.isEnabled();
long totalEvaluations = monitor.getTotalEvaluations();
double avgTime = monitor.getAverageEvaluationTimeMillis();
```

## Performance Metrics Reference

### RulePerformanceMetrics

| Method | Description | Example |
|--------|-------------|---------|
| `getRuleName()` | Name of the evaluated rule | "ValidationRule" |
| `getEvaluationTimeMillis()` | Evaluation time in milliseconds | 2.34 |
| `getEvaluationTimeNanos()` | Evaluation time in nanoseconds | 2340000 |
| `getMemoryUsedBytes()` | Memory used during evaluation | 1024 |
| `getExpressionComplexity()` | Complexity score of the expression | 5 |
| `isCacheHit()` | Whether evaluation was cached | false |
| `hasException()` | Whether an exception occurred | false |
| `getPerformanceSummary()` | Formatted summary string | "Rule: MyRule, Time: 2.34ms..." |

### PerformanceSnapshot

| Method | Description | Example |
|--------|-------------|---------|
| `getRuleName()` | Rule name | "ValidationRule" |
| `getEvaluationCount()` | Total number of evaluations | 150 |
| `getAverageEvaluationTimeMillis()` | Average evaluation time | 2.34 |
| `getMinEvaluationTimeMillis()` | Minimum evaluation time | 0.5 |
| `getMaxEvaluationTimeMillis()` | Maximum evaluation time | 15.2 |
| `getSuccessRate()` | Success rate (0.0 to 1.0) | 0.98 |
| `getAverageComplexity()` | Average complexity score | 4.2 |
| `getPerformanceSummary()` | Formatted summary | "Rule: MyRule \| Evaluations: 150..." |

## Performance Insights

### Insight Types

| Type | Severity | Description |
|------|----------|-------------|
| `SLOW_RULE` | HIGH | Rule is significantly slower than average |
| `HIGH_MEMORY` | MEDIUM | Rule uses excessive memory |
| `HIGH_COMPLEXITY` | MEDIUM | Rule has high complexity score |
| `LOW_SUCCESS_RATE` | HIGH | Rule has low success rate |
| `HIGH_VARIANCE` | MEDIUM | Rule has inconsistent execution times |

### Example Insights

```java
List<PerformanceAnalyzer.PerformanceInsight> insights = 
    PerformanceAnalyzer.analyzePerformance(snapshots);

for (PerformanceAnalyzer.PerformanceInsight insight : insights) {
    System.out.println("[" + insight.getSeverity() + "] " + 
                      insight.getType() + ": " + 
                      insight.getRuleName() + " - " + 
                      insight.getMessage());
}
```

Output:
```
[HIGH] SLOW_RULE: complex-validation - Rule is 6.7x slower than average (15.2ms vs 2.3ms)
[MEDIUM] HIGH_COMPLEXITY: data-enrichment - Rule has high complexity score (12.0 vs 4.2 average)
[HIGH] LOW_SUCCESS_RATE: error-prone-rule - Rule has low success rate (85.2%)
```

## Common Use Cases

### 1. Identify Slow Rules

```java
Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
snapshots.values().stream()
    .filter(s -> s.getAverageEvaluationTimeMillis() > 10.0)
    .forEach(s -> System.out.println("Slow rule: " + s.getRuleName() + 
                                   " (" + s.getAverageEvaluationTimeMillis() + "ms)"));
```

### 2. Monitor Success Rates

```java
snapshots.values().stream()
    .filter(s -> s.getSuccessRate() < 0.95)
    .forEach(s -> System.out.println("Low success rate: " + s.getRuleName() + 
                                   " (" + (s.getSuccessRate() * 100) + "%)"));
```

### 3. Track Performance Over Time

```java
// Get historical data for a specific rule
List<RulePerformanceMetrics> history = monitor.getRuleHistory("my-rule");
history.stream()
    .forEach(m -> System.out.println(m.getStartTime() + ": " + 
                                   m.getEvaluationTimeMillis() + "ms"));
```

### 4. Generate Regular Reports

```java
// Generate and log performance report
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

## Best Practices

### 1. Regular Monitoring
- Review performance metrics regularly
- Set up automated reporting
- Monitor for performance regressions

### 2. Optimization Strategy
- Focus on rules with high impact (frequent + slow)
- Use complexity scores to guide simplification
- Monitor memory usage for resource planning

### 3. Production Deployment
- Start with monitoring enabled
- Configure appropriate history retention
- Integrate with existing monitoring systems

### 4. Performance Testing
- Include performance assertions in tests
- Monitor performance during load testing
- Track performance trends over time

## Troubleshooting

### High Memory Usage
```java
// Check memory usage patterns
snapshots.values().stream()
    .sorted((a, b) -> Long.compare(b.getAverageMemoryUsed(), a.getAverageMemoryUsed()))
    .limit(5)
    .forEach(s -> System.out.println(s.getRuleName() + ": " + 
                                   s.getAverageMemoryUsed() + " bytes"));
```

### Performance Degradation
```java
// Compare current vs historical performance
PerformanceSnapshot current = monitor.getRuleSnapshot("my-rule");
List<RulePerformanceMetrics> recent = monitor.getRuleHistory("my-rule")
    .stream()
    .skip(Math.max(0, monitor.getRuleHistory("my-rule").size() - 10))
    .collect(Collectors.toList());

double recentAvg = recent.stream()
    .mapToDouble(RulePerformanceMetrics::getEvaluationTimeMillis)
    .average()
    .orElse(0.0);

if (recentAvg > current.getAverageEvaluationTimeMillis() * 1.5) {
    System.out.println("Performance degradation detected!");
}
```

## Integration Examples

### With Spring Boot Actuator
```java
@Component
public class RuleEngineMetrics {
    @Autowired
    private RulesEngine rulesEngine;
    
    @EventListener
    public void onRuleEvaluation(RuleEvaluationEvent event) {
        if (event.getResult().hasPerformanceMetrics()) {
            RulePerformanceMetrics metrics = event.getResult().getPerformanceMetrics();
            // Send to metrics system
            meterRegistry.timer("rule.evaluation.time", "rule", metrics.getRuleName())
                        .record(metrics.getEvaluationTime());
        }
    }
}
```

### With Logging
```java
// Log slow rules
if (result.hasPerformanceMetrics()) {
    RulePerformanceMetrics metrics = result.getPerformanceMetrics();
    if (metrics.getEvaluationTimeMillis() > 10.0) {
        logger.warn("Slow rule evaluation: {} took {}ms", 
                   metrics.getRuleName(), 
                   metrics.getEvaluationTimeMillis());
    }
}
```

For complete documentation, see `Performance-Monitoring-Guide.md`.
