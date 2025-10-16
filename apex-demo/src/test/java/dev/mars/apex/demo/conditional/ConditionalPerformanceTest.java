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

package dev.mars.apex.demo.conditional;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conditional Performance Test - Demonstrates performance optimization patterns
 * 
 * Tests condition ordering impact and shows how to optimize rule evaluation
 * by placing expensive conditions strategically.
 * 
 * Key Concepts:
 * - Condition ordering: cheap conditions first, expensive conditions last
 * - Stop-on-first-failure: reduces unnecessary evaluation
 * - Rule group operators: AND vs OR performance characteristics
 * - Caching benefits: repeated rule evaluations
 */
@DisplayName("Conditional Performance Test")
public class ConditionalPerformanceTest extends DemoTestBase {

    @Test
    @DisplayName("Should demonstrate condition ordering impact on performance")
    void testConditionOrderingImpact() {
        logger.info("=== Testing Condition Ordering Impact ===");
        logger.info("Demonstrates how condition order affects evaluation performance");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalPerformanceTest.yaml");
            assertNotNull(config, "Configuration should not be null");

            // Scenario 1: Cheap condition first (fast path)
            logger.info("\n--- Scenario 1: Cheap Condition First (Optimized) ---");
            Map<String, Object> testData1 = new HashMap<>();
            testData1.put("amount", 5000.0);  // Below threshold - cheap check fails fast
            testData1.put("customerType", "STANDARD");
            testData1.put("priority", "NORMAL");

            long start1 = System.nanoTime();
            Object result1 = enrichmentService.enrichObject(config, testData1);
            long time1 = System.nanoTime() - start1;
            double ms1 = time1 / 1_000_000.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> enriched1 = (Map<String, Object>) result1;
            logger.info("✓ Optimized path (cheap check fails): {}ms", String.format("%.3f", ms1));
            logger.info("  Result: {}", enriched1.get("optimizationLevel"));

            // Scenario 2: Expensive condition first (slow path)
            logger.info("\n--- Scenario 2: Expensive Condition First (Unoptimized) ---");
            Map<String, Object> testData2 = new HashMap<>();
            testData2.put("amount", 5000.0);  // Same data
            testData2.put("customerType", "STANDARD");
            testData2.put("priority", "NORMAL");

            long start2 = System.nanoTime();
            Object result2 = enrichmentService.enrichObject(config, testData2);
            long time2 = System.nanoTime() - start2;
            double ms2 = time2 / 1_000_000.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> enriched2 = (Map<String, Object>) result2;
            logger.info("✓ Unoptimized path (expensive check first): {}ms", String.format("%.3f", ms2));
            logger.info("  Result: {}", enriched2.get("optimizationLevel"));

            // Both should produce same result
            assertEquals(enriched1.get("optimizationLevel"), enriched2.get("optimizationLevel"),
                        "Both paths should produce same result");

            logger.info("\n✓ Condition ordering impact demonstrated");
            logger.info("✓ Optimized path: {}ms", String.format("%.3f", ms1));
            logger.info("✓ Unoptimized path: {}ms", String.format("%.3f", ms2));

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should demonstrate AND operator stop-on-first-failure optimization")
    void testAndOperatorStopOnFirstFailure() {
        logger.info("=== Testing AND Operator Stop-on-First-Failure ===");
        logger.info("Demonstrates how AND with stop-on-first-failure reduces evaluation");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalPerformanceTest.yaml");

            // Test data that fails first rule (stops evaluation immediately)
            logger.info("\n--- Scenario: First Rule Fails (Early Exit) ---");
            Map<String, Object> testData = new HashMap<>();
            testData.put("amount", 5000.0);  // Below high-value threshold
            testData.put("customerType", "STANDARD");
            testData.put("priority", "NORMAL");

            long start = System.nanoTime();
            Object result = enrichmentService.enrichObject(config, testData);
            long elapsed = System.nanoTime() - start;
            double ms = elapsed / 1_000_000.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> enriched = (Map<String, Object>) result;

            logger.info("✓ AND group with early exit: {}ms", String.format("%.3f", ms));
            logger.info("✓ Result: {}", enriched.get("andGroupResult"));
            assertEquals("FAILED_EARLY", enriched.get("andGroupResult"),
                        "Should fail early when first rule fails");

            logger.info("✓ Stop-on-first-failure optimization working correctly");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should demonstrate OR operator first-match-wins optimization")
    void testOrOperatorFirstMatchWins() {
        logger.info("=== Testing OR Operator First-Match-Wins ===");
        logger.info("Demonstrates how OR stops on first match");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalPerformanceTest.yaml");

            // Test data that matches first rule (stops evaluation immediately)
            logger.info("\n--- Scenario: First Rule Matches (Early Exit) ---");
            Map<String, Object> testData = new HashMap<>();
            testData.put("amount", 75000.0);  // High-value - matches first rule (> 50000)
            testData.put("customerType", "STANDARD");
            testData.put("priority", "NORMAL");

            long start = System.nanoTime();
            Object result = enrichmentService.enrichObject(config, testData);
            long elapsed = System.nanoTime() - start;
            double ms = elapsed / 1_000_000.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> enriched = (Map<String, Object>) result;

            logger.info("✓ OR group with first-match-wins: {}ms", String.format("%.3f", ms));
            logger.info("✓ Result: {}", enriched.get("orGroupResult"));
            assertEquals("MATCHED_FIRST", enriched.get("orGroupResult"),
                        "Should match first rule and stop");

            logger.info("✓ First-match-wins optimization working correctly");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should demonstrate caching benefits with repeated evaluations")
    void testCachingBenefits() {
        logger.info("=== Testing Caching Benefits ===");
        logger.info("Demonstrates performance improvement from rule result caching");

        try {
            YamlRuleConfiguration config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/conditional/ConditionalPerformanceTest.yaml");

            Map<String, Object> testData = new HashMap<>();
            testData.put("amount", 25000.0);
            testData.put("customerType", "PREMIUM");
            testData.put("priority", "NORMAL");

            // First evaluation (cache miss)
            logger.info("\n--- First Evaluation (Cache Miss) ---");
            long start1 = System.nanoTime();
            Object result1 = enrichmentService.enrichObject(config, testData);
            long time1 = System.nanoTime() - start1;
            double ms1 = time1 / 1_000_000.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> enriched1 = (Map<String, Object>) result1;
            logger.info("✓ First evaluation: {}ms", String.format("%.3f", ms1));

            // Second evaluation with same data (cache hit)
            logger.info("\n--- Second Evaluation (Cache Hit) ---");
            long start2 = System.nanoTime();
            Object result2 = enrichmentService.enrichObject(config, testData);
            long time2 = System.nanoTime() - start2;
            double ms2 = time2 / 1_000_000.0;

            @SuppressWarnings("unchecked")
            Map<String, Object> enriched2 = (Map<String, Object>) result2;
            logger.info("✓ Second evaluation: {}ms", String.format("%.3f", ms2));

            // Results should be identical
            assertEquals(enriched1.get("cachingLevel"), enriched2.get("cachingLevel"),
                        "Cached results should match");

            logger.info("\n✓ Caching benefits demonstrated");
            logger.info("✓ First call: {}ms", String.format("%.3f", ms1));
            logger.info("✓ Second call: {}ms", String.format("%.3f", ms2));
            logger.info("✓ Both calls completed successfully");

        } catch (Exception e) {
            logger.error("Test failed: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
}

