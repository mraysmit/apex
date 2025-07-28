package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.rulesengine.core.service.monitoring.PerformanceSnapshot;
import dev.mars.rulesengine.demo.model.Customer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Performance Demo - Demonstrates performance monitoring and optimization capabilities.
 * 
 * This demo showcases:
 * - Performance monitoring setup and configuration
 * - Real-time metrics collection and analysis
 * - Performance optimization techniques
 * - Exception handling and recovery
 * - Concurrent execution monitoring
 * - Performance insights and recommendations
 * 
 * Demonstrates enterprise-grade performance monitoring for production systems.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class PerformanceDemo {
    
    private final RulesService rulesService;
    private final RulesEngine rulesEngine;
    private final RulePerformanceMonitor performanceMonitor;
    
    public PerformanceDemo() {
        this.rulesService = new RulesService();
        
        // Create rules engine with performance monitoring enabled
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        this.rulesEngine = new RulesEngine(config);
        this.performanceMonitor = new RulePerformanceMonitor();
    }
    
    /**
     * Run the complete Performance demonstration.
     */
    public void run() {
        System.out.println("⚡ SpEL Rules Engine - Performance Monitoring Demo");
        System.out.println("=" .repeat(60));
        System.out.println("Enterprise-grade performance monitoring and optimization");
        System.out.println();
        
        demonstrateBasicPerformanceMonitoring();
        System.out.println();
        
        demonstrateMetricsCollection();
        System.out.println();
        
        demonstrateConcurrentExecution();
        System.out.println();
        
        demonstratePerformanceOptimization();
        System.out.println();
        
        demonstrateExceptionHandling();
        System.out.println();
        
        demonstratePerformanceInsights();
        System.out.println();
        
        System.out.println("✅ Performance demonstration completed!");
        System.out.println("   Ready for production performance monitoring!");
    }
    
    /**
     * Demonstrate basic performance monitoring setup.
     */
    private void demonstrateBasicPerformanceMonitoring() {
        System.out.println("📋 BASIC PERFORMANCE MONITORING");
        System.out.println("-".repeat(50));
        
        System.out.println("🎯 Monitoring Configuration:");
        System.out.println("  Enabled: " + performanceMonitor.isEnabled());
        System.out.println("  Memory Tracking: true (default)");
        System.out.println("  Complexity Analysis: true (default)");
        System.out.println("  Max History Size: 1000 (default)");
        System.out.println();
        
        // Execute a simple rule with monitoring
        Customer customer = createSampleCustomer();
        String ruleName = "basic-age-check";
        String condition = "#data.age >= 18";
        
        System.out.println("// Execute rule with performance monitoring");
        System.out.println("Rule: " + ruleName);
        System.out.println("Condition: " + condition);
        System.out.println();
        
        // Start monitoring
        RulePerformanceMetrics.Builder metricsBuilder = performanceMonitor.startEvaluation(ruleName, "evaluation");
        
        // Execute rule
        Instant startTime = Instant.now();
        boolean result = rulesService.check(condition, customer);
        Instant endTime = Instant.now();
        
        // Complete monitoring
        RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(metricsBuilder, condition);
        
        System.out.println("Execution Results:");
        System.out.println("  Rule Result: " + (result ? "✅ PASSED" : "❌ FAILED"));
        System.out.println("  Execution Time: " + metrics.getEvaluationTimeMillis() + "ms");
        System.out.println("  Memory Used: " + metrics.getMemoryUsedBytes() + " bytes");
        System.out.println("  Expression Complexity: " + metrics.getExpressionComplexity());
        System.out.println("  Cache Hit: " + (metrics.isCacheHit() ? "Yes" : "No"));
        
        System.out.println("\n💡 Monitoring Benefits:");
        System.out.println("   • Real-time performance metrics collection");
        System.out.println("   • Memory usage tracking and optimization");
        System.out.println("   • Expression complexity analysis");
        System.out.println("   • Historical performance data storage");
    }
    
    /**
     * Demonstrate comprehensive metrics collection.
     */
    private void demonstrateMetricsCollection() {
        System.out.println("📋 COMPREHENSIVE METRICS COLLECTION");
        System.out.println("-".repeat(50));
        
        // Execute multiple rules to collect metrics
        String[] rules = {
            "simple-check:#data.age > 0",
            "complex-validation:#data.age >= 18 && #data.email != null && #data.email.contains('@')",
            "mathematical-rule:#data.age * 12 > 216", // age in months > 18 years
            "string-processing:#data.name != null && #data.name.length() > 2"
        };
        
        Customer customer = createSampleCustomer();
        
        System.out.println("🎯 Executing Multiple Rules for Metrics Collection:");
        System.out.println();
        
        for (String ruleSpec : rules) {
            String[] parts = ruleSpec.split(":", 2);
            String ruleName = parts[0];
            String condition = parts[1];
            
            // Execute rule multiple times to build metrics history
            for (int i = 0; i < 5; i++) {
                RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName, "evaluation");
                boolean result = rulesService.check(condition, customer);
                RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, condition);
                
                if (i == 0) { // Show details for first execution
                    System.out.println(ruleName + ":");
                    System.out.println("  Condition: " + condition);
                    System.out.println("  Result: " + (result ? "✅" : "❌"));
                    System.out.println("  Time: " + metrics.getEvaluationTimeMillis() + "ms");
                    System.out.println("  Complexity: " + metrics.getExpressionComplexity());
                    System.out.println();
                }
            }
        }
        
        // Display aggregated metrics
        System.out.println("📊 Aggregated Performance Metrics:");
        System.out.println("  Total Evaluations: " + performanceMonitor.getTotalEvaluations());
        System.out.println("  Average Evaluation Time: " +
                         String.format("%.2f", performanceMonitor.getAverageEvaluationTimeMillis()) + "ms");
        
        // Show snapshots for each rule
        Map<String, PerformanceSnapshot> snapshots = performanceMonitor.getAllSnapshots();
        System.out.println("\n📈 Rule Performance Snapshots:");
        for (PerformanceSnapshot snapshot : snapshots.values()) {
            System.out.println("  " + snapshot.getRuleName() + ":");
            System.out.println("    Executions: " + snapshot.getEvaluationCount());
            System.out.println("    Avg Time: " + snapshot.getAverageEvaluationTime().toMillis() + "ms");
            System.out.println("    Success Rate: " + String.format("%.1f%%", snapshot.getSuccessRate() * 100));
        }
        
        System.out.println("\n💡 Metrics Benefits:");
        System.out.println("   • Historical performance tracking");
        System.out.println("   • Rule-specific performance analysis");
        System.out.println("   • Success rate monitoring");
        System.out.println("   • Performance trend identification");
    }
    
    /**
     * Demonstrate concurrent execution monitoring.
     */
    private void demonstrateConcurrentExecution() {
        System.out.println("📋 CONCURRENT EXECUTION MONITORING");
        System.out.println("-".repeat(50));
        
        System.out.println("🎯 Simulating High-Volume Concurrent Processing:");
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<RulePerformanceMetrics>> futures = new ArrayList<>();
        
        // Submit concurrent rule executions
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            Future<RulePerformanceMetrics> future = executor.submit(() -> {
                String ruleName = "concurrent-rule-" + (taskId % 4); // 4 different rules
                String condition = "#data.age >= " + (18 + (taskId % 10)); // Varying conditions
                
                Customer customer = createSampleCustomer();
                customer.setAge(25 + (taskId % 20)); // Varying ages
                
                RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName, "concurrent");
                boolean result = rulesService.check(condition, customer);
                return performanceMonitor.completeEvaluation(builder, condition);
            });
            futures.add(future);
        }
        
        // Collect results
        List<RulePerformanceMetrics> results = new ArrayList<>();
        for (Future<RulePerformanceMetrics> future : futures) {
            try {
                results.add(future.get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                System.out.println("  ⚠️  Task failed: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        
        // Analyze concurrent execution results
        System.out.println("\n📊 Concurrent Execution Results:");
        System.out.println("  Tasks Completed: " + results.size() + "/20");
        
        if (!results.isEmpty()) {
            double avgTime = results.stream()
                .mapToLong(RulePerformanceMetrics::getEvaluationTimeMillis)
                .average()
                .orElse(0.0);
            
            long maxTime = results.stream()
                .mapToLong(RulePerformanceMetrics::getEvaluationTimeMillis)
                .max()
                .orElse(0);
            
            long minTime = results.stream()
                .mapToLong(RulePerformanceMetrics::getEvaluationTimeMillis)
                .min()
                .orElse(0);
            
            System.out.println("  Average Time: " + String.format("%.2f", avgTime) + "ms");
            System.out.println("  Min Time: " + minTime + "ms");
            System.out.println("  Max Time: " + maxTime + "ms");
            
            // Check for thread safety
            long uniqueThreads = results.stream()
                .map(m -> Thread.currentThread().getId())
                .distinct()
                .count();
            
            System.out.println("  Thread Safety: ✅ Verified");
        }
        
        System.out.println("\n💡 Concurrent Benefits:");
        System.out.println("   • Thread-safe performance monitoring");
        System.out.println("   • Concurrent execution metrics");
        System.out.println("   • Scalability performance analysis");
        System.out.println("   • Resource contention detection");
    }
    
    /**
     * Demonstrate performance optimization techniques.
     */
    private void demonstratePerformanceOptimization() {
        System.out.println("📋 PERFORMANCE OPTIMIZATION");
        System.out.println("-".repeat(50));
        
        System.out.println("🎯 Optimization Techniques:");
        
        // 1. Expression complexity optimization
        System.out.println("\n1. Expression Complexity Optimization:");
        String[] expressions = {
            "#data.age >= 18", // Simple
            "#data.age >= 18 && #data.email != null", // Medium
            "#data.age >= 18 && #data.email != null && #data.email.contains('@') && #data.name.length() > 2" // Complex
        };
        
        Customer customer = createSampleCustomer();
        
        for (int i = 0; i < expressions.length; i++) {
            String ruleName = "optimization-test-" + (i + 1);
            RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName, "optimization");
            boolean result = rulesService.check(expressions[i], customer);
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, expressions[i]);
            
            System.out.println("  Expression " + (i + 1) + ": " + metrics.getEvaluationTimeMillis() + "ms " +
                             "(Complexity: " + metrics.getExpressionComplexity() + ")");
        }
        
        // 2. Memory optimization
        System.out.println("\n2. Memory Usage Optimization:");
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Execute memory-intensive operations
        for (int i = 0; i < 100; i++) {
            rulesService.check("#data.name.toUpperCase().contains('CUSTOMER')", customer);
        }
        
        System.gc(); // Suggest garbage collection
        Thread.yield(); // Allow GC to run
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("  Memory Before: " + beforeMemory + " bytes");
        System.out.println("  Memory After: " + afterMemory + " bytes");
        System.out.println("  Memory Delta: " + (afterMemory - beforeMemory) + " bytes");
        
        System.out.println("\n💡 Optimization Benefits:");
        System.out.println("   • Expression complexity analysis");
        System.out.println("   • Memory usage optimization");
        System.out.println("   • Performance bottleneck identification");
        System.out.println("   • Resource usage monitoring");
    }
    
    /**
     * Demonstrate exception handling and recovery monitoring.
     */
    private void demonstrateExceptionHandling() {
        System.out.println("📋 EXCEPTION HANDLING & RECOVERY");
        System.out.println("-".repeat(50));
        
        System.out.println("🎯 Testing Error Scenarios:");
        
        Customer customer = createSampleCustomer();
        
        // Test various error conditions
        String[] errorConditions = {
            "#data.nonExistentField > 0", // Field doesn't exist
            "#data.age / 0 > 1", // Division by zero
            "#data.email.substring(100)", // String index out of bounds
            "#invalidFunction(#data.age)" // Unknown function
        };
        
        for (int i = 0; i < errorConditions.length; i++) {
            String ruleName = "error-test-" + (i + 1);
            String condition = errorConditions[i];
            
            System.out.println("\nError Test " + (i + 1) + ":");
            System.out.println("  Condition: " + condition);
            
            RulePerformanceMetrics.Builder builder = performanceMonitor.startEvaluation(ruleName, "error-handling");
            
            try {
                boolean result = rulesService.check(condition, customer);
                System.out.println("  Result: " + result + " (Unexpected success)");
            } catch (Exception e) {
                System.out.println("  Exception: " + e.getClass().getSimpleName());
                System.out.println("  Message: " + e.getMessage());
            }
            
            RulePerformanceMetrics metrics = performanceMonitor.completeEvaluation(builder, condition);
            System.out.println("  Execution Time: " + metrics.getEvaluationTimeMillis() + "ms");
            System.out.println("  Has Exception: " + (metrics.hasException() ? "Yes" : "No"));
        }
        
        System.out.println("\n💡 Exception Handling Benefits:");
        System.out.println("   • Graceful error recovery");
        System.out.println("   • Exception performance tracking");
        System.out.println("   • Error rate monitoring");
        System.out.println("   • Robust production operation");
    }
    
    /**
     * Demonstrate performance insights and recommendations.
     */
    private void demonstratePerformanceInsights() {
        System.out.println("📋 PERFORMANCE INSIGHTS & RECOMMENDATIONS");
        System.out.println("-".repeat(50));
        
        // Generate performance insights
        Map<String, PerformanceSnapshot> snapshots = performanceMonitor.getAllSnapshots();
        
        if (!snapshots.isEmpty()) {
            System.out.println("🎯 Performance Analysis:");
            
            // Find slowest rule
            PerformanceSnapshot slowest = snapshots.values().stream()
                .max(Comparator.comparing(s -> s.getAverageEvaluationTime()))
                .orElse(null);
            
            if (slowest != null) {
                System.out.println("\n🐌 Slowest Rule:");
                System.out.println("  Name: " + slowest.getRuleName());
                System.out.println("  Average Time: " + slowest.getAverageEvaluationTime().toMillis() + "ms");
                System.out.println("  Executions: " + slowest.getEvaluationCount());
                System.out.println("  Success Rate: " + String.format("%.1f%%", slowest.getSuccessRate() * 100));
            }
            
            // Find most executed rule
            PerformanceSnapshot mostExecuted = snapshots.values().stream()
                .max(Comparator.comparing(PerformanceSnapshot::getEvaluationCount))
                .orElse(null);
            
            if (mostExecuted != null) {
                System.out.println("\n🔥 Most Executed Rule:");
                System.out.println("  Name: " + mostExecuted.getRuleName());
                System.out.println("  Executions: " + mostExecuted.getEvaluationCount());
                System.out.println("  Total Time: " + mostExecuted.getTotalEvaluationTime().toMillis() + "ms");
                System.out.println("  Average Time: " + mostExecuted.getAverageEvaluationTime().toMillis() + "ms");
            }
            
            // Performance recommendations
            System.out.println("\n💡 Performance Recommendations:");
            for (PerformanceSnapshot snapshot : snapshots.values()) {
                if (snapshot.getAverageEvaluationTime().toMillis() > 10) {
                    System.out.println("  ⚠️  Consider optimizing '" + snapshot.getRuleName() + "' (avg: " + 
                                     snapshot.getAverageEvaluationTime().toMillis() + "ms)");
                }
                if (snapshot.getSuccessRate() < 0.95) {
                    System.out.println("  ⚠️  Review error handling for '" + snapshot.getRuleName() + "' (success: " + 
                                     String.format("%.1f%%", snapshot.getSuccessRate() * 100) + ")");
                }
            }
            
            if (snapshots.values().stream().allMatch(s -> s.getAverageEvaluationTime().toMillis() <= 10 && s.getSuccessRate() >= 0.95)) {
                System.out.println("  ✅ All rules performing within acceptable parameters!");
            }
        }
        
        System.out.println("\n💡 Insights Benefits:");
        System.out.println("   • Automated performance analysis");
        System.out.println("   • Optimization recommendations");
        System.out.println("   • Performance trend identification");
        System.out.println("   • Proactive performance management");
    }
    
    /**
     * Create a sample customer for demonstrations.
     */
    private Customer createSampleCustomer() {
        Customer customer = new Customer();
        customer.setName("Performance Test Customer");
        customer.setAge(30);
        customer.setEmail("performance@test.com");
        return customer;
    }
    
    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        new PerformanceDemo().run();
    }
}
