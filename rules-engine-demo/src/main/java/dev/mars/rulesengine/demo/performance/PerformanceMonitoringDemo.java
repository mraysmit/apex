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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Demonstration of the Rule Engine Performance Monitoring capabilities.
 * This demo shows how to track rule evaluation performance, analyze metrics,
 * and generate performance insights and recommendations.
 */
public class PerformanceMonitoringDemo {
    private static final Logger LOGGER = Logger.getLogger(PerformanceMonitoringDemo.class.getName());

    public static void main(String[] args) {
        LOGGER.info("Starting Performance Monitoring Demo");
        
        PerformanceMonitoringDemo demo = new PerformanceMonitoringDemo();
        demo.runDemo();
        
        LOGGER.info("Performance Monitoring Demo completed");
    }

    public void runDemo() {
        // Create rules engine with performance monitoring
        RulesEngineConfiguration configuration = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(configuration);
        RulePerformanceMonitor monitor = engine.getPerformanceMonitor();

        System.out.println("=== RULE ENGINE PERFORMANCE MONITORING DEMO ===\n");

        // 1. Create various rules with different complexity levels
        createDemoRules(configuration);

        // 2. Execute rules with different scenarios
        executeRulesWithVariousScenarios(engine);

        // 3. Display performance metrics
        displayPerformanceMetrics(monitor);

        // 4. Analyze performance and generate insights
        analyzePerformanceAndGenerateInsights(monitor);

        // 5. Demonstrate performance configuration
        demonstratePerformanceConfiguration(monitor);
    }

    private void createDemoRules(RulesEngineConfiguration configuration) {
        System.out.println("1. Creating Demo Rules with Various Complexity Levels");
        System.out.println("=====================================================");

        // Simple rule
        configuration.registerRule(configuration.rule("simple-rule")
                .withCategory("performance-test")
                .withName("Simple Rule")
                .withDescription("A simple rule for performance testing")
                .withPriority(1)
                .withCondition("#value > 10")
                .withMessage("Value is greater than 10")
                .build());

        // Medium complexity rule
        configuration.registerRule(configuration.rule("medium-rule")
                .withCategory("performance-test")
                .withName("Medium Complexity Rule")
                .withDescription("A medium complexity rule")
                .withPriority(2)
                .withCondition("#value > 5 && #value < 100 && #category == 'test'")
                .withMessage("Value meets medium complexity criteria")
                .build());

        // Complex rule with method calls
        configuration.registerRule(configuration.rule("complex-rule")
                .withCategory("performance-test")
                .withName("Complex Rule")
                .withDescription("A complex rule with multiple conditions and method calls")
                .withPriority(3)
                .withCondition("#value > 0 && #value < 1000 && #category != null && #category.length() > 2 && (#type == 'A' || #type == 'B')")
                .withMessage("Value meets complex criteria")
                .build());

        // Rule that might cause errors
        configuration.registerRule(configuration.rule("error-prone-rule")
                .withCategory("performance-test")
                .withName("Error Prone Rule")
                .withDescription("A rule that might cause evaluation errors")
                .withPriority(4)
                .withCondition("#optionalValue != null && #optionalValue.toString().length() > 5")
                .withMessage("Optional value is valid")
                .build());

        System.out.println("Created 4 rules with varying complexity levels\n");
    }

    private void executeRulesWithVariousScenarios(RulesEngine engine) {
        System.out.println("2. Executing Rules with Various Scenarios");
        System.out.println("=========================================");

        Random random = new Random();
        String[] categories = {"test", "prod", "dev"};
        String[] types = {"A", "B", "C"};

        // Execute rules multiple times with different data
        for (int i = 0; i < 50; i++) {
            Map<String, Object> facts = new HashMap<>();
            facts.put("value", random.nextInt(200));
            facts.put("category", categories[random.nextInt(categories.length)]);
            facts.put("type", types[random.nextInt(types.length)]);
            
            // Sometimes include optional value, sometimes don't (to test error scenarios)
            if (random.nextBoolean()) {
                facts.put("optionalValue", "test-value-" + i);
            }

            // Execute each rule
            executeRuleQuietly(engine, "simple-rule", facts);
            executeRuleQuietly(engine, "medium-rule", facts);
            executeRuleQuietly(engine, "complex-rule", facts);
            executeRuleQuietly(engine, "error-prone-rule", facts);
        }

        System.out.println("Executed 200 rule evaluations (50 iterations × 4 rules)\n");
    }

    private void executeRuleQuietly(RulesEngine engine, String ruleId, Map<String, Object> facts) {
        try {
            Rule rule = engine.getConfiguration().getRuleById(ruleId);
            if (rule != null) {
                engine.executeRule(rule, facts);
            }
        } catch (Exception e) {
            // Ignore exceptions for demo purposes
        }
    }

    private void displayPerformanceMetrics(RulePerformanceMonitor monitor) {
        System.out.println("3. Performance Metrics Summary");
        System.out.println("==============================");

        // Global metrics
        System.out.printf("Total Evaluations: %d%n", monitor.getTotalEvaluations());
        System.out.printf("Average Evaluation Time: %.2f ms%n", monitor.getAverageEvaluationTimeMillis());
        System.out.println();

        // Individual rule metrics
        Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
        System.out.println("Individual Rule Performance:");
        System.out.println("----------------------------");
        
        for (PerformanceSnapshot snapshot : snapshots.values()) {
            System.out.printf("Rule: %s%n", snapshot.getRuleName());
            System.out.printf("  Evaluations: %d%n", snapshot.getEvaluationCount());
            System.out.printf("  Avg Time: %.2f ms%n", snapshot.getAverageEvaluationTimeMillis());
            System.out.printf("  Min Time: %.2f ms%n", snapshot.getMinEvaluationTimeMillis());
            System.out.printf("  Max Time: %.2f ms%n", snapshot.getMaxEvaluationTimeMillis());
            System.out.printf("  Success Rate: %.1f%%%n", snapshot.getSuccessRate() * 100);
            System.out.printf("  Avg Complexity: %.1f%n", snapshot.getAverageComplexity());
            System.out.println();
        }
    }

