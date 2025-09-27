package dev.mars.apex.demo.metrics;

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

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Metrics Collection Demo
 *
 * This demo shows how to collect and analyze performance metrics from APEX
 * rule execution. It demonstrates various metrics collection techniques and
 * how to use them for performance analysis and optimization.
 *
 * ============================================================================
 * LEARNING OBJECTIVES:
 * - Learn how to collect detailed performance metrics from rule execution
 * - Understand different types of metrics available in APEX
 * - See how to analyze metrics for performance optimization
 * - Learn best practices for metrics collection in production
 * ============================================================================
 *
 * CONCEPTS INTRODUCED:
 * - Execution timing metrics
 * - Rule success/failure metrics
 * - Data enrichment metrics
 * - Performance trend analysis
 * - Metrics aggregation and reporting
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class MetricsCollectionDemo extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionDemo.class);

    @Test
    @DisplayName("Demo 4: Comprehensive Metrics Collection")
    void demonstrateMetricsCollection() throws Exception {
        logger.info("================================================================================");
        logger.info("METRICS DEMO: Comprehensive APEX Metrics Collection and Analysis");
        logger.info("================================================================================");

        // Load configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/metrics/MetricsCollectionDemo.yaml");
        assertNotNull(config, "YAML configuration should load successfully");

        // Create rules engine
        YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
        RulesEngine rulesEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(rulesEngine, "Rules engine should be created successfully");

        logger.info("\n" + "=".repeat(70));
        logger.info("COMPREHENSIVE METRICS COLLECTION");
        logger.info("=".repeat(70));

        // Collect metrics from multiple test scenarios
        List<MetricsData> allMetrics = new ArrayList<>();
        
        // Scenario 1: High-quality data
        logger.info("\n--- Collecting Metrics: High-Quality Data ---");
        MetricsData highQualityMetrics = collectMetrics(rulesEngine, config, 
            createHighQualityData(), "High-Quality Data");
        allMetrics.add(highQualityMetrics);

        // Scenario 2: Medium-quality data
        logger.info("\n--- Collecting Metrics: Medium-Quality Data ---");
        MetricsData mediumQualityMetrics = collectMetrics(rulesEngine, config, 
            createMediumQualityData(), "Medium-Quality Data");
        allMetrics.add(mediumQualityMetrics);

        // Scenario 3: Low-quality data
        logger.info("\n--- Collecting Metrics: Low-Quality Data ---");
        MetricsData lowQualityMetrics = collectMetrics(rulesEngine, config, 
            createLowQualityData(), "Low-Quality Data");
        allMetrics.add(lowQualityMetrics);

        // Scenario 4: Multiple executions for trend analysis
        logger.info("\n--- Collecting Metrics: Performance Trend Analysis ---");
        List<MetricsData> trendMetrics = collectTrendMetrics(rulesEngine, config);
        
        // Analyze all collected metrics
        analyzeComprehensiveMetrics(allMetrics, trendMetrics);

        logger.info("\n================================================================================");
        logger.info("DEMO COMPLETED: Comprehensive Metrics Collection");
        logger.info("Key Takeaways:");
        logger.info("- APEX provides detailed execution metrics for performance analysis");
        logger.info("- Data quality significantly impacts rule execution performance");
        logger.info("- Metrics collection enables proactive performance optimization");
        logger.info("- Trend analysis helps identify performance degradation over time");
        logger.info("- Comprehensive metrics support production monitoring and alerting");
        logger.info("================================================================================");
    }

    /**
     * Collect detailed metrics from a single rule execution.
     */
    private MetricsData collectMetrics(RulesEngine rulesEngine, YamlRuleConfiguration config, 
                                     Map<String, Object> testData, String scenario) {
        long startTime = System.nanoTime();
        RuleResult result = rulesEngine.evaluate(config, testData);
        long endTime = System.nanoTime();
        
        double executionMs = (endTime - startTime) / 1_000_000.0;
        
        MetricsData metrics = new MetricsData();
        metrics.scenario = scenario;
        metrics.executionTimeMs = executionMs;
        metrics.success = result.isSuccess();
        metrics.hasFailures = result.hasFailures();
        metrics.enrichedFieldCount = result.getEnrichedData().size();
        metrics.failureCount = result.hasFailures() ? result.getFailureMessages().size() : 0;
        metrics.inputDataSize = testData.size();
        
        logger.info("Metrics for {}: {}ms, Success: {}, Enriched: {}, Failures: {}", 
            scenario, String.format("%.2f", executionMs), metrics.success, 
            metrics.enrichedFieldCount, metrics.failureCount);
            
        return metrics;
    }

    /**
     * Collect metrics for trend analysis by running multiple executions.
     */
    private List<MetricsData> collectTrendMetrics(RulesEngine rulesEngine, YamlRuleConfiguration config) {
        List<MetricsData> trendMetrics = new ArrayList<>();
        Map<String, Object> standardData = createHighQualityData();
        
        logger.info("Running 10 executions for trend analysis...");
        
        for (int i = 1; i <= 10; i++) {
            MetricsData metrics = collectMetrics(rulesEngine, config, standardData, 
                "Trend Analysis Run " + i);
            trendMetrics.add(metrics);
            
            // Small delay to simulate real-world timing variations
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return trendMetrics;
    }

    /**
     * Analyze comprehensive metrics from all scenarios.
     */
    private void analyzeComprehensiveMetrics(List<MetricsData> scenarioMetrics, 
                                           List<MetricsData> trendMetrics) {
        logger.info("\n--- Comprehensive Metrics Analysis ---");
        
        // Performance analysis by data quality
        logger.info("\nPerformance by Data Quality:");
        for (MetricsData metrics : scenarioMetrics) {
            logger.info("  {} - Execution: {}ms, Success Rate: {}, Enrichment Rate: {}", 
                metrics.scenario, 
                String.format("%.2f", metrics.executionTimeMs),
                metrics.success ? "100%" : "0%",
                String.format("%.1f%%", (metrics.enrichedFieldCount * 100.0) / metrics.inputDataSize));
        }
        
        // Trend analysis
        logger.info("\nPerformance Trend Analysis:");
        double avgTime = trendMetrics.stream().mapToDouble(m -> m.executionTimeMs).average().orElse(0.0);
        double minTime = trendMetrics.stream().mapToDouble(m -> m.executionTimeMs).min().orElse(0.0);
        double maxTime = trendMetrics.stream().mapToDouble(m -> m.executionTimeMs).max().orElse(0.0);
        
        logger.info("  Average execution time: {}ms", String.format("%.2f", avgTime));
        logger.info("  Minimum execution time: {}ms", String.format("%.2f", minTime));
        logger.info("  Maximum execution time: {}ms", String.format("%.2f", maxTime));
        logger.info("  Performance variance: {}ms", String.format("%.2f", maxTime - minTime));
        
        // Success rate analysis
        long successCount = scenarioMetrics.stream().mapToLong(m -> m.success ? 1 : 0).sum();
        double successRate = (successCount * 100.0) / scenarioMetrics.size();
        logger.info("  Overall success rate: {}%", String.format("%.1f", successRate));
        
        // Recommendations based on metrics
        provideOptimizationRecommendations(scenarioMetrics, avgTime, successRate);
    }

    /**
     * Provide optimization recommendations based on collected metrics.
     */
    private void provideOptimizationRecommendations(List<MetricsData> metrics, 
                                                   double avgTime, double successRate) {
        logger.info("\n--- Performance Optimization Recommendations ---");
        
        if (avgTime > 50.0) {
            logger.info("⚠ High average execution time detected ({}ms)", String.format("%.2f", avgTime));
            logger.info("  • Consider optimizing rule conditions for faster evaluation");
            logger.info("  • Review complex expressions that may be causing delays");
        } else {
            logger.info("✓ Execution time is within acceptable range ({}ms)", String.format("%.2f", avgTime));
        }
        
        if (successRate < 80.0) {
            logger.info("⚠ Low success rate detected ({}%)", String.format("%.1f", successRate));
            logger.info("  • Review error recovery strategies");
            logger.info("  • Consider improving input data validation");
        } else {
            logger.info("✓ Success rate is acceptable ({}%)", String.format("%.1f", successRate));
        }
        
        // Data quality impact analysis
        MetricsData highQuality = metrics.stream()
            .filter(m -> m.scenario.contains("High-Quality"))
            .findFirst().orElse(null);
        MetricsData lowQuality = metrics.stream()
            .filter(m -> m.scenario.contains("Low-Quality"))
            .findFirst().orElse(null);
            
        if (highQuality != null && lowQuality != null) {
            double qualityImpact = lowQuality.executionTimeMs - highQuality.executionTimeMs;
            if (qualityImpact > 10.0) {
                logger.info("⚠ Data quality significantly impacts performance ({}ms difference)", 
                    String.format("%.2f", qualityImpact));
                logger.info("  • Implement data quality checks upstream");
                logger.info("  • Consider caching validated data");
            }
        }
    }

    // Test data creation methods
    private Map<String, Object> createHighQualityData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", "CUST001");
        data.put("amount", 5000.0);
        data.put("riskScore", 75);
        data.put("creditRating", "A");
        data.put("region", "AMERICAS");
        data.put("transactionType", "PURCHASE");
        return data;
    }

    private Map<String, Object> createMediumQualityData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", "CUST002");
        data.put("amount", 3000.0);
        data.put("riskScore", 60);
        // Missing creditRating and region
        data.put("transactionType", "SALE");
        return data;
    }

    private Map<String, Object> createLowQualityData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", "");
        data.put("amount", -1000.0);
        // Missing most fields
        return data;
    }

    /**
     * Simple data class to hold metrics information.
     */
    private static class MetricsData {
        String scenario;
        double executionTimeMs;
        boolean success;
        boolean hasFailures;
        int enrichedFieldCount;
        int failureCount;
        int inputDataSize;
    }
}
