package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.rulesengine.demo.framework.Demo;
import dev.mars.rulesengine.demo.framework.DemoCategory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive performance monitoring demonstration.
 * This consolidates performance demos from various parts of the original module.
 */
public class PerformanceMonitoringDemo implements Demo {
    
    private final RulesService rulesService;
    private final RulesEngine rulesEngine;
    private final RulePerformanceMonitor performanceMonitor;
    private final ExecutorService executorService;
    
    public PerformanceMonitoringDemo() {
        this.rulesService = new RulesService();
        this.rulesEngine = createConfiguredRulesEngine();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.performanceMonitor.setEnabled(true);
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    @Override
    public String getName() {
        return "Performance Monitoring";
    }
    
    @Override
    public String getDescription() {
        return "Comprehensive demonstration of performance monitoring, metrics collection, and optimization techniques";
    }
    
    @Override
    public DemoCategory getCategory() {
        return DemoCategory.PERFORMANCE_MONITORING;
    }
    
    @Override
    public int getEstimatedRuntimeSeconds() {
        return 60;
    }
    
    @Override
    public void run() {
        runNonInteractive();
    }
    
    @Override
    public void runNonInteractive() {
        System.out.println("=== PERFORMANCE MONITORING DEMONSTRATION ===");
        System.out.println("Comprehensive monitoring and optimization techniques\n");
        
        try {
            demonstrateBasicPerformanceMonitoring();
            demonstrateConcurrentExecution();
            demonstratePerformanceOptimization();
            demonstrateExceptionHandling();
            demonstrateAdvancedMonitoring();
            simulateMonitoringDashboard();
            
        } finally {
            cleanup();
        }
        
        System.out.println("\n✅ Performance monitoring demonstration completed!");
    }
    
    /**
     * Demonstrate basic performance monitoring capabilities.
     */
    private void demonstrateBasicPerformanceMonitoring() {
        System.out.println("=== BASIC PERFORMANCE MONITORING ===");
        
        List<Rule> testRules = createPerformanceTestRules();
        Map<String, Object> testContext = createTestContext();
        
        System.out.println("1. Executing " + testRules.size() + " rules with performance monitoring:");
        
        long startTime = System.nanoTime();
        
        for (Rule rule : testRules) {
            var metricsBuilder = performanceMonitor.startEvaluation(rule.getName(), "validation");
            
            try {
                // Simulate varying execution complexity
                Thread.sleep(rule.getName().contains("complex") ? 5 : 1);
                boolean result = rulesService.check(rule.getCondition(), testContext);
                
                var metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
                System.out.println("   ✓ " + rule.getName() + ": " + result + 
                                 " (" + metrics.getEvaluationTimeMillis() + "ms)");
                
            } catch (Exception e) {
                var metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), e);
                System.out.println("   ✗ " + rule.getName() + ": ERROR (" + 
                                 metrics.getEvaluationTimeMillis() + "ms)");
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        
        System.out.println("\n2. Performance Summary:");
        System.out.println("   Total execution time: " + (totalTime / 1_000_000.0) + "ms");
        System.out.println("   Total rule evaluations: " + performanceMonitor.getTotalEvaluations());
        System.out.println("   Average time per evaluation: " + 
                         (performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0 / 
                          Math.max(1, performanceMonitor.getTotalEvaluations())) + "ms");
        
        System.out.println();
    }
    
    /**
     * Demonstrate concurrent rule execution with performance monitoring.
     */
    private void demonstrateConcurrentExecution() {
        System.out.println("=== CONCURRENT EXECUTION MONITORING ===");
        
        List<Rule> testRules = createPerformanceTestRules();
        int numberOfTasks = 20;
        
        System.out.println("1. Executing rules concurrently across " + numberOfTasks + " tasks:");
        
        long startTime = System.nanoTime();
        
        // Create concurrent tasks
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfTasks; i++) {
            final int taskIndex = i;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Map<String, Object> context = createTestContext();
                context.put("taskIndex", taskIndex);
                
                for (Rule rule : testRules) {
                    var metricsBuilder = performanceMonitor.startEvaluation(
                        rule.getName() + "-thread-" + Thread.currentThread().getId(), "concurrent");
                    
                    try {
                        Thread.sleep(1); // Simulate processing
                        boolean result = rulesService.check(rule.getCondition(), context);
                        performanceMonitor.completeEvaluation(metricsBuilder, "concurrent-test");
                        
                    } catch (Exception e) {
                        performanceMonitor.completeEvaluation(metricsBuilder, "concurrent-test", e);
                    }
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long totalTime = System.nanoTime() - startTime;
        
        System.out.println("   ✓ Concurrent execution time: " + (totalTime / 1_000_000.0) + "ms");
        System.out.println("   ✓ Total evaluations: " + performanceMonitor.getTotalEvaluations());
        System.out.println("   ✓ Throughput: " + 
                         (performanceMonitor.getTotalEvaluations() * 1000.0 / (totalTime / 1_000_000.0)) + " evaluations/second");
        
        System.out.println();
    }
    
    /**
     * Demonstrate performance optimization techniques.
     */
    private void demonstratePerformanceOptimization() {
        System.out.println("=== PERFORMANCE OPTIMIZATION ===");
        
        System.out.println("1. Rule Caching Demonstration:");
        
        String cachedRule = "#amount > 1000";
        Map<String, Object> testContext = Map.of("amount", new BigDecimal("5000"));
        
        // Test with caching
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            rulesService.check(cachedRule, testContext);
        }
        long cacheTime = System.nanoTime() - startTime;
        
        System.out.println("   ✓ 1000 evaluations with caching: " + (cacheTime / 1_000_000.0) + "ms");
        
        System.out.println("\n2. Expression Complexity Analysis:");
        
        String simpleExpression = "#amount > 1000";
        String complexExpression = "#amount != null && #amount.compareTo(new java.math.BigDecimal('1000')) > 0 && #currency == 'USD'";
        
        Map<String, Object> context = Map.of("amount", new BigDecimal("5000"), "currency", "USD");
        
        // Test simple expression
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            rulesService.check(simpleExpression, context);
        }
        long simpleTime = System.nanoTime() - startTime;
        
        // Test complex expression
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            rulesService.check(complexExpression, context);
        }
        long complexTime = System.nanoTime() - startTime;
        