    private void analyzePerformanceAndGenerateInsights(RulePerformanceMonitor monitor) {
        System.out.println("4. Performance Analysis and Insights");
        System.out.println("====================================");

        Map<String, PerformanceSnapshot> snapshots = monitor.getAllSnapshots();
        
        // Generate performance insights
        List<PerformanceAnalyzer.PerformanceInsight> insights = PerformanceAnalyzer.analyzePerformance(snapshots);
        
        if (!insights.isEmpty()) {
            System.out.println("Performance Insights:");
            System.out.println("--------------------");
            for (PerformanceAnalyzer.PerformanceInsight insight : insights) {
                System.out.println("• " + insight.toString());
            }
            System.out.println();
        }

        // Generate recommendations
        List<String> recommendations = PerformanceAnalyzer.generateRecommendations(insights);
        if (!recommendations.isEmpty()) {
            System.out.println("Performance Recommendations:");
            System.out.println("----------------------------");
            for (String recommendation : recommendations) {
                System.out.println("• " + recommendation);
            }
            System.out.println();
        }

        // Generate full performance report
        System.out.println("Full Performance Report:");
        System.out.println("========================");
        String report = PerformanceAnalyzer.generatePerformanceReport(snapshots);
        System.out.println(report);
    }

    private void demonstratePerformanceConfiguration(RulePerformanceMonitor monitor) {
        System.out.println("5. Performance Monitor Configuration");
        System.out.println("===================================");

        System.out.println("Current Configuration:");
        System.out.printf("  Monitoring Enabled: %s%n", monitor.isEnabled());
        System.out.printf("  Total Evaluations Tracked: %d%n", monitor.getTotalEvaluations());
        System.out.println();

        // Demonstrate configuration options
        System.out.println("Configuration Options:");
        System.out.println("  - Enable/Disable monitoring: monitor.setEnabled(boolean)");
        System.out.println("  - Set history size: monitor.setMaxHistorySize(int)");
        System.out.println("  - Track memory usage: monitor.setTrackMemory(boolean)");
        System.out.println("  - Track complexity: monitor.setTrackComplexity(boolean)");
        System.out.println("  - Clear metrics: monitor.clearMetrics()");
        System.out.println();

        // Show how to access specific rule history
        System.out.println("Accessing Rule History:");
        List<RulePerformanceMetrics> simpleRuleHistory = monitor.getRuleHistory("simple-rule");
        if (!simpleRuleHistory.isEmpty()) {
            System.out.printf("  Simple rule has %d historical evaluations%n", simpleRuleHistory.size());
            RulePerformanceMetrics lastEvaluation = simpleRuleHistory.get(simpleRuleHistory.size() - 1);
            System.out.printf("  Last evaluation took: %.2f ms%n", lastEvaluation.getEvaluationTimeMillis());
            System.out.printf("  Performance summary: %s%n", lastEvaluation.getPerformanceSummary());
        }
        System.out.println();

        // Demonstrate detailed snapshot report
        PerformanceSnapshot snapshot = monitor.getRuleSnapshot("complex-rule");
        if (snapshot != null) {
            System.out.println("Detailed Report for Complex Rule:");
            System.out.println("---------------------------------");
            System.out.println(snapshot.getDetailedReport());
        }
    }

    /**
     * Utility method to demonstrate performance monitoring in a specific scenario.
     */
    public static void demonstrateSpecificScenario() {
        System.out.println("\n=== SPECIFIC SCENARIO DEMONSTRATION ===");
        
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        RulesEngine engine = new RulesEngine(config);
        
        // Create a rule for demonstration
        Rule demoRule = config.registerRule(config.rule("demo-scenario")
                .withCategory("demo")
                .withName("Demo Scenario Rule")
                .withCondition("#amount > 1000 && #currency == 'USD' && #region != 'RESTRICTED'")
                .withMessage("High value USD transaction in allowed region")
                .build());

        // Execute with performance monitoring
        Map<String, Object> facts = new HashMap<>();
        facts.put("amount", 1500.0);
        facts.put("currency", "USD");
        facts.put("region", "US");

        RuleResult result = engine.executeRule(demoRule, facts);
        
        System.out.println("Rule Execution Result:");
        System.out.printf("  Rule Triggered: %s%n", result.isTriggered());
        System.out.printf("  Message: %s%n", result.getMessage());
        
        if (result.hasPerformanceMetrics()) {
            RulePerformanceMetrics metrics = result.getPerformanceMetrics();
            System.out.println("Performance Metrics:");
            System.out.printf("  Evaluation Time: %.2f ms%n", metrics.getEvaluationTimeMillis());
            System.out.printf("  Expression Complexity: %d%n", metrics.getExpressionComplexity());
            System.out.printf("  Memory Used: %d bytes%n", metrics.getMemoryUsedBytes());
            System.out.printf("  Cache Hit: %s%n", metrics.isCacheHit());
            System.out.printf("  Performance Summary: %s%n", metrics.getPerformanceSummary());
        }
    }
}
