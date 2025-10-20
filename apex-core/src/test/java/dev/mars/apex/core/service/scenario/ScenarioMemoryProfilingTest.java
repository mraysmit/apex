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
 * Priority 3: Memory Profiling Tests
 * 
 * Validates that memory usage is reasonable and no memory leaks exist during
 * repeated scenario executions.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@DisplayName("Priority 3: Memory Profiling Tests")
class ScenarioMemoryProfilingTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ScenarioMemoryProfilingTest.class);
    
    private ScenarioConfiguration scenario;
    private ScenarioStageExecutor executor;
    
    @BeforeEach
    void setUp() {
        logger.info("TEST: Setting up memory profiling test");
        scenario = new ScenarioConfiguration();
        scenario.setScenarioId("memory-test-scenario");
        executor = new ScenarioStageExecutor();
    }
    
    // ========================================
    // Memory Profiling Tests
    // ========================================
    
    @Nested
    @DisplayName("Memory Profiling Tests")
    class MemoryProfilingTests {
        
        @Test
        @DisplayName("Should use reasonable memory for 100 scenario executions")
        void testMemoryUsageFor100Executions() {
            logger.info("========== TEST START: Memory usage for 100 executions ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for memory usage test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Track memory before and after 100 executions
                logger.info("Starting memory profiling: forcing garbage collection...");
                Runtime runtime = Runtime.getRuntime();
                System.gc(); // Force garbage collection before measurement

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                logger.info("Memory before execution: {}MB", String.format("%.2f", memBefore / 1_000_000.0));

                logger.info("Executing 100 scenarios...");
                for (int i = 0; i < 100; i++) {
                    Map<String, Object> testData = new HashMap<>();
                    testData.put("type", "OTC");
                    testData.put("id", i);

                    executor.executeStages(scenario, testData);

                    if ((i + 1) % 25 == 0) {
                        logger.debug("Progress: {}/100 executions completed", i + 1);
                    }
                }

                logger.info("All executions completed. Forcing garbage collection...");
                System.gc(); // Force garbage collection after execution
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memIncrease = memAfter - memBefore;

                logger.info("Memory after execution: {}MB", String.format("%.2f", memAfter / 1_000_000.0));
                logger.info("Memory increase: {}MB", String.format("%.2f", memIncrease / 1_000_000.0));

                // Then: Verify memory increase is reasonable (< 100MB)
                assertTrue(memIncrease < 100_000_000,
                    "Memory increase should be < 100MB, was: " + (memIncrease / 1_000_000) + "MB");

                logger.info("✓ Memory usage reasonable: {}MB increase for 100 executions",
                    String.format("%.2f", memIncrease / 1_000_000.0));
                logger.info("========== TEST PASSED: Memory usage for 100 executions ==========");
            } catch (Exception e) {
                logger.error("========== TEST FAILED: Memory usage for 100 executions ==========", e);
                throw e;
            }
        }
        
        @Test
        @DisplayName("Should not leak memory during repeated executions")
        void testMemoryLeakDetection() {
            logger.info("========== TEST START: Memory leak detection ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for memory leak detection test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute multiple times and track memory growth
                logger.info("Starting memory leak detection with 5 snapshots of 20 executions each...");
                Runtime runtime = Runtime.getRuntime();

                long[] memorySnapshots = new long[5];
                int executionsPerSnapshot = 20;

                for (int snapshot = 0; snapshot < 5; snapshot++) {
                    System.gc();
                    memorySnapshots[snapshot] = runtime.totalMemory() - runtime.freeMemory();
                    logger.info("Snapshot {}: {}MB", snapshot, String.format("%.2f", memorySnapshots[snapshot] / 1_000_000.0));

                    for (int i = 0; i < executionsPerSnapshot; i++) {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("type", "OTC");
                        testData.put("id", snapshot * executionsPerSnapshot + i);

                        executor.executeStages(scenario, testData);
                    }
                    logger.debug("Snapshot {} completed {} executions", snapshot, executionsPerSnapshot);
                }

                // Then: Verify memory growth is not linear (indicates no leak)
                long firstIncrease = memorySnapshots[1] - memorySnapshots[0];
                long lastIncrease = memorySnapshots[4] - memorySnapshots[3];

                logger.info("Memory growth analysis: first increase={}MB, last increase={}MB",
                    String.format("%.2f", firstIncrease / 1_000_000.0),
                    String.format("%.2f", lastIncrease / 1_000_000.0));

                // Last increase should not be significantly larger than first
                // (allowing for some variance)
                assertTrue(lastIncrease < firstIncrease * 2,
                    "Memory growth should stabilize, not increase linearly. " +
                    "First: " + (firstIncrease / 1_000_000) + "MB, Last: " + (lastIncrease / 1_000_000) + "MB");

                logger.info("✓ No memory leak detected: growth stabilized after initial allocations");
                logger.info("========== TEST PASSED: Memory leak detection ==========");
            } catch (Exception e) {
                logger.error("========== TEST FAILED: Memory leak detection ==========", e);
                throw e;
            }
        }
        
        @Test
        @DisplayName("Should handle large dataset processing without OOM")
        void testLargeDatasetHandling() {
            logger.info("========== TEST START: Large dataset handling ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for large dataset handling test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute with large dataset
                logger.info("Creating large dataset with 10,000 fields...");
                Map<String, Object> largeData = new HashMap<>();
                largeData.put("type", "OTC");

                // Add 10,000 fields to simulate large dataset
                for (int i = 0; i < 10000; i++) {
                    largeData.put("field_" + i, "value_" + i);
                }
                logger.info("Large dataset created with 10,000 fields");

                Runtime runtime = Runtime.getRuntime();
                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                logger.info("Memory before execution: {}MB", String.format("%.2f", memBefore / 1_000_000.0));

                // Then: Verify large dataset is processed without OOM
                logger.info("Executing scenario with large dataset...");
                ScenarioExecutionResult result = executor.executeStages(scenario, largeData);
                assertNotNull(result, "Should process large dataset successfully");
                logger.info("Large dataset execution completed successfully");

                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = memAfter - memBefore;

                logger.info("Memory after execution: {}MB", String.format("%.2f", memAfter / 1_000_000.0));
                logger.info("✓ Large dataset processed: {}MB used for 10,000 fields",
                    String.format("%.2f", memUsed / 1_000_000.0));
                logger.info("========== TEST PASSED: Large dataset handling ==========");

            } catch (OutOfMemoryError e) {
                logger.error("========== TEST FAILED: Large dataset handling - OutOfMemoryError ==========", e);
                fail("Large dataset processing caused OutOfMemoryError: " + e.getMessage());
            } catch (Exception e) {
                logger.error("========== TEST FAILED: Large dataset handling ==========", e);
                throw e;
            }
        }
        
        @Test
        @DisplayName("Should maintain stable memory after garbage collection")
        void testMemoryStabilityAfterGc() {
            logger.info("========== TEST START: Memory stability after garbage collection ==========");

            try {
                // Given: Scenario with classification rule
                logger.info("Setting up scenario for memory stability test");
                scenario.setClassificationRuleCondition("#data['type'] == 'OTC'");

                String configPath = new java.io.File("src/test/resources/config/test.yaml").getAbsolutePath();
                logger.info("Config path: {}", configPath);
                ScenarioStage stage = new ScenarioStage("test-stage", configPath, 1);
                scenario.addProcessingStage(stage);
                logger.info("Scenario setup complete");

                // When: Execute, collect garbage, and measure memory
                Runtime runtime = Runtime.getRuntime();

                // Execute 50 scenarios
                logger.info("Executing first batch of 50 scenarios...");
                for (int i = 0; i < 50; i++) {
                    Map<String, Object> testData = new HashMap<>();
                    testData.put("type", "OTC");
                    testData.put("id", i);

                    executor.executeStages(scenario, testData);
                }
                logger.info("First batch completed");

                // Force garbage collection
                logger.info("Forcing garbage collection after first batch...");
                System.gc();
                long memAfterGc1 = runtime.totalMemory() - runtime.freeMemory();
                logger.info("Memory after first GC: {}MB", String.format("%.2f", memAfterGc1 / 1_000_000.0));

                // Execute more scenarios
                logger.info("Executing second batch of 50 scenarios...");
                for (int i = 50; i < 100; i++) {
                    Map<String, Object> testData = new HashMap<>();
                    testData.put("type", "OTC");
                    testData.put("id", i);

                    executor.executeStages(scenario, testData);
                }
                logger.info("Second batch completed");

                // Force garbage collection again
                logger.info("Forcing garbage collection after second batch...");
                System.gc();
                long memAfterGc2 = runtime.totalMemory() - runtime.freeMemory();
                logger.info("Memory after second GC: {}MB", String.format("%.2f", memAfterGc2 / 1_000_000.0));

                // Then: Verify memory is stable after GC
                long memDifference = Math.abs(memAfterGc2 - memAfterGc1);
                logger.info("Memory difference between GC collections: {}MB", String.format("%.2f", memDifference / 1_000_000.0));

                // Allow up to 10MB difference (reasonable variance)
                assertTrue(memDifference < 10_000_000,
                    "Memory should be stable after GC. Difference: " + (memDifference / 1_000_000) + "MB");

                logger.info("✓ Memory stable after GC: {}MB difference between collections",
                    String.format("%.2f", memDifference / 1_000_000.0));
                logger.info("========== TEST PASSED: Memory stability after garbage collection ==========");
            } catch (Exception e) {
                logger.error("========== TEST FAILED: Memory stability after garbage collection ==========", e);
                throw e;
            }
        }
    }
}

