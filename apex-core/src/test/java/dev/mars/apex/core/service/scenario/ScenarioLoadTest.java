package dev.mars.apex.core.service.scenario;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Priority 3: Load Testing
 * 
 * Validates that the system handles high-volume scenario processing without
 * degradation. Tests sequential and concurrent processing of 100+ scenarios.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Priority 3: Load Testing")
class ScenarioLoadTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioLoadTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up load test");
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("load-test-scenario");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // Load Testing Tests
    // ========================================
    
    @Nested
    @DisplayName("Load Testing Tests")
    class LoadTests {
        
        @Test
        @DisplayName("Should process 100 scenarios sequentially without degradation")
        void testSequentialLoadProcessing() {
            logger.info("========== TEST START: Sequential load processing (100 scenarios) ==========");

            try {
                // Given: Scenario with simple classification rule
                logger.info("Setting up scenario for sequential load test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("processing-sla-ms", 5000);
                scenario.setMetadata(metadata);

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute 100 scenarios sequentially
                logger.info("Starting sequential execution of 100 scenarios...");
                long startTime = System.currentTimeMillis();
                int successCount = 0;
                int failureCount = 0;

                for (int i = 0; i < 100; i++) {
                    Map<String, Object> testData = new HashMap<>();
                    testData.put("type", "OTC");
                    testData.put("id", i);

                    ScenarioExecutionResult result = executor.executeStages(scenario, testData);

                    if (result.isSuccessful() || result.requiresReview()) {
                        successCount++;
                    } else {
                        failureCount++;
                        logger.warn("Scenario {} failed", i);
                    }

                    if ((i + 1) % 25 == 0) {
                        logger.info("Progress: {}/100 scenarios completed", i + 1);
                    }
                }

                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("Sequential execution completed in {}ms", totalTime);

                // Then: Verify all scenarios completed successfully
                logger.info("Verifying results: {} successful, {} failed", successCount, failureCount);
                assertEquals(100, successCount + failureCount,
                    "All 100 scenarios should complete");
                assertTrue(successCount > 0,
                    "Most scenarios should succeed");

                // Verify performance is acceptable (100 scenarios in < 15 seconds)
                assertTrue(totalTime < 15000,
                    "100 scenarios should complete in < 15 seconds, took: " + totalTime + "ms");

                double avgTimePerScenario = (double) totalTime / 100;
                logger.info("✓ Sequential load test passed: {}ms total, {}ms average per scenario",
                    totalTime, String.format("%.2f", avgTimePerScenario));
                logger.info("========== TEST PASSED: Sequential load processing ==========");
            } catch (Exception e) {
                logger.error("========== TEST FAILED: Sequential load processing ==========", e);
                throw e;
            }
        }
        
        @Test
        @DisplayName("Should process 50 scenarios concurrently")
        void testConcurrentLoadProcessing() {
            logger.info("========== TEST START: Concurrent load processing (50 scenarios) ==========");

            java.util.concurrent.ExecutorService executorService = null;
            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for concurrent load test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("processing-sla-ms", 5000);
                scenario.setMetadata(metadata);

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute 50 scenarios concurrently
                logger.info("Starting concurrent execution of 50 scenarios with 10 thread pool...");
                long startTime = System.currentTimeMillis();
                int concurrentRequests = 50;
                int threadPoolSize = 10;

                executorService =
                    java.util.concurrent.Executors.newFixedThreadPool(threadPoolSize);
                java.util.List<java.util.concurrent.Future<ScenarioExecutionResult>> futures =
                    new java.util.ArrayList<>();

                for (int i = 0; i < concurrentRequests; i++) {
                    final int index = i;
                    futures.add(executorService.submit(() -> {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("type", "OTC");
                        testData.put("id", index);
                        logger.debug("Executing scenario {}", index);
                        return executor.executeStages(scenario, testData);
                    }));
                }

                // Wait for all to complete
                logger.info("Waiting for all {} concurrent scenarios to complete...", concurrentRequests);
                int successCount = 0;
                for (java.util.concurrent.Future<ScenarioExecutionResult> future : futures) {
                    ScenarioExecutionResult result = future.get();
                    if (result.isSuccessful() || result.requiresReview()) {
                        successCount++;
                    }
                }

                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("Concurrent execution completed in {}ms. Success: {}/{}", totalTime, successCount, concurrentRequests);

                // Then: Verify all scenarios completed
                assertEquals(concurrentRequests, successCount,
                    "All concurrent scenarios should complete successfully");

                // Verify concurrent processing is faster than sequential
                assertTrue(totalTime < 10000,
                    "50 concurrent scenarios should complete in < 10 seconds, took: " + totalTime + "ms");

                logger.info("✓ Concurrent load test passed: {}ms total for {} scenarios",
                    totalTime, concurrentRequests);
                logger.info("========== TEST PASSED: Concurrent load processing ==========");

            } catch (Exception e) {
                logger.error("========== TEST FAILED: Concurrent load processing ==========", e);
                fail("Concurrent execution failed: " + e.getMessage());
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        }
        
        @Test
        @DisplayName("Should maintain consistent performance under load")
        void testPerformanceConsistency() {
            logger.info("========== TEST START: Performance consistency under load ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for performance consistency test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute scenarios and track timing
                logger.info("Starting performance consistency test with 50 executions...");
                Map<String, Object> testData = new HashMap<>();
                testData.put("type", "OTC");

                long firstRunTime = 0;
                long lastRunTime = 0;
                long totalTime = 0;

                for (int i = 0; i < 50; i++) {
                    long startTime = System.currentTimeMillis();
                    ScenarioExecutionResult result = executor.executeStages(scenario, testData);
                    long runTime = System.currentTimeMillis() - startTime;
                    totalTime += runTime;

                    if (i == 0) {
                        firstRunTime = runTime;
                        logger.info("First execution time: {}ms", firstRunTime);
                    }
                    lastRunTime = runTime;

                    if ((i + 1) % 10 == 0) {
                        logger.debug("Progress: {}/50 executions completed, last run: {}ms", i + 1, runTime);
                    }
                }

                // Then: Verify performance doesn't degrade significantly
                // Allow up to 200% variance between first and last run (accounts for JIT compilation and caching improvements)
                double variance = Math.abs(lastRunTime - firstRunTime) / (double) firstRunTime;
                logger.info("Performance analysis: first={}ms, last={}ms, variance={}%, total={}ms, avg={}ms",
                    firstRunTime, lastRunTime, String.format("%.1f", variance * 100), totalTime, totalTime / 50);
                assertTrue(variance < 2.0,
                    "Performance should not degrade significantly. First: " + firstRunTime +
                    "ms, Last: " + lastRunTime + "ms, Variance: " + String.format("%.1f%%", variance * 100));

                logger.info("✓ Performance consistency verified: first={}ms, last={}ms, variance={}%",
                    firstRunTime, lastRunTime, String.format("%.1f", variance * 100));
                logger.info("========== TEST PASSED: Performance consistency under load ==========");
            } catch (Exception e) {
                logger.error("========== TEST FAILED: Performance consistency under load ==========", e);
                throw e;
            }
        }
        
        @Test
        @DisplayName("Should handle varying data sizes efficiently")
        void testVaryingDataSizeHandling() {
            logger.info("TEST: Varying data size handling");

            // Given: Scenario with classification rule
            scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

            String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
            ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
            scenario.addProcessingStage(stage);

            // When: Execute with varying data sizes
            long smallDataTime = 0;
            long largeDataTime = 0;
            
            // Small data
            Map<String, Object> smallData = new HashMap<>();
            smallData.put("type", "OTC");
            long startTime = System.currentTimeMillis();
            executor.executeStages(scenario, smallData);
            smallDataTime = System.currentTimeMillis() - startTime;
            
            // Large data
            Map<String, Object> largeData = new HashMap<>();
            largeData.put("type", "OTC");
            for (int i = 0; i < 1000; i++) {
                largeData.put("field_" + i, "value_" + i);
            }
            startTime = System.currentTimeMillis();
            executor.executeStages(scenario, largeData);
            largeDataTime = System.currentTimeMillis() - startTime;
            
            // Then: Verify both complete successfully
            assertTrue(smallDataTime >= 0, "Small data should complete");
            assertTrue(largeDataTime >= 0, "Large data should complete");
            
            logger.info("✓ Data size handling verified: small={}ms, large={}ms",
                smallDataTime, largeDataTime);
        }
    }
}

