package dev.mars.rulesengine.demo.performance;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.monitoring.PerformanceAnalyzer;
import dev.mars.rulesengine.core.service.monitoring.PerformanceSnapshot;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.rulesengine.core.service.monitoring.RulePerformanceMonitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple demonstration of performance monitoring capabilities.
 */
public class SimplePerformanceDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Simple Performance Monitoring Demo ===\n");
        
        // Create rules engine with performance monitoring
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(configuration);
        RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

        // Create a simple rule
        Rule simpleRule = configuration.registerRule(configuration.rule("demo-rule")
                .withCategory("demo")
                .withName("Demo Rule")
                .withDescription("A simple demo rule")
                .withPriority(1)
                .withCondition("#value > 100")
                .withMessage("Value exceeds threshold")
                .build());

        System.out.println("1. Created rule: " + simpleRule.getName());
        System.out.println("   Condition: " + simpleRule.getCondition());
        System.out.println("   Message: " + simpleRule.getMessage());
        System.out.println();

        // Execute the rule multiple times with different values
        System.out.println("2. Executing rule with different values:");
        int[] testValues = {50, 150, 75, 200, 25, 300};
        
        for (int value : testValues) {
            Map<String, Object> facts = new HashMap<>();
            facts.put("value", value);
            
            RuleResult result = engine.executeRule(simpleRule, facts);
            
            System.out.printf("   Value: %3d -> Triggered: %-5s", value, result.isTriggered());
            
            if (result.hasPerformanceMetrics()) {
                RulePerformanceMetrics metrics = result.getPerformanceMetrics();
                System.out.printf(" (Time: %.2fms, Complexity: %d)",
                        metrics.getEvaluationTimeMillis(),
                        metrics.getExpressionComplexity());
            }
            System.out.println();
        }
        System.out.println();

        // Display performance summary
        System.out.println("3. Performance Summary:");
        System.out.printf("   Total evaluations: %d%n", monitor.getTotalEvaluations());
        System.out.printf("   Average time: %.2f ms%n", monitor.getAverageEvaluationTimeMillis());
        System.out.println();

        // Display rule-specific performance
        PerformanceSnapshot snapshot = monitor.getRuleSnapshot("Demo Rule");
        if (snapshot != null) {
            System.out.println("4. Rule Performance Details:");
            System.out.printf("   Rule: %s%n", snapshot.getRuleName());
            System.out.printf("   Evaluations: %d%n", snapshot.getEvaluationCount());
            System.out.printf("   Average time: %.2f ms%n", snapshot.getAverageEvaluationTimeMillis());
            System.out.printf("   Min time: %.2f ms%n", snapshot.getMinEvaluationTimeMillis());
            System.out.printf("   Max time: %.2f ms%n", snapshot.getMaxEvaluationTimeMillis());
            System.out.printf("   Success rate: %.1f%%%n", snapshot.getSuccessRate() * 100);
            System.out.printf("   Average complexity: %.1f%n", snapshot.getAverageComplexity());
            System.out.println();
        }

        // Generate performance report
        System.out.println("5. Performance Report:");
        System.out.println("======================");
        Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
        String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
        System.out.println(report);

        System.out.println("Demo completed successfully!");
    }
}
