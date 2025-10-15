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
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.monitoring.RulePerformanceMetrics;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic Performance Metrics Demo
 *
 * This is the simplest introduction to APEX performance monitoring.
 * It demonstrates how to measure rule execution timing and collect basic metrics
 * without the complexity of error recovery or advanced scenarios.
 *
 * ============================================================================
 * LEARNING OBJECTIVES:
 * - Understand how APEX measures rule execution time
 * - Learn to collect and analyze basic performance metrics
 * - See how different rule complexities affect performance
 * - Introduction to performance monitoring concepts
 * ============================================================================
 *
 * CONCEPTS INTRODUCED:
 * - Rule execution timing
 * - Performance metrics collection
 * - Simple vs complex rule performance comparison
 * - Basic performance analysis
 */
@ExtendWith(ColoredTestOutputExtension.class)
public class BasicPerformanceMetricsDemo extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BasicPerformanceMetricsDemo.class);

    @Test
    @DisplayName("Demo 1: Basic Performance Metrics Introduction")
    void demonstrateBasicPerformanceMetrics() throws Exception {
        logger.info("================================================================================");
        logger.info("BASIC DEMO: Introduction to APEX Performance Metrics");
        logger.info("================================================================================");

        // Step 1: Load a simple YAML configuration with different rule complexities
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/metrics/BasicPerformanceMetricsDemo.yaml");
        assertNotNull(config, "YAML configuration should load successfully");

        // Step 2: Create rules engine using YamlRulesEngineService
        YamlRulesEngineService rulesEngineService = new YamlRulesEngineService();
        RulesEngine rulesEngine = rulesEngineService.createRulesEngineFromYamlConfig(config);
        assertNotNull(rulesEngine, "Rules engine should be created successfully");

        logger.info("\n" + "=".repeat(60));
        logger.info("PERFORMANCE COMPARISON: Simple vs Complex Rules");
        logger.info("=".repeat(60));

        // Step 3: Test with simple data - measure performance
        Map<String, Object> testData = createSimpleTestData();
        
        logger.info("\n--- Testing Simple Rule Performance ---");
        long startTime = System.nanoTime();
        RuleResult simpleResult = rulesEngine.evaluate(config, testData);
        long simpleExecutionTime = System.nanoTime() - startTime;

        // Step 4: Test with complex data - measure performance
        Map<String, Object> complexData = createComplexTestData();

        logger.info("\n--- Testing Complex Rule Performance ---");
        startTime = System.nanoTime();
        RuleResult complexResult = rulesEngine.evaluate(config, complexData);
        long complexExecutionTime = System.nanoTime() - startTime;

        // Step 5: Analyze and display performance results
        analyzePerformanceResults(simpleResult, complexResult, simpleExecutionTime, complexExecutionTime);

        // Step 6: Demonstrate metrics collection
        demonstrateMetricsCollection(rulesEngine, config, testData);

        logger.info("\n================================================================================");
        logger.info("DEMO COMPLETED: Basic Performance Metrics");
        logger.info("Key Takeaways:");
        logger.info("- APEX automatically measures rule execution time");
        logger.info("- Complex rules take longer to execute than simple rules");
        logger.info("- Performance metrics help identify optimization opportunities");
        logger.info("- Timing measurements are available in nanosecond precision");
        logger.info("================================================================================");
    }

    /**
     * Create simple test data for basic performance testing.
     */
    private Map<String, Object> createSimpleTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 1000.0);
        data.put("customerId", "CUST001");
        data.put("status", "ACTIVE");
        return data;
    }

    /**
     * Create complex test data that will trigger more complex rule evaluations.
     */
    private Map<String, Object> createComplexTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 50000.0);
        data.put("customerId", "CUST002");
        data.put("status", "ACTIVE");
        data.put("riskScore", 85);
        data.put("region", "AMERICAS");
        data.put("productType", "DERIVATIVES");
        return data;
    }

    /**
     * Analyze and display performance comparison results.
     */
    private void analyzePerformanceResults(RuleResult simpleResult, RuleResult complexResult,
                                         long simpleTime, long complexTime) {

        double simpleMs = simpleTime / 1_000_000.0;
        double complexMs = complexTime / 1_000_000.0;

        logger.info("\n--- Performance Analysis Results ---");
        logger.info("Simple rules execution:");
        logger.info("  - Execution successful: {}", simpleResult.isSuccess());
        logger.info("  - Execution time: {}ms", String.format("%.2f%%", simpleMs));
        logger.info("  - Has failures: {}", simpleResult.hasFailures());

        logger.info("\nComplex rules execution:");
        logger.info("  - Execution successful: {}", complexResult.isSuccess());
        logger.info("  - Execution time: {}ms", String.format("%.2f%%", complexMs));
        logger.info("  - Has failures: {}", complexResult.hasFailures());
        
        if (complexMs > simpleMs) {
            double overhead = complexMs - simpleMs;
            double overheadPercent = (overhead / simpleMs) * 100;
            logger.info("\nComplexity Impact:");
            logger.info("  - Additional time: +{:.2f}ms", overhead);
            logger.info("  - Performance impact: +{:.1f}%", overheadPercent);
        } else {
            logger.info("\nComplexity Impact: Negligible difference");
        }
    }

    /**
     * Demonstrate how to collect detailed metrics from rule execution.
     */
    private void demonstrateMetricsCollection(RulesEngine rulesEngine, YamlRuleConfiguration config, Map<String, Object> testData) {
        logger.info("\n--- Detailed Metrics Collection ---");

        // Execute rules and collect metrics
        RuleResult result = rulesEngine.evaluate(config, testData);

        logger.info("Rule evaluation completed:");
        logger.info("  - Success: {}", result.isSuccess());
        logger.info("  - Has failures: {}", result.hasFailures());
        logger.info("  - Enriched data fields: {}", result.getEnrichedData().size());

        if (result.hasFailures()) {
            logger.info("  - Failure messages:");
            for (String message : result.getFailureMessages()) {
                logger.info("    * {}", message);
            }
        }

        logger.info("\nMetrics demonstrate:");
        logger.info("- Complete rule evaluation timing");
        logger.info("- Success/failure status tracking");
        logger.info("- Enriched data collection");
        logger.info("- Detailed failure message reporting");
    }
}