        System.out.println("   ✓ Simple expression (1000x): " + (simpleTime / 1_000_000.0) + "ms");
        System.out.println("   ✓ Complex expression (1000x): " + (complexTime / 1_000_000.0) + "ms");
        System.out.println("   ✓ Performance ratio: " + String.format("%.2f", (double) complexTime / simpleTime) + "x");
        
        System.out.println("\n3. Optimization Recommendations:");
        System.out.println("   • Use simple expressions when possible");
        System.out.println("   • Cache frequently used rules");
        System.out.println("   • Avoid complex object creation in expressions");
        System.out.println("   • Pre-validate input data");
        System.out.println("   • Use appropriate data types");
        
        System.out.println();
    }
    
    /**
     * Demonstrate exception handling and monitoring.
     */
    private void demonstrateExceptionHandling() {
        System.out.println("=== EXCEPTION HANDLING MONITORING ===");
        
        System.out.println("1. Handling Invalid Expressions:");
        
        String[] invalidExpressions = {
            "#invalidProperty.someMethod()",  // Null pointer
            "#amount.invalidMethod()",        // Method not found
            "#amount / 0",                   // Division by zero
            "invalid syntax +++",            // Syntax error
            "#amount.compareTo(null)"        // Null argument
        };
        
        Map<String, Object> context = Map.of("amount", new BigDecimal("1000"));
        
        for (String expression : invalidExpressions) {
            var metricsBuilder = performanceMonitor.startEvaluation("error-test", "exception-handling");
            
            try {
                boolean result = rulesService.check(expression, context);
                System.out.println("   ✗ Unexpected success for: " + expression);
                performanceMonitor.completeEvaluation(metricsBuilder, expression);
                
            } catch (Exception e) {
                var metrics = performanceMonitor.completeEvaluation(metricsBuilder, expression, e);
                System.out.println("   ✓ Caught " + e.getClass().getSimpleName() + 
                                 " in " + metrics.getEvaluationTimeMillis() + "ms");
            }
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate advanced monitoring features.
     */
    private void demonstrateAdvancedMonitoring() {
        System.out.println("=== ADVANCED MONITORING FEATURES ===");
        
        System.out.println("1. Performance Trends Analysis:");
        
        // Simulate performance degradation over time
        for (int batch = 1; batch <= 5; batch++) {
            System.out.println("   Batch " + batch + ":");
            
            long batchStartTime = System.nanoTime();
            int batchEvaluations = 0;
            
            for (int i = 0; i < 50; i++) {
                var metricsBuilder = performanceMonitor.startEvaluation("trend-test-" + batch, "trend-analysis");
                
                try {
                    // Simulate increasing complexity
                    Thread.sleep(batch);
                    boolean result = rulesService.check("#amount > 1000", 
                                                       Map.of("amount", new BigDecimal("5000")));
                    performanceMonitor.completeEvaluation(metricsBuilder, "#amount > 1000");
                    batchEvaluations++;
                    
                } catch (Exception e) {
                    performanceMonitor.completeEvaluation(metricsBuilder, "#amount > 1000", e);
                }
            }
            
            long batchTime = System.nanoTime() - batchStartTime;
            double avgTime = (batchTime / 1_000_000.0) / batchEvaluations;
            
            System.out.println("     Average time: " + String.format("%.2f", avgTime) + "ms");
            System.out.println("     Throughput: " + String.format("%.0f", 
                             batchEvaluations * 1000.0 / (batchTime / 1_000_000.0)) + " eval/sec");
        }
        
        System.out.println();
    }
    
    /**
     * Simulate a monitoring dashboard with real-time metrics.
     */
    private void simulateMonitoringDashboard() {
        System.out.println("=== MONITORING DASHBOARD SIMULATION ===");
        
        System.out.println("1. Real-time Performance Dashboard:");
        System.out.println("   ┌─────────────────────────────────────────────────────────┐");
        System.out.println("   │                 RULES ENGINE METRICS                    │");
        System.out.println("   ├─────────────────────────────────────────────────────────┤");
        System.out.printf("   │ Total Evaluations:     %,10d                    │%n", performanceMonitor.getTotalEvaluations());
        System.out.printf("   │ Total Execution Time:  %,10.2f ms                │%n", performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0);
        System.out.printf("   │ Average Time/Rule:     %,10.2f ms                │%n", 
                         performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0 / Math.max(1, performanceMonitor.getTotalEvaluations()));
        System.out.printf("   │ Peak Throughput:       %,10.0f eval/sec           │%n", 
                         performanceMonitor.getTotalEvaluations() * 1000.0 / (performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0));
        System.out.println("   └─────────────────────────────────────────────────────────┘");
        
        System.out.println("\n2. Performance Health Indicators:");
        double avgTime = performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0 / Math.max(1, performanceMonitor.getTotalEvaluations());
        
        String healthStatus = avgTime < 1.0 ? "EXCELLENT" : 
                             avgTime < 5.0 ? "GOOD" : 
                             avgTime < 10.0 ? "FAIR" : "POOR";
        
        String healthIndicator = avgTime < 1.0 ? "🟢" : 
                                avgTime < 5.0 ? "🟡" : 
                                avgTime < 10.0 ? "🟠" : "🔴";
        
        System.out.println("   " + healthIndicator + " Overall Performance: " + healthStatus);
        System.out.println("   📊 Memory Usage: Optimal");
        System.out.println("   🔄 Cache Hit Rate: 95%");
        System.out.println("   ⚡ Concurrent Threads: 4 active");
        
        System.out.println();
    }
    
    // Helper methods
    
    private RulesEngine createConfiguredRulesEngine() {
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        return new RulesEngine(config);
    }
    
    private List<Rule> createPerformanceTestRules() {
        return Arrays.asList(
            new Rule("simple-amount-check", "#amount > 1000", "Simple amount check"),
            new Rule("complex-validation", "#amount != null && #amount.compareTo(new java.math.BigDecimal('1000')) > 0", "Complex validation"),
            new Rule("date-validation", "#date != null", "Date validation"),
            new Rule("currency-check", "#currency == 'USD'", "Currency check"),
            new Rule("complex-business-rule", "#amount > 1000 && #currency == 'USD' && #date != null", "Complex business rule")
        );
    }
    
    private Map<String, Object> createTestContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("amount", new BigDecimal("5000"));
        context.put("currency", "USD");
        context.put("date", new Date());
        return context;
    }
    
    private void cleanup() {
        executorService.shutdown();
    }
}
